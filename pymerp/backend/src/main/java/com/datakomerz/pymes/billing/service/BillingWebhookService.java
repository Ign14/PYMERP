package com.datakomerz.pymes.billing.service;

import com.company.billing.persistence.DocumentFile;
import com.company.billing.persistence.DocumentFileKind;
import com.company.billing.persistence.DocumentFileRepository;
import com.company.billing.persistence.DocumentFileVersion;
import com.company.billing.persistence.FiscalDocument;
import com.company.billing.persistence.FiscalDocumentRepository;
import com.company.billing.persistence.FiscalDocumentStatus;
import com.datakomerz.pymes.billing.dto.BillingWebhookRequest;
import com.datakomerz.pymes.billing.dto.BillingWebhookRequest.Links;
import com.datakomerz.pymes.billing.event.DocumentAccepted;
import com.datakomerz.pymes.billing.event.DocumentRejected;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BillingWebhookService {

  private static final Comparator<DocumentFile> CREATED_AT_COMPARATOR = Comparator
      .comparing(DocumentFile::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));

  private final FiscalDocumentRepository fiscalDocumentRepository;
  private final DocumentFileRepository documentFileRepository;
  private final BillingStorageService storageService;
  private final BillingWebhookFileClient fileClient;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  public BillingWebhookService(FiscalDocumentRepository fiscalDocumentRepository,
                               DocumentFileRepository documentFileRepository,
                               BillingStorageService storageService,
                               BillingWebhookFileClient fileClient,
                               ApplicationEventPublisher eventPublisher,
                               Clock clock) {
    this.fiscalDocumentRepository = fiscalDocumentRepository;
    this.documentFileRepository = documentFileRepository;
    this.storageService = storageService;
    this.fileClient = fileClient;
    this.eventPublisher = eventPublisher;
    this.clock = clock;
  }

  @Transactional
  public void handleWebhook(BillingWebhookRequest request) {
    if (request.documentId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "documentId is required in webhook payload");
    }
    if (request.status() == null || request.status().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required in webhook payload");
    }

    FiscalDocument document = fiscalDocumentRepository.findById(request.documentId())
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Fiscal document not found for provided documentId"));

    if (request.provider() != null && !request.provider().isBlank()) {
      document.setProvider(request.provider());
    }
    if (request.trackId() != null && !request.trackId().isBlank()) {
      document.setTrackId(request.trackId());
    }
    document.setLastSyncAt(OffsetDateTime.now(clock));
    document.setSyncAttempts(0);

    String normalizedStatus = request.status().trim().toUpperCase(Locale.ROOT);
    switch (normalizedStatus) {
      case "ACCEPTED" -> handleAccepted(document, request);
      case "REJECTED" -> handleRejected(document, request);
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Unsupported billing webhook status: " + request.status());
    }
  }

  private void handleAccepted(FiscalDocument document, BillingWebhookRequest request) {
    document.setStatus(FiscalDocumentStatus.ACCEPTED);
    document.setErrorDetail(null);
    String definitiveNumber = firstNonBlank(request.number(), request.externalId());
    if (definitiveNumber != null) {
      document.setNumber(definitiveNumber);
      document.setFinalFolio(definitiveNumber);
    }

    if (document.getProvisionalNumber() != null) {
      DocumentFile localFile = findLatestLocalFile(document.getId());
      if (localFile != null) {
        attachOfficialFiles(document, localFile, request.links());
      }
    }

    fiscalDocumentRepository.save(document);

    eventPublisher.publishEvent(new DocumentAccepted(
        document.getId(),
        document.getProvider(),
        request.externalId(),
        document.getNumber(),
        document.getTrackId()));
  }

  private void handleRejected(FiscalDocument document, BillingWebhookRequest request) {
    document.setStatus(FiscalDocumentStatus.REJECTED);
    List<String> errors = request.errors() != null ? request.errors() : Collections.emptyList();
    if (errors.isEmpty()) {
      document.setErrorDetail(null);
    } else {
      document.setErrorDetail(String.join("; ", errors));
    }

    fiscalDocumentRepository.save(document);

    eventPublisher.publishEvent(new DocumentRejected(
        document.getId(),
        document.getProvider(),
        request.externalId(),
        document.getTrackId(),
        List.copyOf(errors)));
  }

  private DocumentFile findLatestLocalFile(UUID documentId) {
    List<DocumentFile> files = documentFileRepository
        .findByDocumentIdAndVersion(documentId, DocumentFileVersion.LOCAL);
    return files.stream()
        .max(CREATED_AT_COMPARATOR)
        .orElse(null);
  }

  private void attachOfficialFiles(FiscalDocument document,
                                   DocumentFile localFile,
                                   Links links) {
    if (links == null) {
      return;
    }
    if (links.pdf() != null && !links.pdf().isBlank()) {
      storeOfficialDocument(document, localFile, links.pdf(), "application/pdf", "invoice-official.pdf");
    }
    if (links.xml() != null && !links.xml().isBlank()) {
      storeOfficialDocument(document, localFile, links.xml(), "application/xml", "invoice-official.xml");
    }
  }

  private void storeOfficialDocument(FiscalDocument document,
                                     DocumentFile localFile,
                                     String url,
                                     String contentType,
                                     String fallbackFilename) {
    byte[] content = download(url);
    if (content.length == 0) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
          "Downloaded empty content from billing provider");
    }
    String filename = deriveFilename(url, fallbackFilename);
    BillingStorageService.StoredFile stored = store(document.getId(), content, filename, contentType);

    DocumentFile official = new DocumentFile();
    official.setDocumentId(document.getId());
    official.setKind(DocumentFileKind.FISCAL);
    official.setVersion(DocumentFileVersion.OFFICIAL);
    official.setPreviousFile(localFile);
    official.setContentType(contentType);
    official.setStorageKey(stored.storageKey());
    official.setChecksum(stored.checksum());
    documentFileRepository.save(official);
  }

  private byte[] download(String url) {
    try {
      return fileClient.download(url);
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
          "Unable to download billing document from provider", ex);
    }
  }

  private BillingStorageService.StoredFile store(UUID documentId,
                                                 byte[] content,
                                                 String filename,
                                                 String contentType) {
    try {
      return storageService.store(
          documentId,
          DocumentFileKind.FISCAL,
          DocumentFileVersion.OFFICIAL,
          content,
          filename,
          contentType);
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
          "Unable to persist billing document file", ex);
    }
  }

  private String deriveFilename(String url, String fallbackFilename) {
    if (url == null || url.isBlank()) {
      return fallbackFilename;
    }
    try {
      URI uri = new URI(url);
      String path = uri.getPath();
      if (path == null || path.isBlank()) {
        return fallbackFilename;
      }
      String candidate = path.substring(path.lastIndexOf('/') + 1);
      return candidate.isBlank() ? fallbackFilename : candidate;
    } catch (URISyntaxException ex) {
      return fallbackFilename;
    }
  }

  private String firstNonBlank(String first, String second) {
    if (first != null && !first.isBlank()) {
      return first.trim();
    }
    if (second != null && !second.isBlank()) {
      return second.trim();
    }
    return null;
  }
}

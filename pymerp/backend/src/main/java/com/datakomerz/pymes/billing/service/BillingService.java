package com.datakomerz.pymes.billing.service;

import com.company.billing.persistence.ContingencyQueueItem;
import com.company.billing.persistence.ContingencyQueueItemRepository;
import com.company.billing.persistence.ContingencyQueueStatus;
import com.company.billing.persistence.DocumentFile;
import com.company.billing.persistence.DocumentFileKind;
import com.company.billing.persistence.DocumentFileRepository;
import com.company.billing.persistence.DocumentFileVersion;
import com.company.billing.persistence.FiscalDocument;
import com.company.billing.persistence.FiscalDocumentRepository;
import com.company.billing.persistence.FiscalDocumentStatus;
import com.company.billing.persistence.NonFiscalDocument;
import com.company.billing.persistence.NonFiscalDocumentRepository;
import com.company.billing.persistence.NonFiscalDocumentStatus;
import com.company.billing.persistence.SiiDocumentType;
import com.company.billing.persistence.TaxMode;
import com.datakomerz.pymes.billing.model.DocumentCategory;
import com.datakomerz.pymes.billing.model.InvoicePayload;
import com.datakomerz.pymes.billing.provider.BillingProvider;
import com.datakomerz.pymes.billing.provider.BillingProviderException;
import com.datakomerz.pymes.billing.render.LocalInvoiceRenderer;
import com.datakomerz.pymes.billing.service.BillingDocumentView.DocumentFileView;
import com.datakomerz.pymes.billing.service.BillingDocumentView.DocumentLinks;
import com.datakomerz.pymes.sales.Sale;
import com.datakomerz.pymes.sales.SaleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BillingService {

  private final ObjectMapper objectMapper;
  private final FiscalDocumentRepository fiscalDocumentRepository;
  private final DocumentFileRepository documentFileRepository;
  private final ContingencyQueueItemRepository contingencyQueueItemRepository;
  private final NonFiscalDocumentRepository nonFiscalDocumentRepository;
  private final SaleRepository saleRepository;
  private final BillingStorageService storageService;
  private final LocalInvoiceRenderer localInvoiceRenderer;
  private final Optional<BillingProvider> billingProvider;
  private final Clock clock;

  public BillingService(ObjectMapper objectMapper,
                        FiscalDocumentRepository fiscalDocumentRepository,
                        DocumentFileRepository documentFileRepository,
                        ContingencyQueueItemRepository contingencyQueueItemRepository,
                        NonFiscalDocumentRepository nonFiscalDocumentRepository,
                        SaleRepository saleRepository,
                        BillingStorageService storageService,
                        LocalInvoiceRenderer localInvoiceRenderer,
                        ObjectProvider<BillingProvider> billingProviderProvider,
                        ObjectProvider<Clock> clockProvider) {
    this.objectMapper = objectMapper;
    this.fiscalDocumentRepository = fiscalDocumentRepository;
    this.documentFileRepository = documentFileRepository;
    this.contingencyQueueItemRepository = contingencyQueueItemRepository;
    this.nonFiscalDocumentRepository = nonFiscalDocumentRepository;
    this.saleRepository = saleRepository;
    this.storageService = storageService;
    this.localInvoiceRenderer = localInvoiceRenderer;
    this.billingProvider = Optional.ofNullable(billingProviderProvider.getIfAvailable());
    Clock providedClock = clockProvider.getIfAvailable();
    this.clock = providedClock != null ? providedClock : Clock.systemUTC();
  }

  @Transactional
  public BillingDocumentView issueInvoice(boolean forceOffline,
                                          String connectivityHint,
                                          InvoicePayload payload,
                                          String idempotencyKey) {
    String key = requireIdempotencyKey(idempotencyKey);
    BillingDocumentView existing = findExistingDocumentByKey(key);
    if (existing != null) {
      return existing;
    }

    if (!payload.isFiscal()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fiscal document type is required");
    }

    Sale sale = saleRepository.findById(payload.saleId())
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Sale not found for invoice issuance"));

    FiscalDocument document = new FiscalDocument();
    document.setSale(sale);
    document.setDocumentType(payload.fiscalDocumentType());
    document.setTaxMode(Optional.ofNullable(payload.taxMode()).orElse(TaxMode.AFECTA));
    document.setSiiDocumentType(resolveSiiDocumentType(payload));
    if (document.getResolutionNumber() == null) {
      document.setResolutionNumber("0");
    }
    if (document.getResolutionDate() == null) {
      document.setResolutionDate(LocalDate.now(clock));
    }
    document.setStatus(FiscalDocumentStatus.PENDING);
    document.setOffline(false);
    document.setProvisionalNumber(generateProvisionalNumber(sale.getCompanyId()));
    document.setIdempotencyKey(key);
    fiscalDocumentRepository.save(document);

    LocalInvoiceRenderer.RenderedInvoice renderedLocal = localInvoiceRenderer
        .renderContingencyFiscalPdf(document, sale);
    DocumentFile localFile = storeLocal(document.getId(),
        DocumentFileKind.FISCAL, DocumentFileVersion.LOCAL, renderedLocal);

    boolean offline = shouldGoOffline(forceOffline, connectivityHint);
    if (offline) {
      document.setStatus(FiscalDocumentStatus.OFFLINE_PENDING);
      document.setOffline(true);
      fiscalDocumentRepository.save(document);

      ContingencyQueueItem queueItem = new ContingencyQueueItem();
      queueItem.setDocument(document);
      queueItem.setIdempotencyKey(key);
      queueItem.setStatus(ContingencyQueueStatus.OFFLINE_PENDING);
      queueItem.setProviderPayload(buildProviderPayload(payload, document));
      contingencyQueueItemRepository.save(queueItem);

      List<DocumentFile> files = List.of(localFile);
      return mapFiscalDocument(document, files);
    }

    BillingProvider provider = billingProvider.orElseThrow(() ->
        new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Billing provider not configured"));

    BillingProvider.IssueInvoiceResult providerResult;
    try {
      providerResult = provider.issueInvoice(payload, key);
    } catch (BillingProviderException ex) {
      document.setStatus(FiscalDocumentStatus.FAILED);
      fiscalDocumentRepository.save(document);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
          "Billing provider rejected invoice: " + ex.getMessage(), ex);
    }

    document.setStatus(FiscalDocumentStatus.SENT);
    document.setOffline(false);
    document.setProvider(providerResult.provider());
    document.setTrackId(providerResult.trackId());
    document.setNumber(providerResult.number());
    document.setFinalFolio(providerResult.number());
    fiscalDocumentRepository.save(document);

    List<DocumentFile> files = new ArrayList<>();
    files.add(localFile);
    DocumentFile officialFile = storeOfficialDocumentIfPresent(document, localFile, providerResult);
    if (officialFile != null) {
      files.add(officialFile);
    }
    return mapFiscalDocument(document, files);
  }

  @Transactional
  public BillingDocumentView createNonFiscal(InvoicePayload payload) {
    if (payload.fiscalDocumentType() != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Non fiscal request cannot include fiscal type");
    }
    if (payload.nonFiscalDocumentType() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Non fiscal document type is required");
    }
    Sale sale = saleRepository.findById(payload.saleId())
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Sale not found for non-fiscal document"));

    NonFiscalDocument document = new NonFiscalDocument();
    document.setSale(sale);
    document.setDocumentType(payload.nonFiscalDocumentType());
    document.setStatus(NonFiscalDocumentStatus.READY);
    document.setNumber(generateNonFiscalNumber(sale.getCompanyId()));
    nonFiscalDocumentRepository.save(document);

    LocalInvoiceRenderer.RenderedInvoice renderedLocal = localInvoiceRenderer
        .renderNonFiscalPdf(document, sale);
    DocumentFile localFile = storeLocal(document.getId(),
        DocumentFileKind.NON_FISCAL, DocumentFileVersion.LOCAL, renderedLocal);

    List<DocumentFile> files = List.of(localFile);
    return mapNonFiscalDocument(document, files);
  }

  @Transactional(readOnly = true)
  public BillingDocumentView getDocument(UUID id) {
    Optional<FiscalDocument> fiscal = fiscalDocumentRepository.findById(id);
    if (fiscal.isPresent()) {
      return mapFiscalDocument(fiscal.get(), null);
    }
    Optional<NonFiscalDocument> nonFiscal = nonFiscalDocumentRepository.findById(id);
    if (nonFiscal.isPresent()) {
      return mapNonFiscalDocument(nonFiscal.get(), null);
    }
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
  }

  private BillingDocumentView findExistingDocumentByKey(String idempotencyKey) {
    return fiscalDocumentRepository.findByIdempotencyKey(idempotencyKey)
        .map(document -> mapFiscalDocument(document, null))
        .or(() -> contingencyQueueItemRepository.findByIdempotencyKey(idempotencyKey)
            .map(ContingencyQueueItem::getDocument)
            .map(doc -> mapFiscalDocument(doc, null)))
        .orElse(null);
  }

  private DocumentFile storeLocal(UUID documentId,
                                  DocumentFileKind kind,
                                  DocumentFileVersion version,
                                  LocalInvoiceRenderer.RenderedInvoice rendered) {
    try {
      BillingStorageService.StoredFile stored = storageService.store(
          documentId,
          kind,
          version,
          rendered.content(),
          rendered.filename(),
          rendered.contentType());
      DocumentFile file = new DocumentFile();
      file.setDocumentId(documentId);
      file.setKind(kind);
      file.setVersion(version);
      file.setContentType(rendered.contentType());
      file.setStorageKey(stored.storageKey());
      file.setChecksum(stored.checksum());
      return documentFileRepository.save(file);
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
          "Unable to store local document version", ex);
    }
  }

  private DocumentFile storeOfficialDocumentIfPresent(FiscalDocument document,
                                                      DocumentFile localFile,
                                                      BillingProvider.IssueInvoiceResult providerResult) {
    if (providerResult == null || providerResult.officialDocument() == null) {
      return null;
    }
    BillingProvider.OfficialDocument official = providerResult.officialDocument();
    DocumentFile officialFile = new DocumentFile();
    officialFile.setDocumentId(document.getId());
    officialFile.setKind(DocumentFileKind.FISCAL);
    officialFile.setVersion(DocumentFileVersion.OFFICIAL);
    officialFile.setPreviousFile(localFile);
    officialFile.setContentType(
        official.contentType() != null ? official.contentType() : "application/pdf");
    if (official.content() != null && official.content().length > 0) {
      try {
        BillingStorageService.StoredFile stored = storageService.store(
            document.getId(),
            DocumentFileKind.FISCAL,
            DocumentFileVersion.OFFICIAL,
            official.content(),
            official.filename(),
            official.contentType());
        officialFile.setStorageKey(stored.storageKey());
        officialFile.setChecksum(stored.checksum());
      } catch (IOException ex) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
            "Unable to store official document version", ex);
      }
    } else if (official.downloadUrl() != null && !official.downloadUrl().isBlank()) {
      officialFile.setStorageKey(official.downloadUrl());
    } else {
      return null;
    }
    return documentFileRepository.save(officialFile);
  }

  private ObjectNode buildProviderPayload(InvoicePayload payload, FiscalDocument document) {
    ObjectNode node = payload.hasRawPayload()
        ? payload.rawPayload().deepCopy()
        : objectMapper.createObjectNode();
    node.put("documentId", document.getId().toString());
    node.put("provisionalNumber", document.getProvisionalNumber());
    node.put("generatedAt", document.getCreatedAt() != null
        ? document.getCreatedAt().toString()
        : null);
    return node;
  }

  private boolean shouldGoOffline(boolean forceOffline, String connectivityHint) {
    if (forceOffline) {
      return true;
    }
    if (connectivityHint == null || connectivityHint.isBlank()) {
      return false;
    }
    String normalized = connectivityHint.trim().toUpperCase(Locale.ROOT);
    return switch (normalized) {
      case "OFFLINE", "NO_CONNECTIVITY", "UNSTABLE", "LOW" -> true;
      default -> false;
    };
  }

  private String requireIdempotencyKey(String key) {
    if (key == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key header is required");
    }
    String trimmed = key.trim();
    if (trimmed.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key cannot be empty");
    }
    if (trimmed.length() > 100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key exceeds 100 characters");
    }
    return trimmed;
  }

  private SiiDocumentType resolveSiiDocumentType(InvoicePayload payload) {
    if (payload.siiDocumentType() != null) {
      return payload.siiDocumentType();
    }
    if (payload.fiscalDocumentType() == null) {
      return null;
    }
    return SiiDocumentType.from(payload.fiscalDocumentType(), payload.taxMode());
  }

  private String generateProvisionalNumber(UUID companyId) {
    int year = Year.now(clock).getValue();
    String prefix = "CTG-" + year + "-";
    Optional<FiscalDocument> last = fiscalDocumentRepository
        .findTopBySale_CompanyIdAndProvisionalNumberStartingWithOrderByProvisionalNumberDesc(
            companyId, prefix);
    int next = last.map(doc -> parseSequence(doc.getProvisionalNumber(), prefix))
        .orElse(0) + 1;
    return prefix + String.format("%05d", next);
  }

  private String generateNonFiscalNumber(UUID companyId) {
    int year = Year.now(clock).getValue();
    String prefix = "NF-" + year + "-";
    Optional<NonFiscalDocument> last = nonFiscalDocumentRepository
        .findTopBySale_CompanyIdOrderByCreatedAtDesc(companyId);
    int next = last.map(NonFiscalDocument::getNumber)
        .map(number -> parseSequence(number, prefix))
        .orElse(0) + 1;
    return prefix + String.format("%05d", next);
  }

  private int parseSequence(String number, String prefix) {
    if (number != null && number.startsWith(prefix)) {
      String suffix = number.substring(prefix.length());
      try {
        return Integer.parseInt(suffix);
      } catch (NumberFormatException ignored) {
        return 0;
      }
    }
    return 0;
  }

  private BillingDocumentView mapFiscalDocument(FiscalDocument document, List<DocumentFile> filesOrNull) {
    List<DocumentFile> files = filesOrNull != null
        ? filesOrNull
        : documentFileRepository.findByDocumentIdOrderByCreatedAtAsc(document.getId());
    List<DocumentFileView> fileViews = files.stream()
        .map(this::toDocumentFileView)
        .toList();
    DocumentLinks links = buildLinks(files);
    return new BillingDocumentView(
        DocumentCategory.FISCAL,
        document.getId(),
        document.getDocumentType(),
        null,
        document.getStatus(),
        null,
        document.getTaxMode(),
        document.getNumber(),
        document.getProvisionalNumber(),
        document.getProvider(),
        document.getTrackId(),
        document.isOffline(),
        document.getCreatedAt(),
        document.getUpdatedAt(),
        links,
        fileViews);
  }

  private BillingDocumentView mapNonFiscalDocument(NonFiscalDocument document,
                                                   List<DocumentFile> filesOrNull) {
    List<DocumentFile> files = filesOrNull != null
        ? filesOrNull
        : documentFileRepository.findByDocumentIdOrderByCreatedAtAsc(document.getId());
    List<DocumentFileView> fileViews = files.stream()
        .map(this::toDocumentFileView)
        .toList();
    DocumentLinks links = buildLinks(files);
    return new BillingDocumentView(
        DocumentCategory.NON_FISCAL,
        document.getId(),
        null,
        document.getDocumentType(),
        null,
        document.getStatus(),
        null,
        document.getNumber(),
        null,
        null,
        null,
        false,
        document.getCreatedAt(),
        document.getUpdatedAt(),
        links,
        fileViews);
  }

  private DocumentLinks buildLinks(List<DocumentFile> files) {
    String local = files.stream()
        .filter(file -> file.getVersion() == DocumentFileVersion.LOCAL)
        .findFirst()
        .map(DocumentFile::getStorageKey)
        .orElse(null);
    String official = files.stream()
        .filter(file -> file.getVersion() == DocumentFileVersion.OFFICIAL)
        .filter(file -> file.getContentType() != null && file.getContentType().toLowerCase().contains("pdf"))
        .findFirst()
        .map(DocumentFile::getStorageKey)
        .orElse(null);
    String xml = files.stream()
        .filter(file -> file.getVersion() == DocumentFileVersion.OFFICIAL)
        .filter(file -> "application/xml".equalsIgnoreCase(file.getContentType()))
        .findFirst()
        .map(DocumentFile::getStorageKey)
        .orElse(null);
    return new DocumentLinks(local, official, xml);
  }

  private DocumentFileView toDocumentFileView(DocumentFile file) {
    return new DocumentFileView(
        file.getId(),
        file.getKind(),
        file.getVersion(),
        file.getContentType(),
        file.getStorageKey(),
        file.getChecksum(),
        file.getCreatedAt());
  }
}

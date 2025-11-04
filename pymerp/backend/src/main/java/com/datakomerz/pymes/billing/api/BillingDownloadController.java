package com.datakomerz.pymes.billing.api;

import com.company.billing.persistence.DocumentFile;
import com.company.billing.persistence.DocumentFileRepository;
import com.company.billing.persistence.DocumentFileVersion;
import com.datakomerz.pymes.billing.dto.DocumentDetailResponse;
import com.datakomerz.pymes.billing.dto.DocumentLinksResponse;
import com.datakomerz.pymes.billing.service.BillingDocumentView;
import com.datakomerz.pymes.billing.service.BillingService;
import com.datakomerz.pymes.billing.service.BillingStorageService;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingDownloadController {

  private final BillingService billingService;
  private final DocumentFileRepository documentFileRepository;
  private final BillingStorageService storageService;
  private final BillingResponseMapper responseMapper;

  public BillingDownloadController(BillingService billingService,
                                   DocumentFileRepository documentFileRepository,
                                   BillingStorageService storageService,
                                   BillingResponseMapper responseMapper) {
    this.billingService = billingService;
    this.documentFileRepository = documentFileRepository;
    this.storageService = storageService;
    this.responseMapper = responseMapper;
  }

  @GetMapping("/documents/{id}")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public DocumentDetailResponse getDocument(@PathVariable UUID id) {
    BillingDocumentView view = billingService.getDocument(id);
    DocumentLinksResponse links = buildDownloadLinks(view, id);
    return responseMapper.toDetailResponse(view, links);
  }

  @GetMapping("/documents/{id}/files/{version}")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public ResponseEntity<Resource> download(@PathVariable UUID id,
                                           @PathVariable String version,
                                           @RequestParam(name = "contentType", required = false) String requestedContentType) {
    DocumentFileVersion targetVersion = parseVersion(version);
    List<DocumentFile> candidates = documentFileRepository.findByDocumentIdAndVersion(id, targetVersion);
    if (candidates.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No files stored for requested version");
    }
    DocumentFile selected = selectFile(candidates, requestedContentType);
    Resource resource;
    try {
      resource = storageService.loadAsResource(selected.getStorageKey());
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
          "Unable to load stored document", ex);
    }

    String filename = buildDownloadFilename(selected);
    MediaType mediaType = resolveMediaType(selected.getContentType());
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(mediaType);
    headers.setContentDispositionFormData("attachment", filename);
    return ResponseEntity.ok()
        .headers(headers)
        .body(resource);
  }

  private DocumentLinksResponse buildDownloadLinks(BillingDocumentView view, UUID id) {
    BillingDocumentView.DocumentLinks storageLinks = view.links();
    if (storageLinks == null) {
      return new DocumentLinksResponse(null, null, null);
    }
  String basePath = UriComponentsBuilder.fromPath("/api/v1/billing/documents/{id}/files")
    .buildAndExpand(id)
    .toUriString();

    String localLink = storageLinks.localPdf() != null
        ? basePath + "/LOCAL"
        : null;
    String officialPdfLink = storageLinks.officialPdf() != null
        ? basePath + "/OFFICIAL"
        : null;
    String xmlLink = storageLinks.officialXml() != null
        ? basePath + "/OFFICIAL?contentType=xml"
        : null;

    return new DocumentLinksResponse(localLink, officialPdfLink, xmlLink);
  }

  private DocumentFileVersion parseVersion(String version) {
    try {
      return DocumentFileVersion.valueOf(version.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid document version: " + version);
    }
  }

  private DocumentFile selectFile(List<DocumentFile> candidates, String requestedContentType) {
    List<DocumentFile> sorted = candidates.stream()
        .sorted(Comparator.comparing(DocumentFile::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
        .toList();
    if (StringUtils.hasText(requestedContentType)) {
      String normalized = requestedContentType.toLowerCase(Locale.ROOT);
      DocumentFile match = sorted.stream()
          .filter(file -> file.getContentType() != null
              && file.getContentType().toLowerCase(Locale.ROOT).contains(normalized))
          .findFirst()
          .orElse(null);
      if (match != null) {
        return match;
      }
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "No stored file matches requested content type: " + requestedContentType);
    }
    return sorted.stream()
        .filter(file -> file.getContentType() != null
            && file.getContentType().toLowerCase(Locale.ROOT).contains("pdf"))
        .findFirst()
        .orElse(sorted.get(0));
  }

  private String buildDownloadFilename(DocumentFile file) {
    String extension = ".bin";
    if (file.getContentType() != null) {
      if (file.getContentType().toLowerCase(Locale.ROOT).contains("pdf")) {
        extension = ".pdf";
      } else if (file.getContentType().toLowerCase(Locale.ROOT).contains("xml")) {
        extension = ".xml";
      }
    }
    return file.getId() + extension;
  }

  private MediaType resolveMediaType(String contentType) {
    if (!StringUtils.hasText(contentType)) {
      return MediaType.APPLICATION_OCTET_STREAM;
    }
    try {
      return MediaType.parseMediaType(contentType);
    } catch (IllegalArgumentException ex) {
      return MediaType.APPLICATION_OCTET_STREAM;
    }
  }
}

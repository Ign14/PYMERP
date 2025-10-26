package com.datakomerz.pymes.billing.api;

import com.datakomerz.pymes.billing.dto.DocumentDetailResponse;
import com.datakomerz.pymes.billing.dto.DocumentFileResponse;
import com.datakomerz.pymes.billing.dto.DocumentLinksResponse;
import com.datakomerz.pymes.billing.dto.IssueInvoiceRequest;
import com.datakomerz.pymes.billing.dto.InvoiceResponse;
import com.datakomerz.pymes.billing.dto.NonFiscalDocumentRequest;
import com.datakomerz.pymes.billing.dto.NonFiscalDocumentResponse;
import com.datakomerz.pymes.billing.model.DocumentCategory;
import com.datakomerz.pymes.billing.model.InvoicePayload;
import com.datakomerz.pymes.billing.service.BillingDocumentView;
import com.datakomerz.pymes.billing.service.BillingService;
import com.datakomerz.pymes.billing.service.InvoicePayloadFactory;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

  private final BillingService billingService;
  private final InvoicePayloadFactory payloadFactory;

  public BillingController(BillingService billingService,
                           InvoicePayloadFactory payloadFactory) {
    this.billingService = billingService;
    this.payloadFactory = payloadFactory;
  }

  @PostMapping("/invoices")
  @ResponseStatus(HttpStatus.CREATED)
  public InvoiceResponse issueInvoice(@RequestHeader(value = "Idempotency-Key", required = false)
                                      String idempotencyKeyHeader,
                                      @Valid @RequestBody IssueInvoiceRequest request) {
    String idempotencyKey = resolveIdempotencyKey(idempotencyKeyHeader, request.idempotencyKey());
    InvoicePayload payload = payloadFactory.fromInvoiceRequest(request);
    BillingDocumentView view = billingService.issueInvoice(
        request.resolvedForceOffline(),
        request.connectivityHint(),
        payload,
        idempotencyKey);
    if (view.category() != DocumentCategory.FISCAL) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
          "Expected fiscal document view");
    }
    return toInvoiceResponse(view);
  }

  @PostMapping("/non-fiscal")
  @ResponseStatus(HttpStatus.CREATED)
  public NonFiscalDocumentResponse createNonFiscal(
      @Valid @RequestBody NonFiscalDocumentRequest request) {
    InvoicePayload payload = payloadFactory.fromNonFiscalRequest(request);
    BillingDocumentView view = billingService.createNonFiscal(payload);
    if (view.category() != DocumentCategory.NON_FISCAL) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
          "Expected non fiscal document view");
    }
    return toNonFiscalResponse(view);
  }

  @GetMapping("/documents/{id}")
  public DocumentDetailResponse getDocument(@PathVariable UUID id) {
    BillingDocumentView view = billingService.getDocument(id);
    return toDetailResponse(view);
  }

  private InvoiceResponse toInvoiceResponse(BillingDocumentView view) {
    return new InvoiceResponse(
        view.id(),
        view.fiscalDocumentType(),
        view.fiscalStatus(),
        view.taxMode(),
        view.number(),
        view.provisionalNumber(),
        view.trackId(),
        view.provider(),
        view.offline(),
        view.createdAt(),
        view.updatedAt(),
        toLinks(view.links()),
        toFileResponses(view.files()));
  }

  private NonFiscalDocumentResponse toNonFiscalResponse(BillingDocumentView view) {
    return new NonFiscalDocumentResponse(
        view.id(),
        view.nonFiscalDocumentType(),
        view.nonFiscalStatus(),
        view.number(),
        view.createdAt(),
        view.updatedAt(),
        toLinks(view.links()),
        toFileResponses(view.files()));
  }

  private DocumentDetailResponse toDetailResponse(BillingDocumentView view) {
    String status = view.fiscalStatus() != null
        ? view.fiscalStatus().name()
        : view.nonFiscalStatus() != null ? view.nonFiscalStatus().name() : null;
    return new DocumentDetailResponse(
        view.id(),
        view.category(),
        view.fiscalDocumentType(),
        view.nonFiscalDocumentType(),
        status,
        view.taxMode(),
        view.number(),
        view.provisionalNumber(),
        view.provider(),
        view.trackId(),
        view.offline(),
        view.createdAt(),
        view.updatedAt(),
        toLinks(view.links()),
        toFileResponses(view.files()));
  }

  private DocumentLinksResponse toLinks(BillingDocumentView.DocumentLinks links) {
    if (links == null) {
      return new DocumentLinksResponse(null, null);
    }
    return new DocumentLinksResponse(links.localPdf(), links.officialPdf());
  }

  private List<DocumentFileResponse> toFileResponses(List<BillingDocumentView.DocumentFileView> files) {
    return files.stream()
        .map(file -> new DocumentFileResponse(
            file.id(),
            file.kind(),
            file.version(),
            file.contentType(),
            file.storageKey(),
            file.checksum(),
            file.createdAt()))
        .collect(Collectors.toList());
  }

  private String resolveIdempotencyKey(String headerValue, String bodyValue) {
    String header = headerValue != null && !headerValue.isBlank() ? headerValue.trim() : null;
    String body = bodyValue != null && !bodyValue.isBlank() ? bodyValue.trim() : null;
    if (header != null && body != null && !header.equals(body)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Idempotency-Key header and payload value do not match");
    }
    String resolved = header != null ? header : body;
    if (resolved == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Idempotency-Key header is required");
    }
    return resolved;
  }
}

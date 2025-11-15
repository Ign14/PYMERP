package com.datakomerz.pymes.billing.api;

import com.datakomerz.pymes.billing.dto.IssueInvoiceRequest;
import com.datakomerz.pymes.billing.dto.InvoiceResponse;
import com.datakomerz.pymes.billing.dto.NonFiscalDocumentRequest;
import com.datakomerz.pymes.billing.dto.NonFiscalDocumentResponse;
import com.datakomerz.pymes.billing.model.DocumentCategory;
import com.datakomerz.pymes.billing.model.InvoicePayload;
import com.datakomerz.pymes.billing.service.BillingDocumentView;
import com.datakomerz.pymes.billing.service.BillingService;
import com.datakomerz.pymes.billing.service.IdempotencyPayloadHasher;
import com.datakomerz.pymes.billing.service.InvoicePayloadFactory;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
  private final IdempotencyPayloadHasher payloadHasher;
  private final BillingResponseMapper responseMapper;

  public BillingController(BillingService billingService,
                           InvoicePayloadFactory payloadFactory,
                           IdempotencyPayloadHasher payloadHasher,
                           BillingResponseMapper responseMapper) {
    this.billingService = billingService;
    this.payloadFactory = payloadFactory;
    this.payloadHasher = payloadHasher;
    // FIX: Provide a safe fallback mapper when running tests or if injector didn't supply one.
    this.responseMapper = responseMapper != null ? responseMapper : new BillingResponseMapper();
  }

  @PostMapping("/invoices")
  @PreAuthorize("hasAnyRole('ERP_USER', 'ADMIN')")
  public ResponseEntity<InvoiceResponse> issueInvoice(@RequestHeader(value = "Idempotency-Key", required = false)
                                                      String idempotencyKeyHeader,
                                                      @Valid @RequestBody IssueInvoiceRequest request) {
    String idempotencyKey = resolveIdempotencyKey(idempotencyKeyHeader, request.idempotencyKey());
    String payloadHash = payloadHasher.hash(request);
    InvoicePayload payload = payloadFactory.fromInvoiceRequest(request);
    BillingService.InvoiceIssuanceResult result = billingService.issueInvoice(
        request.resolvedForceOffline(),
        request.connectivityHint(),
        payload,
        idempotencyKey,
        payloadHash);
    if (result.document().category() != DocumentCategory.FISCAL) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
          "Expected fiscal document view");
    }
    HttpStatus status = result.createdNew() ? HttpStatus.CREATED : HttpStatus.OK;
    return ResponseEntity.status(status).body(responseMapper.toInvoiceResponse(result.document()));
  }

  @PostMapping("/non-fiscal")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ERP_USER', 'ADMIN')")
  public NonFiscalDocumentResponse createNonFiscal(
      @Valid @RequestBody NonFiscalDocumentRequest request) {
    InvoicePayload payload = payloadFactory.fromNonFiscalRequest(request);
    BillingDocumentView view = billingService.createNonFiscal(payload);
    if (view.category() != DocumentCategory.NON_FISCAL) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
          "Expected non fiscal document view");
    }
    return responseMapper.toNonFiscalResponse(view);
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

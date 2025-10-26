package com.datakomerz.pymes.billing.api;

import com.datakomerz.pymes.billing.dto.BillingWebhookRequest;
import com.datakomerz.pymes.billing.security.BillingWebhookSignatureVerifier;
import com.datakomerz.pymes.billing.service.BillingWebhookService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class WebhookController {

  private final BillingWebhookSignatureVerifier signatureVerifier;
  private final BillingWebhookService webhookService;
  private final ObjectMapper objectMapper;

  public WebhookController(BillingWebhookSignatureVerifier signatureVerifier,
                           BillingWebhookService webhookService,
                           ObjectMapper objectMapper) {
    this.signatureVerifier = signatureVerifier;
    this.webhookService = webhookService;
    this.objectMapper = objectMapper;
  }

  @PostMapping("/webhooks/billing")
  public ResponseEntity<Void> receiveWebhook(
      @RequestHeader(name = "X-Signature", required = false) String signatureHeader,
      @RequestBody String payload) {
    signatureVerifier.verify(signatureHeader, payload);

    BillingWebhookRequest request = parsePayload(payload);
    webhookService.handleWebhook(request);
    return ResponseEntity.accepted().build();
  }

  private BillingWebhookRequest parsePayload(String payload) {
    try {
      return objectMapper.readValue(payload, BillingWebhookRequest.class);
    } catch (JsonProcessingException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Invalid webhook payload", ex);
    }
  }
}

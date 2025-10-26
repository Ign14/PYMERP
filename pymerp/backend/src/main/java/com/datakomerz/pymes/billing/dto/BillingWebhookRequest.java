package com.datakomerz.pymes.billing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BillingWebhookRequest(
    String provider,
    String externalId,
    UUID documentId,
    String status,
    String trackId,
    Links links,
    List<String> errors,
    String number) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Links(String pdf, String xml) {
  }
}

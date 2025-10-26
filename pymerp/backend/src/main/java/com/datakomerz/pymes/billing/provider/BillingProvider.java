package com.datakomerz.pymes.billing.provider;

import com.datakomerz.pymes.billing.model.InvoicePayload;

public interface BillingProvider {

  IssueInvoiceResult issueInvoice(InvoicePayload payload, String idempotencyKey)
      throws BillingProviderException;

  ProviderDocument fetchDocument(String providerDocumentId) throws BillingProviderException;

  record IssueInvoiceResult(
      String provider,
      String providerDocumentId,
      String trackId,
      String number,
      OfficialDocument officialDocument) {
  }

  record ProviderDocument(
      String provider,
      String providerDocumentId,
      String trackId,
      String number,
      OfficialDocument officialDocument) {
  }

  record OfficialDocument(
      byte[] content,
      String filename,
      String contentType,
      String downloadUrl) {
  }
}

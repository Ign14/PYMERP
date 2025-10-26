package com.datakomerz.pymes.billing.dto;

import com.company.billing.persistence.FiscalDocumentType;
import com.company.billing.persistence.TaxMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IssueInvoiceRequest(
    @NotNull FiscalDocumentType documentType,
    @NotNull TaxMode taxMode,
    @NotNull @Valid InvoiceSaleDto sale,
    @Size(max = 100) String idempotencyKey,
    Boolean forceOffline,
    @Size(max = 30) String connectivityHint
) {

  public boolean resolvedForceOffline() {
    return Boolean.TRUE.equals(forceOffline);
  }
}

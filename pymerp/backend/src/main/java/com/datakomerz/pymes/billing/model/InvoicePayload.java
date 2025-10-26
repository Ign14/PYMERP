package com.datakomerz.pymes.billing.model;

import com.company.billing.persistence.FiscalDocumentType;
import com.company.billing.persistence.NonFiscalDocumentType;
import com.company.billing.persistence.TaxMode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;

public record InvoicePayload(
    UUID saleId,
    FiscalDocumentType fiscalDocumentType,
    NonFiscalDocumentType nonFiscalDocumentType,
    TaxMode taxMode,
    String deviceId,
    String pointOfSale,
    ObjectNode rawPayload
) {

  public boolean isFiscal() {
    return fiscalDocumentType != null;
  }

  public boolean hasRawPayload() {
    return rawPayload != null;
  }
}

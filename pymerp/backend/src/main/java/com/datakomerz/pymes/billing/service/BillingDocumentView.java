package com.datakomerz.pymes.billing.service;

import com.company.billing.persistence.DocumentFileKind;
import com.company.billing.persistence.DocumentFileVersion;
import com.company.billing.persistence.FiscalDocumentStatus;
import com.company.billing.persistence.FiscalDocumentType;
import com.company.billing.persistence.NonFiscalDocumentStatus;
import com.company.billing.persistence.NonFiscalDocumentType;
import com.company.billing.persistence.TaxMode;
import com.datakomerz.pymes.billing.model.DocumentCategory;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BillingDocumentView(
    DocumentCategory category,
    UUID id,
    FiscalDocumentType fiscalDocumentType,
    NonFiscalDocumentType nonFiscalDocumentType,
    FiscalDocumentStatus fiscalStatus,
    NonFiscalDocumentStatus nonFiscalStatus,
    TaxMode taxMode,
    String number,
    String provisionalNumber,
    String provider,
    String trackId,
    boolean offline,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    DocumentLinks links,
    List<DocumentFileView> files
) {

  public record DocumentLinks(String localPdf, String officialPdf, String officialXml) {
  }

  public record DocumentFileView(
      UUID id,
      DocumentFileKind kind,
      DocumentFileVersion version,
      String contentType,
      String storageKey,
      String checksum,
      OffsetDateTime createdAt) {
  }
}

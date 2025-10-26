package com.datakomerz.pymes.billing.dto;

import com.company.billing.persistence.FiscalDocumentType;
import com.company.billing.persistence.NonFiscalDocumentType;
import com.company.billing.persistence.TaxMode;
import com.datakomerz.pymes.billing.model.DocumentCategory;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DocumentDetailResponse(
    UUID id,
    DocumentCategory category,
    FiscalDocumentType fiscalDocumentType,
    NonFiscalDocumentType nonFiscalDocumentType,
    String status,
    TaxMode taxMode,
    String number,
    String provisionalNumber,
    String provider,
    String trackId,
    boolean offline,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    DocumentLinksResponse links,
    List<DocumentFileResponse> files
) {
}

package com.datakomerz.pymes.billing.dto;

import com.company.billing.persistence.FiscalDocumentStatus;
import com.company.billing.persistence.FiscalDocumentType;
import com.company.billing.persistence.TaxMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
    UUID id,
    FiscalDocumentType documentType,
    FiscalDocumentStatus status,
    TaxMode taxMode,
    String number,
    String provisionalNumber,
    String trackId,
    String provider,
    boolean offline,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    DocumentLinksResponse links,
    List<DocumentFileResponse> files
) {
}

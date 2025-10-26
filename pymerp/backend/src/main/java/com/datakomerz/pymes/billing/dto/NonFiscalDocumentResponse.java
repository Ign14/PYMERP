package com.datakomerz.pymes.billing.dto;

import com.company.billing.persistence.NonFiscalDocumentStatus;
import com.company.billing.persistence.NonFiscalDocumentType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record NonFiscalDocumentResponse(
    UUID id,
    NonFiscalDocumentType documentType,
    NonFiscalDocumentStatus status,
    String number,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    DocumentLinksResponse links,
    List<DocumentFileResponse> files
) {
}

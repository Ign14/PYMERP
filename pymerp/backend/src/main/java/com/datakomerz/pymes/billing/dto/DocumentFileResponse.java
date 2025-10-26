package com.datakomerz.pymes.billing.dto;

import com.company.billing.persistence.DocumentFileKind;
import com.company.billing.persistence.DocumentFileVersion;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentFileResponse(
    UUID id,
    DocumentFileKind kind,
    DocumentFileVersion version,
    String contentType,
    String storageKey,
    String checksum,
    OffsetDateTime createdAt
) {
}

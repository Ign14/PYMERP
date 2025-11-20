package com.datakomerz.pymes.inventory.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryLocationResponse(
    UUID id,
    String code,
    String name,
    String description,
    boolean enabled,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}

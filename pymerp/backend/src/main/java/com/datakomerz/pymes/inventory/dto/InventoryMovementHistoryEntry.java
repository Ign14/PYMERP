package com.datakomerz.pymes.inventory.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryMovementHistoryEntry(
    UUID id,
    String type,
    BigDecimal qtyChange,
    BigDecimal beforeQty,
    BigDecimal afterQty,
    UUID productId,
    UUID lotId,
    InventoryMovementLocation locationFrom,
    InventoryMovementLocation locationTo,
    String userId,
    String traceId,
    String refType,
    UUID refId,
    OffsetDateTime createdAt,
    String reasonCode,
    String note
) {
  public record InventoryMovementLocation(UUID id, String name) {}
}

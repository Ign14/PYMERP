package com.datakomerz.pymes.inventory.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryMovementSummary(
  UUID id,
  UUID productId,
  String productName,
  UUID lotId,
  String type,
  BigDecimal qty,
  String refType,
  UUID refId,
  String note,
  String createdBy,
  String userIp,
  String reasonCode,
  BigDecimal previousQty,
  BigDecimal newQty,
  OffsetDateTime createdAt
) {}

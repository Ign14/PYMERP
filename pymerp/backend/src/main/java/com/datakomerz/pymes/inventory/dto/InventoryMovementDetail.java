package com.datakomerz.pymes.inventory.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryMovementDetail(
  UUID id,
  String type,
  OffsetDateTime createdAt,
  UUID productId,
  String productSku,
  String productName,
  UUID lotId,
  String batchName,
  BigDecimal qty,
  UUID locationId,
  String locationCode,
  String locationName,
  String refType,
  UUID refId,
  String note,
  String createdBy
) {}

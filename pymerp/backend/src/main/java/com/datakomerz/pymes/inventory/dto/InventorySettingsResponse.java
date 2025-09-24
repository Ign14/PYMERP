package com.datakomerz.pymes.inventory.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record InventorySettingsResponse(
  BigDecimal lowStockThreshold,
  OffsetDateTime updatedAt
) {}

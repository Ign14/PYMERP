package com.datakomerz.pymes.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InventoryAdjustmentResponse(
  UUID productId,
  BigDecimal appliedQuantity,
  String direction
) {}

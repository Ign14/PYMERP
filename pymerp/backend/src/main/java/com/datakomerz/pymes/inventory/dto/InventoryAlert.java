package com.datakomerz.pymes.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryAlert(
  UUID lotId,
  UUID productId,
  BigDecimal qtyAvailable,
  OffsetDateTime createdAt,
  LocalDate expDate
) {}

package com.datakomerz.pymes.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record InventorySettingsUpdateRequest(
  @NotNull @DecimalMin(value = "0.000", inclusive = false) BigDecimal lowStockThreshold
) {}

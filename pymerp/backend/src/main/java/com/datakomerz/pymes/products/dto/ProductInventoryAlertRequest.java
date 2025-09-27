package com.datakomerz.pymes.products.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProductInventoryAlertRequest(
  @NotNull @DecimalMin(value = "0", inclusive = true) BigDecimal criticalStock
) {}

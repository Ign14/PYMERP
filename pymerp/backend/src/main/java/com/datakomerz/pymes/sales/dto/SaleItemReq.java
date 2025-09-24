package com.datakomerz.pymes.sales.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record SaleItemReq(
  @NotNull UUID productId,
  @NotNull @DecimalMin(value = "0.000", inclusive = false) BigDecimal qty,
  @NotNull @DecimalMin(value = "0.0001", inclusive = false) BigDecimal unitPrice,
  BigDecimal discount
) {}

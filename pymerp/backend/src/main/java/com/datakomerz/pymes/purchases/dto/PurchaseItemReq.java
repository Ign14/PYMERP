package com.datakomerz.pymes.purchases.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record PurchaseItemReq(
  @NotNull UUID productId,
  @NotNull @DecimalMin(value = "0.000", inclusive = false) BigDecimal qty,
  @NotNull @DecimalMin(value = "0.0001", inclusive = false) BigDecimal unitCost,
  BigDecimal vatRate,
  LocalDate mfgDate,
  LocalDate expDate
) {}

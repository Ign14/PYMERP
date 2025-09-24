package com.datakomerz.pymes.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InventoryAdjustmentRequest(
  @NotNull UUID productId,
  @NotNull @DecimalMin(value = "0.0001", inclusive = true) BigDecimal quantity,
  @NotBlank String reason,
  @NotNull @Pattern(regexp = "(?i)increase|decrease") String direction,
  BigDecimal unitCost,
  UUID lotId,
  LocalDate mfgDate,
  LocalDate expDate
) {}

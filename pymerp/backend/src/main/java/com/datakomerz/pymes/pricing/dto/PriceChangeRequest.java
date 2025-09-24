package com.datakomerz.pymes.pricing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PriceChangeRequest(
  @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal price,
  OffsetDateTime validFrom
) {}

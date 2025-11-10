package com.datakomerz.pymes.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record LotTransferRequest(
  @NotNull UUID targetLocationId,
  @NotNull @Positive BigDecimal qty,
  String note
) {}

package com.datakomerz.pymes.sales.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record SaleReq(
  @NotNull UUID customerId,
  String paymentMethod,
  String docType,
  @NotEmpty List<@Valid SaleItemReq> items
) {}

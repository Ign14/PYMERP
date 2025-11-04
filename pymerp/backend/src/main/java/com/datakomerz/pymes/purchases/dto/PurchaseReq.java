package com.datakomerz.pymes.purchases.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record PurchaseReq(
  @NotNull UUID supplierId,
  @NotBlank String docType,
  String docNumber,
  @NotNull BigDecimal net,
  @NotNull BigDecimal vat,
  @NotNull BigDecimal total,
  String pdfUrl,
  @NotNull OffsetDateTime issuedAt,
  OffsetDateTime receivedAt,
  @NotEmpty List<@Valid PurchaseItemReq> items
) {}

package com.datakomerz.pymes.purchases.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PurchaseSummary(
  UUID id,
  UUID supplierId,
  String supplierName,
  String docType,
  String docNumber,
  String status,
  BigDecimal net,
  BigDecimal vat,
  BigDecimal total,
  OffsetDateTime issuedAt
) {}

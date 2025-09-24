package com.datakomerz.pymes.sales.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SaleRes(
  UUID id,
  UUID customerId,
  String customerName,
  String status,
  BigDecimal net,
  BigDecimal vat,
  BigDecimal total,
  OffsetDateTime issuedAt,
  String docType,
  String paymentMethod
) {}

package com.datakomerz.pymes.sales.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SaleSummary(
  UUID id,
  UUID customerId,
  String customerName,
  String docType,
  String paymentMethod,
  Integer paymentTermDays,
  OffsetDateTime dueDate,
  String status,
  BigDecimal net,
  BigDecimal vat,
  BigDecimal total,
  OffsetDateTime issuedAt
) {}

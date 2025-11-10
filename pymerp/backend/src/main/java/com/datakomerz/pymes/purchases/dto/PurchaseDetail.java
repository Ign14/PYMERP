package com.datakomerz.pymes.purchases.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PurchaseDetail(
  UUID id,
  OffsetDateTime issuedAt,
  OffsetDateTime receivedAt,
  OffsetDateTime dueDate,
  String docType,
  String docNumber,
  Integer paymentTermDays,
  String status,
  PurchaseDetailSupplier supplier,
  List<PurchaseDetailLine> items,
  BigDecimal net,
  BigDecimal vat,
  BigDecimal total
) {}

package com.datakomerz.pymes.finances.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountPayable(
  UUID id,
  UUID purchaseId,
  UUID supplierId,
  String supplierName,
  String docType,
  String docNumber,
  BigDecimal total,
  BigDecimal paid,
  BigDecimal balance,
  String status,
  OffsetDateTime issuedAt,
  OffsetDateTime dueDate,
  Long daysOverdue,
  String paymentStatus,
  Integer paymentTermDays
) {}

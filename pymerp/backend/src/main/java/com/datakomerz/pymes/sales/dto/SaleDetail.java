package com.datakomerz.pymes.sales.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SaleDetail(
  UUID id,
  OffsetDateTime issuedAt,
  OffsetDateTime dueDate,
  String docType,
  String paymentMethod,
  Integer paymentTermDays,
  String status,
  SaleDetailCustomer customer,
  List<SaleDetailLine> items,
  BigDecimal net,
  BigDecimal vat,
  BigDecimal total,
  String thermalTicket
) {}

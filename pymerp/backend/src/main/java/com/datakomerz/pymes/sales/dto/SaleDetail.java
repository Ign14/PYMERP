package com.datakomerz.pymes.sales.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SaleDetail(
  UUID id,
  OffsetDateTime issuedAt,
  String docType,
  String paymentMethod,
  String status,
  SaleDetailCustomer customer,
  List<SaleDetailLine> items,
  BigDecimal net,
  BigDecimal vat,
  BigDecimal total,
  String thermalTicket
) {}

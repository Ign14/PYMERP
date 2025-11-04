package com.datakomerz.pymes.customers.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CustomerSaleHistoryItem(
  String saleId,
  Instant saleDate,
  String docType,
  String docNumber,
  BigDecimal total,
  Integer itemCount
) {}

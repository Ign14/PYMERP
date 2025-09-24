package com.datakomerz.pymes.sales.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SaleDetailLine(
  UUID productId,
  String productName,
  BigDecimal qty,
  BigDecimal unitPrice,
  BigDecimal discount,
  BigDecimal lineTotal
) {}

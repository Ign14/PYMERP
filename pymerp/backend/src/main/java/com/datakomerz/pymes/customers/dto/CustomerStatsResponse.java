package com.datakomerz.pymes.customers.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CustomerStatsResponse(
  Integer totalSales,
  BigDecimal totalRevenue,
  LocalDate lastSaleDate,
  List<TopProduct> topProducts
) {
  public record TopProduct(
    String productId,
    String productName,
    Integer quantity,
    BigDecimal revenue
  ) {}
}

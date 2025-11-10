package com.datakomerz.pymes.products.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LowStockProduct(
  UUID productId,
  String sku,
  String name,
  String category,
  BigDecimal currentStock,
  BigDecimal criticalStock,
  BigDecimal deficit
) {}

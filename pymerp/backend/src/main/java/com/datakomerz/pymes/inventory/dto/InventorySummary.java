package com.datakomerz.pymes.inventory.dto;

import java.math.BigDecimal;

public record InventorySummary(
  BigDecimal totalValue,
  long activeProducts,
  long inactiveProducts,
  long totalProducts,
  long lowStockAlerts,
  BigDecimal lowStockThreshold
) {}

package com.datakomerz.pymes.products.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductStockResponse(
  UUID productId,
  BigDecimal total,
  List<ProductStockLot> lots
) {}

package com.datakomerz.pymes.products.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProductStockLot(
  UUID lotId,
  BigDecimal quantity,
  String location,
  LocalDate expiresAt
) {}

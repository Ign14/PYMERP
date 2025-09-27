package com.datakomerz.pymes.products.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRes(
  UUID id,
  String sku,
  String name,
  String description,
  String category,
  String barcode,
  String imageUrl,
  String qrUrl,
  BigDecimal criticalStock,
  BigDecimal currentPrice,
  boolean active
) {}

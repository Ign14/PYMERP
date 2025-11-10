package com.datakomerz.pymes.purchases.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PurchaseDetailLine(
  UUID id,
  UUID productId,
  UUID serviceId,
  String productName,
  String serviceName,
  String productSku,
  BigDecimal qty,
  BigDecimal unitCost,
  BigDecimal vatRate,
  LocalDate mfgDate,
  LocalDate expDate,
  UUID locationId,
  String locationCode
) {}

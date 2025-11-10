package com.datakomerz.pymes.products.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProductStockLot(
  UUID lotId,
  BigDecimal quantity,
  BigDecimal costUnit,
  String batchName,
  UUID purchaseId,
  String purchaseDocNumber,
  UUID locationId,
  String locationCode,
  String locationName,
  LocalDate mfgDate,
  LocalDate expDate
) {}

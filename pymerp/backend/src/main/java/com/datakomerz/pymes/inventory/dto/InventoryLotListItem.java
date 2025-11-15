package com.datakomerz.pymes.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryLotListItem(
    UUID lotId,
    ProductInfo product,
    SupplierInfo supplier,
    LocationInfo location,
    BigDecimal qtyAvailable,
    BigDecimal qtyReserved,
    String status,
    OffsetDateTime fechaIngreso,
    LocalDate fechaExpiracion
) {
  public record ProductInfo(UUID id, String name, String sku) {}

  public record SupplierInfo(UUID id, String name) {}

  public record LocationInfo(UUID id, String name) {}
}

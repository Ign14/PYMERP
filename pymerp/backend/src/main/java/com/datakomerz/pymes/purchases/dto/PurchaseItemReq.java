package com.datakomerz.pymes.purchases.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record PurchaseItemReq(
  UUID productId,   // Opcional: se usa si es un producto
  UUID serviceId,   // Opcional: se usa si es un servicio
  @NotNull @DecimalMin(value = "0.000", inclusive = false) BigDecimal qty,
  @NotNull @DecimalMin(value = "0.0001", inclusive = false) BigDecimal unitCost,
  BigDecimal vatRate,
  LocalDate mfgDate,
  LocalDate expDate,
  UUID locationId
) {
  public PurchaseItemReq {
    // Validar que se proporcione exactamente uno de productId o serviceId
    if ((productId == null && serviceId == null) || (productId != null && serviceId != null)) {
      throw new IllegalArgumentException("Debe proporcionar exactamente uno de: productId o serviceId");
    }
  }
  
  public boolean isProduct() {
    return productId != null;
  }
  
  public boolean isService() {
    return serviceId != null;
  }
}

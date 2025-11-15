package com.datakomerz.pymes.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record StockByLocationDTO(
    UUID locationId,
    String locationCode,
    String locationName,
    BigDecimal totalQuantity
) {}

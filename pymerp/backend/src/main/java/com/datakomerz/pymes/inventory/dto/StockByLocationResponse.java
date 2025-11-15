package com.datakomerz.pymes.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record StockByLocationResponse(
    UUID productId,
    UUID locationId,
    String locationName,
    BigDecimal availableQty
) {}

package com.datakomerz.pymes.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record StockByLocationAggregation(
    UUID productId,
    UUID locationId,
    BigDecimal qtyAvailable
) {}

package com.datakomerz.pymes.locations.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record LocationStockDTO(
    UUID locationId,
    String locationCode,
    String locationName,
    String locationType,
    List<ProductStock> products
) {
    public record ProductStock(
        UUID productId,
        String productName,
        String productSku,
        BigDecimal totalQuantity,
        int lotCount
    ) {}
}

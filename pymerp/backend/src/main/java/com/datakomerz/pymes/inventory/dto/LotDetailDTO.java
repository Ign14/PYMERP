package com.datakomerz.pymes.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LotDetailDTO(
    UUID id,
    String lotNumber,
    UUID productId,
    String productName,
    String productSku,
    UUID supplierId,
    String supplierName,
    UUID locationId,
    String locationCode,
    String locationName,
    BigDecimal quantityAvailable,
    BigDecimal quantityReserved,
    String status, // OK, BAJO_STOCK, POR_VENCER, VENCIDO
    LocalDate ingressDate,
    LocalDate expiryDate,
    OffsetDateTime createdAt
) {}

package com.datakomerz.pymes.purchases.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseCreationResult(
    UUID id,
    String docNumber,
    BigDecimal total,
    int itemsCreated,
    int lotsCreated
) {}

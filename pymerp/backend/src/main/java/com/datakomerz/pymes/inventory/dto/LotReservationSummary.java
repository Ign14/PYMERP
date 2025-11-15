package com.datakomerz.pymes.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LotReservationSummary(UUID lotId, BigDecimal reservedQty) {}

package com.datakomerz.pymes.pricing.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PriceHistoryResponse(
  UUID id,
  UUID productId,
  BigDecimal price,
  OffsetDateTime validFrom
) {}

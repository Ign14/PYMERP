package com.datakomerz.pymes.finances.dto;

import java.math.BigDecimal;

public record PaymentBucketSummary(
  String key,
  String label,
  int minDays,
  int maxDays,
  BigDecimal amount,
  long documents
) {}

package com.datakomerz.pymes.sales.dto;

import java.math.BigDecimal;

public record SalesWindowMetrics(
    String window,
    BigDecimal totalWithTax,
    BigDecimal dailyAverage,
    long documentCount
) {}
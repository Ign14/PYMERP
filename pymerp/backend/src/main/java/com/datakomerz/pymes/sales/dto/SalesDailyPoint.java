package com.datakomerz.pymes.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesDailyPoint(LocalDate date, BigDecimal total, long count) {}

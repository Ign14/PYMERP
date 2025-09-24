package com.datakomerz.pymes.purchases.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseDailyPoint(LocalDate date, BigDecimal total, long count) {}

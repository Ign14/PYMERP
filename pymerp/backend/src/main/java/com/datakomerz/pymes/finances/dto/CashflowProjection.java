package com.datakomerz.pymes.finances.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CashflowProjection(
  LocalDate date,
  BigDecimal expectedIncome,
  BigDecimal expectedExpense,
  BigDecimal netFlow,
  BigDecimal cumulativeBalance,
  String period
) {}

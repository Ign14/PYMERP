package com.datakomerz.pymes.finances.dto;

import java.math.BigDecimal;

public record FinanceSummary(
  BigDecimal totalCash,
  BigDecimal accountsReceivable,
  BigDecimal accountsPayable,
  BigDecimal netPosition,
  long pendingInvoices,
  long overdueInvoices,
  long pendingPurchases,
  long overduePurchases,
  BigDecimal next7DaysReceivable,
  BigDecimal next7DaysPayable,
  BigDecimal next30DaysReceivable,
  BigDecimal next30DaysPayable
) {}

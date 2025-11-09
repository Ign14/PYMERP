package com.datakomerz.pymes.finances.dto;

import java.math.BigDecimal;
import java.util.List;

public record FinanceSummary(
  BigDecimal cashOnHand,
  BigDecimal totalReceivables,
  BigDecimal totalPayables,
  BigDecimal netPosition,
  long pendingReceivables,
  long overdueReceivables,
  long pendingPayables,
  long overduePayables,
  BigDecimal next7DaysIncome,
  BigDecimal next7DaysExpense,
  BigDecimal next30DaysIncome,
  BigDecimal next30DaysExpense,
  List<PaymentBucketSummary> receivableBuckets,
  List<PaymentBucketSummary> payableBuckets
) {}

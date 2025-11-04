package com.datakomerz.pymes.suppliers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class SupplierMetrics {
  private Long totalPurchases;
  private BigDecimal totalAmount;
  private BigDecimal averageOrderValue;
  private OffsetDateTime lastPurchaseDate;
  private Long purchasesLastMonth;
  private BigDecimal amountLastMonth;
  private Long purchasesPreviousMonth;
  private BigDecimal amountPreviousMonth;

  // Constructor vacío
  public SupplierMetrics() {}

  // Constructor completo
  public SupplierMetrics(Long totalPurchases, BigDecimal totalAmount, BigDecimal averageOrderValue,
                        OffsetDateTime lastPurchaseDate, Long purchasesLastMonth, BigDecimal amountLastMonth,
                        Long purchasesPreviousMonth, BigDecimal amountPreviousMonth) {
    this.totalPurchases = totalPurchases;
    this.totalAmount = totalAmount;
    this.averageOrderValue = averageOrderValue;
    this.lastPurchaseDate = lastPurchaseDate;
    this.purchasesLastMonth = purchasesLastMonth;
    this.amountLastMonth = amountLastMonth;
    this.purchasesPreviousMonth = purchasesPreviousMonth;
    this.amountPreviousMonth = amountPreviousMonth;
  }

  // Getters & Setters
  public Long getTotalPurchases() { return totalPurchases; }
  public void setTotalPurchases(Long totalPurchases) { this.totalPurchases = totalPurchases; }

  public BigDecimal getTotalAmount() { return totalAmount; }
  public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

  public BigDecimal getAverageOrderValue() { return averageOrderValue; }
  public void setAverageOrderValue(BigDecimal averageOrderValue) { this.averageOrderValue = averageOrderValue; }

  public OffsetDateTime getLastPurchaseDate() { return lastPurchaseDate; }
  public void setLastPurchaseDate(OffsetDateTime lastPurchaseDate) { this.lastPurchaseDate = lastPurchaseDate; }

  public Long getPurchasesLastMonth() { return purchasesLastMonth; }
  public void setPurchasesLastMonth(Long purchasesLastMonth) { this.purchasesLastMonth = purchasesLastMonth; }

  public BigDecimal getAmountLastMonth() { return amountLastMonth; }
  public void setAmountLastMonth(BigDecimal amountLastMonth) { this.amountLastMonth = amountLastMonth; }

  public Long getPurchasesPreviousMonth() { return purchasesPreviousMonth; }
  public void setPurchasesPreviousMonth(Long purchasesPreviousMonth) { this.purchasesPreviousMonth = purchasesPreviousMonth; }

  public BigDecimal getAmountPreviousMonth() { return amountPreviousMonth; }
  public void setAmountPreviousMonth(BigDecimal amountPreviousMonth) { this.amountPreviousMonth = amountPreviousMonth; }
}

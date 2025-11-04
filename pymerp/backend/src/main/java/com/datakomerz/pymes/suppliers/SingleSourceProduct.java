package com.datakomerz.pymes.suppliers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class SingleSourceProduct {
  private UUID productId;
  private String productName;
  private UUID supplierId;
  private String supplierName;
  private BigDecimal currentPrice;
  private Long purchasesLast12Months;
  private BigDecimal totalSpentLast12Months;
  private LocalDate lastPurchaseDate;
  private String riskLevel; // "CRITICAL", "HIGH", "MEDIUM", "LOW"
  private String recommendation;

  public SingleSourceProduct() {}

  public UUID getProductId() { return productId; }
  public void setProductId(UUID productId) { this.productId = productId; }

  public String getProductName() { return productName; }
  public void setProductName(String productName) { this.productName = productName; }

  public UUID getSupplierId() { return supplierId; }
  public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }

  public String getSupplierName() { return supplierName; }
  public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

  public BigDecimal getCurrentPrice() { return currentPrice; }
  public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

  public Long getPurchasesLast12Months() { return purchasesLast12Months; }
  public void setPurchasesLast12Months(Long purchasesLast12Months) { 
    this.purchasesLast12Months = purchasesLast12Months; 
  }

  public BigDecimal getTotalSpentLast12Months() { return totalSpentLast12Months; }
  public void setTotalSpentLast12Months(BigDecimal totalSpentLast12Months) { 
    this.totalSpentLast12Months = totalSpentLast12Months; 
  }

  public LocalDate getLastPurchaseDate() { return lastPurchaseDate; }
  public void setLastPurchaseDate(LocalDate lastPurchaseDate) { this.lastPurchaseDate = lastPurchaseDate; }

  public String getRiskLevel() { return riskLevel; }
  public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

  public String getRecommendation() { return recommendation; }
  public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
}

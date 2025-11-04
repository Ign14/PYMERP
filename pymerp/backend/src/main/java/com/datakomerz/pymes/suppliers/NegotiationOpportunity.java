package com.datakomerz.pymes.suppliers;

import java.math.BigDecimal;
import java.util.UUID;

public class NegotiationOpportunity {
  private UUID supplierId;
  private String supplierName;
  private UUID productId;
  private String productName;
  private BigDecimal currentPrice;
  private BigDecimal marketAverage;
  private BigDecimal priceDifference;
  private Double pricePercentageAboveMarket;
  private Long purchasesLast12Months;
  private BigDecimal totalSpentLast12Months;
  private BigDecimal potentialSavings;
  private String priority; // "HIGH", "MEDIUM", "LOW"
  private String recommendation;

  public NegotiationOpportunity() {}

  public UUID getSupplierId() { return supplierId; }
  public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }

  public String getSupplierName() { return supplierName; }
  public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

  public UUID getProductId() { return productId; }
  public void setProductId(UUID productId) { this.productId = productId; }

  public String getProductName() { return productName; }
  public void setProductName(String productName) { this.productName = productName; }

  public BigDecimal getCurrentPrice() { return currentPrice; }
  public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

  public BigDecimal getMarketAverage() { return marketAverage; }
  public void setMarketAverage(BigDecimal marketAverage) { this.marketAverage = marketAverage; }

  public BigDecimal getPriceDifference() { return priceDifference; }
  public void setPriceDifference(BigDecimal priceDifference) { this.priceDifference = priceDifference; }

  public Double getPricePercentageAboveMarket() { return pricePercentageAboveMarket; }
  public void setPricePercentageAboveMarket(Double pricePercentageAboveMarket) { 
    this.pricePercentageAboveMarket = pricePercentageAboveMarket; 
  }

  public Long getPurchasesLast12Months() { return purchasesLast12Months; }
  public void setPurchasesLast12Months(Long purchasesLast12Months) { 
    this.purchasesLast12Months = purchasesLast12Months; 
  }

  public BigDecimal getTotalSpentLast12Months() { return totalSpentLast12Months; }
  public void setTotalSpentLast12Months(BigDecimal totalSpentLast12Months) { 
    this.totalSpentLast12Months = totalSpentLast12Months; 
  }

  public BigDecimal getPotentialSavings() { return potentialSavings; }
  public void setPotentialSavings(BigDecimal potentialSavings) { this.potentialSavings = potentialSavings; }

  public String getPriority() { return priority; }
  public void setPriority(String priority) { this.priority = priority; }

  public String getRecommendation() { return recommendation; }
  public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
}

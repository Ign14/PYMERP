package com.datakomerz.pymes.suppliers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class SupplierPriceHistory {
  private UUID supplierId;
  private String supplierName;
  private UUID productId;
  private String productName;
  private List<PricePoint> priceHistory;
  private BigDecimal currentPrice;
  private BigDecimal averagePrice;
  private BigDecimal minPrice;
  private BigDecimal maxPrice;
  private String trend; // "UP", "DOWN", "STABLE"
  private BigDecimal trendPercentage; // % de cambio en últimos 3 meses

  public static class PricePoint {
    private LocalDate date;
    private BigDecimal unitPrice;
    private BigDecimal quantity;

    public PricePoint() {}

    public PricePoint(LocalDate date, BigDecimal unitPrice, BigDecimal quantity) {
      this.date = date;
      this.unitPrice = unitPrice;
      this.quantity = quantity;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
  }

  public SupplierPriceHistory() {}

  public UUID getSupplierId() { return supplierId; }
  public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }

  public String getSupplierName() { return supplierName; }
  public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

  public UUID getProductId() { return productId; }
  public void setProductId(UUID productId) { this.productId = productId; }

  public String getProductName() { return productName; }
  public void setProductName(String productName) { this.productName = productName; }

  public List<PricePoint> getPriceHistory() { return priceHistory; }
  public void setPriceHistory(List<PricePoint> priceHistory) { this.priceHistory = priceHistory; }

  public BigDecimal getCurrentPrice() { return currentPrice; }
  public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

  public BigDecimal getAveragePrice() { return averagePrice; }
  public void setAveragePrice(BigDecimal averagePrice) { this.averagePrice = averagePrice; }

  public BigDecimal getMinPrice() { return minPrice; }
  public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

  public BigDecimal getMaxPrice() { return maxPrice; }
  public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

  public String getTrend() { return trend; }
  public void setTrend(String trend) { this.trend = trend; }

  public BigDecimal getTrendPercentage() { return trendPercentage; }
  public void setTrendPercentage(BigDecimal trendPercentage) { this.trendPercentage = trendPercentage; }
}

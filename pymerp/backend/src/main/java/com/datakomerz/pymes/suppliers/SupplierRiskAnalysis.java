package com.datakomerz.pymes.suppliers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class SupplierRiskAnalysis {
  private List<SupplierCategory> categoryA; // Top 80% volume
  private List<SupplierCategory> categoryB; // Next 15% volume
  private List<SupplierCategory> categoryC; // Last 5% volume
  private Double concentrationIndex; // Herfindahl index (0-1)
  private Integer singleSourceProductsCount;
  private BigDecimal totalPurchaseVolume;

  // Constructor vacío
  public SupplierRiskAnalysis() {}

  // Clase interna para categorías
  public static class SupplierCategory {
    private UUID supplierId;
    private String supplierName;
    private BigDecimal purchaseAmount;
    private Double percentage;

    public SupplierCategory() {}

    public SupplierCategory(UUID supplierId, String supplierName, BigDecimal purchaseAmount, Double percentage) {
      this.supplierId = supplierId;
      this.supplierName = supplierName;
      this.purchaseAmount = purchaseAmount;
      this.percentage = percentage;
    }

    // Getters & Setters
    public UUID getSupplierId() { return supplierId; }
    public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public BigDecimal getPurchaseAmount() { return purchaseAmount; }
    public void setPurchaseAmount(BigDecimal purchaseAmount) { this.purchaseAmount = purchaseAmount; }

    public Double getPercentage() { return percentage; }
    public void setPercentage(Double percentage) { this.percentage = percentage; }
  }

  // Getters & Setters
  public List<SupplierCategory> getCategoryA() { return categoryA; }
  public void setCategoryA(List<SupplierCategory> categoryA) { this.categoryA = categoryA; }

  public List<SupplierCategory> getCategoryB() { return categoryB; }
  public void setCategoryB(List<SupplierCategory> categoryB) { this.categoryB = categoryB; }

  public List<SupplierCategory> getCategoryC() { return categoryC; }
  public void setCategoryC(List<SupplierCategory> categoryC) { this.categoryC = categoryC; }

  public Double getConcentrationIndex() { return concentrationIndex; }
  public void setConcentrationIndex(Double concentrationIndex) { this.concentrationIndex = concentrationIndex; }

  public Integer getSingleSourceProductsCount() { return singleSourceProductsCount; }
  public void setSingleSourceProductsCount(Integer singleSourceProductsCount) { 
    this.singleSourceProductsCount = singleSourceProductsCount; 
  }

  public BigDecimal getTotalPurchaseVolume() { return totalPurchaseVolume; }
  public void setTotalPurchaseVolume(BigDecimal totalPurchaseVolume) { 
    this.totalPurchaseVolume = totalPurchaseVolume; 
  }
}

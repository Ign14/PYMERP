package com.datakomerz.pymes.suppliers;

import java.math.BigDecimal;
import java.util.UUID;

public class SupplierRanking {
  private UUID supplierId;
  private String supplierName;
  private Integer rank;
  private Double score;
  private Long totalPurchases;
  private BigDecimal totalAmount;
  private Double reliability; // 0-100
  private String category; // A, B, C

  // Constructor vacío
  public SupplierRanking() {}

  // Constructor completo
  public SupplierRanking(UUID supplierId, String supplierName, Integer rank, Double score,
                        Long totalPurchases, BigDecimal totalAmount, Double reliability, String category) {
    this.supplierId = supplierId;
    this.supplierName = supplierName;
    this.rank = rank;
    this.score = score;
    this.totalPurchases = totalPurchases;
    this.totalAmount = totalAmount;
    this.reliability = reliability;
    this.category = category;
  }

  // Getters & Setters
  public UUID getSupplierId() { return supplierId; }
  public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }

  public String getSupplierName() { return supplierName; }
  public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

  public Integer getRank() { return rank; }
  public void setRank(Integer rank) { this.rank = rank; }

  public Double getScore() { return score; }
  public void setScore(Double score) { this.score = score; }

  public Long getTotalPurchases() { return totalPurchases; }
  public void setTotalPurchases(Long totalPurchases) { this.totalPurchases = totalPurchases; }

  public BigDecimal getTotalAmount() { return totalAmount; }
  public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

  public Double getReliability() { return reliability; }
  public void setReliability(Double reliability) { this.reliability = reliability; }

  public String getCategory() { return category; }
  public void setCategory(String category) { this.category = category; }
}

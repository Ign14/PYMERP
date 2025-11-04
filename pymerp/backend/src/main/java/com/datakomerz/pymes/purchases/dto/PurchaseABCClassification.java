package com.datakomerz.pymes.purchases.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Clasificación ABC de proveedores basada en el análisis de Pareto (80-15-5).
 * Permite identificar proveedores críticos (A), importantes (B) y ocasionales (C).
 */
public class PurchaseABCClassification {
    
    private String supplierId;
    private String supplierName;
    private BigDecimal totalSpent;
    private Long purchaseCount;
    private BigDecimal percentageOfTotal;
    private String classification; // "A", "B", "C"
    private BigDecimal cumulativePercentage;
    private BigDecimal averageOrderValue;
    private OffsetDateTime lastPurchaseDate;
    private String recommendedAction;

    public PurchaseABCClassification() {
    }

    public PurchaseABCClassification(
            String supplierId,
            String supplierName,
            BigDecimal totalSpent,
            Long purchaseCount,
            BigDecimal percentageOfTotal,
            String classification,
            BigDecimal cumulativePercentage,
            BigDecimal averageOrderValue,
            OffsetDateTime lastPurchaseDate,
            String recommendedAction) {
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.totalSpent = totalSpent;
        this.purchaseCount = purchaseCount;
        this.percentageOfTotal = percentageOfTotal;
        this.classification = classification;
        this.cumulativePercentage = cumulativePercentage;
        this.averageOrderValue = averageOrderValue;
        this.lastPurchaseDate = lastPurchaseDate;
        this.recommendedAction = recommendedAction;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }

    public Long getPurchaseCount() {
        return purchaseCount;
    }

    public void setPurchaseCount(Long purchaseCount) {
        this.purchaseCount = purchaseCount;
    }

    public BigDecimal getPercentageOfTotal() {
        return percentageOfTotal;
    }

    public void setPercentageOfTotal(BigDecimal percentageOfTotal) {
        this.percentageOfTotal = percentageOfTotal;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public BigDecimal getCumulativePercentage() {
        return cumulativePercentage;
    }

    public void setCumulativePercentage(BigDecimal cumulativePercentage) {
        this.cumulativePercentage = cumulativePercentage;
    }

    public BigDecimal getAverageOrderValue() {
        return averageOrderValue;
    }

    public void setAverageOrderValue(BigDecimal averageOrderValue) {
        this.averageOrderValue = averageOrderValue;
    }

    public OffsetDateTime getLastPurchaseDate() {
        return lastPurchaseDate;
    }

    public void setLastPurchaseDate(OffsetDateTime lastPurchaseDate) {
        this.lastPurchaseDate = lastPurchaseDate;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }
}

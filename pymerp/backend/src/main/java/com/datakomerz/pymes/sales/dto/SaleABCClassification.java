package com.datakomerz.pymes.sales.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Clasificación ABC de productos basada en el análisis de Pareto (80-15-5).
 * Permite identificar productos críticos (A), importantes (B) y ocasionales (C) por ingresos.
 */
public class SaleABCClassification {
    
    private String productId;
    private String productName;
    private BigDecimal totalRevenue;
    private Long salesCount;
    private BigDecimal percentageOfTotal;
    private String classification; // "A", "B", "C"
    private BigDecimal cumulativePercentage;
    private BigDecimal averagePrice;
    private OffsetDateTime lastSaleDate;
    private String recommendedAction;

    public SaleABCClassification() {
    }

    public SaleABCClassification(
            String productId,
            String productName,
            BigDecimal totalRevenue,
            Long salesCount,
            BigDecimal percentageOfTotal,
            String classification,
            BigDecimal cumulativePercentage,
            BigDecimal averagePrice,
            OffsetDateTime lastSaleDate,
            String recommendedAction) {
        this.productId = productId;
        this.productName = productName;
        this.totalRevenue = totalRevenue;
        this.salesCount = salesCount;
        this.percentageOfTotal = percentageOfTotal;
        this.classification = classification;
        this.cumulativePercentage = cumulativePercentage;
        this.averagePrice = averagePrice;
        this.lastSaleDate = lastSaleDate;
        this.recommendedAction = recommendedAction;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Long getSalesCount() {
        return salesCount;
    }

    public void setSalesCount(Long salesCount) {
        this.salesCount = salesCount;
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

    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(BigDecimal averagePrice) {
        this.averagePrice = averagePrice;
    }

    public OffsetDateTime getLastSaleDate() {
        return lastSaleDate;
    }

    public void setLastSaleDate(OffsetDateTime lastSaleDate) {
        this.lastSaleDate = lastSaleDate;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }
}

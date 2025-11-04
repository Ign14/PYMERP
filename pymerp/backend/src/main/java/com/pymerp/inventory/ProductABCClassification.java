package com.pymerp.inventory;

import java.time.LocalDate;

/**
 * DTO para clasificación ABC de productos
 * 
 * Método ABC (Pareto):
 * - Clase A: 20% productos que representan 80% del valor
 * - Clase B: 30% productos que representan 15% del valor
 * - Clase C: 50% productos que representan 5% del valor
 */
public class ProductABCClassification {
    
    private Long productId;
    private String productName;
    private String category;
    private String classification; // "A", "B", "C"
    private Double totalValue;
    private Integer totalQuantity;
    private Double percentageOfTotalValue;
    private Double cumulativePercentage;
    private Integer salesFrequency; // Número de transacciones en período
    private LocalDate lastMovementDate;
    
    // Constructors
    
    public ProductABCClassification() {
    }
    
    public ProductABCClassification(Long productId, String productName, String category, 
                                   String classification, Double totalValue, Integer totalQuantity,
                                   Double percentageOfTotalValue, Double cumulativePercentage,
                                   Integer salesFrequency, LocalDate lastMovementDate) {
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.classification = classification;
        this.totalValue = totalValue;
        this.totalQuantity = totalQuantity;
        this.percentageOfTotalValue = percentageOfTotalValue;
        this.cumulativePercentage = cumulativePercentage;
        this.salesFrequency = salesFrequency;
        this.lastMovementDate = lastMovementDate;
    }
    
    // Getters and Setters
    
    public Long getProductId() {
        return productId;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getClassification() {
        return classification;
    }
    
    public void setClassification(String classification) {
        this.classification = classification;
    }
    
    public Double getTotalValue() {
        return totalValue;
    }
    
    public void setTotalValue(Double totalValue) {
        this.totalValue = totalValue;
    }
    
    public Integer getTotalQuantity() {
        return totalQuantity;
    }
    
    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }
    
    public Double getPercentageOfTotalValue() {
        return percentageOfTotalValue;
    }
    
    public void setPercentageOfTotalValue(Double percentageOfTotalValue) {
        this.percentageOfTotalValue = percentageOfTotalValue;
    }
    
    public Double getCumulativePercentage() {
        return cumulativePercentage;
    }
    
    public void setCumulativePercentage(Double cumulativePercentage) {
        this.cumulativePercentage = cumulativePercentage;
    }
    
    public Integer getSalesFrequency() {
        return salesFrequency;
    }
    
    public void setSalesFrequency(Integer salesFrequency) {
        this.salesFrequency = salesFrequency;
    }
    
    public LocalDate getLastMovementDate() {
        return lastMovementDate;
    }
    
    public void setLastMovementDate(LocalDate lastMovementDate) {
        this.lastMovementDate = lastMovementDate;
    }
}

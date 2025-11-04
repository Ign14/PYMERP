package com.datakomerz.pymes.purchases.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para KPIs avanzados de compras
 */
public class PurchaseKPIs {
    
    private BigDecimal totalSpent;
    private BigDecimal totalQuantity;
    private Integer totalOrders;
    private BigDecimal averageOrderValue;
    private BigDecimal purchaseGrowth; // Porcentaje vs período anterior
    private Integer uniqueSuppliers;
    private BigDecimal supplierConcentration; // Porcentaje del top supplier
    private String topSupplierName;
    private BigDecimal topSupplierSpent;
    private String topCategoryName;
    private BigDecimal topCategorySpent;
    private BigDecimal onTimeDeliveryRate; // Porcentaje
    private BigDecimal costPerUnit; // Costo promedio por unidad
    private Integer pendingOrders;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    public PurchaseKPIs() {
    }

    public PurchaseKPIs(BigDecimal totalSpent, BigDecimal totalQuantity, Integer totalOrders,
                        BigDecimal averageOrderValue, BigDecimal purchaseGrowth, Integer uniqueSuppliers,
                        BigDecimal supplierConcentration, String topSupplierName, BigDecimal topSupplierSpent,
                        String topCategoryName, BigDecimal topCategorySpent, BigDecimal onTimeDeliveryRate,
                        BigDecimal costPerUnit, Integer pendingOrders, LocalDate periodStart, LocalDate periodEnd) {
        this.totalSpent = totalSpent;
        this.totalQuantity = totalQuantity;
        this.totalOrders = totalOrders;
        this.averageOrderValue = averageOrderValue;
        this.purchaseGrowth = purchaseGrowth;
        this.uniqueSuppliers = uniqueSuppliers;
        this.supplierConcentration = supplierConcentration;
        this.topSupplierName = topSupplierName;
        this.topSupplierSpent = topSupplierSpent;
        this.topCategoryName = topCategoryName;
        this.topCategorySpent = topCategorySpent;
        this.onTimeDeliveryRate = onTimeDeliveryRate;
        this.costPerUnit = costPerUnit;
        this.pendingOrders = pendingOrders;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    // Getters and Setters
    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(BigDecimal totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getAverageOrderValue() {
        return averageOrderValue;
    }

    public void setAverageOrderValue(BigDecimal averageOrderValue) {
        this.averageOrderValue = averageOrderValue;
    }

    public BigDecimal getPurchaseGrowth() {
        return purchaseGrowth;
    }

    public void setPurchaseGrowth(BigDecimal purchaseGrowth) {
        this.purchaseGrowth = purchaseGrowth;
    }

    public Integer getUniqueSuppliers() {
        return uniqueSuppliers;
    }

    public void setUniqueSuppliers(Integer uniqueSuppliers) {
        this.uniqueSuppliers = uniqueSuppliers;
    }

    public BigDecimal getSupplierConcentration() {
        return supplierConcentration;
    }

    public void setSupplierConcentration(BigDecimal supplierConcentration) {
        this.supplierConcentration = supplierConcentration;
    }

    public String getTopSupplierName() {
        return topSupplierName;
    }

    public void setTopSupplierName(String topSupplierName) {
        this.topSupplierName = topSupplierName;
    }

    public BigDecimal getTopSupplierSpent() {
        return topSupplierSpent;
    }

    public void setTopSupplierSpent(BigDecimal topSupplierSpent) {
        this.topSupplierSpent = topSupplierSpent;
    }

    public String getTopCategoryName() {
        return topCategoryName;
    }

    public void setTopCategoryName(String topCategoryName) {
        this.topCategoryName = topCategoryName;
    }

    public BigDecimal getTopCategorySpent() {
        return topCategorySpent;
    }

    public void setTopCategorySpent(BigDecimal topCategorySpent) {
        this.topCategorySpent = topCategorySpent;
    }

    public BigDecimal getOnTimeDeliveryRate() {
        return onTimeDeliveryRate;
    }

    public void setOnTimeDeliveryRate(BigDecimal onTimeDeliveryRate) {
        this.onTimeDeliveryRate = onTimeDeliveryRate;
    }

    public BigDecimal getCostPerUnit() {
        return costPerUnit;
    }

    public void setCostPerUnit(BigDecimal costPerUnit) {
        this.costPerUnit = costPerUnit;
    }

    public Integer getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(Integer pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }
}

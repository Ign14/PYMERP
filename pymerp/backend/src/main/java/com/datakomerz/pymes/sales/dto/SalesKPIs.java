package com.datakomerz.pymes.sales.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * DTO para KPIs avanzados de ventas
 */
public class SalesKPIs {
    
    private BigDecimal totalRevenue;
    private BigDecimal totalCost;
    private BigDecimal grossProfit;
    private BigDecimal profitMargin; // Porcentaje
    private Integer totalOrders;
    private BigDecimal averageTicket;
    private BigDecimal salesGrowth; // Porcentaje vs período anterior
    private Integer uniqueCustomers;
    private BigDecimal customerRetentionRate; // Porcentaje
    private String topProductName;
    private BigDecimal topProductRevenue;
    private String topCustomerName;
    private BigDecimal topCustomerRevenue;
    private BigDecimal conversionRate; // Porcentaje (ventas emitidas vs canceladas)
    private LocalDate periodStart;
    private LocalDate periodEnd;

    public SalesKPIs() {
    }

    public SalesKPIs(BigDecimal totalRevenue, BigDecimal totalCost, BigDecimal grossProfit,
                     BigDecimal profitMargin, Integer totalOrders, BigDecimal averageTicket,
                     BigDecimal salesGrowth, Integer uniqueCustomers, BigDecimal customerRetentionRate,
                     String topProductName, BigDecimal topProductRevenue, String topCustomerName,
                     BigDecimal topCustomerRevenue, BigDecimal conversionRate, LocalDate periodStart,
                     LocalDate periodEnd) {
        this.totalRevenue = totalRevenue;
        this.totalCost = totalCost;
        this.grossProfit = grossProfit;
        this.profitMargin = profitMargin;
        this.totalOrders = totalOrders;
        this.averageTicket = averageTicket;
        this.salesGrowth = salesGrowth;
        this.uniqueCustomers = uniqueCustomers;
        this.customerRetentionRate = customerRetentionRate;
        this.topProductName = topProductName;
        this.topProductRevenue = topProductRevenue;
        this.topCustomerName = topCustomerName;
        this.topCustomerRevenue = topCustomerRevenue;
        this.conversionRate = conversionRate;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    public static SalesKPIs empty(LocalDate periodStart, LocalDate periodEnd) {
        SalesKPIs kpis = new SalesKPIs();
        kpis.setTotalRevenue(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        kpis.setTotalCost(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        kpis.setGrossProfit(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        kpis.setProfitMargin(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        kpis.setTotalOrders(0);
        kpis.setAverageTicket(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        kpis.setSalesGrowth(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        kpis.setUniqueCustomers(0);
        kpis.setCustomerRetentionRate(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        kpis.setTopProductName("N/A");
        kpis.setTopProductRevenue(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        kpis.setTopCustomerName("N/A");
        kpis.setTopCustomerRevenue(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        kpis.setConversionRate(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        kpis.setPeriodStart(periodStart);
        kpis.setPeriodEnd(periodEnd);
        return kpis;
    }

    // Getters and Setters
    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getGrossProfit() {
        return grossProfit;
    }

    public void setGrossProfit(BigDecimal grossProfit) {
        this.grossProfit = grossProfit;
    }

    public BigDecimal getProfitMargin() {
        return profitMargin;
    }

    public void setProfitMargin(BigDecimal profitMargin) {
        this.profitMargin = profitMargin;
    }

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getAverageTicket() {
        return averageTicket;
    }

    public void setAverageTicket(BigDecimal averageTicket) {
        this.averageTicket = averageTicket;
    }

    public BigDecimal getSalesGrowth() {
        return salesGrowth;
    }

    public void setSalesGrowth(BigDecimal salesGrowth) {
        this.salesGrowth = salesGrowth;
    }

    public Integer getUniqueCustomers() {
        return uniqueCustomers;
    }

    public void setUniqueCustomers(Integer uniqueCustomers) {
        this.uniqueCustomers = uniqueCustomers;
    }

    public BigDecimal getCustomerRetentionRate() {
        return customerRetentionRate;
    }

    public void setCustomerRetentionRate(BigDecimal customerRetentionRate) {
        this.customerRetentionRate = customerRetentionRate;
    }

    public String getTopProductName() {
        return topProductName;
    }

    public void setTopProductName(String topProductName) {
        this.topProductName = topProductName;
    }

    public BigDecimal getTopProductRevenue() {
        return topProductRevenue;
    }

    public void setTopProductRevenue(BigDecimal topProductRevenue) {
        this.topProductRevenue = topProductRevenue;
    }

    public String getTopCustomerName() {
        return topCustomerName;
    }

    public void setTopCustomerName(String topCustomerName) {
        this.topCustomerName = topCustomerName;
    }

    public BigDecimal getTopCustomerRevenue() {
        return topCustomerRevenue;
    }

    public void setTopCustomerRevenue(BigDecimal topCustomerRevenue) {
        this.topCustomerRevenue = topCustomerRevenue;
    }

    public BigDecimal getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(BigDecimal conversionRate) {
        this.conversionRate = conversionRate;
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

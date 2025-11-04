package com.datakomerz.pymes.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para pronóstico de demanda de inventario.
 * Utiliza análisis de series temporales (Moving Average) para predecir demanda futura.
 */
public class InventoryForecast {

    private Long productId;
    private String productName;
    private String category;
    private LocalDate forecastDate;
    private BigDecimal predictedDemand;
    private BigDecimal confidence; // Porcentaje de confianza (0-100)
    private BigDecimal historicalAverage;
    private String trend; // "increasing", "decreasing", "stable"
    private BigDecimal recommendedOrderQty;
    private String stockStatus; // "understocked", "optimal", "overstocked"
    private BigDecimal currentStock;
    private Integer daysOfStock; // Días de inventario restantes

    public InventoryForecast() {
    }

    public InventoryForecast(Long productId, String productName, String category,
                             LocalDate forecastDate, BigDecimal predictedDemand,
                             BigDecimal confidence, BigDecimal historicalAverage,
                             String trend, BigDecimal recommendedOrderQty,
                             String stockStatus, BigDecimal currentStock,
                             Integer daysOfStock) {
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.forecastDate = forecastDate;
        this.predictedDemand = predictedDemand;
        this.confidence = confidence;
        this.historicalAverage = historicalAverage;
        this.trend = trend;
        this.recommendedOrderQty = recommendedOrderQty;
        this.stockStatus = stockStatus;
        this.currentStock = currentStock;
        this.daysOfStock = daysOfStock;
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

    public LocalDate getForecastDate() {
        return forecastDate;
    }

    public void setForecastDate(LocalDate forecastDate) {
        this.forecastDate = forecastDate;
    }

    public BigDecimal getPredictedDemand() {
        return predictedDemand;
    }

    public void setPredictedDemand(BigDecimal predictedDemand) {
        this.predictedDemand = predictedDemand;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public BigDecimal getHistoricalAverage() {
        return historicalAverage;
    }

    public void setHistoricalAverage(BigDecimal historicalAverage) {
        this.historicalAverage = historicalAverage;
    }

    public String getTrend() {
        return trend;
    }

    public void setTrend(String trend) {
        this.trend = trend;
    }

    public BigDecimal getRecommendedOrderQty() {
        return recommendedOrderQty;
    }

    public void setRecommendedOrderQty(BigDecimal recommendedOrderQty) {
        this.recommendedOrderQty = recommendedOrderQty;
    }

    public String getStockStatus() {
        return stockStatus;
    }

    public void setStockStatus(String stockStatus) {
        this.stockStatus = stockStatus;
    }

    public BigDecimal getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(BigDecimal currentStock) {
        this.currentStock = currentStock;
    }

    public Integer getDaysOfStock() {
        return daysOfStock;
    }

    public void setDaysOfStock(Integer daysOfStock) {
        this.daysOfStock = daysOfStock;
    }
}

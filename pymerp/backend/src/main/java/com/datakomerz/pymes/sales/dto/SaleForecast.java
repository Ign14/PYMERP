package com.datakomerz.pymes.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SaleForecast {
    private String productId;
    private String productName;
    private BigDecimal historicalAverage;
    private String trend; // "increasing", "stable", "decreasing"
    private BigDecimal forecastedDemand;
    private BigDecimal confidence; // 0-100
    private LocalDate nextSaleDate;
    private BigDecimal recommendedStock;
    private BigDecimal seasonalityFactor;

    public SaleForecast() {
    }

    public SaleForecast(String productId, String productName, BigDecimal historicalAverage,
                        String trend, BigDecimal forecastedDemand, BigDecimal confidence,
                        LocalDate nextSaleDate, BigDecimal recommendedStock, BigDecimal seasonalityFactor) {
        this.productId = productId;
        this.productName = productName;
        this.historicalAverage = historicalAverage;
        this.trend = trend;
        this.forecastedDemand = forecastedDemand;
        this.confidence = confidence;
        this.nextSaleDate = nextSaleDate;
        this.recommendedStock = recommendedStock;
        this.seasonalityFactor = seasonalityFactor;
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

    public BigDecimal getForecastedDemand() {
        return forecastedDemand;
    }

    public void setForecastedDemand(BigDecimal forecastedDemand) {
        this.forecastedDemand = forecastedDemand;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public LocalDate getNextSaleDate() {
        return nextSaleDate;
    }

    public void setNextSaleDate(LocalDate nextSaleDate) {
        this.nextSaleDate = nextSaleDate;
    }

    public BigDecimal getRecommendedStock() {
        return recommendedStock;
    }

    public void setRecommendedStock(BigDecimal recommendedStock) {
        this.recommendedStock = recommendedStock;
    }

    public BigDecimal getSeasonalityFactor() {
        return seasonalityFactor;
    }

    public void setSeasonalityFactor(BigDecimal seasonalityFactor) {
        this.seasonalityFactor = seasonalityFactor;
    }
}

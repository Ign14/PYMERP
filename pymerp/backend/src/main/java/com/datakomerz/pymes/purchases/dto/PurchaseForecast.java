package com.datakomerz.pymes.purchases.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pronóstico de demanda de compras por proveedor.
 * Utiliza media móvil y análisis de tendencia para predecir gastos futuros.
 */
public class PurchaseForecast {
    
    private String supplierId;
    private String supplierName;
    private BigDecimal historicalAverage; // Promedio de gasto mensual histórico
    private String trend; // "increasing", "stable", "decreasing"
    private BigDecimal forecastedSpending; // Gasto previsto para próximo mes
    private BigDecimal confidence; // Nivel de confianza 0-100
    private LocalDate nextPurchaseDate; // Fecha estimada de próxima compra
    private BigDecimal recommendedOrderQuantity; // Cantidad sugerida basada en histórico
    private BigDecimal seasonalityFactor; // Factor estacional si aplica

    public PurchaseForecast() {
    }

    public PurchaseForecast(
            String supplierId,
            String supplierName,
            BigDecimal historicalAverage,
            String trend,
            BigDecimal forecastedSpending,
            BigDecimal confidence,
            LocalDate nextPurchaseDate,
            BigDecimal recommendedOrderQuantity,
            BigDecimal seasonalityFactor) {
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.historicalAverage = historicalAverage;
        this.trend = trend;
        this.forecastedSpending = forecastedSpending;
        this.confidence = confidence;
        this.nextPurchaseDate = nextPurchaseDate;
        this.recommendedOrderQuantity = recommendedOrderQuantity;
        this.seasonalityFactor = seasonalityFactor;
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

    public BigDecimal getForecastedSpending() {
        return forecastedSpending;
    }

    public void setForecastedSpending(BigDecimal forecastedSpending) {
        this.forecastedSpending = forecastedSpending;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public LocalDate getNextPurchaseDate() {
        return nextPurchaseDate;
    }

    public void setNextPurchaseDate(LocalDate nextPurchaseDate) {
        this.nextPurchaseDate = nextPurchaseDate;
    }

    public BigDecimal getRecommendedOrderQuantity() {
        return recommendedOrderQuantity;
    }

    public void setRecommendedOrderQuantity(BigDecimal recommendedOrderQuantity) {
        this.recommendedOrderQuantity = recommendedOrderQuantity;
    }

    public BigDecimal getSeasonalityFactor() {
        return seasonalityFactor;
    }

    public void setSeasonalityFactor(BigDecimal seasonalityFactor) {
        this.seasonalityFactor = seasonalityFactor;
    }
}

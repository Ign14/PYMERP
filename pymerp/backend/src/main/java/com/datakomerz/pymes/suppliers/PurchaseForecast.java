package com.datakomerz.pymes.suppliers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class PurchaseForecast {
  private UUID supplierId;
  private String supplierName;
  private List<MonthlyForecast> monthlyForecasts;
  private BigDecimal averageMonthlySpend;
  private BigDecimal projectedNextMonthSpend;
  private Long averageMonthlyOrders;
  private String trend; // "INCREASING", "DECREASING", "STABLE"
  private String recommendation;

  public static class MonthlyForecast {
    private String month; // "2025-11"
    private LocalDate monthDate;
    private BigDecimal actualSpend;
    private BigDecimal forecastSpend;
    private Long actualOrders;
    private Long forecastOrders;
    private boolean isForecast; // true si es proyección futura

    public MonthlyForecast() {}

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public LocalDate getMonthDate() { return monthDate; }
    public void setMonthDate(LocalDate monthDate) { this.monthDate = monthDate; }

    public BigDecimal getActualSpend() { return actualSpend; }
    public void setActualSpend(BigDecimal actualSpend) { this.actualSpend = actualSpend; }

    public BigDecimal getForecastSpend() { return forecastSpend; }
    public void setForecastSpend(BigDecimal forecastSpend) { this.forecastSpend = forecastSpend; }

    public Long getActualOrders() { return actualOrders; }
    public void setActualOrders(Long actualOrders) { this.actualOrders = actualOrders; }

    public Long getForecastOrders() { return forecastOrders; }
    public void setForecastOrders(Long forecastOrders) { this.forecastOrders = forecastOrders; }

    public boolean isForecast() { return isForecast; }
    public void setForecast(boolean forecast) { isForecast = forecast; }
  }

  public PurchaseForecast() {}

  public UUID getSupplierId() { return supplierId; }
  public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }

  public String getSupplierName() { return supplierName; }
  public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

  public List<MonthlyForecast> getMonthlyForecasts() { return monthlyForecasts; }
  public void setMonthlyForecasts(List<MonthlyForecast> monthlyForecasts) { 
    this.monthlyForecasts = monthlyForecasts; 
  }

  public BigDecimal getAverageMonthlySpend() { return averageMonthlySpend; }
  public void setAverageMonthlySpend(BigDecimal averageMonthlySpend) { 
    this.averageMonthlySpend = averageMonthlySpend; 
  }

  public BigDecimal getProjectedNextMonthSpend() { return projectedNextMonthSpend; }
  public void setProjectedNextMonthSpend(BigDecimal projectedNextMonthSpend) { 
    this.projectedNextMonthSpend = projectedNextMonthSpend; 
  }

  public Long getAverageMonthlyOrders() { return averageMonthlyOrders; }
  public void setAverageMonthlyOrders(Long averageMonthlyOrders) { 
    this.averageMonthlyOrders = averageMonthlyOrders; 
  }

  public String getTrend() { return trend; }
  public void setTrend(String trend) { this.trend = trend; }

  public String getRecommendation() { return recommendation; }
  public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
}

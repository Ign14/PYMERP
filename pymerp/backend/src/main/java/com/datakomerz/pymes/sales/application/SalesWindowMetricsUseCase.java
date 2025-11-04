package com.datakomerz.pymes.sales.application;

import com.datakomerz.pymes.sales.SalesService;
import com.datakomerz.pymes.sales.dto.SalesDailyPoint;
import com.datakomerz.pymes.sales.dto.SalesWindowMetrics;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SalesWindowMetricsUseCase {

  private final SalesService service;

  public SalesWindowMetricsUseCase(SalesService service) {
    this.service = service;
  }

  public SalesWindowMetrics handle(String window) {
    // Parse window string (e.g., "14d" -> 14 days)
    int days = parseWindowToDays(window);

    // Get daily metrics
    List<SalesDailyPoint> dailyPoints = service.dailyMetrics(days);

    // Calculate aggregated metrics
    BigDecimal totalWithTax = dailyPoints.stream()
        .map(SalesDailyPoint::total)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    long documentCount = dailyPoints.stream()
        .mapToLong(SalesDailyPoint::count)
        .sum();

    BigDecimal dailyAverage = dailyPoints.isEmpty()
        ? BigDecimal.ZERO
        : totalWithTax.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);

    return new SalesWindowMetrics(window, totalWithTax, dailyAverage, documentCount);
  }

  private int parseWindowToDays(String window) {
    if (window == null || window.trim().isEmpty()) {
      return 14; // default
    }

    String trimmed = window.trim().toLowerCase();
    if (trimmed.endsWith("d")) {
      try {
        return Integer.parseInt(trimmed.substring(0, trimmed.length() - 1));
      } catch (NumberFormatException e) {
        return 14; // default on parse error
      }
    }

    // If no suffix, assume it's days
    try {
      return Integer.parseInt(trimmed);
    } catch (NumberFormatException e) {
      return 14; // default on parse error
    }
  }
}

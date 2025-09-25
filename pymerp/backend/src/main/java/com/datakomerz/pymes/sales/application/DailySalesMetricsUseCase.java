package com.datakomerz.pymes.sales.application;

import com.datakomerz.pymes.sales.SalesService;
import com.datakomerz.pymes.sales.dto.SalesDailyPoint;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DailySalesMetricsUseCase {

  private final SalesService service;

  public DailySalesMetricsUseCase(SalesService service) {
    this.service = service;
  }

  public List<SalesDailyPoint> handle(int days) {
    return service.dailyMetrics(days);
  }
}

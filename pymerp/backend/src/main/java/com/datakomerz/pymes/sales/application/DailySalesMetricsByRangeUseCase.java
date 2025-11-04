package com.datakomerz.pymes.sales.application;

import com.datakomerz.pymes.sales.SalesService;
import com.datakomerz.pymes.sales.dto.SalesDailyPoint;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DailySalesMetricsByRangeUseCase {

  private final SalesService service;

  public DailySalesMetricsByRangeUseCase(SalesService service) {
    this.service = service;
  }

  public List<SalesDailyPoint> handle(LocalDate from, LocalDate to) {
    return service.dailyMetricsByRange(from, to);
  }
}

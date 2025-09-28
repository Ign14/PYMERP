package com.datakomerz.pymes.sales.api;

import com.datakomerz.pymes.sales.reports.SalesReportService;
import com.datakomerz.pymes.sales.reports.SalesSummaryReport;
import com.datakomerz.pymes.sales.reports.SalesTimeseriesPoint;
import com.datakomerz.pymes.sales.reports.SalesTimeseriesResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/reports/sales")
public class SalesReportController {

  private final SalesReportService service;

  public SalesReportController(SalesReportService service) {
    this.service = service;
  }

  @GetMapping("/summary")
  public SalesSummaryReport summary(@RequestParam(defaultValue = "14") int days) {
    if (days <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "days must be greater than zero");
    }
    return service.getSummary(days);
  }

  @GetMapping("/timeseries")
  public SalesTimeseriesResponse timeseries(@RequestParam(defaultValue = "14") int days,
                                            @RequestParam(defaultValue = "day") String bucket) {
    if (days <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "days must be greater than zero");
    }
    if (!"day".equalsIgnoreCase(bucket)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only day bucket is supported");
    }

    List<SalesTimeseriesPoint> points = service.getDailySeries(days);
    return new SalesTimeseriesResponse(points);
  }
}

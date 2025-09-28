package com.datakomerz.pymes.sales.reports;

import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.datakomerz.pymes.sales.Sale;
import com.datakomerz.pymes.sales.SaleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SalesReportService {

  private final SaleRepository saleRepository;
  private final CompanyContext companyContext;
  private final Clock clock;

  public SalesReportService(SaleRepository saleRepository,
                            CompanyContext companyContext,
                            Clock clock) {
    this.saleRepository = saleRepository;
    this.companyContext = companyContext;
    this.clock = clock;
  }

  public SalesSummaryReport getSummary(int days) {
    SeriesResult result = buildSeries(days);
    BigDecimal total = result.total();
    BigDecimal average = days <= 0
      ? BigDecimal.ZERO
      : total.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
    return new SalesSummaryReport(total, average);
  }

  public List<SalesTimeseriesPoint> getDailySeries(int days) {
    return buildSeries(days).points();
  }

  private SeriesResult buildSeries(int days) {
    if (days <= 0) {
      throw new IllegalArgumentException("days must be greater than zero");
    }

    UUID companyId = companyContext.require();
    ZoneId zone = clock.getZone();
    LocalDate end = LocalDate.now(clock);
    LocalDate start = end.minusDays(days - 1L);
    OffsetDateTime from = start.atStartOfDay(zone).toOffsetDateTime();
    OffsetDateTime toExclusive = end.plusDays(1L).atStartOfDay(zone).toOffsetDateTime();

    List<Sale> range = saleRepository
      .findByCompanyIdAndIssuedAtGreaterThanEqualOrderByIssuedAtAsc(companyId, from);

    Map<LocalDate, BigDecimal> totals = new LinkedHashMap<>();
    LocalDate cursor = start;
    while (!cursor.isAfter(end)) {
      totals.put(cursor, BigDecimal.ZERO);
      cursor = cursor.plusDays(1);
    }

    for (Sale sale : range) {
      if (sale.getIssuedAt() == null) {
        continue;
      }
      if (!sale.getIssuedAt().isBefore(toExclusive)) {
        continue;
      }
      if (isCancelled(sale.getStatus())) {
        continue;
      }

      LocalDate day = sale.getIssuedAt().atZoneSameInstant(zone).toLocalDate();
      if (day.isBefore(start) || day.isAfter(end)) {
        continue;
      }

      totals.merge(day, safeAmount(sale.getTotal()), BigDecimal::add);
    }

    List<SalesTimeseriesPoint> points = new ArrayList<>(totals.size());
    totals.forEach((date, total) -> points.add(new SalesTimeseriesPoint(date, total)));

    BigDecimal total = points.stream()
      .map(SalesTimeseriesPoint::total)
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    return new SeriesResult(points, total);
  }

  private boolean isCancelled(String status) {
    return status != null && status.equalsIgnoreCase("cancelled");
  }

  private BigDecimal safeAmount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private record SeriesResult(List<SalesTimeseriesPoint> points, BigDecimal total) {
  }
}

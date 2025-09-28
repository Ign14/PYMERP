package com.datakomerz.pymes.sales;

import static org.assertj.core.api.Assertions.assertThat;

import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.datakomerz.pymes.sales.reports.SalesReportService;
import com.datakomerz.pymes.sales.reports.SalesSummaryReport;
import com.datakomerz.pymes.sales.reports.SalesTimeseriesPoint;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class SalesReportServiceTest {

  @Autowired
  private SaleRepository saleRepository;

  private SalesReportService service;
  private CompanyContext companyContext;
  private UUID companyId;

  @BeforeEach
  void setUp() {
    companyId = UUID.randomUUID();
    companyContext = Mockito.mock(CompanyContext.class);
    Mockito.when(companyContext.require()).thenReturn(companyId);
    Clock clock = Clock.fixed(Instant.parse("2024-05-15T12:00:00Z"), ZoneOffset.UTC);
    service = new SalesReportService(saleRepository, companyContext, clock);
  }

  @Test
  void returnsZerosWhenNoSales() {
    SalesSummaryReport summary = service.getSummary(14);
    assertThat(summary.total14d()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(summary.avgDaily14d()).isEqualByComparingTo(BigDecimal.ZERO);

    List<SalesTimeseriesPoint> series = service.getDailySeries(14);
    assertThat(series).hasSize(14);
    assertThat(series).allSatisfy(point -> assertThat(point.total()).isEqualByComparingTo(BigDecimal.ZERO));
    assertThat(series.getFirst().date()).isEqualTo(LocalDate.of(2024, 5, 2));
    assertThat(series.getLast().date()).isEqualTo(LocalDate.of(2024, 5, 15));
  }

  @Test
  void fillsMissingDaysWithZeros() {
    persistSale(LocalDate.of(2024, 5, 10), new BigDecimal("100"), "emitida", companyId);
    persistSale(LocalDate.of(2024, 5, 15), new BigDecimal("200"), "emitida", companyId);

    List<SalesTimeseriesPoint> series = service.getDailySeries(14);
    assertThat(series).hasSize(14);

    SalesTimeseriesPoint tenth = findPoint(series, LocalDate.of(2024, 5, 10));
    assertThat(tenth.total()).isEqualByComparingTo(new BigDecimal("100"));

    SalesTimeseriesPoint eleventh = findPoint(series, LocalDate.of(2024, 5, 11));
    assertThat(eleventh.total()).isEqualByComparingTo(BigDecimal.ZERO);

    SalesTimeseriesPoint last = findPoint(series, LocalDate.of(2024, 5, 15));
    assertThat(last.total()).isEqualByComparingTo(new BigDecimal("200"));
  }

  @Test
  void excludesCancelledSalesAndOtherCompanies() {
    persistSale(LocalDate.of(2024, 5, 12), new BigDecimal("120"), "cancelled", companyId);
    persistSale(LocalDate.of(2024, 5, 13), new BigDecimal("450"), "emitida", companyId);
    persistSale(LocalDate.of(2024, 5, 14), new BigDecimal("300"), "emitida", UUID.randomUUID());
    persistSale(LocalDate.of(2024, 4, 28), new BigDecimal("500"), "emitida", companyId);

    SalesSummaryReport summary = service.getSummary(14);
    assertThat(summary.total14d()).isEqualByComparingTo(new BigDecimal("450"));
    assertThat(summary.avgDaily14d()).isEqualByComparingTo(new BigDecimal("32.14"));

    List<SalesTimeseriesPoint> series = service.getDailySeries(14);
    SalesTimeseriesPoint cancelledDay = findPoint(series, LocalDate.of(2024, 5, 12));
    assertThat(cancelledDay.total()).isEqualByComparingTo(BigDecimal.ZERO);

    SalesTimeseriesPoint validDay = findPoint(series, LocalDate.of(2024, 5, 13));
    assertThat(validDay.total()).isEqualByComparingTo(new BigDecimal("450"));
  }

  private SalesTimeseriesPoint findPoint(List<SalesTimeseriesPoint> series, LocalDate date) {
    return series.stream()
      .filter(point -> point.date().equals(date))
      .findFirst()
      .orElseThrow(() -> new AssertionError("Point not found for " + date));
  }

  private void persistSale(LocalDate date, BigDecimal total, String status, UUID owner) {
    Sale sale = new Sale();
    sale.setCompanyId(owner);
    sale.setStatus(status);
    sale.setNet(total);
    sale.setVat(BigDecimal.ZERO);
    sale.setTotal(total);
    sale.setIssuedAt(OffsetDateTime.of(date.atStartOfDay(), ZoneOffset.UTC));
    saleRepository.save(sale);
  }
}

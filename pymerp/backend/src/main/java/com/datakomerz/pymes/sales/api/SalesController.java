package com.datakomerz.pymes.sales.api;

import com.datakomerz.pymes.multitenancy.ValidateTenant;
import com.datakomerz.pymes.sales.Sale;
import com.datakomerz.pymes.sales.application.CancelSaleUseCase;
import com.datakomerz.pymes.sales.application.CreateSaleUseCase;
import com.datakomerz.pymes.sales.application.DailySalesMetricsUseCase;
import com.datakomerz.pymes.sales.application.DailySalesMetricsByRangeUseCase;
import com.datakomerz.pymes.sales.application.GetSaleDetailUseCase;
import com.datakomerz.pymes.sales.application.ListSalesUseCase;
import com.datakomerz.pymes.sales.application.SalesWindowMetricsUseCase;
import com.datakomerz.pymes.sales.application.UpdateSaleUseCase;
import com.datakomerz.pymes.sales.dto.SaleDetail;
import com.datakomerz.pymes.sales.dto.SaleReq;
import com.datakomerz.pymes.sales.dto.SaleRes;
import com.datakomerz.pymes.sales.dto.SaleSummary;
import com.datakomerz.pymes.sales.dto.SaleUpdateRequest;
import com.datakomerz.pymes.sales.dto.SalesDailyPoint;
import com.datakomerz.pymes.sales.dto.SalesWindowMetrics;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sales")
public class SalesController {

  private final CreateSaleUseCase createSaleUseCase;
  private final UpdateSaleUseCase updateSaleUseCase;
  private final CancelSaleUseCase cancelSaleUseCase;
  private final ListSalesUseCase listSalesUseCase;
  private final GetSaleDetailUseCase getSaleDetailUseCase;
  private final DailySalesMetricsUseCase dailySalesMetricsUseCase;
  private final DailySalesMetricsByRangeUseCase dailySalesMetricsByRangeUseCase;
  private final SalesWindowMetricsUseCase salesWindowMetricsUseCase;
  private final com.datakomerz.pymes.sales.SalesService salesService;

  public SalesController(CreateSaleUseCase createSaleUseCase,
                         UpdateSaleUseCase updateSaleUseCase,
                         CancelSaleUseCase cancelSaleUseCase,
                         ListSalesUseCase listSalesUseCase,
                         GetSaleDetailUseCase getSaleDetailUseCase,
                         DailySalesMetricsUseCase dailySalesMetricsUseCase,
                         DailySalesMetricsByRangeUseCase dailySalesMetricsByRangeUseCase,
                         SalesWindowMetricsUseCase salesWindowMetricsUseCase,
                         com.datakomerz.pymes.sales.SalesService salesService) {
    this.createSaleUseCase = createSaleUseCase;
    this.updateSaleUseCase = updateSaleUseCase;
    this.cancelSaleUseCase = cancelSaleUseCase;
    this.listSalesUseCase = listSalesUseCase;
    this.getSaleDetailUseCase = getSaleDetailUseCase;
    this.dailySalesMetricsUseCase = dailySalesMetricsUseCase;
    this.dailySalesMetricsByRangeUseCase = dailySalesMetricsByRangeUseCase;
    this.salesWindowMetricsUseCase = salesWindowMetricsUseCase;
    this.salesService = salesService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ERP_USER', 'ADMIN')")
  public SaleRes create(@Valid @RequestBody SaleReq req) {
    return createSaleUseCase.handle(req);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ERP_USER', 'ADMIN')")
  @ValidateTenant(entityClass = Sale.class)
  public SaleRes update(@PathVariable UUID id, @RequestBody SaleUpdateRequest req) {
    return updateSaleUseCase.handle(id, req);
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasRole('ADMIN')")
  @ValidateTenant(entityClass = Sale.class)
  public SaleRes cancel(@PathVariable UUID id) {
    return cancelSaleUseCase.handle(id);
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public Page<SaleSummary> list(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(required = false) String status,
                                @RequestParam(required = false) String docType,
                                @RequestParam(required = false) String paymentMethod,
                                @RequestParam(required = false) String search,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                OffsetDateTime from,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                OffsetDateTime to) {
    return listSalesUseCase.handle(status, docType, paymentMethod, search, from, to, page, size);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  @ValidateTenant(entityClass = Sale.class)
  public SaleDetail detail(@PathVariable UUID id) {
    return getSaleDetailUseCase.handle(id);
  }

  @GetMapping("/metrics")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public SalesWindowMetrics windowMetrics(@RequestParam(defaultValue = "14d") String window) {
    return salesWindowMetricsUseCase.handle(window);
  }

  @GetMapping("/metrics/daily")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public List<SalesDailyPoint> metrics(@RequestParam(defaultValue = "14") int days) {
    return dailySalesMetricsUseCase.handle(days);
  }

  @GetMapping("/metrics/daily-range")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public List<SalesDailyPoint> metricsByRange(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return dailySalesMetricsByRangeUseCase.handle(from, to);
  }

  @GetMapping("/kpis")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public com.datakomerz.pymes.sales.dto.SalesKPIs salesKPIs(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    
    LocalDate startDate = from != null ? from : LocalDate.now().minusDays(30);
    LocalDate endDate = to != null ? to : LocalDate.now();
    
    return salesService.getSalesKPIs(startDate, endDate);
  }

  @GetMapping("/abc-analysis")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public List<com.datakomerz.pymes.sales.dto.SaleABCClassification> salesABCAnalysis(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    
    LocalDate startDate = from != null ? from : LocalDate.now().minusDays(90);
    LocalDate endDate = to != null ? to : LocalDate.now();
    
    return salesService.getSalesABCAnalysis(startDate, endDate);
  }

  @GetMapping("/forecast")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public List<com.datakomerz.pymes.sales.dto.SaleForecast> salesForecast(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false, defaultValue = "30") int horizonDays) {
    
    LocalDate startDate = from != null ? from : LocalDate.now().minusDays(90);
    LocalDate endDate = to != null ? to : LocalDate.now();
    
    return salesService.getSalesForecast(startDate, endDate, horizonDays);
  }

  @GetMapping("/export")
  @PreAuthorize("hasAnyRole('ERP_USER', 'ADMIN')")
  public void exportToCSV(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String docType,
      @RequestParam(required = false) String paymentMethod,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
      HttpServletResponse response) throws IOException {

    // Obtener todas las ventas sin paginación (límite de 10000)
    Page<SaleSummary> salesPage = listSalesUseCase.handle(status, docType, paymentMethod, search, from, to, 0, 10000);
    List<SaleSummary> sales = salesPage.getContent();

    response.setContentType("text/csv; charset=UTF-8");
    response.setHeader("Content-Disposition", "attachment; filename=\"ventas.csv\"");

    try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
      // UTF-8 BOM para compatibilidad con Excel
      writer.write('\uFEFF');

      // Headers
      writer.println("ID,Documento,Cliente,Método de Pago,Estado,Neto,IVA,Total,Fecha Emisión");

      // Formato de fecha
      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

      // Datos
      for (SaleSummary sale : sales) {
        writer.print(escapeCsv(sale.id().toString()));
        writer.print(',');
        writer.print(escapeCsv(sale.docType() != null ? sale.docType() : ""));
        writer.print(',');
        writer.print(escapeCsv(sale.customerName() != null ? sale.customerName() : ""));
        writer.print(',');
        writer.print(escapeCsv(sale.paymentMethod() != null ? sale.paymentMethod() : ""));
        writer.print(',');
        writer.print(escapeCsv(sale.status() != null ? sale.status() : ""));
        writer.print(',');
        writer.print(sale.net() != null ? sale.net().toString() : "0");
        writer.print(',');
        writer.print(sale.vat() != null ? sale.vat().toString() : "0");
        writer.print(',');
        writer.print(sale.total() != null ? sale.total().toString() : "0");
        writer.print(',');
        writer.print(sale.issuedAt() != null ? dateFormatter.format(sale.issuedAt()) : "");
        writer.println();
      }
    }
  }

  private String escapeCsv(String value) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}

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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Sales", description = "Gestión de ventas y facturación")
@RestController
@RequestMapping("/api/v1/sales")
@SecurityRequirement(name = "bearerAuth")
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

  @Operation(
    summary = "Crear venta",
    description = "Registra una nueva venta con sus ítems, impuestos y método de pago."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Venta creada", content = @Content(schema = @Schema(implementation = SaleRes.class))),
    @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ERP_USER', 'ADMIN')")
  public SaleRes create(@Valid @RequestBody SaleReq req) {
    return createSaleUseCase.handle(req);
  }

  @Operation(
    summary = "Actualizar venta",
    description = "Modifica montos, estados o metadatos de una venta existente."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Venta actualizada", content = @Content(schema = @Schema(implementation = SaleRes.class))),
    @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos"),
    @ApiResponse(responseCode = "404", description = "Venta no encontrada")
  })
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ERP_USER', 'ADMIN')")
  @ValidateTenant(entityClass = Sale.class)
  public SaleRes update(
      @Parameter(description = "ID de la venta a actualizar", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
      @PathVariable UUID id,
      @RequestBody SaleUpdateRequest req) {
    return updateSaleUseCase.handle(id, req);
  }

  @Operation(
    summary = "Anular venta",
    description = "Cancela una venta emitida y revierte su impacto financiero."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Venta anulada", content = @Content(schema = @Schema(implementation = SaleRes.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos"),
    @ApiResponse(responseCode = "404", description = "Venta no encontrada")
  })
  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasRole('ADMIN')")
  @ValidateTenant(entityClass = Sale.class)
  public SaleRes cancel(
      @Parameter(description = "ID de la venta a anular", required = true)
      @PathVariable UUID id) {
    return cancelSaleUseCase.handle(id);
  }

  @Operation(
    summary = "Listar ventas",
    description = "Retorna las ventas de la empresa con filtros por estado, documento, método de pago y rango de fechas."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Página de ventas", content = @Content(schema = @Schema(implementation = Page.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public Page<SaleSummary> list(
                                @Parameter(description = "Número de página (0-index)", example = "0")
                                @RequestParam(defaultValue = "0") int page,
                                @Parameter(description = "Tamaño de página", example = "10")
                                @RequestParam(defaultValue = "10") int size,
                                @Parameter(description = "Estado del documento (Ej: COMPLETED, DRAFT)", example = "COMPLETED")
                                @RequestParam(required = false) String status,
                                @Parameter(description = "Tipo de documento (Ej: FACTURA, BOLETA)", example = "FACTURA")
                                @RequestParam(required = false) String docType,
                                @Parameter(description = "Método de pago (Ej: TRANSFER, CASH)", example = "TRANSFER")
                                @RequestParam(required = false) String paymentMethod,
                                @Parameter(description = "Texto para buscar por cliente o folio", example = "Acme Ltda.")
                                @RequestParam(required = false) String search,
                                @Parameter(description = "Fecha inicial en ISO 8601", example = "2024-01-01T00:00:00Z")
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                OffsetDateTime from,
                                @Parameter(description = "Fecha final en ISO 8601", example = "2024-01-31T23:59:59Z")
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                OffsetDateTime to) {
    return listSalesUseCase.handle(status, docType, paymentMethod, search, from, to, page, size);
  }

  @Operation(
    summary = "Obtener venta por ID",
    description = "Devuelve el detalle completo de una venta específica."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Venta encontrada", content = @Content(schema = @Schema(implementation = SaleDetail.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos"),
    @ApiResponse(responseCode = "404", description = "Venta no encontrada")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  @ValidateTenant(entityClass = Sale.class)
  public SaleDetail detail(
      @Parameter(description = "ID de la venta", required = true)
      @PathVariable UUID id) {
    return getSaleDetailUseCase.handle(id);
  }

  @Operation(
    summary = "KPIs de ventana móvil",
    description = "Calcula métricas agregadas de ventas en una ventana móvil (por ejemplo 14 días)."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Métricas calculadas", content = @Content(schema = @Schema(implementation = SalesWindowMetrics.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/metrics")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public SalesWindowMetrics windowMetrics(
      @Parameter(description = "Ventana de tiempo (ej: 7d, 14d, 30d)", example = "14d")
      @RequestParam(defaultValue = "14d") String window) {
    return salesWindowMetricsUseCase.handle(window);
  }

  @Operation(
    summary = "Tendencia diaria",
    description = "Devuelve puntos diarios de ventas para los últimos N días."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lista de puntos diarios"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/metrics/daily")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public List<SalesDailyPoint> metrics(
      @Parameter(description = "Cantidad de días a retroceder", example = "14")
      @RequestParam(defaultValue = "14") int days) {
    return dailySalesMetricsUseCase.handle(days);
  }

  @Operation(
    summary = "Tendencia diaria por rango",
    description = "Calcula las ventas diarias exactas entre dos fechas."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Lista de puntos diarios"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/metrics/daily-range")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public List<SalesDailyPoint> metricsByRange(
      @Parameter(description = "Fecha de inicio", example = "2024-01-01")
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @Parameter(description = "Fecha de término", example = "2024-01-31")
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return dailySalesMetricsByRangeUseCase.handle(from, to);
  }

  @Operation(
    summary = "KPIs de ventas",
    description = "Entrega indicadores clave (tickets promedio, crecimiento, etc.) dentro del rango indicado."
  )
  @ApiResponses({
    @ApiResponse(
      responseCode = "200",
      description = "KPIs calculados",
      content = @Content(schema = @Schema(implementation = com.datakomerz.pymes.sales.dto.SalesKPIs.class))
    ),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/kpis")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public com.datakomerz.pymes.sales.dto.SalesKPIs salesKPIs(
      @Parameter(description = "Fecha inicial (inclusive)", example = "2024-04-01")
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @Parameter(description = "Fecha final (inclusive)", example = "2024-04-30")
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

    LocalDate startDate = from != null ? from : LocalDate.now().minusDays(30);
    LocalDate endDate = to != null ? to : LocalDate.now();

    return salesService.getSalesKPIs(startDate, endDate);
  }

  @Operation(
    summary = "Análisis ABC de ventas",
    description = "Clasifica los productos según su contribución a la facturación en el rango solicitado."
  )
  @ApiResponses({
    @ApiResponse(
      responseCode = "200",
      description = "Clasificación generada",
      content = @Content(array = @ArraySchema(schema = @Schema(implementation = com.datakomerz.pymes.sales.dto.SaleABCClassification.class)))
    ),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/abc-analysis")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public List<com.datakomerz.pymes.sales.dto.SaleABCClassification> salesABCAnalysis(
      @Parameter(description = "Fecha inicial del análisis", example = "2024-03-01")
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @Parameter(description = "Fecha final del análisis", example = "2024-05-31")
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

    LocalDate startDate = from != null ? from : LocalDate.now().minusDays(90);
    LocalDate endDate = to != null ? to : LocalDate.now();

    return salesService.getSalesABCAnalysis(startDate, endDate);
  }

  @Operation(
    summary = "Pronóstico de ventas",
    description = "Genera una proyección de ventas para los próximos días con base en el histórico."
  )
  @ApiResponses({
    @ApiResponse(
      responseCode = "200",
      description = "Pronóstico generado",
      content = @Content(array = @ArraySchema(schema = @Schema(implementation = com.datakomerz.pymes.sales.dto.SaleForecast.class)))
    ),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/forecast")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public List<com.datakomerz.pymes.sales.dto.SaleForecast> salesForecast(
      @Parameter(description = "Fecha inicial usada para el histórico", example = "2024-01-01")
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @Parameter(description = "Fecha final usada para el histórico", example = "2024-04-01")
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @Parameter(description = "Cantidad de días a proyectar", example = "30")
      @RequestParam(required = false, defaultValue = "30") int horizonDays) {

    LocalDate startDate = from != null ? from : LocalDate.now().minusDays(90);
    LocalDate endDate = to != null ? to : LocalDate.now();

    return salesService.getSalesForecast(startDate, endDate, horizonDays);
  }

  @Operation(
    summary = "Exportar ventas a CSV",
    description = "Genera un archivo CSV con hasta 10.000 ventas según los mismos filtros de la lista."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Archivo CSV generado", content = @Content(mediaType = "text/csv")),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/export")
  @PreAuthorize("hasAnyRole('ERP_USER', 'ADMIN')")
  public void exportToCSV(
      @Parameter(description = "Estado del documento", example = "COMPLETED")
      @RequestParam(required = false) String status,
      @Parameter(description = "Tipo de documento", example = "FACTURA")
      @RequestParam(required = false) String docType,
      @Parameter(description = "Método de pago", example = "CREDIT_CARD")
      @RequestParam(required = false) String paymentMethod,
      @Parameter(description = "Texto libre de búsqueda")
      @RequestParam(required = false) String search,
      @Parameter(description = "Fecha inicial en ISO 8601")
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
      @Parameter(description = "Fecha final en ISO 8601")
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
      @Parameter(hidden = true)
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

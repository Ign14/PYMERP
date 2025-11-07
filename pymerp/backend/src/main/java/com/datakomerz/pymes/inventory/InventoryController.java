package com.datakomerz.pymes.inventory;

import com.datakomerz.pymes.inventory.dto.InventoryAdjustmentRequest;
import com.datakomerz.pymes.inventory.dto.InventoryAdjustmentResponse;
import com.datakomerz.pymes.inventory.dto.InventoryAlert;
import com.datakomerz.pymes.inventory.dto.InventoryKPIs;
import com.datakomerz.pymes.inventory.dto.InventoryMovementSummary;
import com.datakomerz.pymes.inventory.dto.InventorySettingsResponse;
import com.datakomerz.pymes.inventory.dto.InventorySettingsUpdateRequest;
import com.datakomerz.pymes.inventory.dto.InventorySummary;
import com.datakomerz.pymes.inventory.dto.ProductABCClassification;
import com.datakomerz.pymes.inventory.dto.StockMovementStats;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

@Tag(name = "Inventory", description = "Control de inventario y stock")
@RestController
@RequestMapping("/api/v1/inventory")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

  private final InventoryService inventoryService;

  public InventoryController(InventoryService inventoryService) {
    this.inventoryService = inventoryService;
  }

  @Operation(
    summary = "Alertas de stock",
    description = "Lista los productos con stock crítico por debajo del umbral configurado."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Alertas listadas", content = @Content(array = @ArraySchema(schema = @Schema(implementation = InventoryAlert.class)))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/alerts")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public List<InventoryAlert> lowStock(
      @Parameter(description = "Umbral personalizado para filtrar", example = "10")
      @RequestParam(required = false) BigDecimal threshold) {
    return inventoryService.lowStock(threshold);
  }

  @Operation(
    summary = "Resumen de inventario",
    description = "Devuelve totales agregados de stock y valoración."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Resumen generado", content = @Content(schema = @Schema(implementation = InventorySummary.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/summary")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public InventorySummary summary() {
    return inventoryService.summary();
  }

  @Operation(
    summary = "Obtener ajustes de inventario",
    description = "Recupera las configuraciones globales de inventario."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Configuración actual", content = @Content(schema = @Schema(implementation = InventorySettingsResponse.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/settings")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public InventorySettingsResponse settings() {
    return inventoryService.getSettings();
  }

  @Operation(
    summary = "Actualizar ajustes de inventario",
    description = "Permite modificar umbrales, estrategias y parámetros globales."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Configuración actualizada", content = @Content(schema = @Schema(implementation = InventorySettingsResponse.class))),
    @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @PutMapping("/settings")
  @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
  public InventorySettingsResponse updateSettings(@Valid @RequestBody InventorySettingsUpdateRequest request) {
    return inventoryService.updateSettings(request);
  }

  @Operation(
    summary = "Registrar ajuste de inventario",
    description = "Crea un ajuste positivo o negativo sobre el stock."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Ajuste registrado", content = @Content(schema = @Schema(implementation = InventoryAdjustmentResponse.class))),
    @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @PostMapping("/adjustments")
  @PreAuthorize("hasAnyRole('ERP_USER', 'ADMIN')")
  public InventoryAdjustmentResponse adjust(@Valid @RequestBody InventoryAdjustmentRequest request) {
    return inventoryService.adjust(request);
  }

  @Operation(
    summary = "Listar movimientos de inventario",
    description = "Consulta paginada de movimientos filtrados por producto, tipo y fechas."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Movimientos encontrados", content = @Content(schema = @Schema(implementation = Page.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/movements")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public Page<InventoryMovementSummary> movements(
      @Parameter(description = "ID del producto") @RequestParam(required = false) UUID productId,
      @Parameter(description = "Tipo de movimiento (IN, OUT, ADJUSTMENT)", example = "IN") @RequestParam(required = false) String type,
      @Parameter(description = "Fecha inicial en ISO 8601") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
      @Parameter(description = "Fecha final en ISO 8601") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
      @Parameter(description = "Número de página", example = "0") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Tamaño de página", example = "20") @RequestParam(defaultValue = "20") int size) {
    return inventoryService.listMovements(productId, type, from, to, PageRequest.of(page, size));
  }

  @Operation(
    summary = "KPIs de inventario",
    description = "Retorna indicadores clave como rotación, cobertura y velocidad."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "KPIs generados", content = @Content(schema = @Schema(implementation = InventoryKPIs.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/kpis")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public InventoryKPIs kpis() {
    return inventoryService.getKPIs();
  }

  @Operation(
    summary = "Estadísticas de movimientos",
    description = "Muestra acumulados de entradas, salidas y ajustes por período."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Estadísticas generadas", content = @Content(schema = @Schema(implementation = StockMovementStats.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/movement-stats")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public StockMovementStats movementStats() {
    return inventoryService.getMovementStats();
  }

  @Operation(
    summary = "Análisis ABC de productos",
    description = "Clasifica los productos según su contribución al inventario."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Clasificación generada", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProductABCClassification.class)))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/abc-analysis")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public List<ProductABCClassification> abcAnalysis(
    @Parameter(description = "Filtrar por clasificación específica", example = "A")
    @RequestParam(required = false) String classification
  ) {
    List<ProductABCClassification> analysis = inventoryService.getABCAnalysis();
    
    // Filtrar por clasificación si se especifica
    if (classification != null && !classification.isEmpty()) {
      return analysis.stream()
        .filter(item -> classification.equalsIgnoreCase(item.getClassification()))
        .collect(Collectors.toList());
    }
    
    return analysis;
  }
  
  @Operation(
    summary = "Pronóstico de inventario",
    description = "Proyecta el comportamiento del stock para apoyar la planificación."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Pronóstico generado", content = @Content(array = @ArraySchema(schema = @Schema(implementation = com.datakomerz.pymes.inventory.dto.InventoryForecast.class)))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/forecast")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public List<com.datakomerz.pymes.inventory.dto.InventoryForecast> forecast(
    @Parameter(description = "Producto a proyectar") @RequestParam(required = false) Long productId,
    @Parameter(description = "Horizonte en días", example = "30") @RequestParam(required = false) Integer days
  ) {
    return inventoryService.getForecastAnalysis(productId, days);
  }
}

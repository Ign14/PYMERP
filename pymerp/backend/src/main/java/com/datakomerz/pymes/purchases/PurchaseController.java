package com.datakomerz.pymes.purchases;

import com.datakomerz.pymes.multitenancy.ValidateTenant;
import com.datakomerz.pymes.purchases.dto.PurchaseDailyPoint;
import com.datakomerz.pymes.purchases.dto.PurchaseReq;
import com.datakomerz.pymes.purchases.dto.PurchaseSummary;
import com.datakomerz.pymes.purchases.dto.PurchaseUpdateRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Purchases", description = "Gestión de compras y facturas proveedores")
@RestController
@RequestMapping("/api/v1/purchases")
@SecurityRequirement(name = "bearerAuth")
public class PurchaseController {
  private final PurchaseService service;
  public PurchaseController(PurchaseService service) { this.service = service; }

  @Operation(
    summary = "Registrar compra",
    description = "Crea una compra con sus montos y proveedor."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Compra creada"),
    @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ERP_USER', 'ADMIN')")
  public Map<String,Object> create(@Valid @RequestBody PurchaseReq req) {
    var id = service.create(req);
    return Map.of("id", id);
  }

  @Operation(
    summary = "Registrar compra con archivo",
    description = "Permite adjuntar el PDF o XML respaldatorio al crear la compra."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Compra creada"),
    @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ERP_USER', 'ADMIN')")
  public Map<String,Object> createWithFile(
      @Parameter(description = "Datos de la compra", required = true)
      @RequestPart("data") @Valid PurchaseReq req,
      @Parameter(
        description = "Archivo relacionado con la compra",
        content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(type = "string", format = "binary"))
      )
      @RequestPart(value = "file", required = false) MultipartFile file) {
    var id = service.createWithFile(req, file);
    return Map.of("id", id);
  }

  @Operation(
    summary = "Actualizar compra",
    description = "Permite modificar totales, estados u observaciones de una compra."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Compra actualizada", content = @Content(schema = @Schema(implementation = PurchaseSummary.class))),
    @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos"),
    @ApiResponse(responseCode = "404", description = "Compra no encontrada")
  })
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ERP_USER', 'ADMIN')")
  @ValidateTenant(entityClass = Purchase.class)
  public PurchaseSummary update(
      @Parameter(description = "ID de la compra", required = true)
      @PathVariable UUID id,
      @RequestBody PurchaseUpdateRequest req) {
    return service.update(id, req);
  }

  @Operation(
    summary = "Anular compra",
    description = "Cancela una compra registrada previamente."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Compra anulada", content = @Content(schema = @Schema(implementation = PurchaseSummary.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos"),
    @ApiResponse(responseCode = "404", description = "Compra no encontrada")
  })
  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasRole('ADMIN')")
  @ValidateTenant(entityClass = Purchase.class)
  public PurchaseSummary cancel(
      @Parameter(description = "ID de la compra", required = true)
      @PathVariable UUID id) {
    return service.cancel(id);
  }

  @Operation(
    summary = "Listar compras",
    description = "Retorna compras paginadas filtrando por estado, tipo de documento y fechas."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Compras encontradas", content = @Content(schema = @Schema(implementation = Page.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public Page<PurchaseSummary> list(
      @Parameter(description = "Número de página (0-index)", example = "0")
      @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Tamaño de página", example = "10")
      @RequestParam(defaultValue = "10") int size,
      @Parameter(description = "Estado del documento", example = "APPROVED")
      @RequestParam(required = false) String status,
      @Parameter(description = "Tipo de documento", example = "FACTURA")
      @RequestParam(required = false) String docType,
      @Parameter(description = "Texto de búsqueda por proveedor o folio")
      @RequestParam(required = false) String search,
      @Parameter(description = "Fecha inicial en ISO 8601")
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
      OffsetDateTime from,
      @Parameter(description = "Fecha final en ISO 8601")
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
      OffsetDateTime to) {
    return service.list(status, docType, search, from, to, PageRequest.of(page, size));
  }

  @Operation(
    summary = "Tendencia diaria de compras",
    description = "Entrega los montos diarios de compras para los últimos N días."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Serie generada", content = @Content(array = @ArraySchema(schema = @Schema(implementation = PurchaseDailyPoint.class)))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/metrics/daily")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public List<PurchaseDailyPoint> metrics(
      @Parameter(description = "Cantidad de días hacia atrás", example = "14")
      @RequestParam(defaultValue = "14") int days) {
    return service.dailyMetrics(days);
  }

  @Operation(
    summary = "KPIs de compras",
    description = "Devuelve indicadores clave de compras en un rango de fechas."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "KPIs calculados", content = @Content(schema = @Schema(implementation = com.datakomerz.pymes.purchases.dto.PurchaseKPIs.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/kpis")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public com.datakomerz.pymes.purchases.dto.PurchaseKPIs purchaseKPIs(
      @Parameter(description = "Fecha inicial (inclusive)", example = "2024-04-01")
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @Parameter(description = "Fecha final (inclusive)", example = "2024-04-30")
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    
    LocalDate startDate = from != null ? from : LocalDate.now().minusDays(30);
    LocalDate endDate = to != null ? to : LocalDate.now();
    
    return service.getPurchaseKPIs(startDate, endDate);
  }

  @Operation(
    summary = "Análisis ABC de compras",
    description = "Clasifica a los proveedores según su impacto en el gasto durante el rango indicado."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Clasificación generada", content = @Content(array = @ArraySchema(schema = @Schema(implementation = com.datakomerz.pymes.purchases.dto.PurchaseABCClassification.class)))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/abc-analysis")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public List<com.datakomerz.pymes.purchases.dto.PurchaseABCClassification> purchaseABCAnalysis(
      @Parameter(description = "Fecha inicial del análisis", example = "2024-01-01")
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @Parameter(description = "Fecha final del análisis", example = "2024-03-31")
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    
    LocalDate startDate = from != null ? from : LocalDate.now().minusDays(90);
    LocalDate endDate = to != null ? to : LocalDate.now();
    
    return service.getPurchaseABCAnalysis(startDate, endDate);
  }

  @Operation(
    summary = "Pronóstico de compras",
    description = "Proyecta las compras esperadas según el historial del rango seleccionado."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Pronóstico generado", content = @Content(array = @ArraySchema(schema = @Schema(implementation = com.datakomerz.pymes.purchases.dto.PurchaseForecast.class)))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/forecast")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public List<com.datakomerz.pymes.purchases.dto.PurchaseForecast> purchaseForecast(
      @Parameter(description = "Fecha inicial usada para el histórico", example = "2024-01-01")
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @Parameter(description = "Fecha final usada para el histórico", example = "2024-04-01")
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @Parameter(description = "Días a proyectar", example = "30")
      @RequestParam(required = false, defaultValue = "30") int horizonDays) {
    
    LocalDate startDate = from != null ? from : LocalDate.now().minusDays(90);
    LocalDate endDate = to != null ? to : LocalDate.now();
    
    return service.getPurchaseForecast(startDate, endDate, horizonDays);
  }

  @Operation(
    summary = "Exportar compras",
    description = "Genera un CSV con hasta 10.000 compras aplicando los mismos filtros de la lista."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "CSV generado", content = @Content(mediaType = "text/csv")),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/export")
  @PreAuthorize("hasAnyRole('ERP_USER', 'ADMIN')")
  public void exportToCSV(
      @Parameter(description = "Estado del documento", example = "APPROVED") @RequestParam(required = false) String status,
      @Parameter(description = "Tipo de documento", example = "FACTURA") @RequestParam(required = false) String docType,
      @Parameter(description = "Texto de búsqueda") @RequestParam(required = false) String search,
      @Parameter(description = "Fecha inicial en ISO 8601") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
      @Parameter(description = "Fecha final en ISO 8601") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
      @Parameter(hidden = true) HttpServletResponse response) throws IOException {

    Page<PurchaseSummary> purchasesPage = service.list(status, docType, search, from, to, PageRequest.of(0, 10000));
    List<PurchaseSummary> purchases = purchasesPage.getContent();

    response.setContentType("text/csv; charset=UTF-8");
    response.setHeader("Content-Disposition", "attachment; filename=\"compras.csv\"");

    try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
      writer.write('\uFEFF');
      writer.println("ID,Tipo Documento,Número,Proveedor,Estado,Neto,IVA,Total,Fecha Emisión");

      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

      for (PurchaseSummary purchase : purchases) {
        writer.print(escapeCsv(purchase.id().toString()));
        writer.print(',');
        writer.print(escapeCsv(purchase.docType() != null ? purchase.docType() : ""));
        writer.print(',');
        writer.print(escapeCsv(purchase.docNumber() != null ? purchase.docNumber() : ""));
        writer.print(',');
        writer.print(escapeCsv(purchase.supplierName() != null ? purchase.supplierName() : ""));
        writer.print(',');
        writer.print(escapeCsv(purchase.status() != null ? purchase.status() : ""));
        writer.print(',');
        writer.print(purchase.net() != null ? purchase.net().toString() : "0");
        writer.print(',');
        writer.print(purchase.vat() != null ? purchase.vat().toString() : "0");
        writer.print(',');
        writer.print(purchase.total() != null ? purchase.total().toString() : "0");
        writer.print(',');
        writer.print(purchase.issuedAt() != null ? dateFormatter.format(purchase.issuedAt()) : "");
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

  @Operation(
    summary = "Importar compras",
    description = "Carga compras desde un archivo CSV respetando la plantilla."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Archivo procesado"),
    @ApiResponse(responseCode = "400", description = "Archivo inválido")
  })
  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
  public Map<String, Object> importFromCSV(
      @Parameter(
        description = "Archivo CSV con columnas: Tipo Documento,Número,Proveedor ID,Estado,Neto,IVA,Total,Fecha Emisión",
        required = true,
        content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(type = "string", format = "binary"))
      )
      @RequestPart("file") MultipartFile file) throws IOException {
    if (file.isEmpty()) {
      return Map.of("success", false, "message", "Archivo vacío", "imported", 0, "errors", List.of());
    }

    List<String> errors = new ArrayList<>();
    int imported = 0;
    int line = 0;

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      String headerLine = reader.readLine();
      line++;
      
      if (headerLine == null) {
        return Map.of("success", false, "message", "Archivo CSV sin contenido", "imported", 0, "errors", List.of());
      }

      // Skip BOM if present
      if (headerLine.startsWith("\uFEFF")) {
        headerLine = headerLine.substring(1);
      }

      String csvLine;
      while ((csvLine = reader.readLine()) != null) {
        line++;
        if (csvLine.trim().isEmpty()) {
          continue;
        }

        try {
          String[] values = parseCsvLine(csvLine);
          
          if (values.length < 8) {
            errors.add("Línea " + line + ": Formato inválido (se esperan al menos 8 columnas)");
            continue;
          }

          // Construir PurchaseReq desde CSV
          // Formato esperado: Tipo Documento, Número, Proveedor ID, Estado, Neto, IVA, Total, Fecha Emisión
          
          UUID supplierId;
          try {
            supplierId = values[2].trim().isEmpty() ? null : UUID.fromString(values[2].trim());
          } catch (IllegalArgumentException e) {
            errors.add("Línea " + line + ": ID de proveedor inválido");
            continue;
          }

          if (supplierId == null) {
            errors.add("Línea " + line + ": ID de proveedor es requerido");
            continue;
          }

          String docType = values[0].trim();
          String docNumber = values[1].trim();

          if (docType.isEmpty()) {
            errors.add("Línea " + line + ": Tipo de documento es requerido");
            continue;
          }

          java.math.BigDecimal net, vat, total;
          try {
            net = values[4].trim().isEmpty() ? java.math.BigDecimal.ZERO : java.math.BigDecimal.valueOf(Double.parseDouble(values[4].trim()));
            vat = values[5].trim().isEmpty() ? java.math.BigDecimal.ZERO : java.math.BigDecimal.valueOf(Double.parseDouble(values[5].trim()));
            total = values[6].trim().isEmpty() ? java.math.BigDecimal.ZERO : java.math.BigDecimal.valueOf(Double.parseDouble(values[6].trim()));
          } catch (NumberFormatException e) {
            errors.add("Línea " + line + ": Formato de monto inválido");
            continue;
          }

          OffsetDateTime issuedAt;
          try {
            if (values[7].trim().isEmpty()) {
              issuedAt = OffsetDateTime.now();
            } else {
              issuedAt = OffsetDateTime.parse(values[7].trim() + "Z", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            }
          } catch (Exception e) {
            errors.add("Línea " + line + ": Formato de fecha inválido (esperado: yyyy-MM-dd HH:mm:ss)");
            continue;
          }

          // PurchaseReq requiere items, usamos lista vacía por defecto
          PurchaseReq req = new PurchaseReq(
            supplierId,
            docType,
            docNumber,
            net,
            vat,
            total,
            null, // pdfUrl
            issuedAt,
            null, // receivedAt
            List.of() // items vacío
          );

          service.create(req);
          imported++;

        } catch (Exception e) {
          errors.add("Línea " + line + ": " + e.getMessage());
        }
      }
    }

    Map<String, Object> result = new HashMap<>();
    result.put("success", errors.isEmpty());
    result.put("imported", imported);
    result.put("total", line - 1);
    result.put("errors", errors);
    
    return result;
  }

  private String[] parseCsvLine(String line) {
    List<String> values = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inQuotes = false;

    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);

      if (c == '"') {
        if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          current.append('"');
          i++;
        } else {
          inQuotes = !inQuotes;
        }
      } else if (c == ',' && !inQuotes) {
        values.add(current.toString());
        current = new StringBuilder();
      } else {
        current.append(c);
      }
    }
    values.add(current.toString());

    return values.toArray(new String[0]);
  }
}

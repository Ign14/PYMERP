package com.datakomerz.pymes.suppliers;

import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.datakomerz.pymes.multitenancy.ValidateTenant;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

@Tag(name = "Suppliers", description = "Gestión de proveedores")
@RestController
@RequestMapping("/api/v1/suppliers")
@SecurityRequirement(name = "bearerAuth")
public class SupplierController {
  private final SupplierRepository repo;
  private final SupplierContactRepository contacts;
  private final SupplierService supplierService;
  private final CompanyContext companyContext;

  public SupplierController(SupplierRepository repo, SupplierContactRepository contacts, 
                           SupplierService supplierService, CompanyContext companyContext) {
    this.repo = repo;
    this.contacts = contacts;
    this.supplierService = supplierService;
    this.companyContext = companyContext;
  }

  @Operation(
    summary = "Listar proveedores",
    description = "Retorna los proveedores de la empresa con filtros por nombre y estado."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Proveedores encontrados", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Supplier.class)))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public List<Supplier> list(
      @Parameter(description = "Texto de búsqueda por nombre, rut o giro") @RequestParam(required = false) String query,
      @Parameter(description = "Filtrar por proveedores activos", example = "true") @RequestParam(required = false) Boolean active,
      @Parameter(description = "Número de página (0-index)", example = "0") @RequestParam(required = false) Integer page,
      @Parameter(description = "Tamaño de página (máx 200)", example = "50") @RequestParam(required = false) Integer size) {
    UUID companyId = companyContext.require();
    Pageable pageable = resolvePageable(page, size);
    return supplierService.findAll(companyId, active, query, pageable).getContent();
  }

  @Operation(
    summary = "Crear proveedor",
    description = "Registra un proveedor y su información de contacto principal."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Proveedor creado", content = @Content(schema = @Schema(implementation = Supplier.class))),
    @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @PostMapping
  @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
  public Supplier create(@Valid @RequestBody SupplierRequest request) {
    Supplier supplier = new Supplier();
    supplier.setCompanyId(companyContext.require());
    apply(supplier, request);
    return supplierService.saveSupplier(supplier);
  }

  @Operation(
    summary = "Actualizar proveedor",
    description = "Modifica los datos registrados para un proveedor."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Proveedor actualizado", content = @Content(schema = @Schema(implementation = Supplier.class))),
    @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos"),
    @ApiResponse(responseCode = "404", description = "Proveedor no encontrado")
  })
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
  @ValidateTenant(entityClass = Supplier.class)
  public Supplier update(
      @Parameter(description = "ID del proveedor", required = true)
      @PathVariable UUID id,
      @Valid @RequestBody SupplierRequest request) {
    UUID companyId = companyContext.require();
    Supplier supplier = supplierService.findSupplier(companyId, id);
    apply(supplier, request);
    return supplierService.saveSupplier(supplier);
  }

  @Operation(
    summary = "Eliminar proveedor",
    description = "Elimina un proveedor del catálogo de la empresa."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Proveedor eliminado"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos"),
    @ApiResponse(responseCode = "404", description = "Proveedor no encontrado")
  })
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN')")
  @ValidateTenant(entityClass = Supplier.class)
  public void delete(
      @Parameter(description = "ID del proveedor", required = true)
      @PathVariable UUID id) {
    UUID companyId = companyContext.require();
    supplierService.deleteSupplier(companyId, id);
  }

  @Operation(
    summary = "Listar contactos de proveedor",
    description = "Obtiene todas las personas de contacto asociadas al proveedor."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Contactos obtenidos", content = @Content(array = @ArraySchema(schema = @Schema(implementation = SupplierContact.class)))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos"),
    @ApiResponse(responseCode = "404", description = "Proveedor no encontrado")
  })
  @GetMapping("/{id}/contacts")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  @ValidateTenant(entityClass = Supplier.class)
  public List<SupplierContact> listContacts(
      @Parameter(description = "ID del proveedor", required = true)
      @PathVariable UUID id) {
    // Verificar que el supplier existe y pertenece al tenant actual (automático via filter)
    supplierService.findSupplier(companyContext.require(), id);
    return contacts.findBySupplierId(id);
  }

  @Operation(
    summary = "Agregar contacto de proveedor",
    description = "Registra un nuevo contacto asociado al proveedor."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Contacto creado", content = @Content(schema = @Schema(implementation = SupplierContact.class))),
    @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos"),
    @ApiResponse(responseCode = "404", description = "Proveedor no encontrado")
  })
  @PostMapping("/{id}/contacts")
  @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
  @ValidateTenant(entityClass = Supplier.class)
  public SupplierContact addContact(
      @Parameter(description = "ID del proveedor", required = true)
      @PathVariable UUID id,
      @Valid @RequestBody SupplierContact contact) {
    // Verificar que el supplier existe y pertenece al tenant actual (automático via filter)
    supplierService.findSupplier(companyContext.require(), id);
    contact.setSupplierId(id);
    return contacts.save(contact);
  }

  @Operation(
    summary = "Exportar proveedores",
    description = "Genera un archivo CSV con los proveedores filtrados."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "CSV generado", content = @Content(mediaType = "text/csv")),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/export")
  @PreAuthorize("hasAnyRole('ERP_USER', 'SETTINGS', 'ADMIN')")
  public void exportToCSV(
      @Parameter(description = "Texto de búsqueda", example = "Proveedor SpA") @RequestParam(required = false) String query,
      @Parameter(description = "Filtrar por estado activo", example = "true") @RequestParam(required = false) Boolean active,
      @Parameter(hidden = true) HttpServletResponse response) throws IOException {

    UUID companyId = companyContext.require();
    List<Supplier> suppliers = supplierService.findAll(companyId, active, query, Pageable.unpaged()).getContent();

    response.setContentType("text/csv; charset=UTF-8");
    response.setHeader("Content-Disposition", "attachment; filename=\"suppliers.csv\"");

    try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
      writer.write('\uFEFF'); // UTF-8 BOM

      writer.println("Nombre,RUT,Dirección,Comuna,Giro,Teléfono,Email,Activo");

      for (Supplier supplier : suppliers) {
        writer.print(escapeCsv(supplier.getName()));
        writer.print(',');
        writer.print(escapeCsv(supplier.getRut()));
        writer.print(',');
        writer.print(escapeCsv(supplier.getAddress()));
        writer.print(',');
        writer.print(escapeCsv(supplier.getCommune()));
        writer.print(',');
        writer.print(escapeCsv(supplier.getBusinessActivity()));
        writer.print(',');
        writer.print(escapeCsv(supplier.getPhone()));
        writer.print(',');
        writer.print(escapeCsv(supplier.getEmail()));
        writer.print(',');
        writer.print((supplier.getActive() != null && supplier.getActive()) ? "Sí" : "No");
        writer.println();
      }
    }
  }

  @Operation(
    summary = "Importar proveedores",
    description = "Carga proveedores desde un archivo CSV UTF-8."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Importación finalizada"),
    @ApiResponse(responseCode = "400", description = "Archivo inválido")
  })
  @PostMapping("/import")
  @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
  public ResponseEntity<Map<String, Object>> importFromCSV(
      @Parameter(
        description = "Archivo CSV de proveedores",
        required = true,
        content = @Content(mediaType = "multipart/form-data", schema = @Schema(type = "string", format = "binary"))
      )
      @RequestParam("file") MultipartFile file) throws IOException {
    Map<String, Object> result = new LinkedHashMap<>();
    List<Map<String, Object>> errors = new ArrayList<>();
    int created = 0;
    int lineNumber = 0;

    try (var reader = new java.io.BufferedReader(
        new java.io.InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

      String line = reader.readLine(); // Skip header
      lineNumber++;

      while ((line = reader.readLine()) != null) {
        lineNumber++;
        if (line.trim().isEmpty()) continue;

        try {
          String[] fields = parseCsvLine(line);
          if (fields.length < 7) {
            errors.add(Map.of("line", lineNumber, "error", "Columnas insuficientes"));
            continue;
          }

          SupplierRequest request = new SupplierRequest(
            fields[0].trim(), // name
            fields[1].isEmpty() ? null : fields[1].trim(), // rut
            fields[2].isEmpty() ? null : fields[2].trim(), // address
            fields[3].isEmpty() ? null : fields[3].trim(), // commune
            fields[4].isEmpty() ? null : fields[4].trim(), // businessActivity
            fields[5].isEmpty() ? null : fields[5].trim(), // phone
            fields[6].isEmpty() ? null : fields[6].trim()  // email
          );

          Supplier supplier = new Supplier();
          supplier.setCompanyId(companyContext.require());
          apply(supplier, request);
          supplierService.saveSupplier(supplier);
          created++;
        } catch (Exception e) {
          errors.add(Map.of("line", lineNumber, "error", e.getMessage()));
        }
      }
    } catch (Exception e) {
      result.put("error", "Error leyendo archivo: " + e.getMessage());
      return ResponseEntity.badRequest().body(result);
    }

    result.put("created", created);
    result.put("errors", errors);
    return ResponseEntity.ok(result);
  }

  private String[] parseCsvLine(String line) {
    List<String> fields = new ArrayList<>();
    StringBuilder currentField = new StringBuilder();
    boolean inQuotes = false;

    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);

      if (c == '"') {
        if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          currentField.append('"');
          i++;
        } else {
          inQuotes = !inQuotes;
        }
      } else if (c == ',' && !inQuotes) {
        fields.add(currentField.toString());
        currentField = new StringBuilder();
      } else {
        currentField.append(c);
      }
    }
    fields.add(currentField.toString());

    return fields.toArray(new String[0]);
  }

  private String escapeCsv(String value) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }

  private void apply(Supplier supplier, SupplierRequest request) {
    supplier.setName(request.name().trim());
    supplier.setRut(normalize(request.rut()));
    supplier.setAddress(normalize(request.address()));
    supplier.setCommune(normalize(request.commune()));
    supplier.setBusinessActivity(normalize(request.businessActivity()));
    supplier.setPhone(normalize(request.phone()));
    supplier.setEmail(normalize(request.email()));
  }

  private String normalize(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private Pageable resolvePageable(Integer page, Integer size) {
    if (size == null) {
      return Pageable.unpaged();
    }
    int pageIndex = page == null ? 0 : Math.max(page, 0);
    int pageSize = Math.max(1, Math.min(size, 200));
    return PageRequest.of(pageIndex, pageSize, Sort.by("name").ascending());
  }

  /**
   * Obtiene métricas de compras para un proveedor específico
   */
  @Operation(
    summary = "Métricas del proveedor",
    description = "KPIs de compras, tiempos de entrega y calidad para un proveedor específico."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Métricas generadas", content = @Content(schema = @Schema(implementation = SupplierMetrics.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos"),
    @ApiResponse(responseCode = "404", description = "Proveedor no encontrado")
  })
  @GetMapping("/{id}/metrics")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  @ValidateTenant(entityClass = Supplier.class)
  public SupplierMetrics getSupplierMetrics(
      @Parameter(description = "ID del proveedor", required = true)
      @PathVariable UUID id) {
    return supplierService.getSupplierMetrics(id);
  }

  /**
   * Obtiene alertas de todos los proveedores
   */
  @Operation(
    summary = "Alertas de proveedores",
    description = "Lista las alertas activas asociadas al desempeño de los proveedores."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Alertas obtenidas", content = @Content(array = @ArraySchema(schema = @Schema(implementation = SupplierAlert.class)))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/alerts")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public List<SupplierAlert> getSupplierAlerts() {
    return supplierService.getSupplierAlerts();
  }

  /**
   * Obtiene ranking de proveedores
   */
  @Operation(
    summary = "Ranking de proveedores",
    description = "Ordena a los proveedores usando el criterio seleccionado."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Ranking calculado", content = @Content(array = @ArraySchema(schema = @Schema(implementation = SupplierRanking.class)))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/ranking")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public List<SupplierRanking> getSupplierRanking(
      @Parameter(description = "Criterio de orden (total_purchases, on_time_deliveries, etc.)", example = "total_purchases")
      @RequestParam(defaultValue = "total_purchases") String criteria) {
    return supplierService.getSupplierRanking(criteria);
  }

  /**
   * Obtiene análisis de riesgo (categorías ABC)
   */
  @Operation(
    summary = "Análisis de riesgo de proveedores",
    description = "Clasificación ABC de los proveedores según su impacto."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Análisis generado", content = @Content(schema = @Schema(implementation = SupplierRiskAnalysis.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/risk-analysis")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public SupplierRiskAnalysis analyzeSupplierRisk() {
    return supplierService.getRiskAnalysis();
  }

  /**
   * Obtiene historial de precios de un producto de un proveedor
   */
  @Operation(
    summary = "Historial de precios",
    description = "Consulta cómo han variado los precios ofrecidos por el proveedor para un producto."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Historial disponible", content = @Content(schema = @Schema(implementation = SupplierPriceHistory.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/{supplierId}/price-history")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public SupplierPriceHistory getSupplierPriceHistory(
      @Parameter(description = "ID del proveedor", required = true)
      @PathVariable UUID supplierId,
      @Parameter(description = "Producto a filtrar") @RequestParam(required = false) UUID productId) {
    return supplierService.getPriceHistory(supplierId, productId);
  }

  /**
   * Obtiene oportunidades de negociación con proveedores
   */
  @Operation(
    summary = "Oportunidades de negociación",
    description = "Identifica oportunidades de mejora en precios o condiciones."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Oportunidades listadas", content = @Content(array = @ArraySchema(schema = @Schema(implementation = NegotiationOpportunity.class)))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/negotiation-opportunities")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public List<NegotiationOpportunity> getNegotiationOpportunities() {
    return supplierService.getNegotiationOpportunities();
  }

  /**
   * Obtiene productos con un solo proveedor (riesgo de concentración)
   */
  @Operation(
    summary = "Productos con proveedor único",
    description = "Detecta productos que dependen de un solo proveedor."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Listado generado", content = @Content(array = @ArraySchema(schema = @Schema(implementation = SingleSourceProduct.class)))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/single-source-products")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public List<SingleSourceProduct> getSingleSourceProducts() {
    return supplierService.getSingleSourceProducts();
  }

  @Operation(
    summary = "Pronóstico de compras por proveedor",
    description = "Proyección de compras futuras necesarias para un proveedor."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Pronóstico generado", content = @Content(schema = @Schema(implementation = PurchaseForecast.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/{supplierId}/forecast")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public ResponseEntity<?> getSupplierForecast(
      @Parameter(description = "ID del proveedor", required = true)
      @PathVariable UUID supplierId) {
    PurchaseForecast forecast = supplierService.getPurchaseForecast(supplierId);
    return ResponseEntity.ok(forecast);
  }
}

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

@RestController
@RequestMapping("/api/v1/suppliers")
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

    @GetMapping
    @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
    public List<Supplier> list(
        @RequestParam(required = false) String query,
        @RequestParam(required = false) Boolean active) {
        return repo.searchSuppliers(active, query);
    }  @PostMapping
  @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
  public Supplier create(@Valid @RequestBody SupplierRequest request) {
    Supplier supplier = new Supplier();
    supplier.setCompanyId(companyContext.require());
    apply(supplier, request);
    return repo.save(supplier);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
  @ValidateTenant(entityClass = Supplier.class)
  public Supplier update(@PathVariable UUID id, @Valid @RequestBody SupplierRequest request) {
    Supplier supplier = repo.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + id));
    apply(supplier, request);
    return repo.save(supplier);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN')")
  @ValidateTenant(entityClass = Supplier.class)
  public void delete(@PathVariable UUID id) {
    Supplier supplier = repo.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + id));
    supplier.setActive(false);
    repo.save(supplier);
  }

  @GetMapping("/{id}/contacts")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  @ValidateTenant(entityClass = Supplier.class)
  public List<SupplierContact> listContacts(@PathVariable UUID id) {
    // Verificar que el supplier existe y pertenece al tenant actual (automático via filter)
    repo.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + id));
    return contacts.findBySupplierId(id);
  }

  @PostMapping("/{id}/contacts")
  @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
  @ValidateTenant(entityClass = Supplier.class)
  public SupplierContact addContact(@PathVariable UUID id, @Valid @RequestBody SupplierContact contact) {
    // Verificar que el supplier existe y pertenece al tenant actual (automático via filter)
    repo.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + id));
    contact.setSupplierId(id);
    return contacts.save(contact);
  }

  @GetMapping("/export")
  @PreAuthorize("hasAnyRole('ERP_USER', 'SETTINGS', 'ADMIN')")
  public void exportToCSV(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) Boolean active,
      HttpServletResponse response) throws IOException {

    List<Supplier> suppliers = repo.searchSuppliers(active, query);

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

    @PostMapping("/import")
  @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
  public ResponseEntity<Map<String, Object>> importFromCSV(@RequestParam("file") MultipartFile file) throws IOException {
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
          repo.save(supplier);
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

  /**
   * Obtiene métricas de compras para un proveedor específico
   */
  @GetMapping("/{id}/metrics")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  @ValidateTenant(entityClass = Supplier.class)
  public SupplierMetrics getSupplierMetrics(@PathVariable UUID id) {
    return supplierService.getSupplierMetrics(id);
  }

  /**
   * Obtiene alertas de todos los proveedores
   */
  @GetMapping("/alerts")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public List<SupplierAlert> getSupplierAlerts() {
    return supplierService.getSupplierAlerts();
  }

  /**
   * Obtiene ranking de proveedores
   */
  @GetMapping("/ranking")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public List<SupplierRanking> getSupplierRanking(
      @RequestParam(defaultValue = "total_purchases") String criteria) {
    return supplierService.getSupplierRanking(criteria);
  }

  /**
   * Obtiene análisis de riesgo (categorías ABC)
   */
  @GetMapping("/risk-analysis")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public SupplierRiskAnalysis analyzeSupplierRisk() {
    return supplierService.getRiskAnalysis();
  }

  /**
   * Obtiene historial de precios de un producto de un proveedor
   */
  @GetMapping("/{supplierId}/price-history")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public SupplierPriceHistory getSupplierPriceHistory(
      @PathVariable UUID supplierId,
      @RequestParam(required = false) UUID productId) {
    return supplierService.getPriceHistory(supplierId, productId);
  }

  /**
   * Obtiene oportunidades de negociación con proveedores
   */
  @GetMapping("/negotiation-opportunities")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public List<NegotiationOpportunity> getNegotiationOpportunities() {
    return supplierService.getNegotiationOpportunities();
  }

  /**
   * Obtiene productos con un solo proveedor (riesgo de concentración)
   */
  @GetMapping("/single-source-products")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public List<SingleSourceProduct> getSingleSourceProducts() {
    return supplierService.getSingleSourceProducts();
  }

  @GetMapping("/{supplierId}/forecast")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public ResponseEntity<?> getSupplierForecast(
      @PathVariable UUID supplierId) {
    PurchaseForecast forecast = supplierService.getPurchaseForecast(supplierId);
    return ResponseEntity.ok(forecast);
  }
}

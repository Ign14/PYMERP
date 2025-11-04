package com.datakomerz.pymes.suppliers;

import com.datakomerz.pymes.core.tenancy.CompanyContext;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    public List<Supplier> list(
        @RequestParam(required = false) String query,
        @RequestParam(required = false) Boolean active) {
        UUID companyId = companyContext.require();
        return repo.searchSuppliers(companyId, active, query);
    }  @PostMapping
  public Supplier create(@Valid @RequestBody SupplierRequest request) {
    Supplier supplier = new Supplier();
    supplier.setCompanyId(companyContext.require());
    apply(supplier, request);
    return repo.save(supplier);
  }

  @PutMapping("/{id}")
  public Supplier update(@PathVariable UUID id, @Valid @RequestBody SupplierRequest request) {
    Supplier supplier = ensureOwnership(id);
    apply(supplier, request);
    return repo.save(supplier);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    Supplier supplier = ensureOwnership(id);
    supplier.setActive(false);
    repo.save(supplier);
  }

  @GetMapping("/{id}/contacts")
  public List<SupplierContact> listContacts(@PathVariable UUID id) {
    ensureOwnership(id);
    return contacts.findBySupplierId(id);
  }

  @PostMapping("/{id}/contacts")
  public SupplierContact addContact(@PathVariable UUID id, @Valid @RequestBody SupplierContact contact) {
    ensureOwnership(id);
    contact.setSupplierId(id);
    return contacts.save(contact);
  }

  @GetMapping("/export")
  public void exportToCSV(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) Boolean active,
      HttpServletResponse response) throws IOException {

    UUID companyId = companyContext.require();
    List<Supplier> suppliers = repo.searchSuppliers(companyId, active, query);

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

  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Map<String, Object>> importFromCSV(@RequestParam("file") MultipartFile file) {
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

  private Supplier ensureOwnership(UUID supplierId) {
    UUID companyId = companyContext.require();
    return repo.findByIdAndCompanyId(supplierId, companyId)
      .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + supplierId));
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
  public SupplierMetrics getMetrics(@PathVariable UUID id) {
    UUID companyId = companyContext.require();
    return supplierService.getSupplierMetrics(id, companyId);
  }

  /**
   * Obtiene alertas de todos los proveedores
   */
  @GetMapping("/alerts")
  public List<SupplierAlert> getAlerts() {
    UUID companyId = companyContext.require();
    return supplierService.getSupplierAlerts(companyId);
  }

  /**
   * Obtiene ranking de proveedores
   */
  @GetMapping("/ranking")
  public List<SupplierRanking> getRanking(
      @RequestParam(defaultValue = "volume") String criteria
  ) {
    UUID companyId = companyContext.require();
    return supplierService.getSupplierRanking(companyId, criteria);
  }

  /**
   * Obtiene análisis de riesgo (categorías ABC)
   */
  @GetMapping("/risk-analysis")
  public SupplierRiskAnalysis getRiskAnalysis() {
    UUID companyId = companyContext.require();
    return supplierService.getRiskAnalysis(companyId);
  }

  /**
   * Obtiene historial de precios de un producto de un proveedor
   */
  @GetMapping("/{supplierId}/price-history")
  public SupplierPriceHistory getPriceHistory(
      @PathVariable UUID supplierId,
      @RequestParam UUID productId
  ) {
    UUID companyId = companyContext.require();
    return supplierService.getPriceHistory(supplierId, productId, companyId);
  }

  /**
   * Obtiene oportunidades de negociación con proveedores
   */
  @GetMapping("/negotiation-opportunities")
  public List<NegotiationOpportunity> getNegotiationOpportunities() {
    UUID companyId = companyContext.require();
    return supplierService.getNegotiationOpportunities(companyId);
  }

  /**
   * Obtiene productos con un solo proveedor (riesgo de concentración)
   */
  @GetMapping("/single-source-products")
  public ResponseEntity<List<SingleSourceProduct>> getSingleSourceProducts() {
    UUID companyId = companyContext.require();
    List<SingleSourceProduct> products = supplierService.getSingleSourceProducts(companyId);
    return ResponseEntity.ok(products);
  }

  @GetMapping("/{supplierId}/forecast")
  public ResponseEntity<PurchaseForecast> getSupplierForecast(@PathVariable UUID supplierId) {
    UUID companyId = companyContext.require();
    PurchaseForecast forecast = supplierService.getPurchaseForecast(supplierId, companyId);
    return ResponseEntity.ok(forecast);
  }
}

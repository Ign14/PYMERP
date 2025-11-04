package com.datakomerz.pymes.customers.api;

import com.datakomerz.pymes.common.api.PagedResponse;
import com.datakomerz.pymes.customers.Customer;
import com.datakomerz.pymes.customers.CustomerService;
import com.datakomerz.pymes.customers.application.CreateCustomerUseCase;
import com.datakomerz.pymes.customers.application.CustomerMapper;
import com.datakomerz.pymes.customers.application.ListCustomersUseCase;
import com.datakomerz.pymes.customers.dto.CustomerRequest;
import com.datakomerz.pymes.customers.dto.CustomerResponse;
import com.datakomerz.pymes.customers.dto.CustomerSaleHistoryItem;
import com.datakomerz.pymes.customers.dto.CustomerSegmentSummary;
import com.datakomerz.pymes.customers.dto.CustomerStatsResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
@RequestMapping("/api/v1/customers")
public class CustomerController {

  private final ListCustomersUseCase listCustomersUseCase;
  private final CreateCustomerUseCase createCustomerUseCase;
  private final CustomerService service;
  private final CustomerMapper mapper;

  public CustomerController(ListCustomersUseCase listCustomersUseCase,
                            CreateCustomerUseCase createCustomerUseCase,
                            CustomerService service,
                            CustomerMapper mapper) {
    this.listCustomersUseCase = listCustomersUseCase;
    this.createCustomerUseCase = createCustomerUseCase;
    this.service = service;
    this.mapper = mapper;
  }

  @GetMapping
  public PagedResponse<CustomerResponse> list(@RequestParam(name = "q", defaultValue = "") String q,
                                              @RequestParam(name = "segment", required = false) String segment,
                                              @RequestParam(name = "active", required = false) Boolean active,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
    return listCustomersUseCase.handle(q, segment, active, page, size);
  }

  @GetMapping("/segments")
  public List<CustomerSegmentSummary> segments() {
    return service.summarizeSegments();
  }

  @GetMapping("/{id}")
  public CustomerResponse get(@PathVariable UUID id) {
    return mapper.toResponse(service.get(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CustomerResponse create(@Valid @RequestBody CustomerRequest request) {
    return createCustomerUseCase.handle(request);
  }

  @PutMapping("/{id}")
  public CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody CustomerRequest request) {
    Customer updated = service.update(id, request);
    return mapper.toResponse(updated);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/stats")
  public CustomerStatsResponse getStats(@PathVariable UUID id) {
    return service.getCustomerStats(id);
  }

  @GetMapping("/{id}/sales")
  public Page<CustomerSaleHistoryItem> getSaleHistory(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    return service.getCustomerSaleHistory(id, PageRequest.of(page, size));
  }

  @GetMapping("/export")
  public void exportToCSV(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) String segment,
      @RequestParam(required = false) Boolean active,
      HttpServletResponse response) throws IOException {

    List<Customer> customers = service.exportToCSV(query, segment, active);

    response.setContentType("text/csv; charset=UTF-8");
    response.setHeader("Content-Disposition", "attachment; filename=\"customers.csv\"");

    try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
      // UTF-8 BOM para compatibilidad con Excel
      writer.write('\uFEFF');

      // Headers
      writer.println("Nombre,RUT,Email,Teléfono,Dirección,Segmento,Persona de Contacto,Notas,Activo,Creado,Actualizado");

      // Formato de fecha
      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(java.time.ZoneId.systemDefault());

      // Datos
      for (Customer customer : customers) {
        writer.print(escapeCsv(customer.getName()));
        writer.print(',');
        writer.print(escapeCsv(customer.getRut()));
        writer.print(',');
        writer.print(escapeCsv(customer.getEmail()));
        writer.print(',');
        writer.print(escapeCsv(customer.getPhone()));
        writer.print(',');
        writer.print(escapeCsv(customer.getAddress()));
        writer.print(',');
        writer.print(escapeCsv(customer.getSegment()));
        writer.print(',');
        writer.print(escapeCsv(customer.getContactPerson()));
        writer.print(',');
        writer.print(escapeCsv(customer.getNotes()));
        writer.print(',');
        writer.print(customer.getActive() ? "Sí" : "No");
        writer.print(',');
        writer.print(customer.getCreatedAt() != null ? dateFormatter.format(customer.getCreatedAt()) : "");
        writer.print(',');
        writer.print(customer.getUpdatedAt() != null ? dateFormatter.format(customer.getUpdatedAt()) : "");
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
          if (fields.length < 9) {
            errors.add(Map.of("line", lineNumber, "error", "Columnas insuficientes"));
            continue;
          }

          CustomerRequest request = new CustomerRequest(
            fields[0].trim(), // name
            fields[1].isEmpty() ? null : fields[1].trim(), // rut
            fields[4].isEmpty() ? null : fields[4].trim(), // address
            null, // lat
            null, // lng
            fields[3].isEmpty() ? null : fields[3].trim(), // phone
            fields[2].isEmpty() ? null : fields[2].trim(), // email
            fields[5].isEmpty() ? null : fields[5].trim(), // segment
            fields[6].isEmpty() ? null : fields[6].trim(), // contactPerson
            fields[7].isEmpty() ? null : fields[7].trim(), // notes
            fields[8].equalsIgnoreCase("Sí") || fields[8].equalsIgnoreCase("true") // active
          );

          createCustomerUseCase.handle(request);
          created++;
        } catch (Exception e) {
          errors.add(Map.of("line", lineNumber, "error", e.getMessage()));
        }
      }

      result.put("created", created);
      result.put("errors", errors);
      result.put("totalErrors", errors.size());
      return ResponseEntity.ok(result);

    } catch (IOException e) {
      result.put("created", 0);
      result.put("errors", List.of(Map.of("line", 0, "error", "Error leyendo archivo: " + e.getMessage())));
      result.put("totalErrors", 1);
      return ResponseEntity.badRequest().body(result);
    }
  }

  private String[] parseCsvLine(String line) {
    List<String> result = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inQuotes = false;

    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);

      if (c == '"') {
        if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          current.append('"');
          i++; // skip next quote
        } else {
          inQuotes = !inQuotes;
        }
      } else if (c == ',' && !inQuotes) {
        result.add(current.toString());
        current = new StringBuilder();
      } else {
        current.append(c);
      }
    }
    result.add(current.toString());
    return result.toArray(new String[0]);
  }

  private String escapeCsv(String value) {
    if (value == null) {
      return "";
    }
    // Si contiene coma, comilla doble o salto de línea, escapar
    if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}

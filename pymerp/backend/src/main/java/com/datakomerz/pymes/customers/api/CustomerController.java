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

@Tag(name = "Customers", description = "Gestión de clientes")
@RestController
@RequestMapping("/api/v1/customers")
@SecurityRequirement(name = "bearerAuth")
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

  @Operation(
    summary = "Listar clientes",
    description = "Retorna clientes paginados filtrando por segmento, estado y texto de búsqueda."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Clientes encontrados", content = @Content(schema = @Schema(implementation = PagedResponse.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public PagedResponse<CustomerResponse> list(
      @Parameter(description = "Texto de búsqueda (nombre, RUT, email)", example = "Jane Doe")
      @RequestParam(name = "q", defaultValue = "") String q,
      @Parameter(description = "Segmento comercial", example = "Retail")
      @RequestParam(name = "segment", required = false) String segment,
      @Parameter(description = "Estado activo", example = "true")
      @RequestParam(name = "active", required = false) Boolean active,
      @Parameter(description = "Número de página (0-index)", example = "0")
      @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Cantidad de registros por página", example = "20")
      @RequestParam(defaultValue = "20") int size) {
    return listCustomersUseCase.handle(q, segment, active, page, size);
  }

  @Operation(
    summary = "Resumen de segmentos",
    description = "Obtiene el total de clientes agrupados por segmento."
  )
  @ApiResponses({
    @ApiResponse(
      responseCode = "200",
      description = "Segmentos disponibles",
      content = @Content(array = @ArraySchema(schema = @Schema(implementation = CustomerSegmentSummary.class)))
    ),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/segments")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public List<CustomerSegmentSummary> segments() {
    return service.summarizeSegments();
  }

  @Operation(
    summary = "Obtener cliente",
    description = "Recupera un cliente por su identificador."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Cliente encontrado", content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos"),
    @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public CustomerResponse get(
      @Parameter(description = "ID del cliente", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
      @PathVariable UUID id) {
    return mapper.toResponse(service.get(id));
  }

  @Operation(
    summary = "Crear cliente",
    description = "Registra un nuevo cliente con su información general y de contacto."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Cliente creado", content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
    @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ERP_USER', 'SETTINGS', 'ADMIN')")
  public CustomerResponse create(@Valid @RequestBody CustomerRequest request) {
    return createCustomerUseCase.handle(request);
  }

  @Operation(
    summary = "Actualizar cliente",
    description = "Modifica los datos de un cliente existente."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Cliente actualizado", content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
    @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos"),
    @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
  })
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ERP_USER', 'SETTINGS', 'ADMIN')")
  public CustomerResponse update(
      @Parameter(description = "ID del cliente", required = true)
      @PathVariable UUID id,
      @Valid @RequestBody CustomerRequest request) {
    Customer updated = service.update(id, request);
    return mapper.toResponse(updated);
  }

  @Operation(
    summary = "Eliminar cliente",
    description = "Elimina lógicamente un cliente del sistema."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Cliente eliminado"),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos"),
    @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
  })
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> delete(
      @Parameter(description = "ID del cliente", required = true)
      @PathVariable UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  @Operation(
    summary = "Estadísticas del cliente",
    description = "Devuelve KPIs y comportamiento de compras de un cliente."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Estadísticas generadas", content = @Content(schema = @Schema(implementation = CustomerStatsResponse.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos"),
    @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
  })
  @GetMapping("/{id}/stats")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public CustomerStatsResponse getStats(
      @Parameter(description = "ID del cliente", required = true)
      @PathVariable UUID id) {
    return service.getCustomerStats(id);
  }

  @Operation(
    summary = "Historial de ventas del cliente",
    description = "Entrega las ventas asociadas a un cliente en forma paginada."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Historial recuperado", content = @Content(schema = @Schema(implementation = Page.class))),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos"),
    @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
  })
  @GetMapping("/{id}/sales")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public Page<CustomerSaleHistoryItem> getSaleHistory(
      @Parameter(description = "ID del cliente", required = true)
      @PathVariable UUID id,
      @Parameter(description = "Número de página (0-index)", example = "0")
      @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Tamaño de página", example = "10")
      @RequestParam(defaultValue = "10") int size) {
    return service.getCustomerSaleHistory(id, PageRequest.of(page, size));
  }

  @Operation(
    summary = "Exportar clientes",
    description = "Genera un archivo CSV con los clientes filtrados."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "CSV generado", content = @Content(mediaType = "text/csv")),
    @ApiResponse(responseCode = "401", description = "No autenticado"),
    @ApiResponse(responseCode = "403", description = "Sin permisos")
  })
  @GetMapping("/export")
  @PreAuthorize("hasAnyRole('ERP_USER', 'SETTINGS', 'ADMIN')")
  public void exportToCSV(
      @Parameter(description = "Texto de búsqueda", example = "Acme")
      @RequestParam(required = false) String query,
      @Parameter(description = "Segmento a filtrar", example = "Retail")
      @RequestParam(required = false) String segment,
      @Parameter(description = "Filtrar por clientes activos", example = "true")
      @RequestParam(required = false) Boolean active,
      @Parameter(hidden = true)
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

  @Operation(
    summary = "Importar clientes",
    description = "Carga clientes desde un archivo CSV siguiendo la plantilla oficial."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Importación procesada"),
    @ApiResponse(responseCode = "400", description = "Archivo inválido")
  })
  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
  public ResponseEntity<Map<String, Object>> importFromCSV(
      @Parameter(
        description = "Archivo CSV UTF-8 con encabezados",
        required = true,
        content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(type = "string", format = "binary"))
      )
      @RequestParam("file") MultipartFile file) {
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

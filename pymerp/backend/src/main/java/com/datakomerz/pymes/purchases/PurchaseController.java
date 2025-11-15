package com.datakomerz.pymes.purchases;

import com.datakomerz.pymes.billing.dto.PurchaseOrderPayload;
import com.datakomerz.pymes.billing.render.LocalInvoiceRenderer;
import com.datakomerz.pymes.multitenancy.ValidateTenant;
import com.datakomerz.pymes.purchases.dto.PurchaseCreationResult;
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
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/api/v1/purchases")
public class PurchaseController {
  private final PurchaseService service;
  private final LocalInvoiceRenderer localInvoiceRenderer;

  public PurchaseController(PurchaseService service, LocalInvoiceRenderer localInvoiceRenderer) {
    this.service = service;
    this.localInvoiceRenderer = localInvoiceRenderer;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ERP_USER', 'ADMIN')")
  public PurchaseCreationResult create(@Valid @RequestBody PurchaseReq req) {
    return service.create(req);
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ERP_USER', 'ADMIN')")
  public PurchaseCreationResult createWithFile(
      @RequestPart("data") @Valid PurchaseReq req,
      @RequestPart(value = "file", required = false) MultipartFile file) {
    return service.createWithFile(req, file);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ERP_USER', 'ADMIN')")
  @ValidateTenant(entityClass = Purchase.class)
  public PurchaseSummary update(@PathVariable UUID id, @RequestBody PurchaseUpdateRequest req) {
    return service.update(id, req);
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasRole('ADMIN')")
  @ValidateTenant(entityClass = Purchase.class)
  public PurchaseSummary cancel(@PathVariable UUID id) {
    return service.cancel(id);
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public Page<PurchaseSummary> list(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String docType,
                                    @RequestParam(required = false) String search,
                                    @RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                    OffsetDateTime from,
                                    @RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                    OffsetDateTime to) {
    return service.list(status, docType, search, from, to, PageRequest.of(page, size));
  }

  @GetMapping("/{id}/detail")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  @ValidateTenant(entityClass = Purchase.class)
  public com.datakomerz.pymes.purchases.dto.PurchaseDetail getDetail(@PathVariable UUID id) {
    return service.getDetail(id);
  }

  @GetMapping("/{id}/pdf")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'PURCHASING', 'WAREHOUSE')")
  @ValidateTenant(entityClass = Purchase.class)
  public ResponseEntity<byte[]> downloadPurchaseOrderPdf(@PathVariable UUID id) {
    Purchase purchase = service.findById(id);
    PurchaseOrderPayload payload = service.buildPurchaseOrderPayload(purchase);
    LocalInvoiceRenderer.RenderedInvoice pdf = localInvoiceRenderer.renderPurchaseOrderPdf(payload);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDisposition(
        ContentDisposition.attachment().filename(pdf.filename()).build()
    );

    return ResponseEntity.ok()
        .headers(headers)
        .body(pdf.content());
  }

  @GetMapping("/metrics/daily")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public List<PurchaseDailyPoint> metrics(@RequestParam(defaultValue = "14") int days) {
    return service.dailyMetrics(days);
  }

  @GetMapping("/kpis")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public com.datakomerz.pymes.purchases.dto.PurchaseKPIs purchaseKPIs(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    
    LocalDate startDate = from != null ? from : LocalDate.now().minusDays(30);
    LocalDate endDate = to != null ? to : LocalDate.now();
    
    return service.getPurchaseKPIs(startDate, endDate);
  }

  @GetMapping("/abc-analysis")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public List<com.datakomerz.pymes.purchases.dto.PurchaseABCClassification> purchaseABCAnalysis(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    
    LocalDate startDate = from != null ? from : LocalDate.now().minusDays(90);
    LocalDate endDate = to != null ? to : LocalDate.now();
    
    return service.getPurchaseABCAnalysis(startDate, endDate);
  }

  @GetMapping("/forecast")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public List<com.datakomerz.pymes.purchases.dto.PurchaseForecast> purchaseForecast(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false, defaultValue = "30") int horizonDays) {
    
    LocalDate startDate = from != null ? from : LocalDate.now().minusDays(90);
    LocalDate endDate = to != null ? to : LocalDate.now();
    
    return service.getPurchaseForecast(startDate, endDate, horizonDays);
  }

  @GetMapping("/export")
  @PreAuthorize("hasAnyRole('ERP_USER', 'ADMIN')")
  public void exportToCSV(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String docType,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
      HttpServletResponse response) throws IOException {

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

  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
  public Map<String, Object> importFromCSV(@RequestPart("file") MultipartFile file) throws IOException {
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
            30, // paymentTermDays default
            null, // status
            List.of(), // items vacío
            null // captcha
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

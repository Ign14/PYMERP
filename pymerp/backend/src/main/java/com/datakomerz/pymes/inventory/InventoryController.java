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

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

  private final InventoryService inventoryService;

  public InventoryController(InventoryService inventoryService) {
    this.inventoryService = inventoryService;
  }

  @GetMapping("/alerts")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public List<InventoryAlert> lowStock(@RequestParam(required = false) BigDecimal threshold) {
    return inventoryService.lowStock(threshold);
  }

  @GetMapping("/summary")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public InventorySummary summary() {
    return inventoryService.summary();
  }

  @GetMapping("/settings")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public InventorySettingsResponse settings() {
    return inventoryService.getSettings();
  }

  @PutMapping("/settings")
  @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
  public InventorySettingsResponse updateSettings(@Valid @RequestBody InventorySettingsUpdateRequest request) {
    return inventoryService.updateSettings(request);
  }

  @PostMapping("/adjustments")
  @PreAuthorize("hasAnyRole('ERP_USER', 'ADMIN')")
  public InventoryAdjustmentResponse adjust(@Valid @RequestBody InventoryAdjustmentRequest request) {
    return inventoryService.adjust(request);
  }

  @GetMapping("/movements")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public Page<InventoryMovementSummary> movements(
      @RequestParam(required = false) UUID productId,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return inventoryService.listMovements(productId, type, from, to, PageRequest.of(page, size));
  }

  @GetMapping("/kpis")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public InventoryKPIs kpis() {
    return inventoryService.getKPIs();
  }

  @GetMapping("/movement-stats")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public StockMovementStats movementStats() {
    return inventoryService.getMovementStats();
  }

  @GetMapping("/abc-analysis")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public List<ProductABCClassification> abcAnalysis(
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
  
  @GetMapping("/forecast")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public List<com.datakomerz.pymes.inventory.dto.InventoryForecast> forecast(
    @RequestParam(required = false) Long productId,
    @RequestParam(required = false) Integer days
  ) {
    return inventoryService.getForecastAnalysis(productId, days);
  }
}

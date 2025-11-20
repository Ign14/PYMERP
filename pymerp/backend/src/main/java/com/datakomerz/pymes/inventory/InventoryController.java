package com.datakomerz.pymes.inventory;

import com.datakomerz.pymes.inventory.dto.InventoryAdjustmentRequest;
import com.datakomerz.pymes.inventory.dto.InventoryAdjustmentResponse;
import com.datakomerz.pymes.inventory.dto.InventoryAlert;
import com.datakomerz.pymes.inventory.dto.InventoryKPIs;
import com.datakomerz.pymes.inventory.dto.InventoryLocationRequest;
import com.datakomerz.pymes.inventory.dto.InventoryLocationResponse;
import com.datakomerz.pymes.inventory.dto.LotDetailDTO;
import com.datakomerz.pymes.inventory.dto.InventoryMovementHistoryEntry;
import com.datakomerz.pymes.inventory.dto.InventorySettingsResponse;
import com.datakomerz.pymes.inventory.dto.InventorySettingsUpdateRequest;
import com.datakomerz.pymes.inventory.dto.InventorySummary;
import com.datakomerz.pymes.inventory.dto.ProductABCClassification;
import com.datakomerz.pymes.inventory.dto.StockByLocationResponse;
import com.datakomerz.pymes.inventory.dto.StockMovementStats;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
  private final InventoryLocationService inventoryLocationService;

  public InventoryController(InventoryService inventoryService,
                             InventoryLocationService inventoryLocationService) {
    this.inventoryService = inventoryService;
    this.inventoryLocationService = inventoryLocationService;
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
  public Page<InventoryMovementHistoryEntry> movements(
      @RequestParam(required = false) UUID productId,
      @RequestParam(required = false) UUID lotId,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) UUID locationId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String sort) {
    Sort sortSpec = buildSort(sort, Sort.by(Sort.Order.desc("createdAt")));
    return inventoryService.listMovements(productId, lotId, type, locationId, dateFrom, dateTo, PageRequest.of(page, size, sortSpec));
  }

  @GetMapping("/lots")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public Page<LotDetailDTO> listLots(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) UUID productId,
      @RequestParam(required = false) UUID supplierId,
      @RequestParam(required = false) UUID locationId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ingressFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ingressTo,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryTo,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return inventoryService.listLots(
        q,
        status,
        productId,
        supplierId,
        locationId,
        ingressFrom,
        ingressTo,
        expiryFrom,
        expiryTo,
        PageRequest.of(page, size));
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

  @GetMapping("/stock/by-product")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")
  public List<StockByLocationResponse> stockByProduct(
      @RequestParam(required = false) UUID productId,
      @RequestParam(name = "productIds[]", required = false) List<UUID> productIds) {
    return inventoryService.stockByProduct(productId, productIds);
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
  
  @PutMapping("/lots/{lotId}/location/{locationId}")
  @PreAuthorize("hasAnyRole('ERP_USER', 'ADMIN')")
  public InventoryLot assignLotLocation(
      @PathVariable UUID lotId,
      @PathVariable UUID locationId) {
    return inventoryService.assignLocationToLot(lotId, locationId);
  }
  
  @GetMapping("/products/{productId}/stock-by-location")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public List<com.datakomerz.pymes.inventory.dto.StockByLocationDTO> getStockByLocation(
      @PathVariable UUID productId) {
    return inventoryService.getStockByLocation(productId);
  }

  @PostMapping("/lots/{lotId}/transfer")
  @PreAuthorize("hasAnyRole('ERP_USER', 'ADMIN')")
  public InventoryLot transferLot(
      @PathVariable UUID lotId,
      @Valid @RequestBody com.datakomerz.pymes.inventory.dto.LotTransferRequest request) {
    return inventoryService.transferLot(lotId, request);
  }

  private Sort buildSort(String sortParam, Sort fallback) {
    if (!StringUtils.hasText(sortParam)) {
      return fallback;
    }
    String[] parts = sortParam.split(",");
    String property = mapSortProperty(parts[0].trim());
    Sort.Direction defaultDirection = fallback.stream()
        .findFirst()
        .map(Sort.Order::getDirection)
        .orElse(Sort.Direction.ASC);
    Sort.Direction direction = Sort.Direction.fromOptionalString(
            parts.length > 1 ? parts[1].trim() : defaultDirection.name())
        .orElse(defaultDirection);
    return Sort.by(new Sort.Order(direction, property));
  }

  private String mapSortProperty(String candidate) {
    if (!StringUtils.hasText(candidate)) {
      return "createdAt";
    }
    return switch (candidate.trim()) {
      case "qty", "quantity" -> "qtyAvailable";
      case "expDate", "fechaExpiracion" -> "expDate";
      case "fechaIngreso" -> "createdAt";
      default -> candidate;
    };
  }
}

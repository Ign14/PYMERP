package com.datakomerz.pymes.inventory;

import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.datakomerz.pymes.inventory.dto.InventoryAdjustmentRequest;
import com.datakomerz.pymes.inventory.dto.InventoryAdjustmentResponse;
import com.datakomerz.pymes.inventory.dto.InventoryAlert;
import com.datakomerz.pymes.inventory.dto.InventoryKPIs;
import com.datakomerz.pymes.inventory.dto.InventoryMovementSummary;
import com.datakomerz.pymes.inventory.dto.InventorySettingsResponse;
import com.datakomerz.pymes.inventory.dto.InventorySettingsUpdateRequest;
import com.datakomerz.pymes.inventory.dto.InventorySummary;
import com.datakomerz.pymes.inventory.dto.StockMovementStats;
import com.datakomerz.pymes.inventory.dto.ProductABCClassification;
import com.datakomerz.pymes.locations.Location;
import com.datakomerz.pymes.locations.LocationRepository;
import com.datakomerz.pymes.products.Product;
import com.datakomerz.pymes.products.ProductRepository;
import com.datakomerz.pymes.sales.SaleLotAllocation;
import com.datakomerz.pymes.sales.SaleLotAllocationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
  private static final BigDecimal FALLBACK_THRESHOLD = new BigDecimal("5");

  private final InventoryLotRepository lots;
  private final InventoryMovementRepository movements;
  private final SaleLotAllocationRepository allocations;
  private final InventorySettingsRepository settingsRepository;
  private final ProductRepository productRepository;
  private final LocationRepository locationRepository;
  private final CompanyContext companyContext;
  private final AuditContextService auditContext;

  public InventoryService(InventoryLotRepository lots,
                          InventoryMovementRepository movements,
                          SaleLotAllocationRepository allocations,
                          InventorySettingsRepository settingsRepository,
                          ProductRepository productRepository,
                          LocationRepository locationRepository,
                          CompanyContext companyContext,
                          AuditContextService auditContext) {
    this.lots = lots;
    this.movements = movements;
    this.allocations = allocations;
    this.settingsRepository = settingsRepository;
    this.productRepository = productRepository;
    this.locationRepository = locationRepository;
    this.companyContext = companyContext;
    this.auditContext = auditContext;
  }

  @Transactional
  public void consumeFIFO(UUID saleId, UUID productId, BigDecimal qty) {
    consumeFIFO(saleId, productId, qty, null, null);
  }

  @Transactional
  public void consumeFIFO(UUID saleId, UUID productId, BigDecimal qty, UUID locationId, UUID lotId) {
    UUID companyId = companyContext.require();
    var remaining = qty;
    
    List<InventoryLot> candidates;
    
    // Si se especifica un lote específico, usarlo
    if (lotId != null) {
      var specificLot = lots.findById(lotId)
          .filter(lot -> lot.getCompanyId().equals(companyId) && lot.getProductId().equals(productId))
          .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado o no válido: " + lotId));
      candidates = List.of(specificLot);
    }
    // Si se especifica ubicación, filtrar por ubicación (FIFO por fecha de vencimiento)
    else if (locationId != null) {
      candidates = lots.findByCompanyIdAndProductIdAndLocationIdAndQtyAvailableGreaterThanOrderByExpDateAscCreatedAtAsc(
          companyId, productId, locationId, BigDecimal.ZERO);
    }
    // FIFO automático por fecha de vencimiento
    else {
      candidates = lots.findByCompanyIdAndProductIdAndQtyAvailableGreaterThanOrderByExpDateAscCreatedAtAsc(
          companyId, productId, BigDecimal.ZERO);
    }

    for (var lot : candidates) {
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
        break;
      }
      if (lot.getQtyAvailable().compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }

      var take = lot.getQtyAvailable().min(remaining);
      lot.setQtyAvailable(lot.getQtyAvailable().subtract(take));
      lots.save(lot);

      var movement = new InventoryMovement();
      movement.setCompanyId(companyId);
      movement.setProductId(productId);
      movement.setLotId(lot.getId());
      movement.setType("SALE_OUT");
      movement.setQty(take);
      movement.setRefType("SALE");
      movement.setRefId(saleId);
      movements.save(movement);

      var allocation = new SaleLotAllocation();
      allocation.setSaleId(saleId);
      allocation.setProductId(productId);
      allocation.setLotId(lot.getId());
      allocation.setQty(take);
      allocations.save(allocation);

      remaining = remaining.subtract(take);
    }

    if (remaining.compareTo(BigDecimal.ZERO) > 0) {
      throw new IllegalStateException("Insufficient stock for product " + productId + " (missing " + remaining + ")");
    }
  }

  @Transactional
  public void restockSale(UUID saleId) {
    UUID companyId = companyContext.require();
    List<SaleLotAllocation> saleAllocations = allocations.findBySaleId(saleId);
    if (saleAllocations.isEmpty()) {
      return;
    }
    for (SaleLotAllocation allocation : saleAllocations) {
      InventoryLot lot = lots.findById(allocation.getLotId())
        .orElseThrow(() -> new IllegalStateException("Lot not found for allocation " + allocation.getLotId()));
      lot.setQtyAvailable(lot.getQtyAvailable().add(allocation.getQty()));
      lots.save(lot);

      InventoryMovement movement = new InventoryMovement();
      movement.setCompanyId(companyId);
      movement.setProductId(allocation.getProductId());
      movement.setLotId(lot.getId());
      movement.setType("SALE_CANCEL");
      movement.setQty(allocation.getQty());
      movement.setRefType("SALE");
      movement.setRefId(saleId);
      movements.save(movement);
    }
    allocations.deleteBySaleId(saleId);
  }

  @Transactional(readOnly = true)
  public List<InventoryAlert> lowStock(BigDecimal thresholdOverride) {
    UUID companyId = companyContext.require();
    BigDecimal threshold = resolveThreshold(companyId, thresholdOverride);
    return lots.findTop5ByCompanyIdAndQtyAvailableLessThanOrderByQtyAvailableAsc(companyId, threshold)
      .stream()
      .map(lot -> new InventoryAlert(
        lot.getId(),
        lot.getProductId(),
        lot.getQtyAvailable(),
        lot.getCreatedAt(),
        lot.getExpDate()
      ))
      .toList();
  }

  @Transactional(readOnly = true)
  public List<InventoryLot> lotsForProduct(UUID productId) {
    UUID companyId = companyContext.require();
    validateProductBelongsToCompany(productId, companyId);
    return lots.findByCompanyIdAndProductIdOrderByCreatedAtAsc(companyId, productId);
  }

  @Transactional(readOnly = true)
  public BigDecimal getTotalStock(UUID productId) {
    UUID companyId = companyContext.require();
    List<InventoryLot> productLots = lots.findByCompanyIdAndProductIdOrderByCreatedAtAsc(companyId, productId);
    return productLots.stream()
      .map(InventoryLot::getQtyAvailable)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  @Transactional(readOnly = true)
  public InventorySummary summary() {
    UUID companyId = companyContext.require();
    BigDecimal totalValue = lots.sumInventoryValue(companyId);
    long active = productRepository.countByDeletedAtIsNullAndActiveTrue();
    long inactive = productRepository.countByDeletedAtIsNullAndActiveFalse();
    long total = active + inactive;
    InventorySettings settings = ensureSettings(companyId);
    BigDecimal threshold = settings.getLowStockThreshold();
    long alerts = lots.countByCompanyIdAndQtyAvailableLessThan(companyId, threshold);
    return new InventorySummary(totalValue, active, inactive, total, alerts, threshold);
  }

  @Transactional(readOnly = true)
  public Page<InventoryMovementSummary> listMovementsSummary(
      UUID productId,
      String type,
      OffsetDateTime from,
      OffsetDateTime to,
      Pageable pageable) {
    UUID companyId = companyContext.require();
    
    List<InventoryMovement> movementList = movements.findByCompanyIdOrderByCreatedAtDesc(companyId);
    
    // Filtrar por producto si se especifica
    if (productId != null) {
      movementList = movementList.stream()
          .filter(m -> m.getProductId().equals(productId))
          .collect(Collectors.toList());
    }
    
    // Filtrar por tipo si se especifica
    if (type != null && !type.isEmpty()) {
      movementList = movementList.stream()
          .filter(m -> m.getType().equalsIgnoreCase(type))
          .collect(Collectors.toList());
    }
    
    // Filtrar por rango de fechas
    if (from != null) {
      movementList = movementList.stream()
          .filter(m -> m.getCreatedAt().isAfter(from) || m.getCreatedAt().isEqual(from))
          .collect(Collectors.toList());
    }
    if (to != null) {
      movementList = movementList.stream()
          .filter(m -> m.getCreatedAt().isBefore(to) || m.getCreatedAt().isEqual(to))
          .collect(Collectors.toList());
    }
    
    // Obtener nombres de productos
    List<UUID> productIds = movementList.stream()
        .map(InventoryMovement::getProductId)
        .distinct()
        .toList();
    
    Map<UUID, String> productNames = productRepository.findAllById(productIds).stream()
        .collect(Collectors.toMap(Product::getId, Product::getName));
    
    // Convertir a DTOs
    List<InventoryMovementSummary> summaries = movementList.stream()
        .map(m -> new InventoryMovementSummary(
            m.getId(),
            m.getProductId(),
            productNames.getOrDefault(m.getProductId(), "Desconocido"),
            m.getLotId(),
            m.getType(),
            m.getQty(),
            m.getRefType(),
            m.getRefId(),
            m.getNote(),
            m.getCreatedBy(),
            m.getUserIp(),
            m.getReasonCode(),
            m.getPreviousQty(),
            m.getNewQty(),
            m.getCreatedAt()
        ))
        .toList();
    
    // Paginación manual
    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), summaries.size());
    List<InventoryMovementSummary> pageContent = start < summaries.size() ? summaries.subList(start, end) : List.of();
    
    return new PageImpl<>(pageContent, pageable, summaries.size());
  }

  @Transactional(readOnly = true)
  public InventorySettingsResponse getSettings() {
    UUID companyId = companyContext.require();
    InventorySettings settings = ensureSettings(companyId);
    return new InventorySettingsResponse(settings.getLowStockThreshold(), settings.getUpdatedAt());
  }

  @Transactional
  public InventorySettingsResponse updateSettings(InventorySettingsUpdateRequest req) {
    UUID companyId = companyContext.require();
    InventorySettings settings = ensureSettings(companyId);
    settings.setLowStockThreshold(req.lowStockThreshold());
    InventorySettings saved = settingsRepository.save(settings);
    return new InventorySettingsResponse(saved.getLowStockThreshold(), saved.getUpdatedAt());
  }

  @Transactional
  public InventoryAdjustmentResponse adjust(InventoryAdjustmentRequest req) {
    UUID companyId = companyContext.require();
    validateProductBelongsToCompany(req.productId(), companyId);
    boolean increase = isIncrease(req.direction());
    BigDecimal quantity = req.quantity();
    if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Quantity must be greater than zero");
    }
    String reason = req.reason() == null ? "" : req.reason().trim();
    if (increase) {
      handleIncrease(companyId, req, quantity, reason);
    } else {
      handleDecrease(companyId, req, quantity, reason);
    }
    return new InventoryAdjustmentResponse(req.productId(), quantity, increase ? "increase" : "decrease");
  }

  private void handleIncrease(UUID companyId, InventoryAdjustmentRequest req, BigDecimal quantity, String reason) {
    InventoryLot lot;
    BigDecimal previousQty = BigDecimal.ZERO;
    
    if (req.lotId() != null) {
      lot = lots.findById(req.lotId())
        .orElseThrow(() -> new IllegalArgumentException("Lot not found: " + req.lotId()));
      validateLotOwnership(lot, companyId, req.productId());
      previousQty = lot.getQtyAvailable();
      lot.setQtyAvailable(lot.getQtyAvailable().add(quantity));
      if (req.unitCost() != null) {
        lot.setCostUnit(req.unitCost());
      }
      if (req.mfgDate() != null) {
        lot.setMfgDate(req.mfgDate());
      }
      if (req.expDate() != null) {
        lot.setExpDate(req.expDate());
      }
    } else {
      lot = new InventoryLot();
      lot.setCompanyId(companyId);
      lot.setProductId(req.productId());
      lot.setPurchaseItemId(null);
      lot.setQtyAvailable(quantity);
      lot.setCostUnit(req.unitCost() != null ? req.unitCost() : BigDecimal.ZERO);
      lot.setMfgDate(req.mfgDate());
      lot.setExpDate(req.expDate());
    }
    lots.save(lot);
    
    BigDecimal newQty = lot.getQtyAvailable();

    InventoryMovement movement = new InventoryMovement();
    movement.setCompanyId(companyId);
    movement.setProductId(req.productId());
    movement.setLotId(lot.getId());
    movement.setType("MANUAL_IN");
    movement.setQty(quantity);
    movement.setRefType("MANUAL");
    movement.setRefId(lot.getId());
    movement.setNote(reason);
    enrichMovementWithAudit(movement, "ADJUSTMENT", previousQty, newQty);
    movements.save(movement);
  }

  private void handleDecrease(UUID companyId, InventoryAdjustmentRequest req, BigDecimal quantity, String reason) {
    BigDecimal remaining = quantity;
    List<InventoryLot> candidates = new ArrayList<>();
    if (req.lotId() != null) {
      InventoryLot lot = lots.findById(req.lotId())
        .orElseThrow(() -> new IllegalArgumentException("Lot not found: " + req.lotId()));
      validateLotOwnership(lot, companyId, req.productId());
      candidates.add(lot);
    } else {
      candidates.addAll(lots.findByCompanyIdAndProductIdOrderByCreatedAtAsc(companyId, req.productId()));
    }

    for (InventoryLot lot : candidates) {
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
        break;
      }
      if (lot.getQtyAvailable().compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      
      BigDecimal previousQty = lot.getQtyAvailable();
      BigDecimal take = lot.getQtyAvailable().min(remaining);
      lot.setQtyAvailable(lot.getQtyAvailable().subtract(take));
      lots.save(lot);
      BigDecimal newQty = lot.getQtyAvailable();

      InventoryMovement movement = new InventoryMovement();
      movement.setCompanyId(companyId);
      movement.setProductId(req.productId());
      movement.setLotId(lot.getId());
      movement.setType("MANUAL_OUT");
      movement.setQty(take);
      movement.setRefType("MANUAL");
      movement.setRefId(lot.getId());
      movement.setNote(reason);
      enrichMovementWithAudit(movement, "ADJUSTMENT", previousQty, newQty);
      movements.save(movement);

      remaining = remaining.subtract(take);
    }

    if (remaining.compareTo(BigDecimal.ZERO) > 0) {
      throw new IllegalStateException("Insufficient stock to decrease " + quantity + " units for product " + req.productId());
    }
  }

  private void validateProductBelongsToCompany(UUID productId, UUID companyId) {
    boolean exists = productRepository.findById(productId).isPresent();
    if (!exists) {
      throw new IllegalArgumentException("Product not found for company: " + productId);
    }
  }

  private void validateLotOwnership(InventoryLot lot, UUID companyId, UUID productId) {
    if (!Objects.equals(lot.getCompanyId(), companyId)) {
      throw new IllegalArgumentException("Lot does not belong to company: " + lot.getId());
    }
    if (!Objects.equals(lot.getProductId(), productId)) {
      throw new IllegalArgumentException("Lot " + lot.getId() + " does not match product " + productId);
    }
  }

  private boolean isIncrease(String direction) {
    String normalized = direction == null ? "" : direction.toLowerCase(Locale.ROOT);
    return switch (normalized) {
      case "increase", "in", "add" -> true;
      case "decrease", "out", "remove" -> false;
      default -> throw new IllegalArgumentException("Unsupported direction: " + direction);
    };
  }

  private BigDecimal resolveThreshold(UUID companyId, BigDecimal override) {
    if (override != null && override.compareTo(BigDecimal.ZERO) > 0) {
      return override;
    }
    InventorySettings settings = ensureSettings(companyId);
    BigDecimal threshold = settings.getLowStockThreshold();
    return threshold != null && threshold.compareTo(BigDecimal.ZERO) > 0 ? threshold : FALLBACK_THRESHOLD;
  }

  private InventorySettings ensureSettings(UUID companyId) {
    return settingsRepository.findById(companyId).orElseGet(() -> {
      InventorySettings settings = new InventorySettings();
      settings.setCompanyId(companyId);
      settings.setLowStockThreshold(FALLBACK_THRESHOLD);
      return settingsRepository.save(settings);
    });
  }

  /**
   * Enriquece un movimiento con datos de auditoría
   */
  private void enrichMovementWithAudit(InventoryMovement movement, String reasonCode, BigDecimal previousQty, BigDecimal newQty) {
    movement.setCreatedBy(auditContext.getCurrentUser());
    movement.setUserIp(auditContext.getUserIp());
    movement.setReasonCode(reasonCode);
    movement.setPreviousQty(previousQty);
    movement.setNewQty(newQty);
  }

  /**
   * Obtiene KPIs ejecutivos consolidados del inventario
   */
  @Transactional(readOnly = true)
  public InventoryKPIs getKPIs() {
    UUID companyId = companyContext.require();
    OffsetDateTime now = OffsetDateTime.now();
    OffsetDateTime thirtyDaysAgo = now.minusDays(30);
    
    // Valor total del inventario
    BigDecimal totalInventoryValue = lots.sumInventoryValue(companyId);
    
    // Productos activos
    long activeProducts = productRepository.countByDeletedAtIsNullAndActiveTrue();
    
    // Stock crítico
    InventorySettings settings = ensureSettings(companyId);
    BigDecimal threshold = settings.getLowStockThreshold();
    long criticalStockProducts = lots.countByCompanyIdAndQtyAvailableLessThan(companyId, threshold);
    
    // Calcular dead stock y overstock (implementación simplificada)
    BigDecimal deadStockValue = BigDecimal.ZERO;
    long deadStockCount = 0;
    BigDecimal overstockValue = BigDecimal.ZERO;
    long overstockCount = 0;
    
    // Obtener todos los movimientos recientes para calcular turnover
    List<InventoryMovement> recentMovements = movements.findAll().stream()
        .filter(m -> m.getCompanyId().equals(companyId))
        .filter(m -> m.getCreatedAt() != null && m.getCreatedAt().isAfter(thirtyDaysAgo))
        .collect(Collectors.toList());
    
    BigDecimal totalOutflows = recentMovements.stream()
        .filter(m -> "sale".equals(m.getType()) || "adjustment_decrease".equals(m.getType()))
        .map(m -> m.getQty() != null ? m.getQty().abs() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal avgInventory = totalInventoryValue.compareTo(BigDecimal.ZERO) > 0 
        ? totalInventoryValue 
        : BigDecimal.ONE;
    
    // Turnover ratio = Outflows / Average Inventory (anualizado)
    BigDecimal turnoverRatio = totalOutflows.multiply(BigDecimal.valueOf(12))
        .divide(avgInventory, 2, java.math.RoundingMode.HALF_UP);
    
    // Días de cobertura = 30 / (totalOutflows / avgInventory)
    Integer stockCoverageDays = totalOutflows.compareTo(BigDecimal.ZERO) > 0
        ? avgInventory.multiply(BigDecimal.valueOf(30))
            .divide(totalOutflows, 0, java.math.RoundingMode.HALF_UP)
            .intValue()
        : null;
    
    // Days Inventory on Hand (DOH)
    Integer daysInventoryOnHand = stockCoverageDays;
    
    // Lead time promedio (simulado - en producción vendría de órdenes de compra)
    Integer averageLeadTimeDays = 7; // Placeholder
    
    InventoryKPIs kpis = new InventoryKPIs();
    kpis.setStockCoverageDays(stockCoverageDays);
    kpis.setTurnoverRatio(turnoverRatio);
    kpis.setDeadStockValue(deadStockValue);
    kpis.setDeadStockCount(deadStockCount);
    kpis.setAverageLeadTimeDays(averageLeadTimeDays);
    kpis.setTotalInventoryValue(totalInventoryValue);
    kpis.setActiveProducts(activeProducts);
    kpis.setCriticalStockProducts(criticalStockProducts);
    kpis.setDaysInventoryOnHand(daysInventoryOnHand);
    kpis.setOverstockValue(overstockValue);
    kpis.setOverstockCount(overstockCount);
    
    return kpis;
  }

  /**
   * Obtiene estadísticas de movimientos de stock (últimos 30 días)
   */
  @Transactional(readOnly = true)
  public StockMovementStats getMovementStats() {
    UUID companyId = companyContext.require();
    OffsetDateTime thirtyDaysAgo = OffsetDateTime.now().minusDays(30);
    
    List<InventoryMovement> recentMovements = movements.findAll().stream()
        .filter(m -> m.getCompanyId().equals(companyId))
        .filter(m -> m.getCreatedAt() != null && m.getCreatedAt().isAfter(thirtyDaysAgo))
        .collect(Collectors.toList());
    
    // Calcular entradas y salidas
    BigDecimal totalInflows = BigDecimal.ZERO;
    BigDecimal totalOutflows = BigDecimal.ZERO;
    long inflowTransactions = 0;
    long outflowTransactions = 0;
    
    Map<UUID, List<InventoryMovement>> movementsByProduct = recentMovements.stream()
        .collect(Collectors.groupingBy(InventoryMovement::getProductId));
    
    for (InventoryMovement movement : recentMovements) {
      BigDecimal qty = movement.getQty() != null ? movement.getQty() : BigDecimal.ZERO;
      String type = movement.getType();
      
      if ("purchase".equals(type) || "adjustment_increase".equals(type)) {
        totalInflows = totalInflows.add(qty.abs());
        inflowTransactions++;
      } else if ("sale".equals(type) || "adjustment_decrease".equals(type)) {
        totalOutflows = totalOutflows.add(qty.abs());
        outflowTransactions++;
      }
    }
    
    // Top 5 productos con más entradas
    List<StockMovementStats.ProductMovement> topInflowProducts = movementsByProduct.entrySet().stream()
        .map(entry -> {
          UUID productId = entry.getKey();
          List<InventoryMovement> movements = entry.getValue();
          
          BigDecimal totalQty = movements.stream()
              .filter(m -> "purchase".equals(m.getType()) || "adjustment_increase".equals(m.getType()))
              .map(m -> m.getQty() != null ? m.getQty().abs() : BigDecimal.ZERO)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
          
          long count = movements.stream()
              .filter(m -> "purchase".equals(m.getType()) || "adjustment_increase".equals(m.getType()))
              .count();
          
          Product product = productRepository.findById(productId).orElse(null);
          String productName = product != null ? product.getName() : productId.toString();
          
          return new StockMovementStats.ProductMovement(
              productId.toString(),
              productName,
              totalQty,
              totalQty, // Placeholder para valor
              count
          );
        })
        .sorted((a, b) -> b.getQuantity().compareTo(a.getQuantity()))
        .limit(5)
        .collect(Collectors.toList());
    
    // Top 5 productos con más salidas
    List<StockMovementStats.ProductMovement> topOutflowProducts = movementsByProduct.entrySet().stream()
        .map(entry -> {
          UUID productId = entry.getKey();
          List<InventoryMovement> movements = entry.getValue();
          
          BigDecimal totalQty = movements.stream()
              .filter(m -> "sale".equals(m.getType()) || "adjustment_decrease".equals(m.getType()))
              .map(m -> m.getQty() != null ? m.getQty().abs() : BigDecimal.ZERO)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
          
          long count = movements.stream()
              .filter(m -> "sale".equals(m.getType()) || "adjustment_decrease".equals(m.getType()))
              .count();
          
          Product product = productRepository.findById(productId).orElse(null);
          String productName = product != null ? product.getName() : productId.toString();
          
          return new StockMovementStats.ProductMovement(
              productId.toString(),
              productName,
              totalQty,
              totalQty,
              count
          );
        })
        .sorted((a, b) -> b.getQuantity().compareTo(a.getQuantity()))
        .limit(5)
        .collect(Collectors.toList());
    
    // Velocidad por categoría (simplificado)
    List<StockMovementStats.CategoryVelocity> categoryVelocities = new ArrayList<>();
    
    StockMovementStats stats = new StockMovementStats();
    stats.setTotalInflows(totalInflows);
    stats.setTotalOutflows(totalOutflows);
    stats.setInflowTransactions(inflowTransactions);
    stats.setOutflowTransactions(outflowTransactions);
    stats.setTopInflowProducts(topInflowProducts);
    stats.setTopOutflowProducts(topOutflowProducts);
    stats.setCategoryVelocities(categoryVelocities);
    
    return stats;
  }
  
  /**
   * Análisis ABC de inventario
   * Clasifica productos según método ABC (Pareto):
   * - Clase A: 20% productos que representan 80% del valor
   * - Clase B: 30% productos que representan 15% del valor  
   * - Clase C: 50% productos que representan 5% del valor
   * 
   * Basado en valor de inventario actual (qty disponible * costo)
   */
  public List<ProductABCClassification> getABCAnalysis() {
    // Obtener todos los productos activos
    List<Product> allProducts = productRepository.findAll().stream()
      .filter(p -> p.getActive() != null && p.getActive())
      .collect(Collectors.toList());
    
    // Obtener todos los lotes de inventario
    List<InventoryLot> allLots = lots.findAll();
    
    // Agrupar por producto y calcular valor total
    Map<UUID, ProductInventoryValue> productValues = new HashMap<>();
    
    for (InventoryLot lot : allLots) {
      UUID productId = lot.getProductId();
      ProductInventoryValue value = productValues.getOrDefault(
        productId,
        new ProductInventoryValue()
      );
      
      BigDecimal costUnit = lot.getCostUnit() != null ? lot.getCostUnit() : BigDecimal.ZERO;
      double lotValue = lot.getQtyAvailable().doubleValue() * costUnit.doubleValue();
      value.addValue(lotValue);
      value.addQuantity(lot.getQtyAvailable().doubleValue());
      value.incrementLotCount();
      
      // Actualizar última fecha de movimiento si existe
      if (lot.getCreatedAt() != null) {
        LocalDate lotDate = lot.getCreatedAt().toLocalDate();
        if (value.getLastMovementDate() == null || lotDate.isAfter(value.getLastMovementDate())) {
          value.setLastMovementDate(lotDate);
        }
      }
      
      productValues.put(productId, value);
    }
    
    // Calcular valor total de inventario
    double totalValue = productValues.values().stream()
      .mapToDouble(ProductInventoryValue::getTotalValue)
      .sum();
    
    // Si no hay inventario, devolver lista vacía
    if (totalValue == 0) {
      return new ArrayList<>();
    }
    
    // Crear clasificaciones
    List<ProductABCClassification> classifications = new ArrayList<>();
    
    for (Map.Entry<UUID, ProductInventoryValue> entry : productValues.entrySet()) {
      UUID productId = entry.getKey();
      ProductInventoryValue value = entry.getValue();
      
      // Buscar información del producto
      Product product = allProducts.stream()
        .filter(p -> p.getId().equals(productId))
        .findFirst()
        .orElse(null);
      
      if (product == null) continue;
      
      double percentage = (value.getTotalValue() / totalValue) * 100;
      
      ProductABCClassification classification = new ProductABCClassification();
      classification.setProductId(productId.getMostSignificantBits());
      classification.setProductName(product.getName());
      classification.setCategory(product.getCategory() != null ? product.getCategory() : "Sin categoría");
      classification.setTotalValue(value.getTotalValue());
      classification.setTotalQuantity(value.getTotalQuantity());
      classification.setPercentageOfTotalValue(percentage);
      classification.setSalesFrequency(value.getLotCount()); // Usar cantidad de lotes como proxy de frecuencia
      classification.setLastMovementDate(value.getLastMovementDate());
      
      classifications.add(classification);
    }
    
    // Ordenar por valor total descendente
    classifications.sort((a, b) -> Double.compare(b.getTotalValue(), a.getTotalValue()));
    
    // Calcular porcentajes acumulativos y asignar clasificaciones ABC
    double cumulativePercentage = 0.0;
    
    for (ProductABCClassification classification : classifications) {
      cumulativePercentage += classification.getPercentageOfTotalValue();
      classification.setCumulativePercentage(cumulativePercentage);
      
      // Asignar clasificación según porcentaje acumulativo
      if (cumulativePercentage <= 80.0) {
        classification.setClassification("A");
      } else if (cumulativePercentage <= 95.0) {
        classification.setClassification("B");
      } else {
        classification.setClassification("C");
      }
    }
    
    return classifications;
  }
  
  /**
   * Clase auxiliar para calcular valor de inventario por producto
   */
  private static class ProductInventoryValue {
    private double totalValue = 0.0;
    private int totalQuantity = 0;
    private int lotCount = 0;
    private LocalDate lastMovementDate;
    
    public void addValue(double value) {
      this.totalValue += value;
    }
    
    public void addQuantity(double quantity) {
      this.totalQuantity += (int) quantity;
    }
    
    public void incrementLotCount() {
      this.lotCount++;
    }
    
    public double getTotalValue() {
      return totalValue;
    }
    
    public int getTotalQuantity() {
      return totalQuantity;
    }
    
    public int getLotCount() {
      return lotCount;
    }
    
    public LocalDate getLastMovementDate() {
      return lastMovementDate;
    }
    
    public void setLastMovementDate(LocalDate date) {
      this.lastMovementDate = date;
    }
  }
  
  /**
   * Genera pronósticos de demanda usando Moving Average (90 días)
   * @param productId Filtro opcional por producto
   * @param forecastDays Número de días a pronosticar (default: 30)
   * @return Lista de pronósticos ordenados por prioridad (Clase A primero)
   */
  public List<com.datakomerz.pymes.inventory.dto.InventoryForecast> getForecastAnalysis(Long productId, Integer forecastDays) {
    UUID companyId = companyContext.require();
    
    if (forecastDays == null || forecastDays <= 0) {
      forecastDays = 30;
    }
    
    // Obtener productos activos (filtrar por productId si se especifica)
    List<Product> products;
    if (productId != null) {
      products = productRepository.findById(UUID.fromString(productId.toString()))
          .filter(p -> p.getCompanyId().equals(companyId) && p.getActive())
          .map(List::of)
          .orElse(List.of());
    } else {
      products = productRepository.findAll().stream()
          .filter(p -> p.getCompanyId().equals(companyId) && p.getActive())
          .collect(Collectors.toList());
    }
    
    if (products.isEmpty()) {
      return List.of();
    }
    
    // Fecha de referencia: últimos 90 días
    LocalDate startDate = LocalDate.now().minusDays(90);
    LocalDate forecastDate = LocalDate.now().plusDays(forecastDays);
    
    List<com.datakomerz.pymes.inventory.dto.InventoryForecast> forecasts = new ArrayList<>();
    
    for (Product product : products) {
      UUID productUuid = product.getId();
      
      // Obtener movimientos de salida de los últimos 90 días
      List<InventoryMovement> movements90Days = movements.findAll().stream()
          .filter(m -> m.getCompanyId().equals(companyId))
          .filter(m -> m.getProductId().equals(productUuid))
          .filter(m -> "OUT".equals(m.getType()) || "SALE".equals(m.getReasonCode()))
          .filter(m -> m.getCreatedAt() != null)
          .filter(m -> {
            LocalDate movementDate = m.getCreatedAt().toLocalDate();
            return !movementDate.isBefore(startDate);
          })
          .collect(Collectors.toList());
      
      if (movements90Days.isEmpty()) {
        continue; // Sin historial suficiente
      }
      
      // Calcular demanda total de los últimos 90 días
      BigDecimal totalDemand = movements90Days.stream()
          .map(m -> m.getQty().abs())
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      
      // Moving Average: demanda diaria promedio
      BigDecimal dailyAverage = totalDemand.divide(new BigDecimal(90), 2, java.math.RoundingMode.HALF_UP);
      
      // Pronóstico: dailyAverage * forecastDays
      BigDecimal predictedDemand = dailyAverage.multiply(new BigDecimal(forecastDays));
      
      // Calcular tendencia (comparar últimos 30 días vs 30 días previos)
      LocalDate last30Start = LocalDate.now().minusDays(30);
      BigDecimal last30Demand = movements90Days.stream()
          .filter(m -> {
            LocalDate movementDate = m.getCreatedAt().toLocalDate();
            return !movementDate.isBefore(last30Start);
          })
          .map(m -> m.getQty().abs())
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      
      LocalDate prev30Start = LocalDate.now().minusDays(60);
      LocalDate prev30End = LocalDate.now().minusDays(30);
      BigDecimal prev30Demand = movements90Days.stream()
          .filter(m -> {
            LocalDate movementDate = m.getCreatedAt().toLocalDate();
            return !movementDate.isBefore(prev30Start) && movementDate.isBefore(prev30End);
          })
          .map(m -> m.getQty().abs())
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      
      String trend = "stable";
      if (prev30Demand.compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal trendRatio = last30Demand.divide(prev30Demand, 2, java.math.RoundingMode.HALF_UP);
        if (trendRatio.compareTo(new BigDecimal("1.15")) >= 0) {
          trend = "increasing";
        } else if (trendRatio.compareTo(new BigDecimal("0.85")) <= 0) {
          trend = "decreasing";
        }
      }
      
      // Calcular confianza basada en consistencia de datos
      // Más movimientos = mayor confianza
      BigDecimal confidence = new BigDecimal(Math.min(100, movements90Days.size() * 5));
      
      // Stock actual
      BigDecimal currentStock = lots.findAll().stream()
          .filter(lot -> lot.getCompanyId().equals(companyId))
          .filter(lot -> lot.getProductId().equals(productUuid))
          .map(InventoryLot::getQtyAvailable)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      
      // Días de stock restantes
      Integer daysOfStock = 0;
      if (dailyAverage.compareTo(BigDecimal.ZERO) > 0) {
        daysOfStock = currentStock.divide(dailyAverage, 0, java.math.RoundingMode.DOWN).intValue();
      }
      
      // Determinar estado de stock
      String stockStatus;
      BigDecimal recommendedOrderQty = BigDecimal.ZERO;
      
      if (daysOfStock < 15) {
        stockStatus = "understocked";
        // Recomendar cantidad para cubrir forecastDays + buffer de 15 días
        BigDecimal targetStock = dailyAverage.multiply(new BigDecimal(forecastDays + 15));
        recommendedOrderQty = targetStock.subtract(currentStock).max(BigDecimal.ZERO);
      } else if (daysOfStock > 60) {
        stockStatus = "overstocked";
      } else {
        stockStatus = "optimal";
      }
      
      com.datakomerz.pymes.inventory.dto.InventoryForecast forecast = 
          new com.datakomerz.pymes.inventory.dto.InventoryForecast(
              product.getId().getMostSignificantBits() & Long.MAX_VALUE,
              product.getName(),
              product.getCategory(),
              forecastDate,
              predictedDemand.setScale(2, java.math.RoundingMode.HALF_UP),
              confidence,
              dailyAverage.setScale(2, java.math.RoundingMode.HALF_UP),
              trend,
              recommendedOrderQty.setScale(2, java.math.RoundingMode.HALF_UP),
              stockStatus,
              currentStock.setScale(2, java.math.RoundingMode.HALF_UP),
              daysOfStock
          );
      
      forecasts.add(forecast);
    }
    
    // Ordenar por prioridad: understocked primero, luego por demanda predicha descendente
    forecasts.sort((f1, f2) -> {
      if ("understocked".equals(f1.getStockStatus()) && !"understocked".equals(f2.getStockStatus())) {
        return -1;
      }
      if (!"understocked".equals(f1.getStockStatus()) && "understocked".equals(f2.getStockStatus())) {
        return 1;
      }
      return f2.getPredictedDemand().compareTo(f1.getPredictedDemand());
    });
    
    return forecasts;
  }
  
  public Page<com.datakomerz.pymes.inventory.dto.InventoryMovementDetail> listMovements(
      UUID productId,
      UUID lotId,
      String type,
      OffsetDateTime dateFrom,
      OffsetDateTime dateTo,
      Pageable pageable) {
    UUID companyId = companyContext.require();
    
    Page<InventoryMovement> movementsPage = movements.findMovementsWithFilters(
        companyId, productId, lotId, type, dateFrom, dateTo, pageable);
    
    // Obtener todos los productIds y lotIds únicos
    List<UUID> productIds = movementsPage.getContent().stream()
        .map(InventoryMovement::getProductId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    
    List<UUID> lotIds = movementsPage.getContent().stream()
        .map(InventoryMovement::getLotId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    
    // Cargar productos y lotes en batch
    Map<UUID, Product> productsMap = new HashMap<>();
    if (!productIds.isEmpty()) {
      productRepository.findAllById(productIds).forEach(p -> productsMap.put(p.getId(), p));
    }
    
    Map<UUID, InventoryLot> lotsMap = new HashMap<>();
    if (!lotIds.isEmpty()) {
      lots.findAllById(lotIds).forEach(lot -> lotsMap.put(lot.getId(), lot));
    }
    
    // Cargar ubicaciones en batch
    List<UUID> locationIds = lotsMap.values().stream()
        .map(InventoryLot::getLocationId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    
    Map<UUID, Location> locationsMap = new HashMap<>();
    if (!locationIds.isEmpty()) {
      locationRepository.findAllById(locationIds).forEach(loc -> locationsMap.put(loc.getId(), loc));
    }
    
    List<com.datakomerz.pymes.inventory.dto.InventoryMovementDetail> details = 
        movementsPage.getContent().stream()
        .map(m -> {
          Product product = productsMap.get(m.getProductId());
          InventoryLot lot = m.getLotId() != null ? lotsMap.get(m.getLotId()) : null;
          Location location = lot != null && lot.getLocationId() != null ? locationsMap.get(lot.getLocationId()) : null;
          
          return new com.datakomerz.pymes.inventory.dto.InventoryMovementDetail(
              m.getId(),
              m.getType(),
              m.getCreatedAt(),
              m.getProductId(),
              product != null ? product.getSku() : null,
              product != null ? product.getName() : null,
              m.getLotId(),
              lot != null ? lot.getBatchName() : null,
              m.getQty(),
              lot != null ? lot.getLocationId() : null,
              location != null ? location.getCode() : null,
              location != null ? location.getName() : null,
              m.getRefType(),
              m.getRefId(),
              m.getNote(),
              m.getCreatedBy()
          );
        })
        .toList();
    
    return new PageImpl<>(details, pageable, movementsPage.getTotalElements());
  }
  
  @Transactional
  public InventoryLot transferLot(UUID lotId, com.datakomerz.pymes.inventory.dto.LotTransferRequest request) {
    UUID companyId = companyContext.require();
    
    InventoryLot lot = lots.findById(lotId)
        .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado: " + lotId));
    
    if (!lot.getCompanyId().equals(companyId)) {
      throw new IllegalArgumentException("Lote no pertenece a la empresa actual");
    }
    
    if (request.qty().compareTo(lot.getQtyAvailable()) > 0) {
      throw new IllegalArgumentException("Cantidad solicitada excede el stock disponible");
    }
    
    if (request.targetLocationId().equals(lot.getLocationId())) {
      throw new IllegalArgumentException("La ubicación destino es la misma que la actual");
    }
    
    // Registrar movimiento de salida de ubicación origen
    InventoryMovement outMovement = new InventoryMovement();
    outMovement.setCompanyId(companyId);
    outMovement.setProductId(lot.getProductId());
    outMovement.setLotId(lot.getId());
    outMovement.setType("TRANSFER_OUT");
    outMovement.setQty(request.qty().negate());
    outMovement.setPreviousQty(lot.getQtyAvailable());
    outMovement.setNewQty(lot.getQtyAvailable().subtract(request.qty()));
    outMovement.setNote("Transferencia a ubicación " + request.targetLocationId());
    outMovement.setCreatedBy(auditContext.getCurrentUser());
    outMovement.setUserIp(auditContext.getUserIp());
    movements.save(outMovement);
    
    // Actualizar cantidad del lote origen
    lot.setQtyAvailable(lot.getQtyAvailable().subtract(request.qty()));
    lots.save(lot);
    
    // Crear o actualizar lote en ubicación destino
    // Buscar lote existente del mismo producto y fechas en la ubicación destino
    List<InventoryLot> candidateLots = lots.findByCompanyIdAndProductIdAndLocationIdAndQtyAvailableGreaterThanOrderByExpDateAscCreatedAtAsc(
        companyId, lot.getProductId(), request.targetLocationId(), BigDecimal.ZERO);
    
    InventoryLot targetLot = candidateLots.stream()
        .filter(l -> Objects.equals(l.getBatchName(), lot.getBatchName()) &&
                     Objects.equals(l.getMfgDate(), lot.getMfgDate()) &&
                     Objects.equals(l.getExpDate(), lot.getExpDate()) &&
                     Objects.equals(l.getCostUnit(), lot.getCostUnit()))
        .findFirst()
        .orElse(null);
    
    if (targetLot != null) {
      // Lote existente, sumar cantidad
      targetLot.setQtyAvailable(targetLot.getQtyAvailable().add(request.qty()));
      lots.save(targetLot);
    } else {
      // Crear nuevo lote en ubicación destino
      targetLot = new InventoryLot();
      targetLot.setCompanyId(companyId);
      targetLot.setProductId(lot.getProductId());
      targetLot.setLocationId(request.targetLocationId());
      // Copiar campos del lote origen
      targetLot.setBatchName(lot.getBatchName());
      targetLot.setCostUnit(lot.getCostUnit());
      targetLot.setMfgDate(lot.getMfgDate());
      targetLot.setExpDate(lot.getExpDate());
      targetLot.setQtyAvailable(request.qty());
      lots.save(targetLot);
    }
    
    // Registrar movimiento de entrada en ubicación destino
    InventoryMovement inMovement = new InventoryMovement();
    inMovement.setCompanyId(companyId);
    inMovement.setProductId(lot.getProductId());
    inMovement.setLotId(targetLot.getId());
    inMovement.setType("TRANSFER_IN");
    inMovement.setQty(request.qty());
    inMovement.setPreviousQty(targetLot.getQtyAvailable().subtract(request.qty()));
    inMovement.setNewQty(targetLot.getQtyAvailable());
    inMovement.setNote(request.note() != null ? request.note() : "Transferencia desde lote " + lot.getId());
    inMovement.setCreatedBy(auditContext.getCurrentUser());
    inMovement.setUserIp(auditContext.getUserIp());
    movements.save(inMovement);
    
    return lot;
  }
}

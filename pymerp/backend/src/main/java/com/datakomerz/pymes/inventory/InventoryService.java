package com.datakomerz.pymes.inventory;

import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.datakomerz.pymes.inventory.dto.InventoryAdjustmentRequest;
import com.datakomerz.pymes.inventory.dto.InventoryAdjustmentResponse;
import com.datakomerz.pymes.inventory.dto.InventoryAlert;
import com.datakomerz.pymes.inventory.dto.InventoryKPIs;
import com.datakomerz.pymes.inventory.dto.LotDetailDTO;
import com.datakomerz.pymes.inventory.dto.InventoryMovementHistoryEntry;
import com.datakomerz.pymes.inventory.dto.InventoryMovementSummary;
import com.datakomerz.pymes.inventory.dto.InventorySettingsResponse;
import com.datakomerz.pymes.inventory.dto.InventorySettingsUpdateRequest;
import com.datakomerz.pymes.inventory.dto.InventorySummary;
import com.datakomerz.pymes.inventory.dto.LotReservationSummary;
import com.datakomerz.pymes.inventory.dto.StockByLocationAggregation;
import com.datakomerz.pymes.inventory.dto.StockByLocationResponse;
import com.datakomerz.pymes.inventory.dto.StockMovementStats;
import com.datakomerz.pymes.inventory.dto.ProductABCClassification;
import com.datakomerz.pymes.multitenancy.CrossTenantAccessException;
import com.datakomerz.pymes.multitenancy.TenantFilterEnabler;
import com.datakomerz.pymes.multitenancy.ValidateTenant;
import com.datakomerz.pymes.products.Product;
import com.datakomerz.pymes.products.ProductRepository;
import com.datakomerz.pymes.purchases.Purchase;
import com.datakomerz.pymes.purchases.PurchaseRepository;
import com.datakomerz.pymes.sales.SaleLotAllocation;
import com.datakomerz.pymes.sales.SaleLotAllocationRepository;
import com.datakomerz.pymes.suppliers.Supplier;
import com.datakomerz.pymes.suppliers.SupplierRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class InventoryService {
  private static final BigDecimal FALLBACK_THRESHOLD = new BigDecimal("5");

  private final InventoryLotRepository lots;
  private final InventoryMovementRepository movements;
  private final SaleLotAllocationRepository allocations;
  private final InventorySettingsRepository settingsRepository;
  private final ProductRepository productRepository;
  private final InventoryLocationRepository inventoryLocationRepository;
  private final PurchaseRepository purchaseRepository;
  private final SupplierRepository supplierRepository;
  private final EntityManager entityManager;
  private final TenantFilterEnabler tenantFilterEnabler;
  private final CompanyContext companyContext;
  private final AuditContextService auditContext;

  public InventoryService(InventoryLotRepository lots,
                          InventoryMovementRepository movements,
                          SaleLotAllocationRepository allocations,
                          InventorySettingsRepository settingsRepository,
                          ProductRepository productRepository,
                          InventoryLocationRepository inventoryLocationRepository,
                          PurchaseRepository purchaseRepository,
                          SupplierRepository supplierRepository,
                          EntityManager entityManager,
                          TenantFilterEnabler tenantFilterEnabler,
                          CompanyContext companyContext,
                          AuditContextService auditContext) {
    this.lots = lots;
    this.movements = movements;
    this.allocations = allocations;
    this.settingsRepository = settingsRepository;
    this.productRepository = productRepository;
    this.inventoryLocationRepository = inventoryLocationRepository;
    this.purchaseRepository = purchaseRepository;
    this.supplierRepository = supplierRepository;
    this.entityManager = entityManager;
    this.tenantFilterEnabler = tenantFilterEnabler;
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

      BigDecimal take = lot.getQtyAvailable().min(remaining);
      BigDecimal previousQty = lot.getQtyAvailable();
      BigDecimal newQty = previousQty.subtract(take);
      lot.setQtyAvailable(newQty);
      lots.save(lot);

      var movement = new InventoryMovement();
      movement.setCompanyId(companyId);
      movement.setProductId(productId);
      movement.setLotId(lot.getId());
      movement.setType("SALE_OUT");
      movement.setQty(take);
      movement.setRefType("SALE");
      movement.setRefId(saleId);
      movement.setLocationFromId(lot.getLocationId());
      movement.setLocationToId(null);
      enrichMovementWithAudit(movement, "SALE", previousQty, newQty);
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
      BigDecimal previousQty = lot.getQtyAvailable();
      BigDecimal newQty = previousQty.add(allocation.getQty());
      lot.setQtyAvailable(newQty);
      lots.save(lot);

      InventoryMovement movement = new InventoryMovement();
      movement.setCompanyId(companyId);
      movement.setProductId(allocation.getProductId());
      movement.setLotId(lot.getId());
      movement.setType("SALE_CANCEL");
      movement.setQty(allocation.getQty());
      movement.setRefType("SALE");
      movement.setRefId(saleId);
      movement.setLocationFromId(null);
      movement.setLocationToId(lot.getLocationId());
      enrichMovementWithAudit(movement, "SALE_CANCEL", previousQty, newQty);
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
  public Page<LotDetailDTO> listLots(
      String q,
      String status,
      UUID productId,
      UUID supplierId,
      UUID locationId,
      LocalDate ingressFrom,
      LocalDate ingressTo,
      LocalDate expiryFrom,
      LocalDate expiryTo,
      Pageable pageable) {
    UUID companyId = companyContext.require();
    if (supplierId != null) {
      Supplier supplier = supplierRepository.findById(supplierId)
          .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado: " + supplierId));
      if (!Objects.equals(supplier.getCompanyId(), companyId)) {
        throw new CrossTenantAccessException("Proveedor " + supplierId + " pertenece a otra empresa");
      }
    }

    CriteriaBuilder cb = entityManager.getCriteriaBuilder();

    CriteriaQuery<InventoryLot> criteriaQuery = cb.createQuery(InventoryLot.class);
    Root<InventoryLot> lotRoot = criteriaQuery.from(InventoryLot.class);
    List<Predicate> predicates = buildLotDetailPredicates(cb, criteriaQuery, lotRoot, companyId, q, productId,
        supplierId, locationId, ingressFrom, ingressTo, expiryFrom, expiryTo);
    criteriaQuery.where(predicates.toArray(Predicate[]::new));
    criteriaQuery.orderBy(cb.desc(lotRoot.get("createdAt")));

    List<InventoryLot> lotsPage = entityManager.createQuery(criteriaQuery)
        .setFirstResult((int) pageable.getOffset())
        .setMaxResults(pageable.getPageSize())
        .getResultList();

    CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
    Root<InventoryLot> countRoot = countQuery.from(InventoryLot.class);
    List<Predicate> countPredicates = buildLotDetailPredicates(cb, countQuery, countRoot, companyId, q, productId,
        supplierId, locationId, ingressFrom, ingressTo, expiryFrom, expiryTo);
    countQuery.select(cb.count(countRoot));
    Long total = entityManager.createQuery(countQuery).getSingleResult();

    List<LotDetailDTO> dtos = mapLotsToDetailDtos(companyId, lotsPage);

    if (StringUtils.hasText(status)) {
      dtos = dtos.stream()
          .filter(dto -> status.equalsIgnoreCase(dto.status()))
          .toList();
      total = (long) dtos.size();
    }

    return new PageImpl<>(dtos, pageable, total);
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

  private List<Predicate> buildLotDetailPredicates(CriteriaBuilder cb,
                                                   CriteriaQuery<?> query,
                                                   Root<InventoryLot> lotRoot,
                                                   UUID companyId,
                                                   String q,
                                                   UUID productId,
                                                   UUID supplierId,
                                                   UUID locationId,
                                                   LocalDate ingressFrom,
                                                   LocalDate ingressTo,
                                                   LocalDate expiryFrom,
                                                   LocalDate expiryTo) {
    List<Predicate> predicates = new ArrayList<>();
    predicates.add(cb.equal(lotRoot.get("companyId"), companyId));

    if (productId != null) {
      predicates.add(cb.equal(lotRoot.get("productId"), productId));
    }
    if (locationId != null) {
      predicates.add(cb.equal(lotRoot.get("locationId"), locationId));
    }
    if (ingressFrom != null) {
      predicates.add(cb.greaterThanOrEqualTo(lotRoot.get("createdAt").as(LocalDate.class), ingressFrom));
    }
    if (ingressTo != null) {
      predicates.add(cb.lessThanOrEqualTo(lotRoot.get("createdAt").as(LocalDate.class), ingressTo));
    }
    if (expiryFrom != null) {
      predicates.add(cb.greaterThanOrEqualTo(lotRoot.get("expDate"), expiryFrom));
    }
    if (expiryTo != null) {
      predicates.add(cb.lessThanOrEqualTo(lotRoot.get("expDate"), expiryTo));
    }
    if (supplierId != null) {
      Subquery<UUID> supplierFilter = query.subquery(UUID.class);
      Root<Purchase> purchaseRoot = supplierFilter.from(Purchase.class);
      supplierFilter.select(purchaseRoot.get("id"));
      supplierFilter.where(
          cb.equal(purchaseRoot.get("companyId"), companyId),
          cb.equal(purchaseRoot.get("id"), lotRoot.get("purchaseId")),
          cb.equal(purchaseRoot.get("supplierId"), supplierId));
      predicates.add(cb.exists(supplierFilter));
    }
    if (StringUtils.hasText(q)) {
      String searchPattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
      Predicate batchPredicate = cb.like(cb.lower(cb.coalesce(lotRoot.get("batchName"), "")), searchPattern);

      Subquery<UUID> productMatch = query.subquery(UUID.class);
      Root<Product> productRoot = productMatch.from(Product.class);
      productMatch.select(productRoot.get("id"));
      productMatch.where(
          cb.equal(productRoot.get("companyId"), companyId),
          cb.equal(productRoot.get("id"), lotRoot.get("productId")),
          cb.or(
              cb.like(cb.lower(cb.coalesce(productRoot.get("name"), "")), searchPattern),
              cb.like(cb.lower(cb.coalesce(productRoot.get("sku"), "")), searchPattern)
          )
      );

      Subquery<UUID> supplierMatch = query.subquery(UUID.class);
      Root<Purchase> purchaseRoot = supplierMatch.from(Purchase.class);
      Root<Supplier> supplierRoot = supplierMatch.from(Supplier.class);
      supplierMatch.select(purchaseRoot.get("id"));
      supplierMatch.where(
          cb.equal(purchaseRoot.get("companyId"), companyId),
          cb.equal(purchaseRoot.get("id"), lotRoot.get("purchaseId")),
          cb.equal(supplierRoot.get("companyId"), companyId),
          cb.equal(supplierRoot.get("id"), purchaseRoot.get("supplierId")),
          cb.like(cb.lower(cb.coalesce(supplierRoot.get("name"), "")), searchPattern)
      );

      predicates.add(cb.or(batchPredicate, cb.exists(productMatch), cb.exists(supplierMatch)));
    }

    return predicates;
  }

  private List<LotDetailDTO> mapLotsToDetailDtos(UUID companyId, List<InventoryLot> lotEntities) {
    if (lotEntities.isEmpty()) {
      return List.of();
    }

    List<UUID> lotIds = lotEntities.stream()
        .map(InventoryLot::getId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();

    Map<UUID, BigDecimal> reservedMap = reservedQuantities(companyId, lotIds);
    List<UUID> productIds = lotEntities.stream()
        .map(InventoryLot::getProductId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    Map<UUID, Product> productMap = productRepository.findAllById(productIds)
        .stream()
        .collect(Collectors.toMap(Product::getId, p -> p));

    List<UUID> locationIds = lotEntities.stream()
        .map(InventoryLot::getLocationId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    Map<UUID, InventoryLocation> locationMap = new HashMap<>();
    if (!locationIds.isEmpty()) {
      inventoryLocationRepository.findAllById(locationIds)
          .forEach(loc -> locationMap.put(loc.getId(), loc));
    }

    List<UUID> purchaseIds = lotEntities.stream()
        .map(InventoryLot::getPurchaseId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    Map<UUID, Purchase> purchaseMap = purchaseRepository.findAllById(purchaseIds)
        .stream()
        .collect(Collectors.toMap(Purchase::getId, p -> p));

    List<UUID> supplierIds = purchaseMap.values().stream()
        .map(Purchase::getSupplierId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    Map<UUID, Supplier> supplierMap = supplierIds.isEmpty()
        ? Map.of()
        : supplierRepository.findAllById(supplierIds)
            .stream()
            .collect(Collectors.toMap(Supplier::getId, s -> s));

    return lotEntities.stream()
        .map(lot -> {
          Product product = productMap.get(lot.getProductId());
          InventoryLocation location = locationMap.get(lot.getLocationId());
          Purchase purchase = lot.getPurchaseId() == null ? null : purchaseMap.get(lot.getPurchaseId());
          Supplier supplier = purchase != null && purchase.getSupplierId() != null
              ? supplierMap.get(purchase.getSupplierId())
              : null;
          BigDecimal reserved = reservedMap.getOrDefault(lot.getId(), BigDecimal.ZERO);
          String statusText = calculateLotStatus(lot, product);
          return new LotDetailDTO(
              lot.getId(),
              lot.getBatchName(),
              product != null ? product.getId() : null,
              product != null ? product.getName() : null,
              product != null ? product.getSku() : null,
              supplier != null ? supplier.getId() : null,
              supplier != null ? supplier.getName() : null,
              lot.getLocationId(),
              location != null ? location.getCode() : null,
              location != null ? location.getName() : null,
              lot.getQtyAvailable(),
              reserved,
              statusText,
              lot.getCreatedAt() != null ? lot.getCreatedAt().toLocalDate() : null,
              lot.getExpDate(),
              lot.getCreatedAt());
        })
        .toList();
  }

  private String calculateLotStatus(InventoryLot lot, Product product) {
    LotStatus computedStatus = evaluateStatus(lot, product);
    return computedStatus != null ? computedStatus.name() : LotStatus.OK.name();
  }

  private LotStatus evaluateStatus(InventoryLot lot, Product product) {
    LocalDate today = LocalDate.now();
    LocalDate window = today.plusDays(30);
    LocalDate expDate = lot.getExpDate();
    if (expDate != null) {
      if (expDate.isBefore(today)) {
        return LotStatus.VENCIDO;
      }
      if (!expDate.isAfter(window)) {
        return LotStatus.POR_VENCER;
      }
    }
    BigDecimal reorderPoint = product != null && product.getCriticalStock() != null
        ? product.getCriticalStock()
        : BigDecimal.ZERO;
    if (lot.getQtyAvailable() != null && reorderPoint.compareTo(BigDecimal.ZERO) > 0
        && lot.getQtyAvailable().compareTo(reorderPoint) < 0) {
      return LotStatus.BAJO_STOCK;
    }
    return LotStatus.OK;
  }

  private Map<UUID, BigDecimal> reservedQuantities(UUID companyId, List<UUID> lotIds) {
    if (lotIds.isEmpty()) {
      return Map.of();
    }
    return allocations.sumReservedByLotIds(companyId, lotIds)
        .stream()
        .collect(Collectors.toMap(LotReservationSummary::lotId, LotReservationSummary::reservedQty));
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
    movement.setLocationFromId(null);
    movement.setLocationToId(lot.getLocationId());
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
      movement.setLocationFromId(lot.getLocationId());
      movement.setLocationToId(null);
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

  private enum LotStatus {
    OK,
    BAJO_STOCK,
    POR_VENCER,
    VENCIDO;

    static LotStatus fromLabel(String label) {
      if (!StringUtils.hasText(label)) {
        return null;
      }
      try {
        return LotStatus.valueOf(label.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException ex) {
        throw new IllegalArgumentException("Estado inválido: " + label);
      }
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
    movement.setTraceId(auditContext.getTraceId());
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
  
  @ValidateTenant(entityClass = InventoryLot.class, entityParam = "lotId")
  public Page<InventoryMovementHistoryEntry> listMovements(
      UUID productId,
      UUID lotId,
      String type,
      UUID locationId,
      OffsetDateTime dateFrom,
      OffsetDateTime dateTo,
      Pageable pageable) {
    UUID companyId = companyContext.require();

    Page<InventoryMovement> movementsPage = movements.findMovementsWithFilters(
        companyId, productId, lotId, type, locationId, dateFrom, dateTo, pageable);

    List<UUID> locationIds = movementsPage.getContent().stream()
        .flatMap(m -> Stream.of(m.getLocationFromId(), m.getLocationToId()))
        .filter(Objects::nonNull)
        .distinct()
        .toList();

    Map<UUID, InventoryLocation> locationMap = new HashMap<>();
    if (!locationIds.isEmpty()) {
      inventoryLocationRepository.findAllById(locationIds)
          .forEach(loc -> locationMap.put(loc.getId(), loc));
    }

    List<InventoryMovementHistoryEntry> history = movementsPage.getContent().stream()
        .map(m -> {
          InventoryLocation from = m.getLocationFromId() != null ? locationMap.get(m.getLocationFromId()) : null;
          InventoryLocation to = m.getLocationToId() != null ? locationMap.get(m.getLocationToId()) : null;
          InventoryMovementHistoryEntry.InventoryMovementLocation fromInfo = from != null
              ? new InventoryMovementHistoryEntry.InventoryMovementLocation(from.getId(), from.getName())
              : null;
          InventoryMovementHistoryEntry.InventoryMovementLocation toInfo = to != null
              ? new InventoryMovementHistoryEntry.InventoryMovementLocation(to.getId(), to.getName())
              : null;
          return new InventoryMovementHistoryEntry(
              m.getId(),
              m.getType(),
              m.getQty(),
              m.getPreviousQty(),
              m.getNewQty(),
              m.getProductId(),
              m.getLotId(),
              fromInfo,
              toInfo,
              m.getCreatedBy(),
              m.getTraceId(),
              m.getRefType(),
              m.getRefId(),
              m.getCreatedAt(),
              m.getReasonCode(),
              m.getNote());
        })
        .collect(Collectors.toList());

    return new PageImpl<>(history, pageable, movementsPage.getTotalElements());
  }

  @Transactional(readOnly = true)
  public List<StockByLocationResponse> stockByProduct(UUID productId, List<UUID> productIds) {
    UUID companyId = companyContext.require();
    List<UUID> filters = new ArrayList<>();
    if (productId != null) {
      filters.add(productId);
    }
    if (productIds != null) {
      productIds.stream().filter(Objects::nonNull).forEach(filters::add);
    }
    List<UUID> normalizedFilters = filters.stream().distinct().toList();
    if (normalizedFilters.isEmpty()) {
      throw new IllegalArgumentException("Debe especificar al menos un productId o productIds[]");
    }
    List<StockByLocationAggregation> aggregates = lots.aggregateStockByProductAndLocation(companyId, normalizedFilters);
    if (aggregates.isEmpty()) {
      return List.of();
    }
    Map<UUID, InventoryLocation> locationMap = new HashMap<>();
    Set<UUID> locationIds = aggregates.stream()
        .map(StockByLocationAggregation::locationId)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    if (!locationIds.isEmpty()) {
      inventoryLocationRepository.findAllById(locationIds)
          .forEach(loc -> locationMap.put(loc.getId(), loc));
    }
    return aggregates.stream()
        .map(agg -> {
          InventoryLocation location = locationMap.get(agg.locationId());
          return new StockByLocationResponse(
              agg.productId(),
              agg.locationId(),
              location != null ? location.getName() : null,
              agg.qtyAvailable());
        })
        .collect(Collectors.toList());
  }

  @Transactional
  @ValidateTenant(entityClass = InventoryLot.class, entityParam = "lotId")
  public InventoryLot assignLocationToLot(UUID lotId, UUID locationId) {
    UUID companyId = companyContext.require();
    if (locationId == null) {
      throw new IllegalArgumentException("locationId es obligatorio");
    }

    InventoryLot lot = lots.findById(lotId)
        .orElseThrow(() -> new IllegalArgumentException("Lote no encontrado: " + lotId));

    InventoryLocation location = resolveInventoryLocation(companyId, locationId);
    UUID previousLocationId = lot.getLocationId();
    if (Objects.equals(previousLocationId, locationId)) {
      return lot;
    }

    lot.setLocationId(location.getId());
    InventoryLot updated = lots.save(lot);

    InventoryMovement movement = new InventoryMovement();
    movement.setCompanyId(companyId);
    movement.setProductId(lot.getProductId());
    movement.setLotId(lot.getId());
    movement.setType("TRANSFER");
    movement.setQty(lot.getQtyAvailable());
    movement.setRefType("LOT_LOCATION");
    movement.setRefId(lot.getId());
    movement.setNote(previousLocationId == null
        ? "Asignación de ubicación " + location.getName()
        : "Cambio de ubicación de " + previousLocationId + " a " + locationId);
    movement.setPreviousQty(lot.getQtyAvailable());
    movement.setNewQty(lot.getQtyAvailable());
    movement.setLocationFromId(previousLocationId);
    movement.setLocationToId(location.getId());
    enrichMovementWithAudit(movement, "TRANSFER", lot.getQtyAvailable(), lot.getQtyAvailable());
    movements.save(movement);

    return updated;
  }

  private InventoryLocation resolveInventoryLocation(UUID companyId, UUID locationId) {
    InventoryLocation location = inventoryLocationRepository.findById(locationId).orElse(null);
    if (location != null) {
      return location;
    }
    InventoryLocation crossTenant = findInventoryLocationIgnoringTenant(locationId);
    if (crossTenant != null) {
      throw new CrossTenantAccessException("Ubicación " + locationId + " pertenece a otra empresa distinta a " + companyId);
    }
    throw new IllegalArgumentException("Ubicación no encontrada: " + locationId);
  }

  private InventoryLocation findInventoryLocationIgnoringTenant(UUID locationId) {
    try {
      tenantFilterEnabler.disableTenantFilter(entityManager);
      return entityManager.find(InventoryLocation.class, locationId);
    } finally {
      tenantFilterEnabler.enableTenantFilter(entityManager);
    }
  }

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
    BigDecimal previousLotQty = lot.getQtyAvailable();
    BigDecimal postLotQty = previousLotQty.subtract(request.qty());
    InventoryMovement outMovement = new InventoryMovement();
    outMovement.setCompanyId(companyId);
    outMovement.setProductId(lot.getProductId());
    outMovement.setLotId(lot.getId());
    outMovement.setType("TRANSFER_OUT");
    outMovement.setQty(request.qty().negate());
    outMovement.setRefType("TRANSFER");
    outMovement.setRefId(lot.getId());
    outMovement.setPreviousQty(previousLotQty);
    outMovement.setNewQty(postLotQty);
    outMovement.setNote("Transferencia a ubicación " + request.targetLocationId());
    outMovement.setLocationFromId(lot.getLocationId());
    outMovement.setLocationToId(null);
    enrichMovementWithAudit(outMovement, "TRANSFER", previousLotQty, postLotQty);
    movements.save(outMovement);
    
    // Actualizar cantidad del lote origen
    lot.setQtyAvailable(postLotQty);
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
    
    BigDecimal targetPreviousQty;
    BigDecimal targetNewQty;
    if (targetLot != null) {
      targetPreviousQty = targetLot.getQtyAvailable();
      targetNewQty = targetPreviousQty.add(request.qty());
      targetLot.setQtyAvailable(targetNewQty);
      lots.save(targetLot);
    } else {
      targetLot = new InventoryLot();
      targetLot.setCompanyId(companyId);
      targetLot.setProductId(lot.getProductId());
      targetLot.setLocationId(request.targetLocationId());
      // Copiar campos del lote origen
      targetLot.setBatchName(lot.getBatchName());
      targetLot.setCostUnit(lot.getCostUnit());
      targetLot.setMfgDate(lot.getMfgDate());
      targetLot.setExpDate(lot.getExpDate());
      targetPreviousQty = BigDecimal.ZERO;
      targetNewQty = request.qty();
      targetLot.setQtyAvailable(targetNewQty);
      lots.save(targetLot);
    }
    
    // Registrar movimiento de entrada en ubicación destino
    InventoryMovement inMovement = new InventoryMovement();
    inMovement.setCompanyId(companyId);
    inMovement.setProductId(lot.getProductId());
    inMovement.setLotId(targetLot.getId());
    inMovement.setType("TRANSFER_IN");
    inMovement.setQty(request.qty());
    inMovement.setRefType("TRANSFER");
    inMovement.setRefId(lot.getId());
    inMovement.setPreviousQty(targetPreviousQty);
    inMovement.setNewQty(targetNewQty);
    inMovement.setLocationFromId(lot.getLocationId());
    inMovement.setLocationToId(request.targetLocationId());
    inMovement.setNote(request.note() != null ? request.note() : "Transferencia desde lote " + lot.getId());
    enrichMovementWithAudit(inMovement, "TRANSFER", targetPreviousQty, targetNewQty);
    movements.save(inMovement);
    
    return lot;
  }
  
  @Transactional(readOnly = true)
  public List<com.datakomerz.pymes.inventory.dto.StockByLocationDTO> getStockByLocation(UUID productId) {
    UUID companyId = companyContext.require();
    
    // Validar que el producto existe y pertenece al tenant
    Product product = productRepository.findById(productId)
        .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
    
    if (!product.getCompanyId().equals(companyId)) {
      throw new CrossTenantAccessException("Cross-tenant access denied");
    }
    
    List<InventoryLot> lotsList = lots.findByCompanyIdAndProductIdOrderByCreatedAtAsc(companyId, productId);
    
    // Agrupar por ubicación y sumar cantidades
    Map<UUID, BigDecimal> stockByLocation = new HashMap<>();
    Map<UUID, InventoryLocation> locationsMap = new HashMap<>();
    
    for (InventoryLot lot : lotsList) {
      if (lot.getLocationId() != null) {
        stockByLocation.merge(lot.getLocationId(), lot.getQtyAvailable(), BigDecimal::add);
        if (!locationsMap.containsKey(lot.getLocationId())) {
          inventoryLocationRepository.findById(lot.getLocationId())
              .ifPresent(loc -> locationsMap.put(loc.getId(), loc));
        }
      }
    }
    
    return stockByLocation.entrySet().stream()
        .map(entry -> {
          InventoryLocation location = locationsMap.get(entry.getKey());
          return new com.datakomerz.pymes.inventory.dto.StockByLocationDTO(
              entry.getKey(),
              location != null ? location.getCode() : "",
              location != null ? location.getName() : "Ubicación eliminada",
              entry.getValue()
          );
        })
        .toList();
  }
}

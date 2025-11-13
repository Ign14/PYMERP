package com.datakomerz.pymes.purchases;

import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.datakomerz.pymes.inventory.InventoryLot;
import com.datakomerz.pymes.inventory.InventoryLotRepository;
import com.datakomerz.pymes.inventory.InventoryMovement;
import com.datakomerz.pymes.inventory.InventoryMovementRepository;
import com.datakomerz.pymes.purchases.dto.PurchaseDailyPoint;
import com.datakomerz.pymes.purchases.dto.PurchaseReq;
import com.datakomerz.pymes.purchases.dto.PurchaseSummary;
import com.datakomerz.pymes.purchases.dto.PurchaseUpdateRequest;
import com.datakomerz.pymes.services.ServiceRepository;
import com.datakomerz.pymes.storage.StorageService;
import com.datakomerz.pymes.suppliers.Supplier;
import com.datakomerz.pymes.suppliers.SupplierRepository;
import com.datakomerz.pymes.products.Product;
import com.datakomerz.pymes.products.ProductRepository;
import com.datakomerz.pymes.locations.Location;
import com.datakomerz.pymes.locations.LocationRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PurchaseService {
  private final PurchaseRepository purchases;
  private final PurchaseItemRepository items;
  private final InventoryLotRepository lots;
  private final InventoryMovementRepository movements;
  private final CompanyContext companyContext;
  private final SupplierRepository suppliers;
  // ServiceRepository removido - ya no se actualiza lastPurchaseDate (eliminado en V37)
  private final StorageService storageService;
  private final ProductRepository productRepository;
  private final LocationRepository locationRepository;

  public PurchaseService(PurchaseRepository purchases,
                         PurchaseItemRepository items,
                         InventoryLotRepository lots,
                         InventoryMovementRepository movements,
                         CompanyContext companyContext,
                         SupplierRepository suppliers,
                         StorageService storageService,
                         ProductRepository productRepository,
                         LocationRepository locationRepository) {
    this.purchases = purchases;
    this.items = items;
    this.lots = lots;
    this.movements = movements;
    this.companyContext = companyContext;
    this.suppliers = suppliers;
    this.storageService = storageService;
    this.productRepository = productRepository;
    this.locationRepository = locationRepository;
  }

  @Transactional
  public UUID create(PurchaseReq req) {
    UUID companyId = companyContext.require();

    var purchase = new Purchase();
    purchase.setCompanyId(companyId);
    purchase.setSupplierId(req.supplierId());
    purchase.setDocType(req.docType());
    purchase.setDocNumber(req.docNumber());
    purchase.setStatus("received");
    purchase.setNet(req.net());
    purchase.setVat(req.vat());
    purchase.setTotal(req.total());
    purchase.setPdfUrl(req.pdfUrl());
    purchase.setIssuedAt(req.issuedAt());
    purchase.setReceivedAt(req.receivedAt());
    purchases.save(purchase);

    for (var itemReq : req.items()) {
      var item = new PurchaseItem();
      item.setPurchaseId(purchase.getId());
      item.setProductId(itemReq.productId());
      item.setQty(itemReq.qty());
      item.setUnitCost(itemReq.unitCost());
      item.setVatRate(itemReq.vatRate());
      item.setMfgDate(itemReq.mfgDate());
      item.setExpDate(itemReq.expDate());
      items.save(item);

      // Si es un producto, crear lote y movimiento de inventario
      if (itemReq.isProduct()) {
        var lot = new InventoryLot();
        lot.setCompanyId(companyId);
        lot.setProductId(itemReq.productId());
        lot.setPurchaseItemId(item.getId());
        lot.setQtyAvailable(itemReq.qty());
        lot.setCostUnit(itemReq.unitCost());
        lot.setMfgDate(itemReq.mfgDate());
        lot.setExpDate(itemReq.expDate());
        lot.setLocationId(itemReq.locationId());
        lots.save(lot);

        var movement = new InventoryMovement();
        movement.setCompanyId(companyId);
        movement.setProductId(itemReq.productId());
        movement.setLotId(lot.getId());
        movement.setType("PURCHASE_IN");
        movement.setQty(itemReq.qty());
        movement.setRefType("PURCHASE");
        movement.setRefId(purchase.getId());
        movements.save(movement);
      } 
      // Si es un servicio, no hay acciones adicionales (campo lastPurchaseDate eliminado en V37)
      // Los servicios se registran en purchase_items pero no generan movimientos de inventario
    }
    return purchase.getId();
  }

  @Transactional
  public UUID createWithFile(PurchaseReq req, MultipartFile file) {
    UUID companyId = companyContext.require();

    var purchase = new Purchase();
    purchase.setCompanyId(companyId);
    purchase.setSupplierId(req.supplierId());
    purchase.setDocType(req.docType());
    purchase.setDocNumber(req.docNumber());
    purchase.setStatus("received");
    purchase.setNet(req.net());
    purchase.setVat(req.vat());
    purchase.setTotal(req.total());
    purchase.setIssuedAt(req.issuedAt());
    purchase.setReceivedAt(req.receivedAt());
    purchases.save(purchase);

    // Guardar archivo PDF si se proporcionó
    if (file != null && !file.isEmpty()) {
      try {
        String pdfUrl = storageService.storePurchaseDocument(companyId, purchase.getId(), file);
        purchase.setPdfUrl(pdfUrl);
        purchases.save(purchase);
      } catch (IOException e) {
        throw new RuntimeException("Error al guardar documento PDF", e);
      }
    } else if (req.pdfUrl() != null) {
      purchase.setPdfUrl(req.pdfUrl());
      purchases.save(purchase);
    }

    for (var itemReq : req.items()) {
      var item = new PurchaseItem();
      item.setPurchaseId(purchase.getId());
      item.setProductId(itemReq.productId());
      item.setQty(itemReq.qty());
      item.setUnitCost(itemReq.unitCost());
      item.setVatRate(itemReq.vatRate());
      item.setMfgDate(itemReq.mfgDate());
      item.setExpDate(itemReq.expDate());
      items.save(item);

      // Si es un producto, crear lote y movimiento de inventario
      if (itemReq.isProduct()) {
        var lot = new InventoryLot();
        lot.setCompanyId(companyId);
        lot.setProductId(itemReq.productId());
        lot.setPurchaseItemId(item.getId());
        lot.setQtyAvailable(itemReq.qty());
        lot.setCostUnit(itemReq.unitCost());
        lot.setMfgDate(itemReq.mfgDate());
        lot.setExpDate(itemReq.expDate());
        lot.setLocationId(itemReq.locationId());
        lots.save(lot);

        var movement = new InventoryMovement();
        movement.setCompanyId(companyId);
        movement.setProductId(itemReq.productId());
        movement.setLotId(lot.getId());
        movement.setType("PURCHASE_IN");
        movement.setQty(itemReq.qty());
        movement.setRefType("PURCHASE");
        movement.setRefId(purchase.getId());
        movements.save(movement);
      } 
      // Si es un servicio, no hay acciones adicionales (campo lastPurchaseDate eliminado en V37)
      // Los servicios se registran en purchase_items pero no generan movimientos de inventario
    }
    return purchase.getId();
  }

  @Transactional(readOnly = true)
  public Page<PurchaseSummary> list(String status,
                                    String docType,
                                    String search,
                                    OffsetDateTime from,
                                    OffsetDateTime to,
                                    Pageable pageable) {
    Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("issuedAt").descending());
    Page<Purchase> page = purchases.search(emptyToNull(status), emptyToNull(docType), emptyToNull(search), from, to, sorted);
    Map<UUID, String> supplierNames = resolveSupplierNames(page.getContent());
    return page.map(purchase -> mapToSummary(purchase, supplierNames.get(purchase.getSupplierId())));
  }

  @Transactional
  public PurchaseSummary update(UUID id, PurchaseUpdateRequest req) {
    Purchase purchase = purchases.findById(id)
      .orElseThrow(() -> new IllegalStateException("Purchase not found: " + id));

    if (req == null) {
      String supplierName = resolveSupplierName(purchase.getSupplierId());
      return mapToSummary(purchase, supplierName);
    }

    if (req.status() != null && !req.status().equalsIgnoreCase(purchase.getStatus())) {
      if ("cancelled".equalsIgnoreCase(req.status())) {
        return cancel(id);
      }
      purchase.setStatus(req.status());
    }

    if (req.docType() != null) {
      purchase.setDocType(req.docType());
    }
    if (req.docNumber() != null) {
      purchase.setDocNumber(req.docNumber());
    }

    purchases.save(purchase);
    String supplierName = resolveSupplierName(purchase.getSupplierId());
    return mapToSummary(purchase, supplierName);
  }

  @Transactional
  public PurchaseSummary cancel(UUID id) {
    Purchase purchase = purchases.findById(id)
      .orElseThrow(() -> new IllegalStateException("Purchase not found: " + id));

    if ("cancelled".equalsIgnoreCase(purchase.getStatus())) {
      String supplierName = resolveSupplierName(purchase.getSupplierId());
      return mapToSummary(purchase, supplierName);
    }

    List<PurchaseItem> purchaseItems = items.findByPurchaseId(purchase.getId());
    for (PurchaseItem item : purchaseItems) {
      List<InventoryLot> relatedLots = lots.findByPurchaseItemId(item.getId());
      for (InventoryLot lot : relatedLots) {
        BigDecimal qty = item.getQty();
        if (lot.getQtyAvailable().compareTo(qty) < 0) {
          throw new IllegalStateException("Cannot cancel purchase because stock was already consumed");
        }
        lot.setQtyAvailable(lot.getQtyAvailable().subtract(qty));
        lots.save(lot);

        InventoryMovement movement = new InventoryMovement();
        movement.setCompanyId(purchase.getCompanyId());
        movement.setProductId(item.getProductId());
        movement.setLotId(lot.getId());
        movement.setType("PURCHASE_CANCEL");
        movement.setQty(qty.negate());
        movement.setRefType("PURCHASE");
        movement.setRefId(purchase.getId());
        movements.save(movement);
      }
    }

    purchase.setStatus("cancelled");
    purchases.save(purchase);
    String supplierName = resolveSupplierName(purchase.getSupplierId());
    return mapToSummary(purchase, supplierName);
  }

  @Transactional(readOnly = true)
  public List<PurchaseDailyPoint> dailyMetrics(int days) {
    OffsetDateTime from = OffsetDateTime.now().minusDays(days);
    List<Purchase> range = purchases.findByIssuedAtGreaterThanEqualOrderByIssuedAtAsc(from);
    return range.stream()
      .collect(Collectors.groupingBy(p -> p.getIssuedAt().toLocalDate()))
      .entrySet().stream()
      .map(entry -> {
        BigDecimal total = entry.getValue().stream()
          .map(Purchase::getTotal)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
        long count = entry.getValue().size();
        return new PurchaseDailyPoint(entry.getKey(), total, count);
      })
      .sorted(java.util.Comparator.comparing(PurchaseDailyPoint::date))
      .toList();
  }

  private PurchaseSummary mapToSummary(Purchase purchase, String supplierName) {
    return new PurchaseSummary(
      purchase.getId(),
      purchase.getSupplierId(),
      supplierName,
      purchase.getDocType(),
      purchase.getDocNumber(),
      purchase.getPaymentTermDays(),
      purchase.getDueDate(),
      purchase.getStatus(),
      purchase.getNet(),
      purchase.getVat(),
      purchase.getTotal(),
      purchase.getIssuedAt()
    );
  }

  private Map<UUID, String> resolveSupplierNames(List<Purchase> purchaseList) {
    Set<UUID> ids = purchaseList.stream()
      .map(Purchase::getSupplierId)
      .filter(Objects::nonNull)
      .collect(Collectors.toCollection(HashSet::new));
    if (ids.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<UUID, String> map = new HashMap<>();
    suppliers.findAllById(ids).forEach(supplier -> map.put(supplier.getId(), supplier.getName()));
    return map;
  }

  private String resolveSupplierName(UUID supplierId) {
    if (supplierId == null) {
      return null;
    }
    Optional<Supplier> supplier = suppliers.findById(supplierId);
    return supplier.map(Supplier::getName).orElse(null);
  }

  private String emptyToNull(String value) {
    return (value != null && !value.isBlank()) ? value : null;
  }
  
  /**
   * Calcula KPIs avanzados de compras para un período específico
   * @param startDate Fecha inicio del período
   * @param endDate Fecha fin del período
   * @return PurchaseKPIs con métricas del período
   */
  public com.datakomerz.pymes.purchases.dto.PurchaseKPIs getPurchaseKPIs(LocalDate startDate, LocalDate endDate) {
    // Obtener todas las compras del período
    List<Purchase> periodPurchases = purchases.findAll().stream()
        .filter(p -> p.getCreatedAt() != null)
        .filter(p -> {
          LocalDate purchaseDate = p.getCreatedAt().toLocalDate();
          return !purchaseDate.isBefore(startDate) && !purchaseDate.isAfter(endDate);
        })
        .collect(Collectors.toList());
    
    // Filtrar compras recibidas
    List<Purchase> receivedPurchases = periodPurchases.stream()
        .filter(p -> "received".equalsIgnoreCase(p.getStatus()))
        .collect(Collectors.toList());
    
    // Total Spent
    BigDecimal totalSpent = receivedPurchases.stream()
        .map(Purchase::getTotal)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    // Total Quantity (suma de items)
    BigDecimal totalQuantity = BigDecimal.ZERO;
    for (Purchase purchase : receivedPurchases) {
      List<PurchaseItem> purchaseItems = items.findAll().stream()
          .filter(item -> item.getPurchaseId().equals(purchase.getId()))
          .collect(Collectors.toList());
      
      for (PurchaseItem item : purchaseItems) {
        totalQuantity = totalQuantity.add(item.getQty());
      }
    }
    
    // Total Orders
    Integer totalOrders = receivedPurchases.size();
    
    // Average Order Value
    BigDecimal averageOrderValue = BigDecimal.ZERO;
    if (totalOrders > 0) {
      averageOrderValue = totalSpent.divide(new BigDecimal(totalOrders), 2, java.math.RoundingMode.HALF_UP);
    }
    
    // Purchase Growth (comparar con período anterior)
    LocalDate prevStartDate = startDate.minusDays(endDate.toEpochDay() - startDate.toEpochDay() + 1);
    List<Purchase> prevPeriodPurchases = purchases.findAll().stream()
        .filter(p -> "received".equalsIgnoreCase(p.getStatus()))
        .filter(p -> p.getCreatedAt() != null)
        .filter(p -> {
          LocalDate purchaseDate = p.getCreatedAt().toLocalDate();
          return !purchaseDate.isBefore(prevStartDate) && purchaseDate.isBefore(startDate);
        })
        .collect(Collectors.toList());
    
    BigDecimal prevSpent = prevPeriodPurchases.stream()
        .map(Purchase::getTotal)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal purchaseGrowth = BigDecimal.ZERO;
    if (prevSpent.compareTo(BigDecimal.ZERO) > 0) {
      purchaseGrowth = totalSpent.subtract(prevSpent)
          .divide(prevSpent, 4, java.math.RoundingMode.HALF_UP)
          .multiply(new BigDecimal("100"));
    }
    
    // Unique Suppliers
    Set<UUID> uniqueSupplierIds = receivedPurchases.stream()
        .map(Purchase::getSupplierId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    Integer uniqueSuppliers = uniqueSupplierIds.size();
    
    // Top Supplier by Spent
    Map<UUID, BigDecimal> supplierSpent = new HashMap<>();
    for (Purchase purchase : receivedPurchases) {
      if (purchase.getSupplierId() != null) {
        supplierSpent.merge(purchase.getSupplierId(), purchase.getTotal(), BigDecimal::add);
      }
    }
    
    String topSupplierName = "N/A";
    BigDecimal topSupplierSpent = BigDecimal.ZERO;
    UUID topSupplierId = supplierSpent.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse(null);
    
    if (topSupplierId != null) {
      topSupplierSpent = supplierSpent.get(topSupplierId);
      topSupplierName = suppliers.findById(topSupplierId)
          .map(Supplier::getName)
          .orElse("Proveedor #" + topSupplierId);
    }
    
    // Supplier Concentration (% del top supplier)
    BigDecimal supplierConcentration = BigDecimal.ZERO;
    if (totalSpent.compareTo(BigDecimal.ZERO) > 0) {
      supplierConcentration = topSupplierSpent.divide(totalSpent, 4, java.math.RoundingMode.HALF_UP)
          .multiply(new BigDecimal("100"));
    }
    
    // Top Category (usando serviceRepository para servicios)
    String topCategoryName = "N/A";
    BigDecimal topCategorySpent = BigDecimal.ZERO;
    
    // On-Time Delivery Rate (asumimos 100% si fue recibida)
    BigDecimal onTimeDeliveryRate = new BigDecimal("100");
    
    // Cost Per Unit
    BigDecimal costPerUnit = BigDecimal.ZERO;
    if (totalQuantity.compareTo(BigDecimal.ZERO) > 0) {
      costPerUnit = totalSpent.divide(totalQuantity, 2, java.math.RoundingMode.HALF_UP);
    }
    
    // Pending Orders
    Integer pendingOrders = (int) periodPurchases.stream()
        .filter(p -> !"received".equalsIgnoreCase(p.getStatus()))
        .filter(p -> !"cancelled".equalsIgnoreCase(p.getStatus()))
        .count();
    
    return new com.datakomerz.pymes.purchases.dto.PurchaseKPIs(
        totalSpent.setScale(2, java.math.RoundingMode.HALF_UP),
        totalQuantity.setScale(2, java.math.RoundingMode.HALF_UP),
        totalOrders,
        averageOrderValue.setScale(2, java.math.RoundingMode.HALF_UP),
        purchaseGrowth.setScale(2, java.math.RoundingMode.HALF_UP),
        uniqueSuppliers,
        supplierConcentration.setScale(2, java.math.RoundingMode.HALF_UP),
        topSupplierName,
        topSupplierSpent.setScale(2, java.math.RoundingMode.HALF_UP),
        topCategoryName,
        topCategorySpent.setScale(2, java.math.RoundingMode.HALF_UP),
        onTimeDeliveryRate.setScale(2, java.math.RoundingMode.HALF_UP),
        costPerUnit.setScale(2, java.math.RoundingMode.HALF_UP),
        pendingOrders,
        startDate,
        endDate
    );
  }

  /**
   * Análisis ABC de proveedores basado en Pareto (80-15-5)
   * Clasifica proveedores en A (80% del gasto), B (15% del gasto), C (5% del gasto)
   */
  public List<com.datakomerz.pymes.purchases.dto.PurchaseABCClassification> getPurchaseABCAnalysis(LocalDate startDate, LocalDate endDate) {
    OffsetDateTime start = startDate.atStartOfDay(java.time.ZoneOffset.UTC).toOffsetDateTime();
    OffsetDateTime end = endDate.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toOffsetDateTime();
    
    // Obtener todas las compras recibidas del período
    List<Purchase> periodPurchases = purchases.findAll().stream()
        .filter(p -> p.getCreatedAt() != null)
        .filter(p -> !p.getCreatedAt().isBefore(start) && p.getCreatedAt().isBefore(end))
        .filter(p -> "received".equals(p.getStatus()))
        .collect(Collectors.toList());
    
    if (periodPurchases.isEmpty()) {
      return Collections.emptyList();
    }
    
    // Agrupar por proveedor y calcular totales
    Map<UUID, SupplierData> supplierStats = new HashMap<>();
    
    for (Purchase p : periodPurchases) {
      UUID supplierId = p.getSupplierId();
      if (supplierId == null) continue;
      
      SupplierData data = supplierStats.getOrDefault(supplierId, new SupplierData());
      data.totalSpent = data.totalSpent.add(p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO);
      data.purchaseCount++;
      if (data.lastPurchaseDate == null || p.getCreatedAt().isAfter(data.lastPurchaseDate)) {
        data.lastPurchaseDate = p.getCreatedAt();
      }
      supplierStats.put(supplierId, data);
    }
    
    // Calcular total global
    BigDecimal totalSpentGlobal = supplierStats.values().stream()
        .map(d -> d.totalSpent)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    if (totalSpentGlobal.compareTo(BigDecimal.ZERO) == 0) {
      return Collections.emptyList();
    }
    
    // Calcular porcentajes y ordenar por gasto descendente
    List<SupplierClassification> classifications = supplierStats.entrySet().stream()
        .map(entry -> {
          UUID supplierId = entry.getKey();
          SupplierData data = entry.getValue();
          
          String supplierName = "Proveedor desconocido";
          Optional<Supplier> supplierOpt = suppliers.findById(supplierId);
          if (supplierOpt.isPresent()) {
            supplierName = supplierOpt.get().getName();
          }
          
          BigDecimal percentage = data.totalSpent
              .divide(totalSpentGlobal, 4, java.math.RoundingMode.HALF_UP)
              .multiply(new BigDecimal("100"));
          
          BigDecimal avgOrderValue = data.purchaseCount > 0
              ? data.totalSpent.divide(new BigDecimal(data.purchaseCount), 2, java.math.RoundingMode.HALF_UP)
              : BigDecimal.ZERO;
          
          return new SupplierClassification(
              supplierId.toString(),
              supplierName,
              data.totalSpent,
              data.purchaseCount,
              percentage,
              data.lastPurchaseDate,
              avgOrderValue
          );
        })
        .sorted((a, b) -> b.totalSpent.compareTo(a.totalSpent))
        .collect(Collectors.toList());
    
    // Aplicar clasificación ABC según Pareto (80-15-5)
    BigDecimal cumulativePercentage = BigDecimal.ZERO;
    List<com.datakomerz.pymes.purchases.dto.PurchaseABCClassification> result = new java.util.ArrayList<>();
    
    for (SupplierClassification sc : classifications) {
      cumulativePercentage = cumulativePercentage.add(sc.percentage);
      
      String classification;
      String recommendedAction;
      
      if (cumulativePercentage.compareTo(new BigDecimal("80")) <= 0) {
        classification = "A";
        recommendedAction = "Proveedor crítico: negociar contratos a largo plazo, gestión estrecha";
      } else if (cumulativePercentage.compareTo(new BigDecimal("95")) <= 0) {
        classification = "B";
        recommendedAction = "Proveedor importante: revisar periódicamente, buscar alternativas";
      } else {
        classification = "C";
        recommendedAction = "Proveedor ocasional: consolidar compras o evaluar eliminación";
      }
      
      result.add(new com.datakomerz.pymes.purchases.dto.PurchaseABCClassification(
          sc.supplierId,
          sc.supplierName,
          sc.totalSpent.setScale(2, java.math.RoundingMode.HALF_UP),
          sc.purchaseCount,
          sc.percentage.setScale(2, java.math.RoundingMode.HALF_UP),
          classification,
          cumulativePercentage.setScale(2, java.math.RoundingMode.HALF_UP),
          sc.avgOrderValue.setScale(2, java.math.RoundingMode.HALF_UP),
          sc.lastPurchaseDate,
          recommendedAction
      ));
    }
    
    return result;
  }
  
  // Clase auxiliar para agrupar datos de proveedores
  private static class SupplierData {
    BigDecimal totalSpent = BigDecimal.ZERO;
    long purchaseCount = 0;
    OffsetDateTime lastPurchaseDate = null;
  }
  
  // Clase auxiliar para clasificación
  private static class SupplierClassification {
    String supplierId;
    String supplierName;
    BigDecimal totalSpent;
    long purchaseCount;
    BigDecimal percentage;
    OffsetDateTime lastPurchaseDate;
    BigDecimal avgOrderValue;
    
    SupplierClassification(String supplierId, String supplierName, BigDecimal totalSpent,
                           long purchaseCount, BigDecimal percentage,
                           OffsetDateTime lastPurchaseDate, BigDecimal avgOrderValue) {
      this.supplierId = supplierId;
      this.supplierName = supplierName;
      this.totalSpent = totalSpent;
      this.purchaseCount = purchaseCount;
      this.percentage = percentage;
      this.lastPurchaseDate = lastPurchaseDate;
      this.avgOrderValue = avgOrderValue;
    }
  }

  /**
   * Pronóstico de demanda de compras usando media móvil y análisis de tendencia.
   * Analiza los últimos 90 días y proyecta el próximo mes.
   */
  public List<com.datakomerz.pymes.purchases.dto.PurchaseForecast> getPurchaseForecast(LocalDate startDate, LocalDate endDate, int horizonDays) {
    OffsetDateTime start = startDate.atStartOfDay(java.time.ZoneOffset.UTC).toOffsetDateTime();
    OffsetDateTime end = endDate.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toOffsetDateTime();
    
    // Obtener compras recibidas del período
    List<Purchase> historicalPurchases = purchases.findAll().stream()
        .filter(p -> p.getCreatedAt() != null)
        .filter(p -> !p.getCreatedAt().isBefore(start) && p.getCreatedAt().isBefore(end))
        .filter(p -> "received".equals(p.getStatus()))
        .collect(Collectors.toList());
    
    if (historicalPurchases.isEmpty()) {
      return Collections.emptyList();
    }
    
    // Agrupar por proveedor
    Map<UUID, List<Purchase>> purchasesBySupplier = historicalPurchases.stream()
        .filter(p -> p.getSupplierId() != null)
        .collect(Collectors.groupingBy(Purchase::getSupplierId));
    
    List<com.datakomerz.pymes.purchases.dto.PurchaseForecast> forecasts = new java.util.ArrayList<>();
    
    for (Map.Entry<UUID, List<Purchase>> entry : purchasesBySupplier.entrySet()) {
      UUID supplierId = entry.getKey();
      List<Purchase> supplierPurchases = entry.getValue();
      
      // Saltar si hay muy pocas compras para hacer predicción confiable
      if (supplierPurchases.size() < 2) {
        continue;
      }
      
      String supplierName = "Proveedor desconocido";
      Optional<Supplier> supplierOpt = suppliers.findById(supplierId);
      if (supplierOpt.isPresent()) {
        supplierName = supplierOpt.get().getName();
      }
      
      // Calcular media móvil del gasto
      BigDecimal totalSpent = supplierPurchases.stream()
          .map(Purchase::getTotal)
          .filter(Objects::nonNull)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      
      long daysCovered = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
      BigDecimal historicalAverage = daysCovered > 0
          ? totalSpent.divide(new BigDecimal(daysCovered), 4, java.math.RoundingMode.HALF_UP)
              .multiply(new BigDecimal("30")) // Convertir a promedio mensual
          : BigDecimal.ZERO;
      
      // Analizar tendencia (comparar primera mitad vs segunda mitad)
      int halfPoint = supplierPurchases.size() / 2;
      List<Purchase> firstHalf = supplierPurchases.subList(0, halfPoint);
      List<Purchase> secondHalf = supplierPurchases.subList(halfPoint, supplierPurchases.size());
      
      BigDecimal firstHalfAvg = firstHalf.stream()
          .map(Purchase::getTotal)
          .filter(Objects::nonNull)
          .reduce(BigDecimal.ZERO, BigDecimal::add)
          .divide(new BigDecimal(firstHalf.size()), 4, java.math.RoundingMode.HALF_UP);
      
      BigDecimal secondHalfAvg = secondHalf.stream()
          .map(Purchase::getTotal)
          .filter(Objects::nonNull)
          .reduce(BigDecimal.ZERO, BigDecimal::add)
          .divide(new BigDecimal(secondHalf.size()), 4, java.math.RoundingMode.HALF_UP);
      
      String trend;
      BigDecimal trendFactor = BigDecimal.ONE;
      if (firstHalfAvg.compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal change = secondHalfAvg.subtract(firstHalfAvg)
            .divide(firstHalfAvg, 4, java.math.RoundingMode.HALF_UP);
        
        if (change.compareTo(new BigDecimal("0.1")) > 0) {
          trend = "increasing";
          trendFactor = BigDecimal.ONE.add(change.multiply(new BigDecimal("0.5"))); // Aplicar 50% del cambio
        } else if (change.compareTo(new BigDecimal("-0.1")) < 0) {
          trend = "decreasing";
          trendFactor = BigDecimal.ONE.add(change.multiply(new BigDecimal("0.5")));
        } else {
          trend = "stable";
        }
      } else {
        trend = "stable";
      }
      
      // Pronóstico = promedio histórico ajustado por tendencia
      BigDecimal forecastedSpending = historicalAverage.multiply(trendFactor)
          .setScale(2, java.math.RoundingMode.HALF_UP);
      
      // Confianza basada en cantidad de datos
      BigDecimal confidence;
      if (supplierPurchases.size() >= 10) {
        confidence = new BigDecimal("85");
      } else if (supplierPurchases.size() >= 5) {
        confidence = new BigDecimal("70");
      } else {
        confidence = new BigDecimal("50");
      }
      
      // Estimar próxima fecha de compra basada en frecuencia histórica
      supplierPurchases.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
      long totalDaysBetweenPurchases = 0;
      for (int i = 1; i < supplierPurchases.size(); i++) {
        totalDaysBetweenPurchases += java.time.temporal.ChronoUnit.DAYS.between(
            supplierPurchases.get(i - 1).getCreatedAt(),
            supplierPurchases.get(i).getCreatedAt()
        );
      }
      long avgDaysBetweenPurchases = supplierPurchases.size() > 1
          ? totalDaysBetweenPurchases / (supplierPurchases.size() - 1)
          : 30;
      
      Purchase lastPurchase = supplierPurchases.get(supplierPurchases.size() - 1);
      LocalDate nextPurchaseDate = lastPurchase.getCreatedAt().toLocalDate().plusDays(avgDaysBetweenPurchases);
      
      // Cantidad recomendada basada en promedio
      BigDecimal totalQuantity = BigDecimal.ZERO;
      for (Purchase p : supplierPurchases) {
        List<PurchaseItem> purchaseItems = items.findAll().stream()
            .filter(item -> item.getPurchaseId().equals(p.getId()))
            .collect(Collectors.toList());
        for (PurchaseItem item : purchaseItems) {
          totalQuantity = totalQuantity.add(item.getQty() != null ? item.getQty() : BigDecimal.ZERO);
        }
      }
      BigDecimal recommendedQuantity = totalQuantity.divide(new BigDecimal(supplierPurchases.size()), 2, java.math.RoundingMode.HALF_UP);
      
      // Factor de estacionalidad (simplificado: 1.0 = sin estacionalidad)
      BigDecimal seasonalityFactor = BigDecimal.ONE;
      
      forecasts.add(new com.datakomerz.pymes.purchases.dto.PurchaseForecast(
          supplierId.toString(),
          supplierName,
          historicalAverage.setScale(2, java.math.RoundingMode.HALF_UP),
          trend,
          forecastedSpending,
          confidence.setScale(2, java.math.RoundingMode.HALF_UP),
          nextPurchaseDate,
          recommendedQuantity.setScale(2, java.math.RoundingMode.HALF_UP),
          seasonalityFactor.setScale(2, java.math.RoundingMode.HALF_UP)
      ));
    }
    
    // Ordenar por gasto pronosticado descendente
    forecasts.sort((a, b) -> b.getForecastedSpending().compareTo(a.getForecastedSpending()));
    
    return forecasts;
  }

  @Transactional(readOnly = true)
  public com.datakomerz.pymes.purchases.dto.PurchaseDetail getDetail(UUID id) {
    UUID companyId = companyContext.require();
    
    Purchase purchase = purchases.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Compra no encontrada"));
    
    if (!purchase.getCompanyId().equals(companyId)) {
      throw new IllegalArgumentException("Acceso denegado a esta compra");
    }
    
    // Obtener proveedor
    Supplier supplier = suppliers.findById(purchase.getSupplierId()).orElse(null);
    var supplierDto = supplier != null 
        ? new com.datakomerz.pymes.purchases.dto.PurchaseDetailSupplier(supplier.getId(), supplier.getName())
        : null;
    
    // Obtener items de compra
    List<PurchaseItem> purchaseItems = items.findByPurchaseId(id);
    
    // Mapear productos
    Set<UUID> productIds = purchaseItems.stream()
        .map(PurchaseItem::getProductId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    
    Map<UUID, String> productNames = new HashMap<>();
    Map<UUID, String> productSkus = new HashMap<>();
    if (!productIds.isEmpty()) {
      List<Product> products = productRepository.findAllById(productIds);
      for (Product p : products) {
        productNames.put(p.getId(), p.getName());
        productSkus.put(p.getId(), p.getSku());
      }
    }
    
    // Obtener ubicaciones
    Set<UUID> locationIds = purchaseItems.stream()
        .map(item -> lots.findByPurchaseItemId(item.getId()).stream()
            .map(InventoryLot::getLocationId)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    
    Map<UUID, String> locationCodes = new HashMap<>();
    if (!locationIds.isEmpty()) {
      List<Location> locations = locationRepository.findAllById(locationIds);
      for (Location loc : locations) {
        locationCodes.put(loc.getId(), loc.getCode());
      }
    }
    
    // Mapear items
    List<com.datakomerz.pymes.purchases.dto.PurchaseDetailLine> lines = purchaseItems.stream()
        .map(item -> {
          InventoryLot lot = lots.findByPurchaseItemId(item.getId()).stream().findFirst().orElse(null);
          
          return new com.datakomerz.pymes.purchases.dto.PurchaseDetailLine(
              item.getId(),
              item.getProductId(),
              null, // serviceId - no está en PurchaseItem actual
              productNames.getOrDefault(item.getProductId(), "Producto " + item.getProductId()),
              null, // serviceName
              productSkus.get(item.getProductId()),
              item.getQty(),
              item.getUnitCost(),
              item.getVatRate(),
              item.getMfgDate(),
              item.getExpDate(),
              lot != null ? lot.getLocationId() : null,
              lot != null && lot.getLocationId() != null ? locationCodes.get(lot.getLocationId()) : null
          );
        })
        .collect(Collectors.toList());
    
    // Calcular dueDate si hay paymentTermDays
    OffsetDateTime dueDate = null;
    if (purchase.getPaymentTermDays() > 0 && purchase.getIssuedAt() != null) {
      dueDate = purchase.getIssuedAt().plusDays(purchase.getPaymentTermDays());
    }
    
    return new com.datakomerz.pymes.purchases.dto.PurchaseDetail(
        purchase.getId(),
        purchase.getIssuedAt(),
        purchase.getReceivedAt(),
        dueDate,
        purchase.getDocType(),
        purchase.getDocNumber(),
        purchase.getPaymentTermDays(),
        purchase.getStatus(),
        supplierDto,
        lines,
        purchase.getNet(),
        purchase.getVat(),
        purchase.getTotal()
    );
  }
}

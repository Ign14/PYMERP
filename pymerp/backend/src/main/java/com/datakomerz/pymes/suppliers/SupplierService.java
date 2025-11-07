package com.datakomerz.pymes.suppliers;

import com.datakomerz.pymes.products.Product;
import com.datakomerz.pymes.products.ProductRepository;
import com.datakomerz.pymes.purchases.Purchase;
import com.datakomerz.pymes.purchases.PurchaseRepository;
import com.datakomerz.pymes.purchases.PurchaseItem;
import com.datakomerz.pymes.purchases.PurchaseItemRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SupplierService {

  private final SupplierRepository supplierRepository;
  private final PurchaseRepository purchaseRepository;
  private final PurchaseItemRepository purchaseItemRepository;
  private final ProductRepository productRepository;

  public SupplierService(SupplierRepository supplierRepository,
                        PurchaseRepository purchaseRepository,
                        PurchaseItemRepository purchaseItemRepository,
                        ProductRepository productRepository) {
    this.supplierRepository = supplierRepository;
    this.purchaseRepository = purchaseRepository;
    this.purchaseItemRepository = purchaseItemRepository;
    this.productRepository = productRepository;
  }

  @Cacheable(value = "suppliers", key = "#companyId + ':' + #id")
  public Supplier findSupplier(UUID companyId, UUID id) {
    return supplierRepository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + id));
  }

  @Cacheable(
    value = "suppliers",
    key = "#companyId + ':all:' + ((#pageable != null && #pageable.isPaged()) ? #pageable.pageNumber : 0)"
  )
  public Page<Supplier> findAll(UUID companyId, Boolean active, String query, Pageable pageable) {
    Pageable effectivePageable = pageable == null ? Pageable.unpaged() : pageable;
    String normalizedQuery = query == null ? null : query.trim();
    String effectiveQuery = (normalizedQuery == null || normalizedQuery.isBlank()) ? null : normalizedQuery;
    return supplierRepository.searchSuppliers(active, effectiveQuery, effectivePageable);
  }

  @CacheEvict(value = "suppliers", allEntries = true)
  @Transactional
  public Supplier saveSupplier(Supplier supplier) {
    return supplierRepository.save(supplier);
  }

  @Caching(evict = {
    @CacheEvict(value = "suppliers", key = "#companyId + ':' + #id"),
    @CacheEvict(value = "suppliers", allEntries = true)
  })
  @Transactional
  public void deleteSupplier(UUID companyId, UUID id) {
    Supplier supplier = supplierRepository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Supplier not found: " + id));
    supplier.setActive(false);
    supplierRepository.save(supplier);
  }

  /**
   * Calcula métricas de compras para un proveedor específico
   */
  public SupplierMetrics getSupplierMetrics(UUID supplierId) {
    // Verificar que el proveedor existe
    supplierRepository.findById(supplierId)
        .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

    // Obtener todas las compras del proveedor
    List<Purchase> allPurchases = purchaseRepository.findByIssuedAtGreaterThanEqualOrderByIssuedAtAsc(
        OffsetDateTime.now().minusYears(5) // Últimos 5 años para historial completo
    ).stream()
        .filter(p -> supplierId.equals(p.getSupplierId()))
        .collect(Collectors.toList());

    if (allPurchases.isEmpty()) {
      return new SupplierMetrics(0L, BigDecimal.ZERO, BigDecimal.ZERO, null, 0L, BigDecimal.ZERO, 0L, BigDecimal.ZERO);
    }

    // Calcular totales
    Long totalPurchases = (long) allPurchases.size();
    BigDecimal totalAmount = allPurchases.stream()
        .map(Purchase::getTotal)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal averageOrderValue = totalPurchases > 0
        ? totalAmount.divide(BigDecimal.valueOf(totalPurchases), 2, RoundingMode.HALF_UP)
        : BigDecimal.ZERO;

    // Última fecha de compra
    OffsetDateTime lastPurchaseDate = allPurchases.stream()
        .map(Purchase::getIssuedAt)
        .filter(Objects::nonNull)
        .max(OffsetDateTime::compareTo)
        .orElse(null);

    // Compras del último mes
    OffsetDateTime startOfLastMonth = OffsetDateTime.now().minusMonths(1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
    OffsetDateTime endOfLastMonth = OffsetDateTime.now().withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);

    List<Purchase> lastMonthPurchases = allPurchases.stream()
        .filter(p -> p.getIssuedAt() != null)
        .filter(p -> !p.getIssuedAt().isBefore(startOfLastMonth) && p.getIssuedAt().isBefore(endOfLastMonth))
        .collect(Collectors.toList());

    Long purchasesLastMonth = (long) lastMonthPurchases.size();
    BigDecimal amountLastMonth = lastMonthPurchases.stream()
        .map(Purchase::getTotal)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Compras del mes anterior al último
    OffsetDateTime startOfPreviousMonth = startOfLastMonth.minusMonths(1);
    OffsetDateTime endOfPreviousMonth = startOfLastMonth;

    List<Purchase> previousMonthPurchases = allPurchases.stream()
        .filter(p -> p.getIssuedAt() != null)
        .filter(p -> !p.getIssuedAt().isBefore(startOfPreviousMonth) && p.getIssuedAt().isBefore(endOfPreviousMonth))
        .collect(Collectors.toList());

    Long purchasesPreviousMonth = (long) previousMonthPurchases.size();
    BigDecimal amountPreviousMonth = previousMonthPurchases.stream()
        .map(Purchase::getTotal)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    return new SupplierMetrics(
        totalPurchases,
        totalAmount,
        averageOrderValue,
        lastPurchaseDate,
        purchasesLastMonth,
        amountLastMonth,
        purchasesPreviousMonth,
        amountPreviousMonth
    );
  }

  /**
   * Genera alertas para todos los proveedores de una compañía
   */
  public List<SupplierAlert> getSupplierAlerts() {
    List<SupplierAlert> alerts = new ArrayList<>();

    List<Supplier> suppliers = supplierRepository.findAllByOrderByNameAsc();

    // Obtener todas las compras de los últimos 12 meses
    OffsetDateTime oneYearAgo = OffsetDateTime.now().minusYears(1);
    List<Purchase> recentPurchases = purchaseRepository.findByIssuedAtGreaterThanEqualOrderByIssuedAtAsc(oneYearAgo);

    // Calcular total de compras para análisis de concentración
    BigDecimal totalPurchaseAmount = recentPurchases.stream()
        .map(Purchase::getTotal)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Agrupar compras por proveedor
    Map<UUID, List<Purchase>> purchasesBySupplier = recentPurchases.stream()
        .filter(p -> p.getSupplierId() != null)
        .collect(Collectors.groupingBy(Purchase::getSupplierId));

    // Análisis de concentración - Top 3 proveedores
    List<Map.Entry<UUID, BigDecimal>> topSuppliersByAmount = purchasesBySupplier.entrySet().stream()
        .map(entry -> {
          BigDecimal total = entry.getValue().stream()
              .map(Purchase::getTotal)
              .filter(Objects::nonNull)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
          return Map.entry(entry.getKey(), total);
        })
        .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
        .limit(3)
        .collect(Collectors.toList());

    if (!topSuppliersByAmount.isEmpty() && totalPurchaseAmount.compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal top3Total = topSuppliersByAmount.stream()
          .map(Map.Entry::getValue)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      double concentrationPercentage = top3Total.divide(totalPurchaseAmount, 4, RoundingMode.HALF_UP)
          .multiply(BigDecimal.valueOf(100)).doubleValue();

      if (concentrationPercentage > 70.0) {
        alerts.add(new SupplierAlert(
            null,
            "Concentración de Compras",
            SupplierAlert.AlertType.HIGH_CONCENTRATION,
            SupplierAlert.Severity.WARNING,
            String.format("%.1f%% de las compras concentradas en 3 proveedores - Riesgo de dependencia", concentrationPercentage),
            "Diversificar proveedores",
            null,
            concentrationPercentage
        ));
      }
    }

    // Revisar cada proveedor
    OffsetDateTime threeMonthsAgo = OffsetDateTime.now().minusMonths(3);

    for (Supplier supplier : suppliers) {
      List<Purchase> supplierPurchases = purchasesBySupplier.getOrDefault(supplier.getId(), Collections.emptyList());

      // Alerta: Proveedor activo sin compras en 90 días
      if (Boolean.TRUE.equals(supplier.getActive())) {
        if (supplierPurchases.isEmpty()) {
          // Nunca ha tenido compras
          alerts.add(new SupplierAlert(
              supplier.getId(),
              supplier.getName(),
              SupplierAlert.AlertType.NO_RECENT_PURCHASES,
              SupplierAlert.Severity.INFO,
              "Proveedor activo sin compras registradas",
              "Revisar estado",
              null,
              null
          ));
        } else {
          // Verificar última compra
          Optional<OffsetDateTime> lastPurchase = supplierPurchases.stream()
              .map(Purchase::getIssuedAt)
              .filter(Objects::nonNull)
              .max(OffsetDateTime::compareTo);

          if (lastPurchase.isPresent() && lastPurchase.get().isBefore(threeMonthsAgo)) {
            long daysSinceLastPurchase = ChronoUnit.DAYS.between(lastPurchase.get(), OffsetDateTime.now());
            alerts.add(new SupplierAlert(
                supplier.getId(),
                supplier.getName(),
                SupplierAlert.AlertType.NO_RECENT_PURCHASES,
                SupplierAlert.Severity.WARNING,
                String.format("Sin compras hace %d días", daysSinceLastPurchase),
                "Contactar proveedor",
                daysSinceLastPurchase,
                null
            ));
          }
        }
      }

      // Alerta: Proveedor inactivo con datos incompletos
      if (Boolean.FALSE.equals(supplier.getActive())) {
        if (supplier.getEmail() == null && supplier.getPhone() == null) {
          alerts.add(new SupplierAlert(
              supplier.getId(),
              supplier.getName(),
              SupplierAlert.AlertType.INACTIVE_SUPPLIER,
              SupplierAlert.Severity.INFO,
              "Proveedor inactivo con datos de contacto incompletos",
              "Actualizar datos",
              null,
              null
          ));
        }
      }
    }

    return alerts;
  }

  /**
   * Genera ranking de proveedores por diferentes criterios
   */
  public List<SupplierRanking> getSupplierRanking(String criteria) {
    List<Supplier> suppliers = supplierRepository.findAllByOrderByNameAsc();

    // Obtener compras del último año
    OffsetDateTime oneYearAgo = OffsetDateTime.now().minusYears(1);
    List<Purchase> recentPurchases = purchaseRepository.findByIssuedAtGreaterThanEqualOrderByIssuedAtAsc(
        oneYearAgo
    );

    // Calcular métricas por proveedor
    Map<UUID, SupplierMetrics> metricsMap = new HashMap<>();
    for (Supplier supplier : suppliers) {
      List<Purchase> supplierPurchases = recentPurchases.stream()
          .filter(p -> supplier.getId().equals(p.getSupplierId()))
          .collect(Collectors.toList());

      Long totalPurchases = (long) supplierPurchases.size();
      BigDecimal totalAmount = supplierPurchases.stream()
          .map(Purchase::getTotal)
          .filter(Objects::nonNull)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      SupplierMetrics metrics = new SupplierMetrics();
      metrics.setTotalPurchases(totalPurchases);
      metrics.setTotalAmount(totalAmount);
      metrics.setAverageOrderValue(
          totalPurchases > 0 ? totalAmount.divide(BigDecimal.valueOf(totalPurchases), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO
      );
      
      metricsMap.put(supplier.getId(), metrics);
    }

    // Calcular total para porcentajes
    BigDecimal grandTotal = metricsMap.values().stream()
        .map(SupplierMetrics::getTotalAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Crear lista de rankings
    List<SupplierRanking> rankings = new ArrayList<>();
    for (Supplier supplier : suppliers) {
      SupplierMetrics metrics = metricsMap.get(supplier.getId());
      if (metrics == null) continue;

      Double score = calculateScore(metrics, criteria, grandTotal);
      Double reliability = calculateReliability(recentPurchases.stream()
          .filter(p -> supplier.getId().equals(p.getSupplierId()))
          .collect(Collectors.toList()));

      rankings.add(new SupplierRanking(
          supplier.getId(),
          supplier.getName(),
          0, // rank se asigna después
          score,
          metrics.getTotalPurchases(),
          metrics.getTotalAmount(),
          reliability,
          null // category se asigna en riskAnalysis
      ));
    }

    // Ordenar por score descendente
    rankings.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

    // Asignar ranks
    for (int i = 0; i < rankings.size(); i++) {
      rankings.get(i).setRank(i + 1);
    }

    return rankings;
  }

  /**
   * Análisis de concentración de riesgo (ABC)
   */
  public SupplierRiskAnalysis getRiskAnalysis() {
    List<Supplier> suppliers = supplierRepository.findAllByOrderByNameAsc();

    // Obtener compras del último año
    OffsetDateTime oneYearAgo = OffsetDateTime.now().minusYears(1);
    List<Purchase> recentPurchases = purchaseRepository.findByIssuedAtGreaterThanEqualOrderByIssuedAtAsc(
        oneYearAgo
    );
    Map<UUID, List<PurchaseItem>> itemsByPurchase = groupItemsByPurchase(recentPurchases);

    // Agrupar por proveedor y calcular montos
    Map<UUID, BigDecimal> amountBySupplier = new HashMap<>();
    Map<UUID, Set<UUID>> productSuppliers = new HashMap<>();
    for (Purchase purchase : recentPurchases) {
      UUID supplierId = purchase.getSupplierId();
      if (supplierId == null) continue;

      BigDecimal amount = purchase.getTotal() != null ? purchase.getTotal() : BigDecimal.ZERO;
      amountBySupplier.merge(supplierId, amount, BigDecimal::add);

      List<PurchaseItem> items = itemsByPurchase.getOrDefault(purchase.getId(), Collections.emptyList());
      for (PurchaseItem item : items) {
        UUID productId = item.getProductId();
        if (productId != null) {
          productSuppliers.computeIfAbsent(productId, id -> new HashSet<>()).add(supplierId);
        }
      }
    }

    // Calcular total
    BigDecimal totalAmount = amountBySupplier.values().stream()
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Crear lista con porcentajes
    List<SupplierRiskAnalysis.SupplierCategory> allCategories = new ArrayList<>();
    for (Supplier supplier : suppliers) {
      BigDecimal amount = amountBySupplier.getOrDefault(supplier.getId(), BigDecimal.ZERO);
      if (amount.compareTo(BigDecimal.ZERO) > 0) {
        Double percentage = totalAmount.compareTo(BigDecimal.ZERO) > 0
            ? amount.divide(totalAmount, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
            : 0.0;

        allCategories.add(new SupplierRiskAnalysis.SupplierCategory(
            supplier.getId(),
            supplier.getName(),
            amount,
            percentage
        ));
      }
    }

    // Ordenar por monto descendente
    allCategories.sort((a, b) -> b.getPurchaseAmount().compareTo(a.getPurchaseAmount()));

    // Clasificar en A, B, C
    List<SupplierRiskAnalysis.SupplierCategory> categoryA = new ArrayList<>();
    List<SupplierRiskAnalysis.SupplierCategory> categoryB = new ArrayList<>();
    List<SupplierRiskAnalysis.SupplierCategory> categoryC = new ArrayList<>();

    double accumulated = 0.0;
    for (SupplierRiskAnalysis.SupplierCategory category : allCategories) {
      accumulated += category.getPercentage();
      if (accumulated <= 80.0) {
        categoryA.add(category);
      } else if (accumulated <= 95.0) {
        categoryB.add(category);
      } else {
        categoryC.add(category);
      }
    }

    // Calcular índice de concentración de Herfindahl (suma de cuadrados de participaciones)
    Double concentrationIndex = allCategories.stream()
        .map(c -> Math.pow(c.getPercentage() / 100.0, 2))
        .reduce(0.0, Double::sum);

    int singleSourceProducts = (int) productSuppliers.values().stream()
        .filter(suppliersSet -> suppliersSet.size() == 1)
        .count();

    SupplierRiskAnalysis analysis = new SupplierRiskAnalysis();
    analysis.setCategoryA(categoryA);
    analysis.setCategoryB(categoryB);
    analysis.setCategoryC(categoryC);
    analysis.setConcentrationIndex(concentrationIndex);
    analysis.setSingleSourceProductsCount(singleSourceProducts);
    analysis.setTotalPurchaseVolume(totalAmount);

    return analysis;
  }

  // Métodos auxiliares privados

  private Map<UUID, List<PurchaseItem>> groupItemsByPurchase(List<Purchase> purchases) {
    if (purchases == null || purchases.isEmpty()) {
      return Collections.emptyMap();
    }
    Set<UUID> purchaseIds = purchases.stream()
        .map(Purchase::getId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    if (purchaseIds.isEmpty()) {
      return Collections.emptyMap();
    }
    List<PurchaseItem> items = purchaseItemRepository.findByPurchaseIdIn(purchaseIds);
    Map<UUID, List<PurchaseItem>> grouped = new HashMap<>();
    for (PurchaseItem item : items) {
      grouped.computeIfAbsent(item.getPurchaseId(), id -> new ArrayList<>()).add(item);
    }
    return grouped;
  }

    private Map<UUID, String> resolveProductNames(Collection<UUID> productIds) {
    if (productIds == null || productIds.isEmpty()) {
      return Collections.emptyMap();
    }
    List<Product> products = productRepository.findByIdIn(productIds);
    Map<UUID, String> names = new HashMap<>();
    for (Product product : products) {
      names.put(product.getId(), product.getName());
    }
    for (UUID productId : productIds) {
      names.putIfAbsent(productId, fallbackProductName(productId));
    }
    return names;
  }

    private String resolveProductName(UUID productId) {
    if (productId == null) {
      return "Todos los productos";
    }
    return productRepository.findById(productId)
      .map(Product::getName)
        .orElse(fallbackProductName(productId));
  }

  private String fallbackProductName(UUID productId) {
    if (productId == null) {
      return "Producto desconocido";
    }
    String id = productId.toString();
    return "Producto " + id.substring(0, Math.min(8, id.length()));
  }

  private Double calculateReliability(List<Purchase> purchases) {
    if (purchases.isEmpty()) return 0.0;
    
    // Calcular regularidad basada en frecuencia de compras
    // 100% si hay compras todos los meses, menos si hay gaps
    OffsetDateTime oneYearAgo = OffsetDateTime.now().minusYears(1);
    Set<String> monthsWithPurchases = purchases.stream()
        .map(Purchase::getIssuedAt)
        .filter(Objects::nonNull)
        .filter(date -> !date.isBefore(oneYearAgo))
        .map(date -> date.getYear() + "-" + date.getMonthValue())
        .collect(Collectors.toSet());

    int expectedMonths = 12;
    return (monthsWithPurchases.size() / (double) expectedMonths) * 100.0;
  }

  private Double calculateScore(SupplierMetrics metrics, String criteria, BigDecimal grandTotal) {
    // Criteria: volume, reliability, price, value
    if ("volume".equals(criteria)) {
      // Score basado en volumen de compras
      if (grandTotal.compareTo(BigDecimal.ZERO) == 0) return 0.0;
      return metrics.getTotalAmount().divide(grandTotal, 4, RoundingMode.HALF_UP)
          .multiply(BigDecimal.valueOf(100)).doubleValue();
    }
    
    // Por defecto: score combinado (volumen 60% + cantidad de órdenes 40%)
    Double volumeScore = grandTotal.compareTo(BigDecimal.ZERO) > 0
        ? metrics.getTotalAmount().divide(grandTotal, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(60)).doubleValue()
        : 0.0;
    
    Double orderScore = metrics.getTotalPurchases() * 2.0; // 2 puntos por orden, máx 40
    if (orderScore > 40.0) orderScore = 40.0;
    
    return volumeScore + orderScore;
  }

  /**
   * Obtiene el historial de precios de un producto específico de un proveedor
   */
  public SupplierPriceHistory getPriceHistory(UUID supplierId, UUID productId) {
    // Verificar que el proveedor existe
    Supplier supplier = supplierRepository.findById(supplierId)
        .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

    SupplierPriceHistory history = new SupplierPriceHistory();
    history.setSupplierId(supplierId);
    history.setSupplierName(supplier.getName());
    history.setProductId(productId);
    history.setProductName(resolveProductName(productId));

    // Obtener compras del último año del proveedor
    OffsetDateTime oneYearAgo = OffsetDateTime.now().minus(1, ChronoUnit.YEARS);
    List<Purchase> purchases = purchaseRepository.findByIssuedAtGreaterThanEqualOrderByIssuedAtAsc(
          oneYearAgo
      ).stream()
        .filter(p -> supplierId.equals(p.getSupplierId()))
        .collect(Collectors.toList());
    Map<UUID, List<PurchaseItem>> itemsByPurchase = groupItemsByPurchase(purchases);

    // Obtener items del producto en estas compras
    List<SupplierPriceHistory.PricePoint> pricePoints = new ArrayList<>();
    
    for (Purchase purchase : purchases) {
      List<PurchaseItem> items = itemsByPurchase.getOrDefault(purchase.getId(), Collections.emptyList());
      for (PurchaseItem item : items) {
        if (productId.equals(item.getProductId())) {
          LocalDate date = purchase.getIssuedAt() != null 
              ? purchase.getIssuedAt().toLocalDate() 
              : LocalDate.now();
          pricePoints.add(new SupplierPriceHistory.PricePoint(
              date,
              item.getUnitCost(),
              item.getQty()
          ));
        }
      }
    }

    // Ordenar por fecha
    pricePoints.sort(Comparator.comparing(SupplierPriceHistory.PricePoint::getDate));
    history.setPriceHistory(pricePoints);

    if (pricePoints.isEmpty()) {
      history.setCurrentPrice(BigDecimal.ZERO);
      history.setAveragePrice(BigDecimal.ZERO);
      history.setMinPrice(BigDecimal.ZERO);
      history.setMaxPrice(BigDecimal.ZERO);
      history.setTrend("STABLE");
      history.setTrendPercentage(BigDecimal.ZERO);
      return history;
    }

    // Calcular estadísticas
    BigDecimal currentPrice = pricePoints.get(pricePoints.size() - 1).getUnitPrice();
    BigDecimal minPrice = pricePoints.stream()
        .map(SupplierPriceHistory.PricePoint::getUnitPrice)
        .min(BigDecimal::compareTo)
        .orElse(BigDecimal.ZERO);
    BigDecimal maxPrice = pricePoints.stream()
        .map(SupplierPriceHistory.PricePoint::getUnitPrice)
        .max(BigDecimal::compareTo)
        .orElse(BigDecimal.ZERO);
    BigDecimal avgPrice = pricePoints.stream()
        .map(SupplierPriceHistory.PricePoint::getUnitPrice)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .divide(BigDecimal.valueOf(pricePoints.size()), 4, RoundingMode.HALF_UP);

    history.setCurrentPrice(currentPrice);
    history.setAveragePrice(avgPrice);
    history.setMinPrice(minPrice);
    history.setMaxPrice(maxPrice);

    // Calcular tendencia (últimos 3 meses vs 3 meses anteriores)
    LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
    LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
    
    List<BigDecimal> recentPrices = pricePoints.stream()
        .filter(p -> !p.getDate().isBefore(threeMonthsAgo))
        .map(SupplierPriceHistory.PricePoint::getUnitPrice)
        .collect(Collectors.toList());
    
    List<BigDecimal> previousPrices = pricePoints.stream()
        .filter(p -> !p.getDate().isBefore(sixMonthsAgo) && p.getDate().isBefore(threeMonthsAgo))
        .map(SupplierPriceHistory.PricePoint::getUnitPrice)
        .collect(Collectors.toList());

    if (!recentPrices.isEmpty() && !previousPrices.isEmpty()) {
      BigDecimal recentAvg = recentPrices.stream()
          .reduce(BigDecimal.ZERO, BigDecimal::add)
          .divide(BigDecimal.valueOf(recentPrices.size()), 4, RoundingMode.HALF_UP);
      
      BigDecimal previousAvg = previousPrices.stream()
          .reduce(BigDecimal.ZERO, BigDecimal::add)
          .divide(BigDecimal.valueOf(previousPrices.size()), 4, RoundingMode.HALF_UP);

      if (previousAvg.compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal change = recentAvg.subtract(previousAvg)
            .divide(previousAvg, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
        
        history.setTrendPercentage(change);
        
        if (change.abs().compareTo(BigDecimal.valueOf(5)) < 0) {
          history.setTrend("STABLE");
        } else if (change.compareTo(BigDecimal.ZERO) > 0) {
          history.setTrend("UP");
        } else {
          history.setTrend("DOWN");
        }
      } else {
        history.setTrend("STABLE");
        history.setTrendPercentage(BigDecimal.ZERO);
      }
    } else {
      history.setTrend("STABLE");
      history.setTrendPercentage(BigDecimal.ZERO);
    }

    return history;
  }

  /**
   * Identifica oportunidades de negociación con proveedores
   * Busca productos donde el precio actual está significativamente por encima del promedio del mercado
   */
  public List<NegotiationOpportunity> getNegotiationOpportunities() {
    List<NegotiationOpportunity> opportunities = new ArrayList<>();

    // Obtener compras del último año
    OffsetDateTime oneYearAgo = OffsetDateTime.now().minus(1, ChronoUnit.YEARS);
    List<Purchase> purchases = purchaseRepository.findByIssuedAtGreaterThanEqualOrderByIssuedAtAsc(
        oneYearAgo
    );
    Map<UUID, List<PurchaseItem>> itemsByPurchase = groupItemsByPurchase(purchases);

    // Agrupar por producto y calcular precios promedio por proveedor
    Map<UUID, Map<UUID, List<BigDecimal>>> productSupplierPrices = new HashMap<>();
    Map<UUID, Map<UUID, Long>> productSupplierCounts = new HashMap<>();
    
    for (Purchase purchase : purchases) {
      List<PurchaseItem> items = itemsByPurchase.getOrDefault(purchase.getId(), Collections.emptyList());
      for (PurchaseItem item : items) {
        UUID productId = item.getProductId();
        UUID supplierId = purchase.getSupplierId();
        
        if (productId != null && supplierId != null) {
          productSupplierPrices
              .computeIfAbsent(productId, k -> new HashMap<>())
              .computeIfAbsent(supplierId, k -> new ArrayList<>())
              .add(item.getUnitCost());
          
          productSupplierCounts
              .computeIfAbsent(productId, k -> new HashMap<>())
              .merge(supplierId, 1L, Long::sum);
        }
      }
    }

    // Analizar cada producto que tiene múltiples proveedores
    Map<UUID, String> productNames = resolveProductNames(productSupplierPrices.keySet());
    for (Map.Entry<UUID, Map<UUID, List<BigDecimal>>> entry : productSupplierPrices.entrySet()) {
      UUID productId = entry.getKey();
      Map<UUID, List<BigDecimal>> supplierPrices = entry.getValue();
      
      // Solo analizar si hay al menos 2 proveedores
      if (supplierPrices.size() < 2) continue;
      
      // Calcular promedio del mercado (todos los proveedores)
      List<BigDecimal> allPrices = supplierPrices.values().stream()
          .flatMap(List::stream)
          .collect(Collectors.toList());
      
      BigDecimal marketAvg = allPrices.stream()
          .reduce(BigDecimal.ZERO, BigDecimal::add)
          .divide(BigDecimal.valueOf(allPrices.size()), 4, RoundingMode.HALF_UP);

      // Analizar cada proveedor
      for (Map.Entry<UUID, List<BigDecimal>> supplierEntry : supplierPrices.entrySet()) {
        UUID supplierId = supplierEntry.getKey();
        List<BigDecimal> prices = supplierEntry.getValue();
        
        BigDecimal avgPrice = prices.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(prices.size()), 4, RoundingMode.HALF_UP);

        // Si el precio promedio del proveedor está >10% por encima del mercado
        BigDecimal difference = avgPrice.subtract(marketAvg);
        double percentageAbove = marketAvg.compareTo(BigDecimal.ZERO) > 0
            ? difference.divide(marketAvg, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue()
            : 0.0;

        if (percentageAbove > 10.0) {
          Long purchaseCount = productSupplierCounts.get(productId).get(supplierId);
          BigDecimal totalSpent = avgPrice.multiply(BigDecimal.valueOf(purchaseCount));
          BigDecimal potentialSavings = difference.multiply(BigDecimal.valueOf(purchaseCount));

          NegotiationOpportunity opp = new NegotiationOpportunity();
          opp.setSupplierId(supplierId);
          opp.setSupplierName(getSupplierName(supplierId));
          opp.setProductId(productId);
          opp.setProductName(productNames.getOrDefault(productId, fallbackProductName(productId)));
          opp.setCurrentPrice(avgPrice);
          opp.setMarketAverage(marketAvg);
          opp.setPriceDifference(difference);
          opp.setPricePercentageAboveMarket(percentageAbove);
          opp.setPurchasesLast12Months(purchaseCount);
          opp.setTotalSpentLast12Months(totalSpent);
          opp.setPotentialSavings(potentialSavings);
          
          // Determinar prioridad basada en savings potenciales
          if (potentialSavings.compareTo(BigDecimal.valueOf(100000)) > 0) {
            opp.setPriority("HIGH");
            opp.setRecommendation("Negociar precio urgente - alto impacto en costos");
          } else if (potentialSavings.compareTo(BigDecimal.valueOf(50000)) > 0) {
            opp.setPriority("MEDIUM");
            opp.setRecommendation("Revisar precios y considerar alternativas");
          } else {
            opp.setPriority("LOW");
            opp.setRecommendation("Monitorear y evaluar en próxima renovación");
          }
          
          opportunities.add(opp);
        }
      }
    }

    // Ordenar por savings potenciales (mayor primero)
    opportunities.sort((a, b) -> b.getPotentialSavings().compareTo(a.getPotentialSavings()));
    
    return opportunities;
  }

  /**
   * Identifica productos que solo tienen un proveedor (riesgo de concentración)
   */
    public List<SingleSourceProduct> getSingleSourceProducts() {
    List<SingleSourceProduct> singleSourceProducts = new ArrayList<>();
    
    // Obtener compras del último año
    OffsetDateTime oneYearAgo = OffsetDateTime.now().minus(1, ChronoUnit.YEARS);
    List<Purchase> purchases = purchaseRepository.findByIssuedAtGreaterThanEqualOrderByIssuedAtAsc(
        oneYearAgo
    );
    Map<UUID, List<PurchaseItem>> itemsByPurchase = groupItemsByPurchase(purchases);

    // Agrupar por producto y contar proveedores únicos
    Map<UUID, Set<UUID>> productSuppliers = new HashMap<>();
    Map<UUID, List<PurchaseItem>> productItems = new HashMap<>();
    Map<UUID, LocalDate> productLastPurchase = new HashMap<>();
    
    for (Purchase purchase : purchases) {
      List<PurchaseItem> items = itemsByPurchase.getOrDefault(purchase.getId(), Collections.emptyList());
      for (PurchaseItem item : items) {
        UUID productId = item.getProductId();
        UUID supplierId = purchase.getSupplierId();
        
        if (productId != null && supplierId != null) {
          productSuppliers.computeIfAbsent(productId, k -> new HashSet<>()).add(supplierId);
          productItems.computeIfAbsent(productId, k -> new ArrayList<>()).add(item);
          
          LocalDate purchaseDate = purchase.getIssuedAt() != null 
              ? purchase.getIssuedAt().toLocalDate() 
              : LocalDate.now();
          
          productLastPurchase.merge(productId, purchaseDate, (d1, d2) -> d1.isAfter(d2) ? d1 : d2);
        }
      }
    }

    // Identificar productos con un solo proveedor
    Map<UUID, String> productNames = resolveProductNames(productSuppliers.keySet());
    for (Map.Entry<UUID, Set<UUID>> entry : productSuppliers.entrySet()) {
      UUID productId = entry.getKey();
      Set<UUID> suppliers = entry.getValue();
      
      if (suppliers.size() == 1) {
        UUID supplierId = suppliers.iterator().next();
        List<PurchaseItem> items = productItems.get(productId);
        if (items == null || items.isEmpty()) {
          continue;
        }
        
        // Calcular métricas
        Long purchaseCount = (long) items.size();
        BigDecimal avgPrice = items.stream()
            .map(PurchaseItem::getUnitCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(items.size()), 4, RoundingMode.HALF_UP);
        
        BigDecimal totalSpent = items.stream()
            .map(item -> item.getUnitCost().multiply(item.getQty()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        SingleSourceProduct product = new SingleSourceProduct();
        product.setProductId(productId);
        product.setProductName(productNames.getOrDefault(productId, fallbackProductName(productId)));
        product.setSupplierId(supplierId);
        product.setSupplierName(getSupplierName(supplierId));
        product.setCurrentPrice(avgPrice);
        product.setPurchasesLast12Months(purchaseCount);
        product.setTotalSpentLast12Months(totalSpent);
        product.setLastPurchaseDate(productLastPurchase.get(productId));
        
        // Determinar nivel de riesgo basado en gasto
        if (totalSpent.compareTo(BigDecimal.valueOf(500000)) > 0) {
          product.setRiskLevel("CRITICAL");
          product.setRecommendation("Urgente: Buscar proveedores alternativos - alta dependencia");
        } else if (totalSpent.compareTo(BigDecimal.valueOf(200000)) > 0) {
          product.setRiskLevel("HIGH");
          product.setRecommendation("Diversificar proveedores para reducir riesgo");
        } else if (totalSpent.compareTo(BigDecimal.valueOf(50000)) > 0) {
          product.setRiskLevel("MEDIUM");
          product.setRecommendation("Evaluar opciones de proveedores adicionales");
        } else {
          product.setRiskLevel("LOW");
          product.setRecommendation("Monitorear disponibilidad del proveedor");
        }
        
        singleSourceProducts.add(product);
      }
    }

    // Ordenar por gasto total (mayor primero)
    singleSourceProducts.sort((a, b) -> b.getTotalSpentLast12Months().compareTo(a.getTotalSpentLast12Months()));
    
    return singleSourceProducts;
  }

  private String getSupplierName(UUID supplierId) {
    return supplierRepository.findById(supplierId)
        .map(Supplier::getName)
        .orElse("Proveedor " + supplierId.toString().substring(0, 8));
  }

  /**
   * Genera forecast de compras por proveedor basado en historial
   */
  public PurchaseForecast getPurchaseForecast(UUID supplierId) {
    Supplier supplier = supplierRepository.findById(supplierId)
        .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

    PurchaseForecast forecast = new PurchaseForecast();
    forecast.setSupplierId(supplierId);
    forecast.setSupplierName(supplier.getName());

    // Obtener compras de los últimos 12 meses
    OffsetDateTime twelveMonthsAgo = OffsetDateTime.now().minus(12, ChronoUnit.MONTHS);
    List<Purchase> purchases = purchaseRepository.findByIssuedAtGreaterThanEqualOrderByIssuedAtAsc(
        twelveMonthsAgo
    ).stream()
        .filter(p -> supplierId.equals(p.getSupplierId()))
        .collect(Collectors.toList());

    // Agrupar por mes
    Map<String, List<Purchase>> purchasesByMonth = new LinkedHashMap<>();
    for (Purchase purchase : purchases) {
      if (purchase.getIssuedAt() != null) {
        String monthKey = purchase.getIssuedAt().getYear() + "-" 
            + String.format("%02d", purchase.getIssuedAt().getMonthValue());
        purchasesByMonth.computeIfAbsent(monthKey, k -> new ArrayList<>()).add(purchase);
      }
    }

    // Crear forecasts mensuales (últimos 6 meses históricos + 3 meses proyectados)
    List<PurchaseForecast.MonthlyForecast> monthlyForecasts = new ArrayList<>();
    LocalDate now = LocalDate.now();
    
    // Generar últimos 6 meses históricos
    for (int i = 5; i >= 0; i--) {
      LocalDate monthDate = now.minusMonths(i).withDayOfMonth(1);
      String monthKey = monthDate.getYear() + "-" + String.format("%02d", monthDate.getMonthValue());
      
      List<Purchase> monthPurchases = purchasesByMonth.getOrDefault(monthKey, new ArrayList<>());
      BigDecimal monthSpend = monthPurchases.stream()
          .map(Purchase::getTotal)
          .filter(java.util.Objects::nonNull)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      
      PurchaseForecast.MonthlyForecast mf = new PurchaseForecast.MonthlyForecast();
      mf.setMonth(monthKey);
      mf.setMonthDate(monthDate);
      mf.setActualSpend(monthSpend);
      mf.setActualOrders((long) monthPurchases.size());
      mf.setForecast(false);
      
      monthlyForecasts.add(mf);
    }

    // Calcular promedio de últimos 6 meses para proyección
    BigDecimal totalSpend = monthlyForecasts.stream()
        .map(PurchaseForecast.MonthlyForecast::getActualSpend)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    long totalOrders = monthlyForecasts.stream()
        .mapToLong(PurchaseForecast.MonthlyForecast::getActualOrders)
        .sum();
    
    BigDecimal avgMonthlySpend = monthlyForecasts.size() > 0
        ? totalSpend.divide(BigDecimal.valueOf(monthlyForecasts.size()), 2, RoundingMode.HALF_UP)
        : BigDecimal.ZERO;
    
    long avgMonthlyOrders = monthlyForecasts.size() > 0
        ? totalOrders / monthlyForecasts.size()
        : 0;

    // Detectar tendencia (comparar últimos 3 vs anteriores 3)
    BigDecimal recent3MonthsAvg = monthlyForecasts.stream()
        .skip(3)
        .map(PurchaseForecast.MonthlyForecast::getActualSpend)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
    
    BigDecimal previous3MonthsAvg = monthlyForecasts.stream()
        .limit(3)
        .map(PurchaseForecast.MonthlyForecast::getActualSpend)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);

    String trend = "STABLE";
    if (previous3MonthsAvg.compareTo(BigDecimal.ZERO) > 0) {
      double changePct = recent3MonthsAvg.subtract(previous3MonthsAvg)
          .divide(previous3MonthsAvg, 4, RoundingMode.HALF_UP)
          .multiply(BigDecimal.valueOf(100))
          .doubleValue();
      
      if (changePct > 10) {
        trend = "INCREASING";
      } else if (changePct < -10) {
        trend = "DECREASING";
      }
    }

    // Generar 3 meses de proyección
    for (int i = 1; i <= 3; i++) {
      LocalDate futureMonth = now.plusMonths(i).withDayOfMonth(1);
      String monthKey = futureMonth.getYear() + "-" + String.format("%02d", futureMonth.getMonthValue());
      
      // Aplicar tendencia a la proyección
      BigDecimal forecastSpend = avgMonthlySpend;
      if ("INCREASING".equals(trend)) {
        forecastSpend = avgMonthlySpend.multiply(BigDecimal.valueOf(1.05)); // +5%
      } else if ("DECREASING".equals(trend)) {
        forecastSpend = avgMonthlySpend.multiply(BigDecimal.valueOf(0.95)); // -5%
      }
      
      PurchaseForecast.MonthlyForecast mf = new PurchaseForecast.MonthlyForecast();
      mf.setMonth(monthKey);
      mf.setMonthDate(futureMonth);
      mf.setForecastSpend(forecastSpend);
      mf.setForecastOrders(avgMonthlyOrders);
      mf.setForecast(true);
      
      monthlyForecasts.add(mf);
    }

    forecast.setMonthlyForecasts(monthlyForecasts);
    forecast.setAverageMonthlySpend(avgMonthlySpend);
    forecast.setProjectedNextMonthSpend(
        monthlyForecasts.stream()
            .filter(PurchaseForecast.MonthlyForecast::isForecast)
            .findFirst()
            .map(PurchaseForecast.MonthlyForecast::getForecastSpend)
            .orElse(avgMonthlySpend)
    );
    forecast.setAverageMonthlyOrders(avgMonthlyOrders);
    forecast.setTrend(trend);

    // Generar recomendación
    if ("INCREASING".equals(trend)) {
      forecast.setRecommendation("Demanda en aumento. Considere negociar descuentos por volumen.");
    } else if ("DECREASING".equals(trend)) {
      forecast.setRecommendation("Demanda decreciente. Revise necesidad del proveedor o renegocie términos.");
    } else {
      forecast.setRecommendation("Demanda estable. Mantenga stock de seguridad según promedio.");
    }

    return forecast;
  }
}

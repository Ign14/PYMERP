package com.datakomerz.pymes.sales;

import com.datakomerz.pymes.common.payments.PaymentTerm;
import com.datakomerz.pymes.company.CompanyRepository;
import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.datakomerz.pymes.customers.Customer;
import com.datakomerz.pymes.customers.CustomerRepository;
import com.datakomerz.pymes.inventory.InventoryService;
import com.datakomerz.pymes.pricing.PricingService;
import com.datakomerz.pymes.products.Product;
import com.datakomerz.pymes.products.ProductRepository;
import com.datakomerz.pymes.sales.dto.SaleDetail;
import com.datakomerz.pymes.sales.dto.SaleDetailCustomer;
import com.datakomerz.pymes.sales.dto.SaleDetailLine;
import com.datakomerz.pymes.sales.dto.SaleReq;
import com.datakomerz.pymes.sales.dto.SaleRes;
import com.datakomerz.pymes.sales.dto.SaleSummary;
import com.datakomerz.pymes.sales.dto.SaleUpdateRequest;
import com.datakomerz.pymes.sales.dto.SalesDailyPoint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesService {
  private static final Logger log = LoggerFactory.getLogger(SalesService.class);
  private static final SaleDocumentType[] KPI_DOC_TYPES = {SaleDocumentType.FACTURA, SaleDocumentType.BOLETA};
  private static final SalePaymentMethod[] KPI_PAYMENT_METHODS = {
    SalePaymentMethod.EFECTIVO,
    SalePaymentMethod.TRANSFERENCIA,
    SalePaymentMethod.TARJETAS
  };

  private final SaleRepository sales;
  private final SaleItemRepository items;
  private final InventoryService inventory;
  private final CompanyContext companyContext;
  private final CustomerRepository customers;
  private final ProductRepository products;
  private final CompanyRepository companies;
  private final PricingService pricingService;
  private final Environment environment;

  public SalesService(SaleRepository sales,
                      SaleItemRepository items,
                      InventoryService inventory,
                      CompanyContext companyContext,
                      CustomerRepository customers,
                      ProductRepository products,
                      CompanyRepository companies,
                      PricingService pricingService,
                      Environment environment) {
    this.sales = sales;
    this.items = items;
    this.inventory = inventory;
    this.companyContext = companyContext;
    this.customers = customers;
    this.products = products;
    this.companies = companies;
    this.pricingService = pricingService;
    this.environment = environment;
  }

  @Transactional
  public SaleRes create(SaleReq req) {
    UUID companyId = companyContext.require();
    BigDecimal net = BigDecimal.ZERO;
    for (var it : req.items()) {
      BigDecimal discount = safeDiscount(it.discount());
      BigDecimal price = it.unitPrice().subtract(discount);
      BigDecimal line = price.multiply(it.qty());
      net = net.add(line);
    }
    BigDecimal vat = net.multiply(new BigDecimal("0.19")).setScale(0, RoundingMode.HALF_UP);
    BigDecimal total = net.add(vat);

    var sale = new Sale();
    sale.setCompanyId(companyId);
    sale.setCustomerId(req.customerId());
    sale.setStatus("emitida");
    sale.setNet(net);
    sale.setVat(vat);
    sale.setTotal(total);
    sale.setPaymentMethod(SalePaymentMethod.from(req.paymentMethod()).label());
    sale.setDocType(SaleDocumentType.from(req.docType()).label());
    sales.save(sale);

    for (var item : req.items()) {
      var saleItem = new SaleItem();
      saleItem.setSaleId(sale.getId());
      saleItem.setProductId(item.productId());
      saleItem.setQty(item.qty());
      saleItem.setUnitPrice(item.unitPrice());
      saleItem.setDiscount(safeDiscount(item.discount()));
      items.save(saleItem);

      // Consumir inventario con FIFO: usa lotId específico si está disponible, sino locationId, sino FIFO automático
      inventory.consumeFIFO(sale.getId(), item.productId(), item.qty(), item.locationId(), item.lotId());
    }

    String customerName = resolveCustomerName(sale.getCustomerId());
    return mapToRes(sale, customerName);
  }

  @Transactional(readOnly = true)
  public Page<SaleSummary> list(String status,
                                String docType,
                                String paymentMethod,
                                String search,
                                OffsetDateTime from,
                                OffsetDateTime to,
                                Pageable pageable) {
    Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("issuedAt").descending());
    Page<Sale> page = sales.search(
      emptyToNull(status),
      emptyToNull(docType),
      emptyToNull(paymentMethod),
      emptyToNull(search),
      from,
      to,
      sorted
    );
    Map<UUID, String> customerNames = resolveCustomerNames(page.getContent());
    return page.map(sale -> mapToSummary(sale, customerNames));
  }

  @Transactional(readOnly = true)
  public SaleDetail detail(UUID id) {
    Sale sale = sales.findById(id)
      .orElseThrow(() -> new IllegalStateException("Sale not found: " + id));

    List<SaleItem> saleItems = items.findBySaleId(sale.getId());
    Map<UUID, Product> productIndex = resolveProducts(saleItems);
    List<SaleDetailLine> lines = saleItems.stream()
      .map(item -> toDetailLine(item, productIndex))
      .toList();

    String customerName = resolveCustomerName(sale.getCustomerId());
    SaleDetailCustomer customerDto = sale.getCustomerId() == null
      ? null
      : new SaleDetailCustomer(sale.getCustomerId(), customerName);

    UUID companyId = sale.getCompanyId();
    String companyName = companyId != null
      ? companies.findById(companyId)
        .map(com.datakomerz.pymes.company.Company::getBusinessName)
        .orElse("PyMEs Suite")
      : "PyMEs Suite";

    String ticket = ThermalTicketFormatter.build(companyName, sale, customerName, lines);

    return new SaleDetail(
      sale.getId(),
      sale.getIssuedAt(),
      sale.getDueDate(),
      safeDocType(sale.getDocType()),
      safePaymentMethod(sale.getPaymentMethod()),
      sale.getPaymentTermDays(),
      sale.getStatus(),
      customerDto,
      lines,
      sale.getNet(),
      sale.getVat(),
      sale.getTotal(),
      ticket
    );
  }

  @Transactional
  public SaleRes update(UUID id, SaleUpdateRequest req) {
    Sale sale = sales.findById(id)
      .orElseThrow(() -> new IllegalStateException("Sale not found: " + id));

    if (req == null) {
      String customerName = resolveCustomerName(sale.getCustomerId());
      return mapToRes(sale, customerName);
    }

    if (req.status() != null && !req.status().equalsIgnoreCase(sale.getStatus())) {
      if ("cancelled".equalsIgnoreCase(req.status())) {
        return cancel(id);
      }
      sale.setStatus(req.status());
    }

    if (req.docType() != null) {
      sale.setDocType(SaleDocumentType.from(req.docType()).label());
    }

    if (req.paymentMethod() != null) {
      sale.setPaymentMethod(SalePaymentMethod.from(req.paymentMethod()).label());
    }

    sales.save(sale);
    String customerName = resolveCustomerName(sale.getCustomerId());
    return mapToRes(sale, customerName);
  }

  @Transactional
  public SaleRes cancel(UUID id) {
    Sale sale = sales.findById(id)
      .orElseThrow(() -> new IllegalStateException("Sale not found: " + id));

    if ("cancelled".equalsIgnoreCase(sale.getStatus())) {
      String customerName = resolveCustomerName(sale.getCustomerId());
      return mapToRes(sale, customerName);
    }

    inventory.restockSale(sale.getId());
    sale.setStatus("cancelled");
    sales.save(sale);
    String customerName = resolveCustomerName(sale.getCustomerId());
    return mapToRes(sale, customerName);
  }

  @Transactional(readOnly = true)
  public List<SalesDailyPoint> dailyMetrics(int days) {
    OffsetDateTime from = OffsetDateTime.now().minusDays(days);
    List<Sale> range = sales.findByIssuedAtGreaterThanEqualOrderByIssuedAtAsc(from);

    return range.stream()
      .collect(Collectors.groupingBy(s -> s.getIssuedAt().toLocalDate()))
      .entrySet().stream()
      .map(entry -> {
        BigDecimal total = entry.getValue().stream()
          .map(Sale::getTotal)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
        long count = entry.getValue().size();
        return new SalesDailyPoint(entry.getKey(), total, count);
      })
      .sorted(java.util.Comparator.comparing(SalesDailyPoint::date))
      .toList();
  }

  @Transactional(readOnly = true)
  public List<SalesDailyPoint> dailyMetricsByRange(LocalDate from, LocalDate to) {
    OffsetDateTime fromDateTime = from.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
    OffsetDateTime toDateTime = to.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
    
    List<Sale> range = sales.findByIssuedAtBetweenOrderByIssuedAtAsc(fromDateTime, toDateTime);

    return range.stream()
      .collect(Collectors.groupingBy(s -> s.getIssuedAt().toLocalDate()))
      .entrySet().stream()
      .map(entry -> {
        BigDecimal total = entry.getValue().stream()
          .map(Sale::getTotal)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
        long count = entry.getValue().size();
        return new SalesDailyPoint(entry.getKey(), total, count);
      })
      .sorted(java.util.Comparator.comparing(SalesDailyPoint::date))
      .toList();
  }

  private SaleRes mapToRes(Sale sale, String customerName) {
    return new SaleRes(
      sale.getId(),
      sale.getCustomerId(),
      customerName,
      sale.getStatus(),
      sale.getNet(),
      sale.getVat(),
      sale.getTotal(),
      sale.getIssuedAt(),
      sale.getDueDate(),
      sale.getPaymentTermDays(),
      safeDocType(sale.getDocType()),
      safePaymentMethod(sale.getPaymentMethod())
    );
  }

  private SaleSummary mapToSummary(Sale sale, Map<UUID, String> customerNames) {
    return new SaleSummary(
      sale.getId(),
      sale.getCustomerId(),
      customerNames.getOrDefault(sale.getCustomerId(), null),
      safeDocType(sale.getDocType()),
      safePaymentMethod(sale.getPaymentMethod()),
      sale.getPaymentTermDays(),
      sale.getDueDate(),
      sale.getStatus(),
      sale.getNet(),
      sale.getVat(),
      sale.getTotal(),
      sale.getIssuedAt()
    );
  }

  private String safeDocType(String docType) {
    return docType == null ? SaleDocumentType.FACTURA.label() : docType;
  }

  private String safePaymentMethod(String paymentMethod) {
    return paymentMethod == null ? SalePaymentMethod.TRANSFERENCIA.label() : paymentMethod;
  }

  private String emptyToNull(String value) {
    return (value != null && !value.isBlank()) ? value : null;
  }

  private BigDecimal safeDiscount(BigDecimal discount) {
    return discount == null ? BigDecimal.ZERO : discount;
  }

  private String resolveCustomerName(UUID customerId) {
    if (customerId == null) {
      return null;
    }
    Optional<Customer> customer = customers.findById(customerId);
    return customer.map(Customer::getName).orElse(null);
  }

  private Map<UUID, String> resolveCustomerNames(Collection<Sale> saleCollection) {
    Set<UUID> ids = saleCollection.stream()
      .map(Sale::getCustomerId)
      .filter(Objects::nonNull)
      .collect(Collectors.toCollection(HashSet::new));
    if (ids.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<UUID, String> map = new HashMap<>();
    customers.findAllById(ids).forEach(customer -> map.put(customer.getId(), customer.getName()));
    return map;
  }

  private Map<UUID, Product> resolveProducts(List<SaleItem> saleItems) {
    Set<UUID> productIds = saleItems.stream()
      .map(SaleItem::getProductId)
      .filter(Objects::nonNull)
      .collect(Collectors.toCollection(HashSet::new));
    if (productIds.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<UUID, Product> productMap = new HashMap<>();
    products.findAllById(productIds).forEach(product -> productMap.put(product.getId(), product));
    return productMap;
  }

  private SaleDetailLine toDetailLine(SaleItem item, Map<UUID, Product> productIndex) {
    BigDecimal discount = safeDiscount(item.getDiscount());
    BigDecimal lineTotal = item.getUnitPrice().subtract(discount).multiply(item.getQty());
    String productName = Optional.ofNullable(productIndex.get(item.getProductId()))
      .map(Product::getName)
      .orElseGet(() -> item.getProductId() != null ? item.getProductId().toString() : "Producto");
    return new SaleDetailLine(
      item.getProductId(),
      productName,
      item.getQty(),
      item.getUnitPrice(),
      discount,
      lineTotal
    );
  }

  @Transactional
  public int seedSalesData() {
    ensureDevProfile();
    UUID companyId = companyContext.require();
    ThreadLocalRandom random = ThreadLocalRandom.current();

    List<Customer> seedCustomers = selectSeedCustomers(random);
    List<PricedProduct> seedProducts = selectSeedProducts(random);
    LocalDate today = LocalDate.now();

    int created = 0;
    for (int i = 0; i < 15; i++) {
      LocalDate saleDate = today.minusDays(random.nextInt(45));
      Customer customer = seedCustomers.get(random.nextInt(seedCustomers.size()));
      int itemsPerSale = 1 + random.nextInt(4);
      List<SeedLine> lines = buildSeedLines(seedProducts, itemsPerSale, random);

      BigDecimal net = lines.stream()
        .map(line -> line.unitPrice().multiply(line.quantity()))
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(2, RoundingMode.HALF_UP);
      BigDecimal vat = net.multiply(new BigDecimal("0.19")).setScale(0, RoundingMode.HALF_UP);
      BigDecimal total = net.add(vat).setScale(2, RoundingMode.HALF_UP);

      SaleDocumentType docType = pickRandomDocType(random);
      SalePaymentMethod paymentMethod = pickRandomPaymentMethod(random);
      PaymentTerm paymentTerm = pickRandomPaymentTerm(random);

      Sale sale = new Sale();
      sale.setCompanyId(companyId);
      sale.setCustomerId(customer.getId());
      sale.setStatus("emitida");
      sale.setNet(net);
      sale.setVat(vat);
      sale.setTotal(total);
      sale.setDocType(docType.label());
      sale.setPaymentMethod(paymentMethod.label());
      sale.setPaymentTermDays(paymentTerm.getDays());
      sale.setIssuedAt(randomIssuedAt(saleDate, random));
      sales.save(sale);

      List<SaleItem> saleItems = new ArrayList<>();
      for (SeedLine line : lines) {
        SaleItem saleItem = new SaleItem();
        saleItem.setSaleId(sale.getId());
        saleItem.setProductId(line.productId());
        saleItem.setQty(line.quantity());
        saleItem.setUnitPrice(line.unitPrice());
        saleItem.setDiscount(BigDecimal.ZERO);
        saleItems.add(saleItem);
      }
      items.saveAll(saleItems);

      created++;
      log.info("Venta seed creada: {} items, cliente {}, fecha {}", lines.size(), customer.getName(), sale.getIssuedAt());
    }

    log.info("✅ Seed completado: {} ventas creadas para la compañía {}", created, companyId);
    return created;
  }

  private List<Customer> selectSeedCustomers(ThreadLocalRandom random) {
    List<Customer> allCustomers = new ArrayList<>(customers.findAll());
    if (allCustomers.size() < 3) {
      throw new IllegalStateException("Debe haber al menos 3 clientes registrados primero");
    }
    Collections.shuffle(allCustomers, random);
    int limit = Math.min(5, allCustomers.size());
    return new ArrayList<>(allCustomers.subList(0, limit));
  }

  private List<PricedProduct> selectSeedProducts(ThreadLocalRandom random) {
    List<Product> allProducts = products.findAll();
    List<PricedProduct> pricedProducts = new ArrayList<>();
    for (Product product : allProducts) {
      pricingService.latestPrice(product.getId()).ifPresent(price -> pricedProducts.add(new PricedProduct(product, price)));
    }
    if (pricedProducts.size() < 3) {
      throw new IllegalStateException("Debe haber al menos 3 productos con precio para generar ventas de prueba");
    }
    Collections.shuffle(pricedProducts, random);
    int limit = Math.min(10, pricedProducts.size());
    return new ArrayList<>(pricedProducts.subList(0, limit));
  }

  private List<SeedLine> buildSeedLines(List<PricedProduct> products,
                                        int itemsPerSale,
                                        ThreadLocalRandom random) {
    List<PricedProduct> pool = new ArrayList<>(products);
    Collections.shuffle(pool, random);
    List<SeedLine> lines = new ArrayList<>();
    for (int i = 0; i < itemsPerSale; i++) {
      PricedProduct selected = pool.get(i % pool.size());
      int quantity = 1 + random.nextInt(5);
      BigDecimal quantityValue = BigDecimal.valueOf(quantity).setScale(3, RoundingMode.HALF_UP);
      double variance = random.nextDouble(0.9, 1.15);
      BigDecimal unitPrice = selected.price()
        .multiply(BigDecimal.valueOf(variance))
        .setScale(4, RoundingMode.HALF_UP);
      lines.add(new SeedLine(selected.product().getId(), quantityValue, unitPrice));
    }
    return lines;
  }

  private SaleDocumentType pickRandomDocType(ThreadLocalRandom random) {
    return KPI_DOC_TYPES[random.nextInt(KPI_DOC_TYPES.length)];
  }

  private SalePaymentMethod pickRandomPaymentMethod(ThreadLocalRandom random) {
    return KPI_PAYMENT_METHODS[random.nextInt(KPI_PAYMENT_METHODS.length)];
  }

  private PaymentTerm pickRandomPaymentTerm(ThreadLocalRandom random) {
    PaymentTerm[] terms = PaymentTerm.values();
    return terms[random.nextInt(terms.length)];
  }

  private OffsetDateTime randomIssuedAt(LocalDate saleDate, ThreadLocalRandom random) {
    int hour = random.nextInt(8, 21);
    int minute = random.nextInt(0, 60);
    return saleDate.atTime(hour, minute)
      .atZone(ZoneId.systemDefault())
      .toOffsetDateTime();
  }

  private void ensureDevProfile() {
    if (!isDevProfileActive()) {
      throw new IllegalStateException("Solo disponible en perfil dev");
    }
  }

  private boolean isDevProfileActive() {
    return Arrays.stream(environment.getActiveProfiles()).anyMatch(this::isDevProfile)
      || Arrays.stream(environment.getDefaultProfiles()).anyMatch(this::isDevProfile);
  }

  private boolean isDevProfile(String profile) {
    return profile != null && profile.equalsIgnoreCase("dev");
  }

  private record PricedProduct(Product product, BigDecimal price) {}
  private record SeedLine(UUID productId, BigDecimal quantity, BigDecimal unitPrice) {}
  
  /**
   * Calcula KPIs avanzados de ventas para un período específico
   * @param startDate Fecha inicio del período
   * @param endDate Fecha fin del período
   * @return SalesKPIs con métricas del período
   */
  public com.datakomerz.pymes.sales.dto.SalesKPIs getSalesKPIs(LocalDate startDate, LocalDate endDate) {
    UUID companyId = companyContext.require();
    log.info("Calculando KPIs para período: {} a {} (tenant={})", startDate, endDate, companyId);

    // Obtener todas las ventas del período
    List<Sale> periodSales = sales.findAll().stream()
        .filter(s -> s.getIssuedAt() != null)
        .filter(s -> {
          LocalDate saleDate = s.getIssuedAt().toLocalDate();
          return !saleDate.isBefore(startDate) && !saleDate.isAfter(endDate);
        })
        .collect(Collectors.toList());

    // Filtrar ventas emitidas
    List<Sale> emittedSales = periodSales.stream()
        .filter(s -> "emitida".equalsIgnoreCase(s.getStatus()))
        .collect(Collectors.toList());

    log.info("Ventas encontradas en período: {} (emitidas: {})", periodSales.size(), emittedSales.size());

    if (emittedSales.isEmpty()) {
      log.warn("⚠️ No hay ventas en el período especificado");
      return com.datakomerz.pymes.sales.dto.SalesKPIs.empty(startDate, endDate);
    }

    // Total Revenue
    BigDecimal totalRevenue = emittedSales.stream()
        .map(Sale::getTotal)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    log.info("Total revenue calculado: {}", totalRevenue);
    
    // Total Cost (aproximado: 60% del revenue como estimación)
    BigDecimal totalCost = totalRevenue.multiply(new BigDecimal("0.60"));
    
    // Gross Profit
    BigDecimal grossProfit = totalRevenue.subtract(totalCost);
    
    // Profit Margin
    BigDecimal profitMargin = BigDecimal.ZERO;
    if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
      profitMargin = grossProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP)
          .multiply(new BigDecimal("100"));
    }
    
    // Total Orders
    Integer totalOrders = emittedSales.size();
    
    // Average Ticket
    BigDecimal averageTicket = BigDecimal.ZERO;
    if (totalOrders > 0) {
      averageTicket = totalRevenue.divide(new BigDecimal(totalOrders), 2, RoundingMode.HALF_UP);
    }
    
    // Sales Growth (comparar con período anterior)
    LocalDate prevStartDate = startDate.minusDays(endDate.toEpochDay() - startDate.toEpochDay() + 1);
    List<Sale> prevPeriodSales = sales.findAll().stream()
        .filter(s -> "emitida".equalsIgnoreCase(s.getStatus()))
        .filter(s -> s.getIssuedAt() != null)
        .filter(s -> {
          LocalDate saleDate = s.getIssuedAt().toLocalDate();
          return !saleDate.isBefore(prevStartDate) && saleDate.isBefore(startDate);
        })
        .collect(Collectors.toList());
    
    BigDecimal prevRevenue = prevPeriodSales.stream()
        .map(Sale::getTotal)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal salesGrowth = BigDecimal.ZERO;
    if (prevRevenue.compareTo(BigDecimal.ZERO) > 0) {
      salesGrowth = totalRevenue.subtract(prevRevenue)
          .divide(prevRevenue, 4, RoundingMode.HALF_UP)
          .multiply(new BigDecimal("100"));
    }
    
    // Unique Customers
    Set<UUID> uniqueCustomerIds = emittedSales.stream()
        .map(Sale::getCustomerId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    Integer uniqueCustomers = uniqueCustomerIds.size();
    
    // Customer Retention Rate (clientes del período anterior que volvieron)
    Set<UUID> prevCustomerIds = prevPeriodSales.stream()
        .map(Sale::getCustomerId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    
    long retainedCustomers = uniqueCustomerIds.stream()
        .filter(prevCustomerIds::contains)
        .count();
    
    BigDecimal retentionRate = BigDecimal.ZERO;
    if (!prevCustomerIds.isEmpty()) {
      retentionRate = new BigDecimal(retainedCustomers)
          .divide(new BigDecimal(prevCustomerIds.size()), 4, RoundingMode.HALF_UP)
          .multiply(new BigDecimal("100"));
    }
    
    // Top Product by Revenue
    Map<UUID, BigDecimal> productRevenues = new HashMap<>();
    for (Sale sale : emittedSales) {
      List<SaleItem> saleItems = items.findAll().stream()
          .filter(item -> item.getSaleId().equals(sale.getId()))
          .collect(Collectors.toList());
      
      for (SaleItem item : saleItems) {
        BigDecimal itemRevenue = item.getUnitPrice()
            .subtract(safeDiscount(item.getDiscount()))
            .multiply(item.getQty());
        productRevenues.merge(item.getProductId(), itemRevenue, BigDecimal::add);
      }
    }
    
    String topProductName = "N/A";
    BigDecimal topProductRevenue = BigDecimal.ZERO;
    UUID topProductId = productRevenues.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse(null);
    
    if (topProductId != null) {
      topProductRevenue = productRevenues.get(topProductId);
      topProductName = products.findById(topProductId)
          .map(Product::getName)
          .orElse("Producto #" + topProductId);
    }
    
    // Top Customer by Revenue
    Map<UUID, BigDecimal> customerRevenues = new HashMap<>();
    for (Sale sale : emittedSales) {
      if (sale.getCustomerId() != null) {
        customerRevenues.merge(sale.getCustomerId(), sale.getTotal(), BigDecimal::add);
      }
    }
    
    String topCustomerName = "N/A";
    BigDecimal topCustomerRevenue = BigDecimal.ZERO;
    UUID topCustomerId = customerRevenues.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse(null);
    
    if (topCustomerId != null) {
      topCustomerRevenue = customerRevenues.get(topCustomerId);
      topCustomerName = customers.findById(topCustomerId)
          .map(Customer::getName)
          .orElse("Cliente #" + topCustomerId);
    }
    
    // Conversion Rate (emitidas vs total)
    BigDecimal conversionRate = BigDecimal.ZERO;
    if (!periodSales.isEmpty()) {
      conversionRate = new BigDecimal(emittedSales.size())
          .divide(new BigDecimal(periodSales.size()), 4, RoundingMode.HALF_UP)
          .multiply(new BigDecimal("100"));
    }
    
    com.datakomerz.pymes.sales.dto.SalesKPIs kpis = new com.datakomerz.pymes.sales.dto.SalesKPIs(
        totalRevenue.setScale(2, RoundingMode.HALF_UP),
        totalCost.setScale(2, RoundingMode.HALF_UP),
        grossProfit.setScale(2, RoundingMode.HALF_UP),
        profitMargin.setScale(2, RoundingMode.HALF_UP),
        totalOrders,
        averageTicket.setScale(2, RoundingMode.HALF_UP),
        salesGrowth.setScale(2, RoundingMode.HALF_UP),
        uniqueCustomers,
        retentionRate.setScale(2, RoundingMode.HALF_UP),
        topProductName,
        topProductRevenue.setScale(2, RoundingMode.HALF_UP),
        topCustomerName,
        topCustomerRevenue.setScale(2, RoundingMode.HALF_UP),
        conversionRate.setScale(2, RoundingMode.HALF_UP),
        startDate,
        endDate
    );
    log.info("✅ KPIs calculados: revenue={}, orders={}, customers={}",
        kpis.getTotalRevenue(), kpis.getTotalOrders(), kpis.getUniqueCustomers());
    return kpis;
  }

  /**
   * Análisis ABC de productos basado en Pareto (80-15-5)
   * Clasifica productos en A (80% de ingresos), B (15%), C (5%)
   */
  public List<com.datakomerz.pymes.sales.dto.SaleABCClassification> getSalesABCAnalysis(LocalDate startDate, LocalDate endDate) {
    OffsetDateTime start = startDate.atStartOfDay(java.time.ZoneOffset.UTC).toOffsetDateTime();
    OffsetDateTime end = endDate.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toOffsetDateTime();
    
    // Obtener ventas emitidas del período
    List<Sale> periodSales = sales.findAll().stream()
        .filter(s -> s.getIssuedAt() != null)
        .filter(s -> !s.getIssuedAt().isBefore(start) && s.getIssuedAt().isBefore(end))
        .filter(s -> "emitida".equalsIgnoreCase(s.getStatus()))
        .collect(java.util.stream.Collectors.toList());
    
    if (periodSales.isEmpty()) {
      return java.util.Collections.emptyList();
    }
    
    // Obtener todos los items de estas ventas
    List<SaleItem> allItems = items.findAll();
    
    // Agrupar por producto y calcular totales
    Map<UUID, ProductData> productStats = new HashMap<>();
    
    for (Sale sale : periodSales) {
      List<SaleItem> saleItems = allItems.stream()
          .filter(item -> item.getSaleId().equals(sale.getId()))
          .collect(java.util.stream.Collectors.toList());
      
      for (SaleItem item : saleItems) {
        UUID productId = item.getProductId();
        if (productId == null) continue;
        
        ProductData data = productStats.getOrDefault(productId, new ProductData());
        BigDecimal itemRevenue = (item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO)
            .multiply(item.getQty() != null ? item.getQty() : BigDecimal.ZERO);
        data.totalRevenue = data.totalRevenue.add(itemRevenue);
        data.salesCount++;
        if (data.lastSaleDate == null || sale.getIssuedAt().isAfter(data.lastSaleDate)) {
          data.lastSaleDate = sale.getIssuedAt();
        }
        productStats.put(productId, data);
      }
    }
    
    // Calcular total global de ingresos
    BigDecimal totalRevenueGlobal = productStats.values().stream()
        .map(d -> d.totalRevenue)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    if (totalRevenueGlobal.compareTo(BigDecimal.ZERO) == 0) {
      return java.util.Collections.emptyList();
    }
    
    // Calcular porcentajes y ordenar por ingresos descendente
    List<ProductClassification> classifications = productStats.entrySet().stream()
        .map(entry -> {
          UUID productId = entry.getKey();
          ProductData data = entry.getValue();
          
          String productName = "Producto desconocido";
          Optional<Product> productOpt = products.findById(productId);
          if (productOpt.isPresent()) {
            productName = productOpt.get().getName();
          }
          
          BigDecimal percentage = data.totalRevenue
              .divide(totalRevenueGlobal, 4, RoundingMode.HALF_UP)
              .multiply(new BigDecimal("100"));
          
          BigDecimal avgPrice = data.salesCount > 0
              ? data.totalRevenue.divide(new BigDecimal(data.salesCount), 2, RoundingMode.HALF_UP)
              : BigDecimal.ZERO;
          
          return new ProductClassification(
              productId.toString(),
              productName,
              data.totalRevenue,
              data.salesCount,
              percentage,
              data.lastSaleDate,
              avgPrice
          );
        })
        .sorted((a, b) -> b.totalRevenue.compareTo(a.totalRevenue))
        .collect(java.util.stream.Collectors.toList());
    
    // Aplicar clasificación ABC según Pareto (80-15-5)
    BigDecimal cumulativePercentage = BigDecimal.ZERO;
    List<com.datakomerz.pymes.sales.dto.SaleABCClassification> result = new java.util.ArrayList<>();
    
    for (ProductClassification pc : classifications) {
      cumulativePercentage = cumulativePercentage.add(pc.percentage);
      
      String classification;
      String recommendedAction;
      
      if (cumulativePercentage.compareTo(new BigDecimal("80")) <= 0) {
        classification = "A";
        recommendedAction = "Producto estrella: mantener stock alto, promocionar activamente";
      } else if (cumulativePercentage.compareTo(new BigDecimal("95")) <= 0) {
        classification = "B";
        recommendedAction = "Producto importante: revisar pricing, optimizar inventario";
      } else {
        classification = "C";
        recommendedAction = "Producto ocasional: evaluar descontinuar o promocionar";
      }
      
      result.add(new com.datakomerz.pymes.sales.dto.SaleABCClassification(
          pc.productId,
          pc.productName,
          pc.totalRevenue.setScale(2, RoundingMode.HALF_UP),
          pc.salesCount,
          pc.percentage.setScale(2, RoundingMode.HALF_UP),
          classification,
          cumulativePercentage.setScale(2, RoundingMode.HALF_UP),
          pc.avgPrice.setScale(2, RoundingMode.HALF_UP),
          pc.lastSaleDate,
          recommendedAction
      ));
    }
    
    return result;
  }
  
  // Clase auxiliar para agrupar datos de productos
  private static class ProductData {
    BigDecimal totalRevenue = BigDecimal.ZERO;
    long salesCount = 0;
    OffsetDateTime lastSaleDate = null;
  }
  
  // Clase auxiliar para clasificación
  private static class ProductClassification {
    String productId;
    String productName;
    BigDecimal totalRevenue;
    long salesCount;
    BigDecimal percentage;
    OffsetDateTime lastSaleDate;
    BigDecimal avgPrice;
    
    ProductClassification(String productId, String productName, BigDecimal totalRevenue,
                         long salesCount, BigDecimal percentage,
                         OffsetDateTime lastSaleDate, BigDecimal avgPrice) {
      this.productId = productId;
      this.productName = productName;
      this.totalRevenue = totalRevenue;
      this.salesCount = salesCount;
      this.percentage = percentage;
      this.lastSaleDate = lastSaleDate;
      this.avgPrice = avgPrice;
    }
  }

  public List<com.datakomerz.pymes.sales.dto.SaleForecast> getSalesForecast(LocalDate startDate, LocalDate endDate, int horizonDays) {
    // Filtrar ventas emitidas en el período (usando el mismo patrón que getSalesKPIs)
    List<Sale> allSales = sales.findAll().stream()
        .filter(s -> "emitida".equals(s.getStatus()))
        .filter(s -> s.getIssuedAt() != null)
        .filter(s -> {
          LocalDate saleDate = s.getIssuedAt().toLocalDate();
          return !saleDate.isBefore(startDate) && !saleDate.isAfter(endDate);
        })
        .collect(Collectors.toList());

    // Agrupar por producto
    Map<String, ProductForecastData> productDataMap = new HashMap<>();
    
    for (Sale sale : allSales) {
      List<SaleItem> saleItems = items.findBySaleId(sale.getId());
      for (SaleItem item : saleItems) {
        String productId = item.getProductId().toString();
        Product product = products.findById(item.getProductId()).orElse(null);
        if (product == null) continue;
        
        ProductForecastData data = productDataMap.computeIfAbsent(productId, 
            k -> new ProductForecastData(product.getName()));
        
        data.addSale(item.getQty(), sale.getIssuedAt().toLocalDate());
      }
    }

    // Calcular pronósticos
    List<com.datakomerz.pymes.sales.dto.SaleForecast> forecasts = new ArrayList<>();
    long periodDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
    if (periodDays == 0) periodDays = 1;

    for (Map.Entry<String, ProductForecastData> entry : productDataMap.entrySet()) {
      String productId = entry.getKey();
      ProductForecastData data = entry.getValue();
      
      // Calcular promedio histórico (convertir a demanda mensual)
      BigDecimal totalQty = data.totalQuantity;
      int salesCount = data.salesCount;
      BigDecimal avgDaily = totalQty.divide(BigDecimal.valueOf(periodDays), 2, java.math.RoundingMode.HALF_UP);
      BigDecimal historicalMonthly = avgDaily.multiply(BigDecimal.valueOf(30));
      
      // Análisis de tendencia: comparar primera mitad vs segunda mitad
      long midPoint = periodDays / 2;
      LocalDate midDate = startDate.plusDays(midPoint);
      
      BigDecimal firstHalfQty = BigDecimal.ZERO;
      BigDecimal secondHalfQty = BigDecimal.ZERO;
      
      for (ProductSaleRecord record : data.sales) {
        if (record.date.isBefore(midDate)) {
          firstHalfQty = firstHalfQty.add(record.quantity);
        } else {
          secondHalfQty = secondHalfQty.add(record.quantity);
        }
      }
      
      // Calcular variación porcentual
      BigDecimal trendChange = BigDecimal.ZERO;
      String trendDirection = "stable";
      
      if (firstHalfQty.compareTo(BigDecimal.ZERO) > 0) {
        trendChange = secondHalfQty.subtract(firstHalfQty)
            .divide(firstHalfQty, 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
        
        if (trendChange.compareTo(BigDecimal.valueOf(10)) > 0) {
          trendDirection = "increasing";
        } else if (trendChange.compareTo(BigDecimal.valueOf(-10)) < 0) {
          trendDirection = "decreasing";
        }
      }
      
      // Aplicar tendencia al pronóstico (ajuste del 50% de la tendencia observada)
      BigDecimal trendFactor = BigDecimal.ONE;
      if (!trendChange.equals(BigDecimal.ZERO)) {
        BigDecimal adjustedChange = trendChange.multiply(BigDecimal.valueOf(0.5))
            .divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
        trendFactor = BigDecimal.ONE.add(adjustedChange);
      }
      
      BigDecimal forecastedMonthly = historicalMonthly.multiply(trendFactor)
          .setScale(2, java.math.RoundingMode.HALF_UP);
      
      // Calcular confianza basada en número de ventas
      BigDecimal confidence;
      if (salesCount >= 15) {
        confidence = BigDecimal.valueOf(85);
      } else if (salesCount >= 8) {
        confidence = BigDecimal.valueOf(70);
      } else if (salesCount >= 3) {
        confidence = BigDecimal.valueOf(50);
      } else {
        confidence = BigDecimal.valueOf(30);
      }
      
      // Estimar próxima fecha de venta basada en frecuencia histórica
      LocalDate nextSaleDate = null;
      if (salesCount > 1 && !data.sales.isEmpty()) {
        long daysBetweenSales = periodDays / salesCount;
        LocalDate lastSale = data.sales.stream()
            .map(r -> r.date)
            .max(LocalDate::compareTo)
            .orElse(endDate);
        nextSaleDate = lastSale.plusDays(daysBetweenSales);
      }
      
      // Stock recomendado: pronóstico mensual * factor de seguridad
      BigDecimal recommendedStock = forecastedMonthly.multiply(BigDecimal.valueOf(1.2))
          .setScale(0, java.math.RoundingMode.HALF_UP);
      
      // Factor de estacionalidad (simplificado a 1.0 por ahora)
      BigDecimal seasonalityFactor = BigDecimal.ONE;
      
      com.datakomerz.pymes.sales.dto.SaleForecast forecast = new com.datakomerz.pymes.sales.dto.SaleForecast(
          productId,
          data.productName,
          historicalMonthly.setScale(2, java.math.RoundingMode.HALF_UP),
          trendDirection,
          forecastedMonthly,
          confidence.setScale(2, java.math.RoundingMode.HALF_UP),
          nextSaleDate,
          recommendedStock,
          seasonalityFactor
      );
      
      forecasts.add(forecast);
    }
    
    // Ordenar por demanda pronosticada descendente
    forecasts.sort((a, b) -> b.getForecastedDemand().compareTo(a.getForecastedDemand()));
    
    return forecasts;
  }

  private static class ProductForecastData {
    String productName;
    BigDecimal totalQuantity = BigDecimal.ZERO;
    int salesCount = 0;
    List<ProductSaleRecord> sales = new ArrayList<>();
    
    ProductForecastData(String productName) {
      this.productName = productName;
    }
    
    void addSale(BigDecimal quantity, LocalDate date) {
      this.totalQuantity = this.totalQuantity.add(quantity);
      this.salesCount++;
      this.sales.add(new ProductSaleRecord(quantity, date));
    }
  }
  
  private static class ProductSaleRecord {
    BigDecimal quantity;
    LocalDate date;
    
    ProductSaleRecord(BigDecimal quantity, LocalDate date) {
      this.quantity = quantity;
      this.date = date;
    }
  }
}


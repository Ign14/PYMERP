package com.datakomerz.pymes.sales;

import com.datakomerz.pymes.company.CompanyRepository;
import com.datakomerz.pymes.core.tenancy.CompanyContext;
import com.datakomerz.pymes.customers.Customer;
import com.datakomerz.pymes.customers.CustomerRepository;
import com.datakomerz.pymes.inventory.InventoryService;
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
import java.time.OffsetDateTime;
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
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesService {
  private final SaleRepository sales;
  private final SaleItemRepository items;
  private final InventoryService inventory;
  private final CompanyContext companyContext;
  private final CustomerRepository customers;
  private final ProductRepository products;
  private final CompanyRepository companies;

  public SalesService(SaleRepository sales,
                      SaleItemRepository items,
                      InventoryService inventory,
                      CompanyContext companyContext,
                      CustomerRepository customers,
                      ProductRepository products,
                      CompanyRepository companies) {
    this.sales = sales;
    this.items = items;
    this.inventory = inventory;
    this.companyContext = companyContext;
    this.customers = customers;
    this.products = products;
    this.companies = companies;
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

      inventory.consumeFIFO(sale.getId(), item.productId(), item.qty());
    }

    String customerName = resolveCustomerName(companyId, sale.getCustomerId());
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
    UUID companyId = companyContext.require();
    Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("issuedAt").descending());
    Page<Sale> page = sales.search(
      companyId,
      emptyToNull(status),
      emptyToNull(docType),
      emptyToNull(paymentMethod),
      emptyToNull(search),
      from,
      to,
      sorted
    );
    Map<UUID, String> customerNames = resolveCustomerNames(companyId, page.getContent());
    return page.map(sale -> mapToSummary(sale, customerNames));
  }

  @Transactional(readOnly = true)
  public SaleDetail detail(UUID id) {
    UUID companyId = companyContext.require();
    Sale sale = sales.findByIdAndCompanyId(id, companyId)
      .orElseThrow(() -> new IllegalStateException("Sale not found: " + id));

    List<SaleItem> saleItems = items.findBySaleId(sale.getId());
    Map<UUID, Product> productIndex = resolveProducts(companyId, saleItems);
    List<SaleDetailLine> lines = saleItems.stream()
      .map(item -> toDetailLine(item, productIndex))
      .toList();

    String customerName = resolveCustomerName(companyId, sale.getCustomerId());
    SaleDetailCustomer customerDto = sale.getCustomerId() == null
      ? null
      : new SaleDetailCustomer(sale.getCustomerId(), customerName);

    String companyName = companies.findById(companyId)
      .map(com.datakomerz.pymes.company.Company::getName)
      .orElse("PyMEs Suite");

    String ticket = ThermalTicketFormatter.build(companyName, sale, customerName, lines);

    return new SaleDetail(
      sale.getId(),
      sale.getIssuedAt(),
      safeDocType(sale.getDocType()),
      safePaymentMethod(sale.getPaymentMethod()),
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
    UUID companyId = companyContext.require();
    Sale sale = sales.findByIdAndCompanyId(id, companyId)
      .orElseThrow(() -> new IllegalStateException("Sale not found: " + id));

    if (req == null) {
      String customerName = resolveCustomerName(companyId, sale.getCustomerId());
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
    String customerName = resolveCustomerName(companyId, sale.getCustomerId());
    return mapToRes(sale, customerName);
  }

  @Transactional
  public SaleRes cancel(UUID id) {
    UUID companyId = companyContext.require();
    Sale sale = sales.findByIdAndCompanyId(id, companyId)
      .orElseThrow(() -> new IllegalStateException("Sale not found: " + id));

    if ("cancelled".equalsIgnoreCase(sale.getStatus())) {
      String customerName = resolveCustomerName(companyId, sale.getCustomerId());
      return mapToRes(sale, customerName);
    }

    inventory.restockSale(sale.getId());
    sale.setStatus("cancelled");
    sales.save(sale);
    String customerName = resolveCustomerName(companyId, sale.getCustomerId());
    return mapToRes(sale, customerName);
  }

  @Transactional(readOnly = true)
  public List<SalesDailyPoint> dailyMetrics(int days) {
    UUID companyId = companyContext.require();
    OffsetDateTime from = OffsetDateTime.now().minusDays(days);
    List<Sale> range = sales.findByCompanyIdAndIssuedAtGreaterThanEqualOrderByIssuedAtAsc(companyId, from);

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

  private String resolveCustomerName(UUID companyId, UUID customerId) {
    if (customerId == null) {
      return null;
    }
    Optional<Customer> customer = customers.findById(customerId);
    return customer.filter(c -> companyId.equals(c.getCompanyId()))
      .map(Customer::getName)
      .orElse(null);
  }

  private Map<UUID, String> resolveCustomerNames(UUID companyId, Collection<Sale> saleCollection) {
    Set<UUID> ids = saleCollection.stream()
      .map(Sale::getCustomerId)
      .filter(Objects::nonNull)
      .collect(Collectors.toCollection(HashSet::new));
    if (ids.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<UUID, String> map = new HashMap<>();
    customers.findAllById(ids).forEach(customer -> {
      if (companyId.equals(customer.getCompanyId())) {
        map.put(customer.getId(), customer.getName());
      }
    });
    return map;
  }

  private Map<UUID, Product> resolveProducts(UUID companyId, List<SaleItem> saleItems) {
    Set<UUID> productIds = saleItems.stream()
      .map(SaleItem::getProductId)
      .filter(Objects::nonNull)
      .collect(Collectors.toCollection(HashSet::new));
    if (productIds.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<UUID, Product> productMap = new HashMap<>();
    products.findAllById(productIds).forEach(product -> {
      if (companyId.equals(product.getCompanyId())) {
        productMap.put(product.getId(), product);
      }
    });
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
}

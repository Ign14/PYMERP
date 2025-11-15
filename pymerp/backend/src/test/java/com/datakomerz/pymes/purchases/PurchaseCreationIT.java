package com.datakomerz.pymes.purchases;

import static org.assertj.core.api.Assertions.assertThat;

import com.datakomerz.pymes.company.Company;
import com.datakomerz.pymes.company.CompanyRepository;
import com.datakomerz.pymes.inventory.InventoryLot;
import com.datakomerz.pymes.inventory.InventoryLotRepository;
import com.datakomerz.pymes.inventory.InventoryMovementRepository;
import com.datakomerz.pymes.multitenancy.TenantContext;
import com.datakomerz.pymes.products.Product;
import com.datakomerz.pymes.products.ProductRepository;
import com.datakomerz.pymes.purchases.dto.PurchaseCreationResult;
import com.datakomerz.pymes.purchases.dto.PurchaseItemReq;
import com.datakomerz.pymes.purchases.dto.PurchaseReq;
import com.datakomerz.pymes.services.Service;
import com.datakomerz.pymes.services.ServiceRepository;
import com.datakomerz.pymes.suppliers.Supplier;
import com.datakomerz.pymes.suppliers.SupplierRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class PurchaseCreationIT {

  @Autowired
  private PurchaseService purchaseService;

  @Autowired
  private PurchaseRepository purchaseRepository;

  @Autowired
  private PurchaseItemRepository purchaseItemRepository;

  @Autowired
  private SupplierRepository supplierRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private ServiceRepository serviceRepository;

  @Autowired
  private CompanyRepository companyRepository;

  @Autowired
  private InventoryLotRepository inventoryLotRepository;

  @Autowired
  private InventoryMovementRepository inventoryMovementRepository;

  @MockBean
  private StringRedisTemplate redisTemplate;

  private UUID companyId;

  @BeforeEach
  void setUp() {
    inventoryMovementRepository.deleteAll();
    inventoryLotRepository.deleteAll();
    purchaseItemRepository.deleteAll();
    purchaseRepository.deleteAll();
    serviceRepository.deleteAll();
    productRepository.deleteAll();
    supplierRepository.deleteAll();
    companyRepository.deleteAll();
    companyId = UUID.randomUUID();
    registerCompany(companyId);
    TenantContext.setTenantId(companyId);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("test-user", "test", Collections.emptyList()));
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldCreatePurchaseWithMultipleProductItems() {
    Supplier supplier = createSupplier("Proveedor múltiples");
    Product productA = createProduct("SKU-MULTI-1", "Producto 1");
    Product productB = createProduct("SKU-MULTI-2", "Producto 2");

    PurchaseReq request = new PurchaseReq(
        supplier.getId(),
        "FACTURA",
        "F-12345",
        new BigDecimal("9500"),
        new BigDecimal("1805"),
        new BigDecimal("11305"),
        null,
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        30,
        null,
        List.of(
            new PurchaseItemReq(
                productA.getId(),
                null,
                new BigDecimal("5"),
                new BigDecimal("1000"),
                new BigDecimal("0.19"),
                null,
                null,
                null
            ),
            new PurchaseItemReq(
                productB.getId(),
                null,
                new BigDecimal("3"),
                new BigDecimal("1500"),
                new BigDecimal("0.19"),
                null,
                null,
                null
            )
        ),
        null
    );

    PurchaseCreationResult result = purchaseService.create(request);

    assertThat(result.itemsCreated()).isEqualTo(2);
    assertThat(result.lotsCreated()).isEqualTo(2);

    UUID purchaseId = result.id();
    assertThat(purchaseRepository.findById(purchaseId)).isPresent();
    assertThat(purchaseItemRepository.findByPurchaseId(purchaseId)).hasSize(2);

    List<InventoryLot> lots = inventoryLotRepository.findAll().stream()
        .filter(lot -> purchaseId.equals(lot.getPurchaseId()))
        .toList();
    assertThat(lots).hasSize(2);
    assertThat(inventoryMovementRepository.findByRefTypeAndRefId("PURCHASE", purchaseId)).hasSize(2);
  }

  @Test
  void shouldCreatePurchaseWithServiceItemWithoutLots() {
    Supplier supplier = createSupplier("Proveedor servicio");
    Service service = createService("SERV-001", "Servicio soporte");

    PurchaseReq request = new PurchaseReq(
        supplier.getId(),
        "FACTURA",
        "F-98765",
        new BigDecimal("10000"),
        new BigDecimal("1900"),
        new BigDecimal("11900"),
        null,
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        30,
        null,
        List.of(
            new PurchaseItemReq(
                null,
                service.getId(),
                new BigDecimal("2"),
                new BigDecimal("5000"),
                new BigDecimal("0.19"),
                null,
                null,
                null
            )
        ),
        null
    );

    PurchaseCreationResult result = purchaseService.create(request);

    assertThat(result.itemsCreated()).isEqualTo(1);
    assertThat(result.lotsCreated()).isEqualTo(0);

    UUID purchaseId = result.id();
    assertThat(purchaseItemRepository.findByPurchaseId(purchaseId)).hasSize(1);
    List<InventoryLot> lots = inventoryLotRepository.findAll().stream()
        .filter(lot -> purchaseId.equals(lot.getPurchaseId()))
        .toList();
    assertThat(lots).isEmpty();
    assertThat(inventoryMovementRepository.findByRefTypeAndRefId("PURCHASE", purchaseId)).isEmpty();
  }

  private void registerCompany(UUID tenant) {
    Company company = new Company();
    company.setId(tenant);
    company.setBusinessName("Tenant " + tenant);
    company.setRut("76" + tenant.toString().substring(0, 7));
    companyRepository.save(company);
  }

  private Supplier createSupplier(String name) {
    Supplier supplier = new Supplier();
    supplier.setName(name);
    supplier.setRut(UUID.randomUUID().toString().substring(0, 8));
    supplier.setCompanyId(companyId);
    return supplierRepository.save(supplier);
  }

  private Product createProduct(String sku, String name) {
    Product product = new Product();
    product.setCompanyId(companyId);
    product.setSku(sku);
    product.setName(name);
    product.setActive(true);
    product.setCriticalStock(BigDecimal.ONE);
    return productRepository.save(product);
  }

  private Service createService(String code, String name) {
    Service service = new Service();
    service.setCompanyId(companyId);
    service.setCode(code);
    service.setName(name);
    service.setUnitPrice(new BigDecimal("5000"));
    service.setStatus(com.datakomerz.pymes.services.ServiceStatus.ACTIVE);
    return serviceRepository.save(service);
  }
  @TestConfiguration
  static class TestAuditConfig {
    @Bean("utcDateTimeProvider")
    DateTimeProvider utcDateTimeProviderOverride() {
      return () -> Optional.of(OffsetDateTime.now(ZoneOffset.UTC));
    }
  }
}

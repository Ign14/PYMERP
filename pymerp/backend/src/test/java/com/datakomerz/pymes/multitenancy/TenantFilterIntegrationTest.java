package com.datakomerz.pymes.multitenancy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.datakomerz.pymes.config.TestJwtDecoderConfig;
import com.datakomerz.pymes.products.Product;
import com.datakomerz.pymes.products.ProductRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests that verify the automatic tenant filtering applied at the
 * repository layer.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
@Transactional
class TenantFilterIntegrationTest {

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private TenantFilterEnabler filterEnabler;

  private UUID tenant1;
  private UUID tenant2;
  private Product product1Tenant1;
  private Product product2Tenant1;
  private Product product1Tenant2;

  @BeforeEach
  void setUp() {
    filterEnabler.disableTenantFilter(entityManager);
    tenant1 = UUID.randomUUID();
    tenant2 = UUID.randomUUID();

    product1Tenant1 = createProduct("Product 1 - Tenant 1", "SKU-T1-001", tenant1);
    product2Tenant1 = createProduct("Product 2 - Tenant 1", "SKU-T1-002", tenant1);
    product1Tenant2 = createProduct("Product 1 - Tenant 2", "SKU-T2-001", tenant2);

    productRepository.save(product1Tenant1);
    productRepository.save(product2Tenant1);
    productRepository.save(product1Tenant2);

    entityManager.flush();
    entityManager.clear();
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void shouldFilterProductsByTenant1() {
    TenantContext.setTenantId(tenant1);
    assertTrue(filterEnabler.enableTenantFilter(entityManager), "Filter should enable for tenant1");

    List<Product> products = productRepository.findAll();

    assertEquals(2, products.size(), "Should return only 2 products for tenant1");
    assertTrue(products.stream().allMatch(p -> p.getCompanyId().equals(tenant1)),
        "All products should belong to tenant1");
  }

  @Test
  void shouldFilterProductsByTenant2() {
    TenantContext.setTenantId(tenant2);
    assertTrue(filterEnabler.enableTenantFilter(entityManager), "Filter should enable for tenant2");

    List<Product> products = productRepository.findAll();

    assertEquals(1, products.size(), "Should return only 1 product for tenant2");
    assertEquals(tenant2, products.get(0).getCompanyId(), "Product should belong to tenant2");
  }

  @Test
  void shouldNotSeeOtherTenantsProducts() {
    TenantContext.setTenantId(tenant1);
    assertTrue(filterEnabler.enableTenantFilter(entityManager), "Filter should enable for tenant1");

    List<Product> products = productRepository.findAll();

    assertFalse(products.stream().anyMatch(p -> p.getId().equals(product1Tenant2.getId())),
        "Should NOT see products from tenant2");
  }

  @Test
  void shouldReturnEmptyWhenNoProductsForTenant() {
    UUID emptyTenant = UUID.randomUUID();
    TenantContext.setTenantId(emptyTenant);
    assertTrue(filterEnabler.enableTenantFilter(entityManager), "Filter should enable for empty tenant");

    List<Product> products = productRepository.findAll();

    assertTrue(products.isEmpty(), "Should return empty list for tenant without products");
  }

  @Test
  void shouldFindByIdOnlyIfBelongsToCurrentTenant() {
    TenantContext.setTenantId(tenant1);
    assertTrue(filterEnabler.enableTenantFilter(entityManager), "Filter should enable for tenant1");

    var found1 = productRepository.findById(product1Tenant1.getId());
    assertTrue(found1.isPresent(), "Should find product from same tenant");

    var found2 = productRepository.findById(product1Tenant2.getId());
    assertFalse(found2.isPresent(), "Should NOT find product from different tenant");
  }

  private Product createProduct(String name, String sku, UUID companyId) {
    Product product = new Product();
    product.setId(UUID.randomUUID());
    product.setCompanyId(companyId);
    product.setSku(sku);
    product.setName(name);
    product.setActive(true);
    product.setCriticalStock(BigDecimal.ZERO);
    product.setCreatedAt(OffsetDateTime.now());
    return product;
  }
}

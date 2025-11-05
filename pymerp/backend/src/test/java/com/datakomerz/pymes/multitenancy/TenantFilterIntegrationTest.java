package com.datakomerz.pymes.multitenancy;

import com.datakomerz.pymes.products.Product;
import com.datakomerz.pymes.products.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de integración para verificar el filtrado automático por tenant.
 * 
 * Este test verifica que:
 * 1. El filtro de tenant se aplica automáticamente
 * 2. Las queries solo retornan datos del tenant actual
 * 3. No se pueden ver datos de otros tenants
 * 
 * TODO: Habilitar después de configurar JwtDecoder en tests
 */
@Disabled("Temporalmente deshabilitado - requiere configuración de JwtDecoder en test context")
@SpringBootTest
@ActiveProfiles("test")
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
    // Crear dos tenants diferentes
    tenant1 = UUID.randomUUID();
    tenant2 = UUID.randomUUID();

    // Crear productos para tenant1
    product1Tenant1 = createProduct("Product 1 - Tenant 1", "SKU-T1-001", tenant1);
    product2Tenant1 = createProduct("Product 2 - Tenant 1", "SKU-T1-002", tenant1);
    
    // Crear producto para tenant2
    product1Tenant2 = createProduct("Product 1 - Tenant 2", "SKU-T2-001", tenant2);

    // Guardar sin filtro (deshabilitado para setup)
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
    // Given
    TenantContext.setTenantId(tenant1);
    filterEnabler.enableTenantFilter(entityManager);

    // When
    List<Product> products = productRepository.findAll();

    // Then
    assertEquals(2, products.size(), "Should return only 2 products for tenant1");
    assertTrue(products.stream().allMatch(p -> p.getCompanyId().equals(tenant1)),
        "All products should belong to tenant1");
  }

  @Test
  void shouldFilterProductsByTenant2() {
    // Given
    TenantContext.setTenantId(tenant2);
    filterEnabler.enableTenantFilter(entityManager);

    // When
    List<Product> products = productRepository.findAll();

    // Then
    assertEquals(1, products.size(), "Should return only 1 product for tenant2");
    assertEquals(tenant2, products.get(0).getCompanyId(),
        "Product should belong to tenant2");
  }

  @Test
  void shouldNotSeeOtherTenantsProducts() {
    // Given - establecer contexto para tenant1
    TenantContext.setTenantId(tenant1);
    filterEnabler.enableTenantFilter(entityManager);

    // When - buscar producto que pertenece a tenant2
    List<Product> products = productRepository.findAll();

    // Then - no debe aparecer el producto de tenant2
    assertFalse(products.stream().anyMatch(p -> p.getId().equals(product1Tenant2.getId())),
        "Should NOT see products from tenant2");
  }

  @Test
  void shouldReturnEmptyWhenNoProductsForTenant() {
    // Given - tenant nuevo sin productos
    UUID emptyTenant = UUID.randomUUID();
    TenantContext.setTenantId(emptyTenant);
    filterEnabler.enableTenantFilter(entityManager);

    // When
    List<Product> products = productRepository.findAll();

    // Then
    assertTrue(products.isEmpty(), "Should return empty list for tenant without products");
  }

  @Test
  void shouldFindByIdOnlyIfBelongsToCurrentTenant() {
    // Given
    TenantContext.setTenantId(tenant1);
    filterEnabler.enableTenantFilter(entityManager);

    // When - buscar producto de tenant1
    var found1 = productRepository.findById(product1Tenant1.getId());
    
    // Then
    assertTrue(found1.isPresent(), "Should find product from same tenant");

    // When - buscar producto de tenant2 con contexto de tenant1
    var found2 = productRepository.findById(product1Tenant2.getId());
    
    // Then
    assertFalse(found2.isPresent(), "Should NOT find product from different tenant");
  }

  /**
   * Helper para crear un producto de prueba.
   */
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

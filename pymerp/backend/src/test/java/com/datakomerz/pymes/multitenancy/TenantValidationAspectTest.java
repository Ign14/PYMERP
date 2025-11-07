package com.datakomerz.pymes.multitenancy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.datakomerz.pymes.products.Product;
import com.datakomerz.pymes.products.ProductRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import({TenantValidationAspectTest.GuardedService.class, com.datakomerz.pymes.config.TestJwtDecoderConfig.class})
class TenantValidationAspectTest {

  @Autowired
  private GuardedService guardedService;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private EntityManager entityManager;

  private UUID tenantA;
  private UUID tenantB;
  private UUID productAId;
  private UUID productBId;

  @BeforeEach
  void setUp() {
    tenantA = UUID.randomUUID();
    tenantB = UUID.randomUUID();

    productAId = persistProduct("Tenant A Product", "SKU-A-1", tenantA);
    productBId = persistProduct("Tenant B Product", "SKU-B-1", tenantB);

    entityManager.flush();
    entityManager.clear();
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void shouldAllowAccessWhenEntityBelongsToCurrentTenant() {
    TenantContext.setTenantId(tenantA);

    assertDoesNotThrow(() -> guardedService.verifyOwnership(productAId));
  }

  @Test
  void shouldRejectCrossTenantAccess() {
    TenantContext.setTenantId(tenantA);

    assertThrows(CrossTenantAccessException.class,
        () -> guardedService.verifyOwnership(productBId));
  }

  @Test
  void shouldThrowWhenEntityDoesNotExist() {
    TenantContext.setTenantId(tenantA);
    UUID missing = UUID.randomUUID();

    assertThrows(jakarta.persistence.EntityNotFoundException.class,
        () -> guardedService.verifyOwnership(missing));
  }

  private UUID persistProduct(String name, String sku, UUID companyId) {
    Product product = new Product();
    product.setCompanyId(companyId);
    product.setSku(sku);
    product.setName(name);
    product.setActive(true);
    product.setCriticalStock(BigDecimal.ZERO);
    product.setCreatedAt(OffsetDateTime.now());
    product.setUpdatedAt(OffsetDateTime.now());
    product.setDescription("Test product");
    product.setCategory("TEST");
    Product saved = productRepository.save(product);
    return saved.getId();
  }

  @Service
  static class GuardedService {

    @ValidateTenant(entityClass = Product.class, entityParam = "productId", entityParamIndex = 0)
    @Transactional(readOnly = true)
    public void verifyOwnership(UUID productId) {
      // Intentionally left blank - validation happens in the aspect
    }
  }
}

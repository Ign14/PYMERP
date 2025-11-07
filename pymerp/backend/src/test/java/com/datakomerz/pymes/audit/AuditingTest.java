package com.datakomerz.pymes.audit;

import static org.junit.jupiter.api.Assertions.*;

import com.datakomerz.pymes.products.Product;
import com.datakomerz.pymes.products.ProductRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AuditingTest {

  @Autowired
  private ProductRepository productRepository;

  @Test
  @WithMockUser(username = "testuser@pymerp.cl")
  void shouldPopulateCreatedByOnInsert() {
    Product product = new Product();
    product.setCompanyId(UUID.randomUUID());
    product.setSku("TEST-001");
    product.setName("Test Product");
    product.setActive(true);
    product.setCriticalStock(BigDecimal.TEN);

    Product saved = productRepository.saveAndFlush(product);

    assertNotNull(saved.getCreatedAt(), "createdAt debe ser poblado automáticamente");
    assertNotNull(saved.getUpdatedAt(), "updatedAt debe ser poblado automáticamente");
    assertEquals("testuser@pymerp.cl", saved.getCreatedBy(), "createdBy debe ser el usuario actual");
    assertEquals("testuser@pymerp.cl", saved.getUpdatedBy(), "updatedBy debe ser el usuario actual");
  }

  @Test
  @WithMockUser(username = "modifier@pymerp.cl")
  void shouldUpdateModifiedByOnUpdate() {
    Product product = new Product();
    product.setCompanyId(UUID.randomUUID());
    product.setSku("TEST-002");
    product.setName("Original Name");
    product.setActive(true);
    product.setCriticalStock(BigDecimal.ONE);

    Product saved = productRepository.saveAndFlush(product);
    String originalCreatedBy = saved.getCreatedBy();

    saved.setName("Modified Name");
    Product updated = productRepository.saveAndFlush(saved);

    assertEquals(originalCreatedBy, updated.getCreatedBy(), "createdBy NO debe cambiar");
    assertEquals("modifier@pymerp.cl", updated.getUpdatedBy(), "updatedBy debe ser el nuevo usuario");
    assertTrue(
        updated.getUpdatedAt().isAfter(updated.getCreatedAt()),
        "updatedAt debe ser posterior a createdAt");
  }

  @Test
  void shouldUseSystemWhenNoAuthentication() {
    Product product = new Product();
    product.setCompanyId(UUID.randomUUID());
    product.setSku("TEST-003");
    product.setName("System Product");
    product.setActive(true);
    product.setCriticalStock(BigDecimal.valueOf(2));

    Product saved = productRepository.saveAndFlush(product);

    assertEquals("system", saved.getCreatedBy(), "Debe usar 'system' sin autenticación");
    assertEquals("system", saved.getUpdatedBy(), "Debe usar 'system' sin autenticación");
  }
}

package com.datakomerz.pymes.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.datakomerz.pymes.multitenancy.TenantContext;
import com.datakomerz.pymes.products.Product;
import com.datakomerz.pymes.products.ProductService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
  "spring.autoconfigure.exclude=",
  "app.cache.redis.enabled=true"
})
@Testcontainers
class ProductCacheTest {

  @Container
  static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
    .withExposedPorts(6379);

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", redis::getFirstMappedPort);
  }

  @Autowired
  private ProductService productService;

  @Autowired
  private CacheManager cacheManager;

  private UUID companyId;
  private Product savedProduct;

  @BeforeEach
  void setUp() {
    companyId = UUID.randomUUID();
    TenantContext.setTenantId(companyId);
    Product product = new Product();
    product.setCompanyId(companyId);
    product.setSku("SKU-" + UUID.randomUUID().toString().substring(0, 8));
    product.setName("Redis cached product");
    product.setDescription("Test");
    product.setCriticalStock(BigDecimal.ONE);
    product.setActive(true);
    savedProduct = productService.save(product);
  }

  @AfterEach
  void tearDown() {
    Cache cache = cacheManager.getCache("products");
    if (cache != null) {
      cache.clear();
    }
    TenantContext.clear();
  }

  @Test
  void shouldCacheProductOnSecondRead() {
    productService.findById(companyId, savedProduct.getId());
    productService.findById(companyId, savedProduct.getId());

    Cache cache = cacheManager.getCache("products");
    assertNotNull(cache);
    assertNotNull(cache.get(companyId + ":" + savedProduct.getId()));
  }

  @Test
  void shouldEvictCacheOnUpdate() {
    productService.findById(companyId, savedProduct.getId());
    savedProduct.setName("Updated name");
    productService.save(savedProduct);

    Cache cache = cacheManager.getCache("products");
    assertNotNull(cache);
    assertNull(cache.get(companyId + ":" + savedProduct.getId()));
  }

  @Test
  void shouldCacheProductListByPage() {
    productService.findAll(companyId, PageRequest.of(0, 20));
    productService.findAll(companyId, PageRequest.of(0, 20));

    Cache cache = cacheManager.getCache("products");
    assertNotNull(cache);
    assertNotNull(cache.get(companyId + ":all:0"));
  }

  @TestConfiguration
  static class RedisTestConfig {
    @Bean
    RedisConnectionFactory redisConnectionFactory() {
      LettuceConnectionFactory factory = new LettuceConnectionFactory(redis.getHost(), redis.getFirstMappedPort());
      factory.afterPropertiesSet();
      return factory;
    }

  }
}

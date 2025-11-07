package com.datakomerz.pymes.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.datakomerz.pymes.multitenancy.TenantContext;
import com.datakomerz.pymes.suppliers.Supplier;
import com.datakomerz.pymes.suppliers.SupplierService;
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
class SupplierCacheTest {

  @Container
  static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
    .withExposedPorts(6379);

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", redis::getFirstMappedPort);
  }

  @Autowired
  private SupplierService supplierService;

  @Autowired
  private CacheManager cacheManager;

  private UUID companyId;
  private Supplier supplier;

  @BeforeEach
  void setUp() {
    companyId = UUID.randomUUID();
    TenantContext.setTenantId(companyId);
    supplier = new Supplier();
    supplier.setCompanyId(companyId);
    supplier.setName("Supplier cached");
    supplier.setRut("76.543.210-1");
    supplier.setEmail("supplier@example.com");
    supplier.setActive(true);
    supplierService.saveSupplier(supplier);
  }

  @AfterEach
  void tearDown() {
    Cache cache = cacheManager.getCache("suppliers");
    if (cache != null) {
      cache.clear();
    }
    TenantContext.clear();
  }

  @Test
  void shouldCacheSuppliersById() {
    supplierService.findSupplier(companyId, supplier.getId());
    supplierService.findSupplier(companyId, supplier.getId());

    Cache cache = cacheManager.getCache("suppliers");
    assertNotNull(cache);
    assertNotNull(cache.get(companyId + ":" + supplier.getId()));
  }

  @Test
  void shouldCacheSuppliersListByPage() {
    supplierService.findAll(companyId, null, null, PageRequest.of(0, 10));
    supplierService.findAll(companyId, null, null, PageRequest.of(0, 10));

    Cache cache = cacheManager.getCache("suppliers");
    assertNotNull(cache);
    assertNotNull(cache.get(companyId + ":all:0"));
  }

  @Test
  void shouldEvictSupplierCachesOnDelete() {
    supplierService.findSupplier(companyId, supplier.getId());
    supplierService.findAll(companyId, null, null, PageRequest.of(0, 10));
    Cache cache = cacheManager.getCache("suppliers");
    assertNotNull(cache);
    assertNotNull(cache.get(companyId + ":" + supplier.getId()));
    assertNotNull(cache.get(companyId + ":all:0"));

    supplierService.deleteSupplier(companyId, supplier.getId());

    assertNull(cache.get(companyId + ":" + supplier.getId()));
    assertNull(cache.get(companyId + ":all:0"));
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

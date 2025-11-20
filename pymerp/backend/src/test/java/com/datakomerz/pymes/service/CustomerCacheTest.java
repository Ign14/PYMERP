package com.datakomerz.pymes.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.datakomerz.pymes.customers.Customer;
import com.datakomerz.pymes.customers.CustomerService;
import com.datakomerz.pymes.customers.dto.CustomerRequest;
import com.datakomerz.pymes.multitenancy.TenantContext;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
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

@SpringBootTest(properties = {
  "spring.autoconfigure.exclude=",
  "app.cache.redis.enabled=true"
})
class CustomerCacheTest {

  private static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
      .withExposedPorts(6379);

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    ensureRedisStarted();
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", redis::getFirstMappedPort);
  }

  @AfterAll
  static void shutdownRedis() {
    if (redis.isRunning()) {
      redis.stop();
    }
  }

  @Autowired
  private CustomerService customerService;

  @Autowired
  private CacheManager cacheManager;

  private UUID companyId;
  private Customer customer;

  @BeforeEach
  void setUp() {
    companyId = UUID.randomUUID();
    TenantContext.setTenantId(companyId);
    customer = customerService.create(request("Cached Customer"));
  }

  @AfterEach
  void tearDown() {
    Cache cache = cacheManager.getCache("customers");
    if (cache != null) {
      cache.clear();
    }
    TenantContext.clear();
  }

  @Test
  void shouldCacheCustomerOnSecondRead() {
    customerService.get(customer.getId());
    customerService.get(customer.getId());

    Cache cache = cacheManager.getCache("customers");
    assertNotNull(cache);
    assertNotNull(cache.get(companyId + ":" + customer.getId()));
  }

  @Test
  void shouldEvictCustomerCacheOnUpdate() {
    customerService.get(customer.getId());
    customerService.update(customer.getId(), request("Updated Customer"));

    Cache cache = cacheManager.getCache("customers");
    assertNotNull(cache);
    assertNull(cache.get(companyId + ":" + customer.getId()));
  }

  @Test
  void shouldEvictListCacheOnCreate() {
    customerService.findAll(PageRequest.of(0, 20));
    customerService.findAll(PageRequest.of(0, 20));
    Cache cache = cacheManager.getCache("customers");
    assertNotNull(cache);
    assertNotNull(cache.get(companyId + ":all:0"));

    customerService.create(request("Another Customer"));

    assertNull(cache.get(companyId + ":all:0"));
  }

  private CustomerRequest request(String name) {
    return new CustomerRequest(
      name,
      "",
      "Test address",
      BigDecimal.ZERO,
      BigDecimal.ZERO,
      "+56912345678",
      "demo@example.com",
      null,
      null,
      null,
      Boolean.TRUE
    );
  }

  @TestConfiguration
  static class RedisTestConfig {
    @Bean
    RedisConnectionFactory redisConnectionFactory() {
      ensureRedisStarted();
      LettuceConnectionFactory factory = new LettuceConnectionFactory(redis.getHost(), redis.getFirstMappedPort());
      factory.afterPropertiesSet();
      return factory;
    }
  }

  private static void ensureRedisStarted() {
    if (!redis.isRunning()) {
      redis.start();
    }
  }
}

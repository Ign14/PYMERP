package com.datakomerz.pymes.integration;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class AbstractIT {

  protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
      .withDatabaseName("pymerp_test")
      .withUsername("test")
      .withPassword("test");

  protected static final RedisContainer REDIS = new RedisContainer("redis:8.2-alpine");

  private static final Object CONTAINER_LOCK = new Object();
  private static int activeSuites = 0;

  @BeforeAll
  static void startContainers() {
    synchronized (CONTAINER_LOCK) {
      if (activeSuites == 0) {
        ensureRunning();
      }
      activeSuites++;
    }
  }

  @AfterAll
  static void stopContainers() {
    synchronized (CONTAINER_LOCK) {
      if (activeSuites > 0) {
        activeSuites--;
      }
      if (activeSuites == 0) {
        if (POSTGRES.isRunning()) {
          POSTGRES.stop();
        }
        if (REDIS.isRunning()) {
          REDIS.stop();
        }
      }
    }
  }

  @DynamicPropertySource
  static void registerContainers(DynamicPropertyRegistry registry) {
    ensureRunning();
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getFirstMappedPort());
  }

  private static void ensureRunning() {
    synchronized (CONTAINER_LOCK) {
      if (!POSTGRES.isRunning()) {
        POSTGRES.start();
      }
      if (!REDIS.isRunning()) {
        REDIS.start();
      }
    }
  }
}

package com.datakomerz.pymes.multitenancy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para TenantContext.
 * 
 * Verifica el comportamiento del ThreadLocal storage para tenant IDs.
 */
class TenantContextTest {

  private UUID testTenantId;

  @BeforeEach
  void setUp() {
    testTenantId = UUID.randomUUID();
    // Asegurar contexto limpio antes de cada test
    TenantContext.clear();
  }

  @AfterEach
  void tearDown() {
    // Limpiar después de cada test
    TenantContext.clear();
  }

  @Test
  void shouldSetAndGetTenantId() {
    // When
    TenantContext.setTenantId(testTenantId);
    
    // Then
    assertEquals(testTenantId, TenantContext.getTenantId());
  }

  @Test
  void shouldReturnNullWhenNoTenantSet() {
    // Given - contexto limpio
    
    // When
    UUID result = TenantContext.getTenantId();
    
    // Then
    assertNull(result);
  }

  @Test
  void shouldThrowExceptionWhenRequireWithNoTenant() {
    // Given - contexto limpio
    
    // When & Then
    assertThrows(TenantNotFoundException.class, TenantContext::require);
  }

  @Test
  void shouldReturnTenantWhenRequireWithTenantSet() {
    // Given
    TenantContext.setTenantId(testTenantId);
    
    // When
    UUID result = TenantContext.require();
    
    // Then
    assertEquals(testTenantId, result);
  }

  @Test
  void shouldClearTenantContext() {
    // Given
    TenantContext.setTenantId(testTenantId);
    
    // When
    TenantContext.clear();
    
    // Then
    assertNull(TenantContext.getTenantId());
  }

  @Test
  void shouldReturnTrueWhenTenantIsPresent() {
    // Given
    TenantContext.setTenantId(testTenantId);
    
    // When
    boolean result = TenantContext.isPresent();
    
    // Then
    assertTrue(result);
  }

  @Test
  void shouldReturnFalseWhenTenantIsNotPresent() {
    // Given - contexto limpio
    
    // When
    boolean result = TenantContext.isPresent();
    
    // Then
    assertFalse(result);
  }

  @Test
  void shouldAllowSettingNullTenant() {
    // Given
    TenantContext.setTenantId(testTenantId);
    
    // When
    TenantContext.setTenantId(null);
    
    // Then
    assertNull(TenantContext.getTenantId());
    assertFalse(TenantContext.isPresent());
  }

  @Test
  void shouldIsolateTenantBetweenThreads() throws InterruptedException {
    // Given
    UUID mainThreadTenant = UUID.randomUUID();
    UUID otherThreadTenant = UUID.randomUUID();
    TenantContext.setTenantId(mainThreadTenant);
    
    // When - ejecutar en otro thread
    Thread otherThread = new Thread(() -> {
      TenantContext.setTenantId(otherThreadTenant);
      assertEquals(otherThreadTenant, TenantContext.getTenantId());
    });
    
    otherThread.start();
    otherThread.join();
    
    // Then - el tenant del main thread no debe cambiar
    assertEquals(mainThreadTenant, TenantContext.getTenantId());
  }
}

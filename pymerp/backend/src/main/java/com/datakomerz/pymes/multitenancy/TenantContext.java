package com.datakomerz.pymes.multitenancy;

import java.util.UUID;

/**
 * Almacena el ID del tenant (empresa) actual en el contexto del thread.
 * Utiliza ThreadLocal para aislar el contexto entre requests concurrentes.
 * 
 * <p>Este contexto es establecido por {@link TenantInterceptor} al inicio de cada request
 * y limpiado automáticamente al finalizar.</p>
 * 
 * <p><strong>Uso:</strong></p>
 * <pre>
 * // Obtener tenant actual (puede ser null)
 * UUID tenantId = TenantContext.getTenantId();
 * 
 * // Obtener tenant actual (lanza excepción si no existe)
 * UUID tenantId = TenantContext.require();
 * 
 * // Establecer tenant (normalmente lo hace TenantInterceptor)
 * TenantContext.setTenantId(tenantId);
 * 
 * // Limpiar contexto (normalmente lo hace TenantInterceptor)
 * TenantContext.clear();
 * </pre>
 * 
 * @see TenantInterceptor
 * @since Sprint 5
 */
public final class TenantContext {

  private static final ThreadLocal<UUID> currentTenant = new ThreadLocal<>();

  private TenantContext() {
    // Utility class - no instantiation
  }

  /**
   * Establece el ID del tenant actual para el thread actual.
   * 
   * @param tenantId ID del tenant (empresa), puede ser null
   */
  public static void setTenantId(UUID tenantId) {
    currentTenant.set(tenantId);
  }

  /**
   * Obtiene el ID del tenant actual para el thread actual.
   * 
   * @return ID del tenant, o null si no hay contexto establecido
   */
  public static UUID getTenantId() {
    return currentTenant.get();
  }

  /**
   * Limpia el contexto del tenant para el thread actual.
   * Debe llamarse siempre después de procesar un request para evitar memory leaks.
   */
  public static void clear() {
    currentTenant.remove();
  }

  /**
   * Obtiene el ID del tenant actual, lanzando excepción si no existe.
   * 
   * @return ID del tenant (nunca null)
   * @throws TenantNotFoundException si no hay tenant en el contexto
   */
  public static UUID require() {
    UUID tenant = currentTenant.get();
    if (tenant == null) {
      throw new TenantNotFoundException("No tenant context available");
    }
    return tenant;
  }

  /**
   * Verifica si hay un tenant establecido en el contexto actual.
   * 
   * @return true si hay tenant, false si no
   */
  public static boolean isPresent() {
    return currentTenant.get() != null;
  }
}

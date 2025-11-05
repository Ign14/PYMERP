package com.datakomerz.pymes.multitenancy;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Interceptor que extrae el tenant ID del header X-Company-Id y lo establece en TenantContext.
 * 
 * <p>Este interceptor se ejecuta ANTES que cualquier otro interceptor (order=1) para garantizar
 * que el contexto del tenant esté disponible para auditoría, validaciones, etc.</p>
 * 
 * <p><strong>Flujo de ejecución:</strong></p>
 * <ol>
 *   <li>preHandle: Extrae X-Company-Id y lo establece en TenantContext</li>
 *   <li>Controller ejecuta con contexto disponible</li>
 *   <li>afterCompletion: Limpia TenantContext (importante para evitar memory leaks)</li>
 * </ol>
 * 
 * <p><strong>Headers esperados:</strong></p>
 * <ul>
 *   <li>X-Company-Id: UUID del tenant (empresa) - OBLIGATORIO excepto en rutas públicas</li>
 * </ul>
 * 
 * <p><strong>Rutas excluidas (públicas):</strong></p>
 * <ul>
 *   <li>/api/v1/auth/** - Login, refresh token, etc.</li>
 *   <li>/actuator/** - Health checks, métricas</li>
 *   <li>/error - Páginas de error</li>
 * </ul>
 * 
 * <p><strong>Ejemplo de uso:</strong></p>
 * <pre>
 * // Request HTTP con header
 * GET /api/v1/products
 * X-Company-Id: 550e8400-e29b-41d4-a716-446655440000
 * 
 * // En el controller, el contexto ya está disponible
 * UUID tenantId = TenantContext.getTenantId(); // 550e8400-e29b-41d4-a716-446655440000
 * </pre>
 * 
 * @see TenantContext
 * @see TenantNotFoundException
 * @since Sprint 5
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(TenantInterceptor.class);
  
  /**
   * Header HTTP que debe contener el ID del tenant (empresa).
   */
  public static final String TENANT_HEADER = "X-Company-Id";

  /**
   * Extrae el tenant ID del header y lo establece en TenantContext.
   * 
   * @param request HTTP request
   * @param response HTTP response
   * @param handler handler que procesará el request
   * @return true si el request debe continuar, false para interrumpir
   * @throws TenantNotFoundException si el header está ausente o es inválido en ruta protegida
   */
  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler) {

    String path = request.getRequestURI();
    
    // Permitir rutas públicas sin tenant
    if (isPublicPath(path)) {
      logger.debug("Public path, no tenant required: {}", path);
      return true;
    }

    // Extraer tenant ID del header
    String tenantIdHeader = request.getHeader(TENANT_HEADER);
    
    if (tenantIdHeader == null || tenantIdHeader.isBlank()) {
      logger.warn("Missing {} header for path: {}", TENANT_HEADER, path);
      throw new TenantNotFoundException(
          "Header " + TENANT_HEADER + " is required for this endpoint");
    }

    // Validar formato UUID y establecer en contexto
    try {
      UUID tenantId = UUID.fromString(tenantIdHeader);
      TenantContext.setTenantId(tenantId);
      logger.debug("Tenant context set: {} for path: {}", tenantId, path);
      return true;
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid {} format: {} for path: {}", TENANT_HEADER, tenantIdHeader, path);
      throw new TenantNotFoundException(
          "Invalid " + TENANT_HEADER + " format. Expected UUID, got: " + tenantIdHeader, e);
    }
  }

  /**
   * Limpia el TenantContext después de completar el request.
   * CRÍTICO: Debe ejecutarse siempre para evitar memory leaks en ThreadLocal.
   * 
   * @param request HTTP request
   * @param response HTTP response
   * @param handler handler que procesó el request
   * @param ex excepción lanzada durante el procesamiento (puede ser null)
   */
  @Override
  public void afterCompletion(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler,
      @Nullable Exception ex) {
    
    UUID tenantId = TenantContext.getTenantId();
    if (tenantId != null) {
      logger.debug("Clearing tenant context: {}", tenantId);
      TenantContext.clear();
    }
  }

  /**
   * Determina si una ruta es pública (no requiere tenant context).
   * 
   * @param path ruta del request
   * @return true si es ruta pública, false si requiere tenant
   */
  private boolean isPublicPath(String path) {
    return path.startsWith("/api/v1/auth/") ||
           path.startsWith("/actuator/") ||
           path.startsWith("/error");
  }
}

package com.datakomerz.pymes.config;

import com.datakomerz.pymes.audit.AuditInterceptor;
import com.datakomerz.pymes.multitenancy.TenantInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de Spring MVC para registrar interceptors.
 * Solo activo cuando NO estamos en perfil "test".
 * 
 * <p><strong>Orden de ejecución:</strong></p>
 * <ol>
 *   <li>TenantInterceptor (order=1) - Establece contexto de tenant</li>
 *   <li>AuditInterceptor (order=2) - Usa contexto de tenant para auditoría</li>
 * </ol>
 */
@Configuration
@Profile("!test")
public class WebConfig implements WebMvcConfigurer {

  private final TenantInterceptor tenantInterceptor;
  private final AuditInterceptor auditInterceptor;

  public WebConfig(TenantInterceptor tenantInterceptor, AuditInterceptor auditInterceptor) {
    this.tenantInterceptor = tenantInterceptor;
    this.auditInterceptor = auditInterceptor;
  }

  @Override
  public void addInterceptors(@NonNull InterceptorRegistry registry) {
    // ORDEN IMPORTANTE: TenantInterceptor PRIMERO para establecer contexto
    registry.addInterceptor(tenantInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns(
            "/api/v1/auth/**",  // Endpoints de autenticación
            "/actuator/**",     // Health checks
            "/error"            // Páginas de error
        )
        .order(1);  // Se ejecuta PRIMERO
    
    // AuditInterceptor SEGUNDO para usar el contexto de tenant
    registry.addInterceptor(auditInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns(
            "/api/v1/auth/login",   // No auditar login (redundante con SecurityEventListener)
            "/api/v1/auth/refresh", // No auditar refresh token
            "/actuator/**"          // No auditar endpoints de actuator
        )
        .order(2);  // Se ejecuta DESPUÉS de TenantInterceptor
  }
}

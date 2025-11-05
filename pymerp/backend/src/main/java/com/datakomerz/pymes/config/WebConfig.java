package com.datakomerz.pymes.config;

import com.datakomerz.pymes.audit.AuditInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de Spring MVC para registrar interceptors.
 * Solo activo cuando NO estamos en perfil "test".
 */
@Configuration
@Profile("!test")
public class WebConfig implements WebMvcConfigurer {

  private final AuditInterceptor auditInterceptor;

  public WebConfig(AuditInterceptor auditInterceptor) {
    this.auditInterceptor = auditInterceptor;
  }

  @Override
  public void addInterceptors(@NonNull InterceptorRegistry registry) {
    registry.addInterceptor(auditInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns(
            "/api/v1/auth/login",   // No auditar login (redundante con SecurityEventListener)
            "/api/v1/auth/refresh", // No auditar refresh token
            "/actuator/**"          // No auditar endpoints de actuator
        );
  }
}

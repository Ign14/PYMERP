package com.datakomerz.pymes.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de Spring MVC para tests.
 * Sobrescribe la configuración por defecto para NO registrar interceptors en tests.
 */
@Configuration
@Profile("test")
public class TestWebConfig implements WebMvcConfigurer {

  @Override
  public void addInterceptors(@NonNull InterceptorRegistry registry) {
    // Intencionalmente vacío - no agregar interceptors en tests
    // Esto sobrescribe el WebConfig.addInterceptors() para evitar que TenantInterceptor
    // y AuditInterceptor interfieran con los tests de integración
  }
}

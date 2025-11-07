package com.datakomerz.pymes.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

  @Bean
  public OpenAPI pymesOpenAPI() {
    final String securitySchemeName = "bearerAuth";

    return new OpenAPI()
      .info(new Info()
        .title("PYMERP API")
        .description("ERP para PyMEs - Gestión de productos, clientes, ventas y compras")
        .version("v1.0.0")
        .contact(new Contact()
          .name("DataKomerz")
          .url("https://pymerp.cl")
          .email("soporte@pymerp.cl"))
        .license(new License()
          .name("Proprietary")
          .url("https://pymerp.cl/license")))
      .servers(List.of(
        new Server().url("http://localhost:8081").description("Development"),
        new Server().url("https://api.pymerp.cl").description("Production")))
      .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
      .components(new Components()
        .addSecuritySchemes(securitySchemeName,
          new SecurityScheme()
            .name(securitySchemeName)
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("JWT token obtenido desde /api/v1/auth/login")));
  }
}

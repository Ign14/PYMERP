package com.datakomerz.pymes.inventory.integration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.datakomerz.pymes.config.TestJwtDecoderConfig;
import io.restassured.RestAssured;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
class InventoryLocationIT extends AbstractInventoryApiIT {

  @LocalServerPort
  private int port;

  private UUID tenantA;
  private UUID tenantB;
  private String tokenA;
  private String tokenB;

  @BeforeEach
  void setUp() {
    truncateTables("inventory_movements", "inventory_lots", "inventory_locations", "users", "companies", "products");
    tenantA = UUID.randomUUID();
    tenantB = UUID.randomUUID();
    createCompany(tenantA, "Tenant A");
    createCompany(tenantB, "Tenant B");
    tokenA = bearerToken(tenantA, "admin-a@example.com");
    tokenB = bearerToken(tenantB, "admin-b@example.com");
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
  }

  @Test
  void locationLifecycleIncludesSoftDeleteFilters() {
    var createPayload = Map.of(
        "code", "LOC-001",
        "name", "Bodega Central",
        "description", "Ubicación principal"
    );

    UUID locationId = UUID.fromString(authenticated(tokenA, tenantA)
        .body(createPayload)
        .post("/api/v1/inventory/locations")
        .then()
        .statusCode(200)
        .body("name", equalTo("Bodega Central"))
        .extract()
        .jsonPath()
        .getString("id"));

    authenticated(tokenA, tenantA)
        .get("/api/v1/inventory/locations")
        .then()
        .statusCode(200)
        .body("content", hasSize(1))
        .body("content[0].enabled", equalTo(true));

    var updatePayload = Map.of(
        "code", "LOC-001",
        "name", "Bodega Central",
        "description", "Ubicación actualizada",
        "enabled", true
    );
    authenticated(tokenA, tenantA)
        .body(updatePayload)
        .put("/api/v1/inventory/locations/{id}", locationId)
        .then()
        .statusCode(200)
        .body("description", equalTo("Ubicación actualizada"));

    var patchPayload = Map.of(
        "description", "Deshabilitada",
        "enabled", false
    );
    authenticated(tokenA, tenantA)
        .body(patchPayload)
        .patch("/api/v1/inventory/locations/{id}", locationId)
        .then()
        .statusCode(200)
        .body("enabled", equalTo(false));

    authenticated(tokenA, tenantA)
        .get("/api/v1/inventory/locations")
        .then()
        .statusCode(200)
        .body("content", hasSize(0));

    authenticated(tokenA, tenantA)
        .queryParam("enabled", false)
        .get("/api/v1/inventory/locations")
        .then()
        .statusCode(200)
        .body("content", hasSize(1));

    authenticated(tokenA, tenantA)
        .delete("/api/v1/inventory/locations/{id}", locationId)
        .then()
        .statusCode(204);

    authenticated(tokenA, tenantA)
        .queryParam("enabled", false)
        .get("/api/v1/inventory/locations")
        .then()
        .statusCode(200)
        .body("content[0].id", equalTo(locationId.toString()))
        .body("content[0].enabled", equalTo(false));
  }

  @Test
  void nameUniquenessIsEnforcedPerTenant() {
    var basePayload = Map.of(
        "code", "LOC-002",
        "name", "Bodega Repetida",
        "description", "Original"
    );
    authenticated(tokenA, tenantA)
        .body(basePayload)
        .post("/api/v1/inventory/locations")
        .then()
        .statusCode(200);

    authenticated(tokenA, tenantA)
        .body(Map.of("code", "LOC-003", "name", "Bodega Repetida"))
        .post("/api/v1/inventory/locations")
        .then()
        .statusCode(400);
  }

  @Test
  void crossTenantUpdatesAreForbidden() {
    UUID otherLocation = UUID.fromString(authenticated(tokenB, tenantB)
        .body(Map.of("name", "Bodega Otro", "code", "LOC-X"))
        .post("/api/v1/inventory/locations")
        .then()
        .statusCode(200)
        .extract()
        .jsonPath()
        .getString("id"));

    authenticated(tokenA, tenantA)
        .body(Map.of("name", "Bodega Robo", "code", "LOC-Z"))
        .put("/api/v1/inventory/locations/{id}", otherLocation)
        .then()
        .statusCode(403);
  }
}

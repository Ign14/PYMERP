package com.datakomerz.pymes.inventory;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

import com.datakomerz.pymes.auth.UserAccount;
import com.datakomerz.pymes.auth.UserAccountRepository;
import com.datakomerz.pymes.company.Company;
import com.datakomerz.pymes.company.CompanyRepository;
import com.datakomerz.pymes.integration.AbstractIT;
import com.datakomerz.pymes.multitenancy.TenantContext;
import com.datakomerz.pymes.multitenancy.TenantInterceptor;
import com.datakomerz.pymes.products.Product;
import com.datakomerz.pymes.products.ProductRepository;
import com.datakomerz.pymes.security.AppUserDetails;
import com.datakomerz.pymes.security.jwt.JwtService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InventoryLocationIT extends AbstractIT {

  private static final UUID COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @LocalServerPort
  private int port;

  @Autowired
  private CompanyRepository companyRepository;

  @Autowired
  private UserAccountRepository userAccountRepository;

  @Autowired
  private JwtService jwtService;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private InventoryLotRepository inventoryLotRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private String adminToken;

  @BeforeEach
  void setup() {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
    truncateTables("inventory_movements", "inventory_lots", "inventory_locations", "purchases", "suppliers", "products", "users", "companies");
    bootstrapCompany();
    adminToken = bearerToken();
  }

  @Test
  void shouldCreateLocationAndAssignToLot() {
    UUID locationId = createLocation("LOC-001", "Almacén Principal");
    UUID productId = createTestProduct(new BigDecimal("100"));
    UUID lotId = createTestLot(productId, new BigDecimal("25"));

    givenAuthenticated()
        .when()
        .put("/api/v1/inventory/lots/{lotId}/location/{locationId}", lotId, locationId)
        .then()
        .statusCode(200)
        .body("locationId", equalTo(locationId.toString()));

    givenAuthenticated()
        .queryParam("lotId", lotId)
        .queryParam("type", "TRANSFER")
        .when()
        .get("/api/v1/inventory/movements")
        .then()
        .statusCode(200)
        .body("content.size()", greaterThan(0))
        .body("content[0].type", equalTo("TRANSFER"))
        .body("content[0].locationTo.id", equalTo(locationId.toString()));
  }

  @Test
  void shouldGetStockByLocation() {
    UUID locationA = createLocation("LOC-A", "Ubicación A");
    UUID locationB = createLocation("LOC-B", "Ubicación B");
    UUID productId = createTestProduct(new BigDecimal("50"));

    createTestLotWithLocation(productId, locationA, new BigDecimal("100"));
    createTestLotWithLocation(productId, locationA, new BigDecimal("50"));
    createTestLotWithLocation(productId, locationB, new BigDecimal("75"));

    givenAuthenticated()
        .when()
        .get("/api/v1/inventory/products/{productId}/stock-by-location", productId)
        .then()
        .statusCode(200)
        .body("$", hasSize(2))
        .body("find { it.locationId == '" + locationA + "' }.totalQuantity", equalTo(150.0f))
        .body("find { it.locationId == '" + locationB + "' }.totalQuantity", equalTo(75.0f));
  }

  @Test
  void shouldNotDeleteLocationWithAssignedLots() {
    UUID locationId = createLocation("LOC-DEL", "Ubicación con lotes");
    UUID productId = createTestProduct(new BigDecimal("30"));
    createTestLotWithLocation(productId, locationId, BigDecimal.TEN);

    givenAuthenticated()
        .when()
        .delete("/api/v1/inventory/locations/{id}", locationId)
        .then()
        .statusCode(409)
        .body("message", containsString("lote(s) asignado(s)"));
  }

  @Test
  void shouldListLotsWithFilters() {
    UUID locationId = createLocation("LOC-FILTER", "Ubicación Filtros");
    UUID productId = createTestProduct(new BigDecimal("50"));
    createTestLotWithLocation(productId, locationId, new BigDecimal("20"));

    givenAuthenticated()
        .queryParam("locationId", locationId)
        .queryParam("status", "BAJO_STOCK")
        .when()
        .get("/api/v1/inventory/lots")
        .then()
        .statusCode(200)
        .body("content.size()", greaterThan(0))
        .body("content[0].status", equalTo("BAJO_STOCK"))
        .body("content[0].locationId", equalTo(locationId.toString()));
  }

  private UUID createLocation(String code, String name) {
    String id = givenAuthenticated()
        .body(Map.of(
            "code", code,
            "name", name,
            "description", "Creada en test",
            "enabled", true
        ))
        .when()
        .post("/api/v1/inventory/locations")
        .then()
        .statusCode(200)
        .extract()
        .path("id");
    return UUID.fromString(id);
  }

  private UUID createTestProduct(BigDecimal criticalStock) {
    return withTenant(() -> {
      Product product = new Product();
      product.setCompanyId(COMPANY_ID);
      product.setSku("SKU-" + System.nanoTime());
      product.setName("Producto Test");
      product.setCriticalStock(criticalStock);
      product.setActive(true);
      return productRepository.save(product).getId();
    });
  }

  private UUID createTestLot(UUID productId, BigDecimal qty) {
    return withTenant(() -> {
      InventoryLot lot = new InventoryLot();
      lot.setCompanyId(COMPANY_ID);
      lot.setProductId(productId);
      lot.setBatchName("LOT-" + System.nanoTime());
      lot.setQtyAvailable(qty);
      lot.setExpDate(LocalDate.now().plusDays(60));
      return inventoryLotRepository.save(lot).getId();
    });
  }

  private UUID createTestLotWithLocation(UUID productId, UUID locationId, BigDecimal qty) {
    return withTenant(() -> {
      InventoryLot lot = new InventoryLot();
      lot.setCompanyId(COMPANY_ID);
      lot.setProductId(productId);
      lot.setBatchName("LOT-" + System.nanoTime());
      lot.setQtyAvailable(qty);
      lot.setExpDate(LocalDate.now().plusDays(90));
      lot.setLocationId(locationId);
      return inventoryLotRepository.save(lot).getId();
    });
  }

  private void bootstrapCompany() {
    Company company = new Company();
    company.setId(COMPANY_ID);
    company.setBusinessName("Test Company");
    company.setRut("12345678-9");
    companyRepository.save(company);
  }

  private String bearerToken() {
    UserAccount account = new UserAccount();
    account.setCompanyId(COMPANY_ID);
    account.setEmail("admin@test.local");
    account.setName("Inventory Admin");
    account.setRoles("ROLE_ADMIN,ROLE_ERP_USER,ROLE_SETTINGS");
    account.setPasswordHash("test");
    userAccountRepository.save(account);
    return "Bearer " + jwtService.generateToken(new AppUserDetails(account));
  }

  private void truncateTables(String... tables) {
    for (String table : tables) {
      jdbcTemplate.execute("TRUNCATE TABLE " + table + " RESTART IDENTITY CASCADE");
    }
  }

  private RequestSpecification givenAuthenticated() {
    return RestAssured.given()
        .contentType(ContentType.JSON)
        .header(HttpHeaders.AUTHORIZATION, adminToken)
        .header(TenantInterceptor.TENANT_HEADER, COMPANY_ID.toString());
  }

  private <T> T withTenant(Supplier<T> supplier) {
    UUID previous = TenantContext.getTenantId();
    TenantContext.setTenantId(COMPANY_ID);
    try {
      return supplier.get();
    } finally {
      if (previous != null) {
        TenantContext.setTenantId(previous);
      } else {
        TenantContext.clear();
      }
    }
  }
}

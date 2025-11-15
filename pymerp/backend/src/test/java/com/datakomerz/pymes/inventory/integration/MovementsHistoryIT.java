package com.datakomerz.pymes.inventory.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.datakomerz.pymes.config.TestJwtDecoderConfig;
import com.datakomerz.pymes.inventory.InventoryLocation;
import com.datakomerz.pymes.inventory.InventoryLocationRepository;
import com.datakomerz.pymes.inventory.InventoryLot;
import com.datakomerz.pymes.inventory.InventoryLotRepository;
import com.datakomerz.pymes.inventory.InventoryMovement;
import com.datakomerz.pymes.inventory.InventoryMovementRepository;
import com.datakomerz.pymes.products.Product;
import com.datakomerz.pymes.products.ProductRepository;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
class MovementsHistoryIT extends AbstractInventoryApiIT {

  @LocalServerPort
  private int port;

  @Autowired
  private InventoryLotRepository inventoryLotRepository;

  @Autowired
  private InventoryLocationRepository inventoryLocationRepository;

  @Autowired
  private InventoryMovementRepository inventoryMovementRepository;

  @Autowired
  private ProductRepository productRepository;

  private UUID tenant;
  private UUID otherTenant;
  private String token;
  private InventoryLocation location;
  private InventoryLot lot;
  private InventoryLot otherLot;

  @BeforeEach
  void setUp() {
    truncateTables("inventory_movements", "inventory_lots", "inventory_locations", "products", "companies", "users");
    tenant = UUID.randomUUID();
    otherTenant = UUID.randomUUID();
    createCompany(tenant, "Movement Tenant");
    createCompany(otherTenant, "Other Movement");
    token = bearerToken(tenant, "movements-admin@example.com");
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;

    Product product = createProduct(tenant, "MOV-001", "Producto Movimiento");
    location = createLocation(tenant, "LOC-MOV", "Bodega Mov");
    lot = createLot(tenant, product, location);
    otherLot = createLot(otherTenant, createProduct(otherTenant, "MOV-OTHER", "Other Prod"), createLocation(otherTenant, "LOC-OTHER", "Otra Bodega"));

    createMovement(tenant, product.getId(), lot.getId(), "PURCHASE_IN",
        new BigDecimal("100"), null, location.getId(), "trace-purchase",
        OffsetDateTime.now().minusDays(2), BigDecimal.ZERO, new BigDecimal("100"));
    createMovement(tenant, product.getId(), lot.getId(), "SALE_OUT",
        new BigDecimal("20"), location.getId(), null, "trace-sale",
        OffsetDateTime.now().minusDays(1), new BigDecimal("100"), new BigDecimal("80"));
  }

  @Test
  void returnsOrderedHistoryAndLocationInfo() {
    List<Map<String, Object>> response = authenticated(token, tenant)
        .queryParam("sort", "createdAt,desc")
        .get("/api/v1/inventory/movements")
        .then()
        .statusCode(200)
        .extract()
        .as(new TypeRef<List<Map<String, Object>>>() {});

    assertEquals(2, response.size());
    Map<String, Object> latest = response.get(0);
    assertEquals("SALE_OUT", latest.get("type"));
    assertEquals("trace-sale", latest.get("traceId"));
    assertEquals("20", new BigDecimal(latest.get("qtyChange").toString()).toString());
    Map<?, ?> locationFrom = (Map<?, ?>) latest.get("locationFrom");
    assertEquals(location.getName(), locationFrom.get("name"));
  }

  @Test
  void rejectsCrossTenantLotFilters() {
    authenticated(token, tenant)
        .queryParam("lotId", otherLot.getId())
        .get("/api/v1/inventory/movements")
        .then()
        .statusCode(403);
  }

  private Product createProduct(UUID companyId, String sku, String name) {
    Product product = new Product();
    product.setCompanyId(companyId);
    product.setSku(sku);
    product.setName(name);
    product.setActive(true);
    return productRepository.save(product);
  }

  private InventoryLocation createLocation(UUID companyId, String code, String name) {
    InventoryLocation location = new InventoryLocation();
    location.setCompanyId(companyId);
    location.setCode(code);
    location.setName(name);
    return inventoryLocationRepository.save(location);
  }

  private InventoryLot createLot(UUID companyId, Product product, InventoryLocation location) {
    InventoryLot lot = new InventoryLot();
    lot.setCompanyId(companyId);
    lot.setProductId(product.getId());
    lot.setLocationId(location.getId());
    lot.setQtyAvailable(new BigDecimal("100"));
    lot.setCreatedAt(OffsetDateTime.now());
    lot.setExpDate(LocalDate.now().plusDays(90));
    return inventoryLotRepository.save(lot);
  }

  private void createMovement(UUID companyId,
                              UUID productId,
                              UUID lotId,
                              String type,
                              BigDecimal qty,
                              UUID locationFrom,
                              UUID locationTo,
                              String traceId,
                              OffsetDateTime createdAt,
                              BigDecimal previousQty,
                              BigDecimal newQty) {
    InventoryMovement movement = new InventoryMovement();
    movement.setCompanyId(companyId);
    movement.setProductId(productId);
    movement.setLotId(lotId);
    movement.setType(type);
    movement.setQty(qty);
    movement.setRefType("TEST");
    movement.setRefId(UUID.randomUUID());
    movement.setLocationFromId(locationFrom);
    movement.setLocationToId(locationTo);
    movement.setTraceId(traceId);
    movement.setReasonCode("TEST");
    movement.setPreviousQty(previousQty);
    movement.setNewQty(newQty);
    movement.setCreatedBy("test");
    movement.setUserIp("127.0.0.1");
    movement.setCreatedAt(createdAt);
    inventoryMovementRepository.save(movement);
  }
}

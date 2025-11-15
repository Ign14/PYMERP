package com.datakomerz.pymes.inventory.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.datakomerz.pymes.config.TestJwtDecoderConfig;
import com.datakomerz.pymes.inventory.InventoryLocation;
import com.datakomerz.pymes.inventory.InventoryLocationRepository;
import com.datakomerz.pymes.inventory.InventoryLot;
import com.datakomerz.pymes.inventory.InventoryLotRepository;
import com.datakomerz.pymes.products.Product;
import com.datakomerz.pymes.products.ProductRepository;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
class StockByProductIT extends AbstractInventoryApiIT {

  @LocalServerPort
  private int port;

  @Autowired
  private InventoryLocationRepository inventoryLocationRepository;

  @Autowired
  private InventoryLotRepository inventoryLotRepository;

  @Autowired
  private ProductRepository productRepository;

  private UUID tenant;
  private String token;
  private Product productA;
  private Product productB;
  private UUID locationOne;
  private UUID locationTwo;

  @BeforeEach
  void setUp() {
    truncateTables("inventory_movements", "inventory_lots", "inventory_locations", "products", "users", "companies");
    tenant = UUID.randomUUID();
    createCompany(tenant, "Stock Tenant");
    token = bearerToken(tenant, "stock-admin@example.com");
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;

    productA = new Product();
    productA.setCompanyId(tenant);
    productA.setSku("STOCK-A");
    productA.setName("Producto A");
    productA.setActive(true);
    productRepository.save(productA);

    productB = new Product();
    productB.setCompanyId(tenant);
    productB.setSku("STOCK-B");
    productB.setName("Producto B");
    productB.setActive(true);
    productRepository.save(productB);

    InventoryLocation first = new InventoryLocation();
    first.setCompanyId(tenant);
    first.setCode("LOC-1");
    first.setName("Bodega Uno");
    inventoryLocationRepository.save(first);
    locationOne = first.getId();

    InventoryLocation second = new InventoryLocation();
    second.setCompanyId(tenant);
    second.setCode("LOC-2");
    second.setName("Bodega Dos");
    inventoryLocationRepository.save(second);
    locationTwo = second.getId();

    InventoryLot lotA1 = new InventoryLot();
    lotA1.setCompanyId(tenant);
    lotA1.setProductId(productA.getId());
    lotA1.setQtyAvailable(new BigDecimal("10"));
    lotA1.setCostUnit(new BigDecimal("5"));
    lotA1.setLocationId(locationOne);
    inventoryLotRepository.save(lotA1);

    InventoryLot lotA2 = new InventoryLot();
    lotA2.setCompanyId(tenant);
    lotA2.setProductId(productA.getId());
    lotA2.setQtyAvailable(new BigDecimal("5"));
    lotA2.setCostUnit(new BigDecimal("5"));
    lotA2.setLocationId(locationTwo);
    inventoryLotRepository.save(lotA2);

    InventoryLot lotB1 = new InventoryLot();
    lotB1.setCompanyId(tenant);
    lotB1.setProductId(productB.getId());
    lotB1.setQtyAvailable(new BigDecimal("20"));
    lotB1.setCostUnit(new BigDecimal("6"));
    lotB1.setLocationId(locationOne);
    inventoryLotRepository.save(lotB1);
  }

  @Test
  void returnsStockPerLocationForSingleProduct() {
    List<Map<String, Object>> response = authenticated(token, tenant)
        .queryParam("productId", productA.getId())
        .get("/api/v1/inventory/stock/by-product")
        .then()
        .statusCode(200)
        .extract()
        .as(new TypeRef<List<Map<String, Object>>>() {});

    assertEquals(2, response.size());
    assertTrue(response.stream()
        .allMatch(entry -> UUID.fromString(entry.get("productId").toString()).equals(productA.getId())));

    Map<UUID, BigDecimal> byLocation = response.stream()
        .collect(Collectors.toMap(
            entry -> UUID.fromString(entry.get("locationId").toString()),
            entry -> new BigDecimal(entry.get("availableQty").toString())
        ));
    assertEquals(new BigDecimal("10"), byLocation.get(locationOne));
    assertEquals(new BigDecimal("5"), byLocation.get(locationTwo));
  }

  @Test
  void supportsMultipleProductIds() {
    List<Map<String, Object>> response = authenticated(token, tenant)
        .queryParam("productIds[]", productA.getId())
        .queryParam("productIds[]", productB.getId())
        .get("/api/v1/inventory/stock/by-product")
        .then()
        .statusCode(200)
        .extract()
        .as(new TypeRef<List<Map<String, Object>>>() {});

    assertEquals(3, response.size());
    Map<String, BigDecimal> flatten = response.stream()
        .collect(Collectors.toMap(
            entry -> entry.get("productId").toString() + ":" + entry.get("locationId").toString(),
            entry -> new BigDecimal(entry.get("availableQty").toString())
        ));

    assertEquals(new BigDecimal("10"), flatten.get(productA.getId().toString() + ":" + locationOne));
    assertEquals(new BigDecimal("5"), flatten.get(productA.getId().toString() + ":" + locationTwo));
    assertEquals(new BigDecimal("20"), flatten.get(productB.getId().toString() + ":" + locationOne));
  }
}

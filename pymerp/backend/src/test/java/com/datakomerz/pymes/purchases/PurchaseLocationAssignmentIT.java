package com.datakomerz.pymes.purchases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.datakomerz.pymes.config.TestJwtDecoderConfig;
import com.datakomerz.pymes.inventory.InventoryLocation;
import com.datakomerz.pymes.inventory.InventoryLocationRepository;
import com.datakomerz.pymes.inventory.InventoryLot;
import com.datakomerz.pymes.inventory.InventoryLotRepository;
import com.datakomerz.pymes.inventory.InventoryMovement;
import com.datakomerz.pymes.inventory.InventoryMovementRepository;
import com.datakomerz.pymes.inventory.integration.AbstractInventoryApiIT;
import com.datakomerz.pymes.products.Product;
import com.datakomerz.pymes.products.ProductRepository;
import com.datakomerz.pymes.suppliers.Supplier;
import com.datakomerz.pymes.suppliers.SupplierRepository;
import io.restassured.RestAssured;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
class PurchaseLocationAssignmentIT extends AbstractInventoryApiIT {

  @LocalServerPort
  private int port;

  @Autowired
  private SupplierRepository supplierRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private InventoryLotRepository inventoryLotRepository;

  @Autowired
  private InventoryLocationRepository inventoryLocationRepository;

  @Autowired
  private InventoryMovementRepository inventoryMovementRepository;

  private UUID companyId;
  private String token;

  @BeforeEach
  void setUp() {
    truncateTables(
        "inventory_movements",
        "inventory_lots",
        "inventory_locations",
        "purchase_items",
        "purchases",
        "products",
        "suppliers",
        "companies",
        "users");
    companyId = UUID.randomUUID();
    createCompany(companyId, "Purchase Tenant");
    token = bearerToken(companyId, "purchases-admin@example.com");
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
  }

  @Test
  void assignsDefaultLocationWhenNoneProvided() {
    Supplier supplier = createSupplier(companyId, "Proveedor default");
    Product product = createProduct(companyId, "SKU-001", "Producto default");

    Map<String, Object> payload = purchasePayload(supplier.getId(), product.getId(), null);

    authenticated(token, companyId)
        .body(payload)
        .post("/api/v1/purchases")
        .then()
        .statusCode(201);

    List<InventoryLot> lots = inventoryLotRepository.findAll();
    assertEquals(1, lots.size());
    InventoryLot lot = lots.get(0);
    assertNotNull(lot.getLocationId());

    InventoryLocation defaultLocation = inventoryLocationRepository.findById(lot.getLocationId())
        .orElseThrow();
    assertEquals("DEFAULT", defaultLocation.getCode());
    assertEquals("Ubicación por defecto", defaultLocation.getName());

    List<InventoryMovement> movements = inventoryMovementRepository.findAll();
    assertEquals(1, movements.size());
    InventoryMovement movement = movements.get(0);
    assertEquals(defaultLocation.getId(), movement.getLocationToId());
    assertNull(movement.getLocationFromId());
    assertNotNull(movement.getNote());
    assertTrue(movement.getNote().contains(defaultLocation.getName()));
  }

  @Test
  void keepsProvidedLocationWhenPresent() {
    Supplier supplier = createSupplier(companyId, "Proveedor ubicaciones");
    Product product = createProduct(companyId, "SKU-002", "Producto con ubicación");
    InventoryLocation customLocation = createLocation(companyId, "RACK-1", "Rack principal");

    Map<String, Object> payload = purchasePayload(supplier.getId(), product.getId(), customLocation.getId());

    authenticated(token, companyId)
        .body(payload)
        .post("/api/v1/purchases")
        .then()
        .statusCode(201);

    List<InventoryLot> lots = inventoryLotRepository.findAll();
    assertEquals(1, lots.size());
    InventoryLot lot = lots.get(0);
    assertEquals(customLocation.getId(), lot.getLocationId());

    List<InventoryMovement> movements = inventoryMovementRepository.findAll();
    assertEquals(1, movements.size());
    InventoryMovement movement = movements.get(0);
    assertEquals(customLocation.getId(), movement.getLocationToId());
    assertTrue(movement.getNote().contains(customLocation.getName()));

    boolean defaultExists = inventoryLocationRepository.existsByCompanyIdAndCode(companyId, "DEFAULT");
    assertFalse(defaultExists);
  }

  private Supplier createSupplier(UUID tenant, String name) {
    Supplier supplier = new Supplier();
    supplier.setCompanyId(tenant);
    supplier.setName(name);
    return supplierRepository.save(supplier);
  }

  private Product createProduct(UUID tenant, String sku, String name) {
    Product product = new Product();
    product.setCompanyId(tenant);
    product.setSku(sku);
    product.setName(name);
    product.setActive(true);
    product.setCriticalStock(BigDecimal.TEN);
    return productRepository.save(product);
  }

  private InventoryLocation createLocation(UUID tenant, String code, String name) {
    InventoryLocation location = new InventoryLocation();
    location.setCompanyId(tenant);
    location.setCode(code);
    location.setName(name);
    location.setEnabled(true);
    return inventoryLocationRepository.save(location);
  }

  private Map<String, Object> purchasePayload(UUID supplierId, UUID productId, UUID locationId) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("supplierId", supplierId.toString());
    payload.put("docType", "Factura");
    payload.put("docNumber", "F-001");
    payload.put("net", 10000);
    payload.put("vat", 1900);
    payload.put("total", 11900);
    payload.put("issuedAt", OffsetDateTime.now().toString());
    payload.put("receivedAt", OffsetDateTime.now().toString());
    payload.put("paymentTermDays", 30);
    List<Map<String, Object>> items = new ArrayList<>();
    Map<String, Object> item = new HashMap<>();
    item.put("productId", productId.toString());
    item.put("qty", 5);
    item.put("unitCost", 2000);
    item.put("vatRate", 19);
    if (locationId != null) {
      item.put("locationId", locationId.toString());
    }
    items.add(item);
    payload.put("items", items);
    return payload;
  }
}

package com.datakomerz.pymes.inventory.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.datakomerz.pymes.config.TestJwtDecoderConfig;
import com.datakomerz.pymes.inventory.InventoryLocation;
import com.datakomerz.pymes.inventory.InventoryLocationRepository;
import com.datakomerz.pymes.inventory.InventoryLot;
import com.datakomerz.pymes.inventory.InventoryLotRepository;
import com.datakomerz.pymes.products.Product;
import com.datakomerz.pymes.products.ProductRepository;
import com.datakomerz.pymes.purchases.Purchase;
import com.datakomerz.pymes.purchases.PurchaseRepository;
import com.datakomerz.pymes.sales.SaleLotAllocation;
import com.datakomerz.pymes.sales.SaleLotAllocationRepository;
import com.datakomerz.pymes.suppliers.Supplier;
import com.datakomerz.pymes.suppliers.SupplierRepository;
import io.restassured.RestAssured;
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
class LotsListIT extends AbstractInventoryApiIT {

  @LocalServerPort
  private int port;

  @Autowired
  private InventoryLotRepository inventoryLotRepository;

  @Autowired
  private InventoryLocationRepository inventoryLocationRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private SupplierRepository supplierRepository;

  @Autowired
  private PurchaseRepository purchaseRepository;

  @Autowired
  private SaleLotAllocationRepository allocationRepository;

  private UUID tenant;
  private UUID otherTenant;
  private String token;
  private Supplier supplier;
  private Supplier otherSupplier;
  private InventoryLocation locationA;
  private InventoryLocation locationB;
  private InventoryLot belowStockLot;

  @BeforeEach
  void setUp() {
    truncateTables(
        "sale_lot_allocations",
        "inventory_movements",
        "inventory_lots",
        "inventory_locations",
        "products",
        "suppliers",
        "purchases",
        "companies",
        "users");
    tenant = UUID.randomUUID();
    otherTenant = UUID.randomUUID();
    createCompany(tenant, "Lots Tenant");
    createCompany(otherTenant, "Other Tenant");
    token = bearerToken(tenant, "lots-admin@example.com");
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;

    supplier = createSupplier(tenant, "Main Supplier");
    otherSupplier = createSupplier(otherTenant, "Cross Supplier");
    Product productA = createProduct(tenant, "LOT-A", "Producto A", new BigDecimal("50"));
    Product productB = createProduct(tenant, "LOT-B", "Producto B", new BigDecimal("10"));

    Purchase purchase = createPurchase(tenant, supplier.getId());
    locationA = createLocation(tenant, "LOC-A", "Bodega A");
    locationB = createLocation(tenant, "LOC-B", "Bodega B");

    belowStockLot = createLot(tenant, productA, purchase, locationA, new BigDecimal("30"), LocalDate.now().plusDays(90));
    createLot(tenant, productB, purchase, locationB, new BigDecimal("120"), LocalDate.now().plusDays(180));
    createAllocation(UUID.randomUUID(), productA, belowStockLot, new BigDecimal("5"));
  }

  @Test
  void returnsLotListWithStatusAndReservations() {
    List<Map<String, Object>> response = authenticated(token, tenant)
        .queryParam("status", "BAJO_STOCK")
        .queryParam("q", "Producto A")
        .queryParam("locationId", locationA.getId())
        .get("/api/v1/inventory/lots")
        .then()
        .statusCode(200)
        .extract()
        .jsonPath()
        .getList("content");

    assertEquals(1, response.size());
    Map<String, Object> lot = response.get(0);
    assertEquals("BAJO_STOCK", lot.get("status"));
    assertEquals(locationA.getId().toString(), lot.get("locationId"));
    assertEquals(locationA.getName(), lot.get("locationName"));
    assertEquals(supplier.getId().toString(), lot.get("supplierId"));
    assertEquals(supplier.getName(), lot.get("supplierName"));
    BigDecimal reserved = new BigDecimal(lot.get("quantityReserved").toString());
    assertEquals(new BigDecimal("5"), reserved);
  }

  @Test
  void rejectsCrossTenantSupplierFilters() {
    authenticated(token, tenant)
        .queryParam("supplierId", otherSupplier.getId())
        .get("/api/v1/inventory/lots")
        .then()
        .statusCode(403);
  }

  private Supplier createSupplier(UUID companyId, String name) {
    Supplier supplier = new Supplier();
    supplier.setCompanyId(companyId);
    supplier.setName(name);
    return supplierRepository.save(supplier);
  }

  private Product createProduct(UUID companyId, String sku, String name, BigDecimal criticalStock) {
    Product product = new Product();
    product.setCompanyId(companyId);
    product.setSku(sku);
    product.setName(name);
    product.setActive(true);
    product.setCriticalStock(criticalStock);
    return productRepository.save(product);
  }

  private Purchase createPurchase(UUID companyId, UUID supplierId) {
    Purchase purchase = new Purchase();
    purchase.setCompanyId(companyId);
    purchase.setSupplierId(supplierId);
    purchase.setStatus("received");
    purchase.setNet(BigDecimal.ZERO);
    purchase.setVat(BigDecimal.ZERO);
    purchase.setTotal(BigDecimal.ZERO);
    purchase.setIssuedAt(OffsetDateTime.now());
    purchase.setReceivedAt(OffsetDateTime.now());
    return purchaseRepository.save(purchase);
  }

  private InventoryLocation createLocation(UUID companyId, String code, String name) {
    InventoryLocation location = new InventoryLocation();
    location.setCompanyId(companyId);
    location.setCode(code);
    location.setName(name);
    return inventoryLocationRepository.save(location);
  }

  private InventoryLot createLot(UUID companyId,
                                Product product,
                                Purchase purchase,
                                InventoryLocation location,
                                BigDecimal qty,
                                LocalDate expDate) {
    InventoryLot lot = new InventoryLot();
    lot.setCompanyId(companyId);
    lot.setProductId(product.getId());
    lot.setPurchaseId(purchase.getId());
    lot.setQtyAvailable(qty);
    lot.setCostUnit(BigDecimal.ONE);
    lot.setLocationId(location.getId());
    lot.setExpDate(expDate);
    lot.setCreatedAt(OffsetDateTime.now());
    return inventoryLotRepository.save(lot);
  }

  private void createAllocation(UUID saleId, Product product, InventoryLot lot, BigDecimal qty) {
    SaleLotAllocation allocation = new SaleLotAllocation();
    allocation.setSaleId(saleId);
    allocation.setProductId(product.getId());
    allocation.setLotId(lot.getId());
    allocation.setQty(qty);
    allocationRepository.save(allocation);
  }
}

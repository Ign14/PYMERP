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
import java.math.BigDecimal;
import java.util.List;
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
class LotLocationAssignmentIT extends AbstractInventoryApiIT {

  @LocalServerPort
  private int port;

  @Autowired
  private InventoryLocationRepository inventoryLocationRepository;

  @Autowired
  private InventoryLotRepository inventoryLotRepository;

  @Autowired
  private InventoryMovementRepository inventoryMovementRepository;

  @Autowired
  private ProductRepository productRepository;

  private UUID tenant;
  private UUID otherTenant;
  private String token;
  private UUID secondaryLocation;
  private UUID otherTenantLocation;
  private InventoryLot lot;

  @BeforeEach
  void setUp() {
    truncateTables("inventory_movements", "inventory_lots", "inventory_locations", "products", "users", "companies");
    tenant = UUID.randomUUID();
    otherTenant = UUID.randomUUID();
    createCompany(tenant, "Location Assigner");
    createCompany(otherTenant, "Foreign Tenant");
    token = bearerToken(tenant, "assigner@example.com");
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;

    Product product = new Product();
    product.setCompanyId(tenant);
    product.setSku("LOT-PROD");
    product.setName("Prod Lot");
    product.setActive(true);
    productRepository.save(product);

    InventoryLocation origin = new InventoryLocation();
    origin.setCompanyId(tenant);
    origin.setCode("ORIGIN");
    origin.setName("Origen");
    inventoryLocationRepository.save(origin);

    InventoryLocation destination = new InventoryLocation();
    destination.setCompanyId(tenant);
    destination.setCode("DEST");
    destination.setName("Destino");
    inventoryLocationRepository.save(destination);
    secondaryLocation = destination.getId();

    lot = new InventoryLot();
    lot.setCompanyId(tenant);
    lot.setProductId(product.getId());
    lot.setQtyAvailable(new BigDecimal("100"));
    lot.setCostUnit(new BigDecimal("15"));
    lot.setLocationId(origin.getId());
    inventoryLotRepository.save(lot);

    InventoryLocation foreign = new InventoryLocation();
    foreign.setCompanyId(otherTenant);
    foreign.setCode("FOREIGN");
    foreign.setName("Origen Ajeno");
    inventoryLocationRepository.save(foreign);
    otherTenantLocation = foreign.getId();
  }

  @Test
  void assigningLocationCreatesTransferMovementExactlyOnce() {
    UUID lotId = lot.getId();

    authenticated(token, tenant)
        .put("/api/v1/inventory/lots/{lotId}/location/{locationId}", lotId, secondaryLocation)
        .then()
        .statusCode(200);

    assertEquals(secondaryLocation, inventoryLotRepository.findById(lotId).orElseThrow().getLocationId());

    List<InventoryMovement> movements = inventoryMovementRepository.findAll().stream()
        .filter(m -> lotId.equals(m.getLotId()) && "TRANSFER".equals(m.getType()))
        .toList();
    assertEquals(1, movements.size());

    authenticated(token, tenant)
        .put("/api/v1/inventory/lots/{lotId}/location/{locationId}", lotId, secondaryLocation)
        .then()
        .statusCode(200);

    List<InventoryMovement> movementsAfter = inventoryMovementRepository.findAll().stream()
        .filter(m -> lotId.equals(m.getLotId()) && "TRANSFER".equals(m.getType()))
        .toList();
    assertEquals(1, movementsAfter.size());
  }

  @Test
  void crossTenantLocationAssignmentIsForbidden() {
    authenticated(token, tenant)
        .put("/api/v1/inventory/lots/{lotId}/location/{locationId}", lot.getId(), otherTenantLocation)
        .then()
        .statusCode(403);
  }
}

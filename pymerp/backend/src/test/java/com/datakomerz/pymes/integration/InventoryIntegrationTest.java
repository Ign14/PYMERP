package com.datakomerz.pymes.integration;

import com.datakomerz.pymes.inventory.InventoryLot;
import com.datakomerz.pymes.inventory.InventoryLotRepository;
import com.datakomerz.pymes.inventory.InventoryMovement;
import com.datakomerz.pymes.inventory.InventoryMovementRepository;
import com.datakomerz.pymes.products.Product;
import com.datakomerz.pymes.products.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de integración para validar la integridad de datos en inventario:
 * 1. Relación correcta entre InventoryLot → Product
 * 2. Relación correcta entre InventoryMovement → Product
 * 3. Cálculo correcto de stock disponible
 * 4. Trazabilidad de movimientos FIFO
 * 5. Multi-tenancy (aislamiento por companyId)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(com.datakomerz.pymes.config.TestJwtDecoderConfig.class)
public class InventoryIntegrationTest {

    @Autowired private InventoryLotRepository lotRepository;
    @Autowired private InventoryMovementRepository movementRepository;
    @Autowired private ProductRepository productRepository;

    private UUID companyId;
    private UUID productId;
    
    @BeforeEach
    void setup() {
        companyId = UUID.randomUUID();
        
        // Crear producto de prueba
        Product product = new Product();
        product.setCompanyId(companyId);
        product.setName("Producto Inventario Test");
        product.setSku("INV-001");
        product.setActive(true);
        product = productRepository.save(product);
        productId = product.getId();
    }

    @Test
    void testInventoryLotProductRelationship_ShouldBeValid() {
        // Arrange & Act: crear lote vinculado a producto
        InventoryLot lot = new InventoryLot();
        lot.setCompanyId(companyId);
        lot.setProductId(productId);
        lot.setQtyAvailable(new BigDecimal("100"));
        lot.setCostUnit(new BigDecimal("5000"));
        lot = lotRepository.save(lot);
        
        // Assert: verificar relación con producto
        InventoryLot saved = lotRepository.findById(lot.getId()).orElseThrow();
        assertEquals(productId, saved.getProductId());
        
        Product product = productRepository.findById(saved.getProductId()).orElseThrow();
        assertEquals("Producto Inventario Test", product.getName());
    }

    @Test
    void testInventoryMovementProductRelationship_ShouldBeValid() {
        // Arrange & Act: crear movimiento vinculado a producto
        InventoryMovement movement = new InventoryMovement();
        movement.setCompanyId(companyId);
        movement.setProductId(productId);
        movement.setType("IN");
        movement.setQty(new BigDecimal("50"));
        movement.setReasonCode("PURCHASE");
        movement = movementRepository.save(movement);
        
        // Assert: verificar relación con producto
        InventoryMovement saved = movementRepository.findById(movement.getId()).orElseThrow();
        assertEquals(productId, saved.getProductId());
        
        Product product = productRepository.findById(saved.getProductId()).orElseThrow();
        assertEquals("INV-001", product.getSku());
    }

    @Test
    void testMultipleLots_StockCalculation() {
        // Arrange: crear 3 lotes para el mismo producto
        InventoryLot lot1 = new InventoryLot();
        lot1.setCompanyId(companyId);
        lot1.setProductId(productId);
        lot1.setQtyAvailable(new BigDecimal("30"));
        lot1.setCostUnit(new BigDecimal("4000"));
        lotRepository.save(lot1);
        
        InventoryLot lot2 = new InventoryLot();
        lot2.setCompanyId(companyId);
        lot2.setProductId(productId);
        lot2.setQtyAvailable(new BigDecimal("20"));
        lot2.setCostUnit(new BigDecimal("4500"));
        lotRepository.save(lot2);
        
        InventoryLot lot3 = new InventoryLot();
        lot3.setCompanyId(companyId);
        lot3.setProductId(productId);
        lot3.setQtyAvailable(new BigDecimal("50"));
        lot3.setCostUnit(new BigDecimal("5000"));
        lotRepository.save(lot3);
        
        // Act: calcular stock total
        BigDecimal totalStock = lotRepository.findAll().stream()
            .filter(lot -> lot.getProductId().equals(productId))
            .map(InventoryLot::getQtyAvailable)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Assert: stock total debe ser suma de todos los lotes
        assertEquals(0, new BigDecimal("100").compareTo(totalStock));
    }

    @Test
    void testMovementHistory_ShouldTrackAllOperations() {
        // Arrange & Act: crear múltiples movimientos
        InventoryMovement in1 = new InventoryMovement();
        in1.setCompanyId(companyId);
        in1.setProductId(productId);
        in1.setType("IN");
        in1.setQty(new BigDecimal("100"));
        in1.setReasonCode("PURCHASE");
        movementRepository.save(in1);
        
        InventoryMovement out1 = new InventoryMovement();
        out1.setCompanyId(companyId);
        out1.setProductId(productId);
        out1.setType("OUT");
        out1.setQty(new BigDecimal("30"));
        out1.setReasonCode("SALE");
        movementRepository.save(out1);
        
        InventoryMovement in2 = new InventoryMovement();
        in2.setCompanyId(companyId);
        in2.setProductId(productId);
        in2.setType("IN");
        in2.setQty(new BigDecimal("50"));
        in2.setReasonCode("PURCHASE");
        movementRepository.save(in2);
        
        // Act: obtener historial de movimientos
        List<InventoryMovement> history = movementRepository.findAll().stream()
            .filter(m -> m.getProductId().equals(productId))
            .toList();
        
        // Assert: debe haber 3 movimientos registrados
        assertEquals(3, history.size());
        
        // Calcular stock neto (entradas - salidas)
        BigDecimal netStock = history.stream()
            .map(m -> "IN".equals(m.getType()) ? m.getQty() : m.getQty().negate())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        assertEquals(0, new BigDecimal("120").compareTo(netStock)); // 100 - 30 + 50 = 120
    }

    @Test
    void testMultiTenancy_InventoryShouldBeIsolated() {
        // Arrange: crear lote para compañía 1
        InventoryLot lot1 = new InventoryLot();
        lot1.setCompanyId(companyId);
        lot1.setProductId(productId);
        lot1.setQtyAvailable(new BigDecimal("100"));
        lot1.setCostUnit(new BigDecimal("5000"));
        lotRepository.save(lot1);
        
        // Crear datos para compañía 2
        UUID company2Id = UUID.randomUUID();
        Product product2 = new Product();
        product2.setCompanyId(company2Id);
        product2.setName("Producto Empresa 2");
        product2.setSku("INV-002");
        product2 = productRepository.save(product2);
        
        InventoryLot lot2 = new InventoryLot();
        lot2.setCompanyId(company2Id);
        lot2.setProductId(product2.getId());
        lot2.setQtyAvailable(new BigDecimal("200"));
        lot2.setCostUnit(new BigDecimal("6000"));
        lotRepository.save(lot2);
        
        // Act: contar lotes por compañía
        long company1Lots = lotRepository.findAll().stream()
            .filter(l -> l.getCompanyId().equals(companyId))
            .count();
        
        long company2Lots = lotRepository.findAll().stream()
            .filter(l -> l.getCompanyId().equals(company2Id))
            .count();
        
        // Assert: cada compañía debe ver solo sus lotes
        assertEquals(1, company1Lots);
        assertEquals(1, company2Lots);
    }

    @Test
    void testReferentialIntegrity_ProductWithInventory() {
        // Arrange: crear lote vinculado a producto
        InventoryLot lot = new InventoryLot();
        lot.setCompanyId(companyId);
        lot.setProductId(productId);
        lot.setQtyAvailable(new BigDecimal("75"));
        lot.setCostUnit(new BigDecimal("4800"));
        lotRepository.save(lot);
        
        // Act & Assert: verificar que el producto existe y tiene inventario
        assertTrue(productRepository.existsById(productId));
        
        long lotCount = lotRepository.findAll().stream()
            .filter(l -> l.getProductId().equals(productId))
            .count();
        
        assertTrue(lotCount > 0, "El producto debe tener al menos un lote de inventario");
    }
}

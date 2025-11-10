package com.datakomerz.pymes.integration;

import com.datakomerz.pymes.inventory.InventoryLot;
import com.datakomerz.pymes.inventory.InventoryLotRepository;
import com.datakomerz.pymes.products.Product;
import com.datakomerz.pymes.products.ProductRepository;
import com.datakomerz.pymes.purchases.Purchase;
import com.datakomerz.pymes.purchases.PurchaseItem;
import com.datakomerz.pymes.purchases.PurchaseItemRepository;
import com.datakomerz.pymes.purchases.PurchaseRepository;
import com.datakomerz.pymes.suppliers.Supplier;
import com.datakomerz.pymes.suppliers.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de integración para validar la integridad de datos en compras:
 * 1. Relación correcta entre Purchase → Supplier
 * 2. Relación correcta entre PurchaseItem → Purchase
 * 3. Relación correcta entre PurchaseItem → Product
 * 4. Relación correcta entre InventoryLot → Product
 * 5. Multi-tenancy (aislamiento por companyId)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(com.datakomerz.pymes.config.TestJwtDecoderConfig.class)
public class PurchasesIntegrationTest {

    @Autowired private PurchaseRepository purchaseRepository;
    @Autowired private PurchaseItemRepository purchaseItemRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private InventoryLotRepository lotRepository;

    private UUID companyId;
    private UUID supplierId;
    private UUID productId;
    
    @BeforeEach
    void setup() {
        companyId = UUID.randomUUID();
        
        // Crear proveedor de prueba
        Supplier supplier = new Supplier();
        supplier.setCompanyId(companyId);
        supplier.setName("Proveedor Test");
        supplier.setRut("76543210-K");
        supplier.setEmail("proveedor@test.com");
        supplier = supplierRepository.save(supplier);
        supplierId = supplier.getId();
        
        // Crear producto de prueba
        Product product = new Product();
        product.setCompanyId(companyId);
        product.setName("Producto Compra Test");
        product.setSku("COMP-001");
        product.setActive(true);
        product = productRepository.save(product);
        productId = product.getId();
    }

    @Test
    void testPurchaseSupplierRelationship_ShouldBeValid() {
        // Arrange & Act: crear compra vinculada a proveedor
        Purchase purchase = new Purchase();
        purchase.setCompanyId(companyId);
        purchase.setSupplierId(supplierId);
        purchase.setDocType("factura");
        purchase.setDocNumber("12345");
        purchase.setStatus("pendiente");
        purchase.setIssuedAt(OffsetDateTime.now());
        purchase.setNet(new BigDecimal("100000"));
        purchase.setVat(new BigDecimal("19000"));
        purchase.setTotal(new BigDecimal("119000"));
        purchase.setPaymentTermDays(30);
        purchase = purchaseRepository.save(purchase);
        
        // Assert: verificar relación con proveedor
        Purchase saved = purchaseRepository.findById(purchase.getId()).orElseThrow();
        assertEquals(supplierId, saved.getSupplierId());
        
        Supplier supplier = supplierRepository.findById(supplierId).orElseThrow();
        assertEquals("Proveedor Test", supplier.getName());
    }

    @Test
    void testPurchaseItemRelationships_ShouldBeValid() {
        // Arrange: crear compra
        Purchase purchase = new Purchase();
        purchase.setCompanyId(companyId);
        purchase.setSupplierId(supplierId);
        purchase.setDocType("factura");
        purchase.setIssuedAt(OffsetDateTime.now());
        purchase.setNet(new BigDecimal("80000"));
        purchase.setVat(new BigDecimal("15200"));
        purchase.setTotal(new BigDecimal("95200"));
        purchase.setPaymentTermDays(30);
        purchase = purchaseRepository.save(purchase);
        
        // Act: crear ítem de compra vinculado a compra y producto
        PurchaseItem item = new PurchaseItem();
        item.setPurchaseId(purchase.getId());
        item.setProductId(productId);
        item.setQty(new BigDecimal("10"));
        item.setUnitCost(new BigDecimal("8000"));
        item = purchaseItemRepository.save(item);
        
        // Assert: verificar relaciones
        PurchaseItem saved = purchaseItemRepository.findById(item.getId()).orElseThrow();
        assertEquals(purchase.getId(), saved.getPurchaseId());
        assertEquals(productId, saved.getProductId());
        
        // Verificar que purchase existe
        assertTrue(purchaseRepository.existsById(saved.getPurchaseId()));
        
        // Verificar que producto existe
        Product product = productRepository.findById(saved.getProductId()).orElseThrow();
        assertEquals("Producto Compra Test", product.getName());
    }

    @Test
    void testInventoryLotProductRelationship_ShouldBeValid() {
        // Arrange: crear compra e ítem
        Purchase purchase = new Purchase();
        purchase.setCompanyId(companyId);
        purchase.setSupplierId(supplierId);
        purchase.setDocType("factura");
        purchase.setIssuedAt(OffsetDateTime.now());
        purchase.setNet(BigDecimal.valueOf(50000));
        purchase.setVat(BigDecimal.valueOf(9500));
        purchase.setTotal(BigDecimal.valueOf(59500));
        purchase.setPaymentTermDays(30);
        purchase = purchaseRepository.save(purchase);
        
        PurchaseItem item = new PurchaseItem();
        item.setPurchaseId(purchase.getId());
        item.setProductId(productId);
        item.setQty(new BigDecimal("5"));
        item.setUnitCost(new BigDecimal("10000"));
        item = purchaseItemRepository.save(item);
        
        // Act: crear lote de inventario vinculado a producto
        InventoryLot lot = new InventoryLot();
        lot.setCompanyId(companyId);
        lot.setProductId(productId);
        lot.setPurchaseItemId(item.getId());
        lot.setQtyAvailable(new BigDecimal("5"));
        lot.setCostUnit(new BigDecimal("10000"));
        lot = lotRepository.save(lot);
        
        // Assert: verificar relación con producto
        InventoryLot saved = lotRepository.findById(lot.getId()).orElseThrow();
        assertEquals(productId, saved.getProductId());
        
        Product product = productRepository.findById(saved.getProductId()).orElseThrow();
        assertEquals("Producto Compra Test", product.getName());
        assertEquals("COMP-001", product.getSku());
    }

    @Test
    void testMultiTenancy_PurchasesShouldBeIsolated() {
        // Arrange: crear compra para compañía 1
        Purchase purchase1 = new Purchase();
        purchase1.setCompanyId(companyId);
        purchase1.setSupplierId(supplierId);
        purchase1.setDocType("factura");
        purchase1.setIssuedAt(OffsetDateTime.now());
        purchase1.setNet(BigDecimal.valueOf(100000));
        purchase1.setVat(BigDecimal.valueOf(19000));
        purchase1.setTotal(BigDecimal.valueOf(119000));
        purchase1.setPaymentTermDays(30);
        purchaseRepository.save(purchase1);
        
        // Crear datos para compañía 2
        UUID company2Id = UUID.randomUUID();
        Supplier supplier2 = new Supplier();
        supplier2.setCompanyId(company2Id);
        supplier2.setName("Proveedor Empresa 2");
        supplier2.setRut("11111111-1");
        supplier2 = supplierRepository.save(supplier2);
        
        Purchase purchase2 = new Purchase();
        purchase2.setCompanyId(company2Id);
        purchase2.setSupplierId(supplier2.getId());
        purchase2.setDocType("factura");
        purchase2.setIssuedAt(OffsetDateTime.now());
        purchase2.setNet(BigDecimal.valueOf(50000));
        purchase2.setVat(BigDecimal.valueOf(9500));
        purchase2.setTotal(BigDecimal.valueOf(59500));
        purchase2.setPaymentTermDays(30);
        purchaseRepository.save(purchase2);
        
        // Act: contar compras por compañía
        long company1Count = purchaseRepository.findAll().stream()
            .filter(p -> p.getCompanyId().equals(companyId))
            .count();
        
        long company2Count = purchaseRepository.findAll().stream()
            .filter(p -> p.getCompanyId().equals(company2Id))
            .count();
        
        // Assert: cada compañía debe ver solo sus datos
        assertEquals(1, company1Count);
        assertEquals(1, company2Count);
    }

    @Test
    void testReferentialIntegrity_SupplierWithPurchases() {
        // Arrange: crear compra vinculada a proveedor
        Purchase purchase = new Purchase();
        purchase.setCompanyId(companyId);
        purchase.setSupplierId(supplierId);
        purchase.setDocType("factura");
        purchase.setIssuedAt(OffsetDateTime.now());
        purchase.setNet(BigDecimal.valueOf(100000));
        purchase.setVat(BigDecimal.valueOf(19000));
        purchase.setTotal(BigDecimal.valueOf(119000));
        purchase.setPaymentTermDays(30);
        purchaseRepository.save(purchase);
        
        // Act & Assert: verificar que el proveedor existe y tiene compras
        assertTrue(supplierRepository.existsById(supplierId));
        
        long purchaseCount = purchaseRepository.findAll().stream()
            .filter(p -> p.getSupplierId().equals(supplierId))
            .count();
        
        assertTrue(purchaseCount > 0, "El proveedor debe tener al menos una compra asociada");
    }
}

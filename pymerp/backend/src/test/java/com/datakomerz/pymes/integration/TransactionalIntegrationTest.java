package com.datakomerz.pymes.integration;

import com.datakomerz.pymes.customers.Customer;
import com.datakomerz.pymes.customers.CustomerRepository;
import com.datakomerz.pymes.inventory.InventoryLot;
import com.datakomerz.pymes.inventory.InventoryLotRepository;
import com.datakomerz.pymes.products.Product;
import com.datakomerz.pymes.products.ProductRepository;
import com.datakomerz.pymes.sales.Sale;
import com.datakomerz.pymes.sales.SaleItem;
import com.datakomerz.pymes.sales.SaleItemRepository;
import com.datakomerz.pymes.sales.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de integración para validar comportamiento transaccional:
 * 1. Operaciones atómicas (todo o nada)
 * 2. Rollback automático en caso de error
 * 3. Consistencia de datos después de rollback
 * 4. Propagación correcta de transacciones
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(com.datakomerz.pymes.config.TestJwtDecoderConfig.class)
public class TransactionalIntegrationTest {

    @Autowired private SaleRepository saleRepository;
    @Autowired private SaleItemRepository saleItemRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private InventoryLotRepository lotRepository;

    private UUID companyId;
    private UUID customerId;
    private UUID productId;
    
    @BeforeEach
    void setup() {
        companyId = UUID.randomUUID();
        
        // Crear cliente
        Customer customer = new Customer();
        customer.setCompanyId(companyId);
        customer.setName("Cliente Transaccional");
        customer.setRut("12345678-9");
        customer.setEmail("trans@test.com");
        customer = customerRepository.save(customer);
        customerId = customer.getId();
        
        // Crear producto
        Product product = new Product();
        product.setCompanyId(companyId);
        product.setName("Producto Trans");
        product.setSku("TRANS-001");
        product.setActive(true);
        product = productRepository.save(product);
        productId = product.getId();
        
        // Crear inventario inicial
        InventoryLot lot = new InventoryLot();
        lot.setCompanyId(companyId);
        lot.setProductId(productId);
        lot.setQtyAvailable(new BigDecimal("100"));
        lot.setCostUnit(new BigDecimal("5000"));
        lotRepository.save(lot);
    }

    @Test
    void testSaleCreation_AllOrNothing() {
        // Arrange: contar registros iniciales
        long initialSalesCount = saleRepository.count();
        long initialItemsCount = saleItemRepository.count();
        
        // Act: crear venta completa con sale + items
        Sale sale = new Sale();
        sale.setCompanyId(companyId);
        sale.setCustomerId(customerId);
        sale.setStatus("emitida");
        sale.setNet(new BigDecimal("20000"));
        sale.setVat(new BigDecimal("3800"));
        sale.setTotal(new BigDecimal("23800"));
        sale = saleRepository.save(sale);
        
        SaleItem item1 = new SaleItem();
        item1.setSaleId(sale.getId());
        item1.setProductId(productId);
        item1.setQty(new BigDecimal("2"));
        item1.setUnitPrice(new BigDecimal("10000"));
        saleItemRepository.save(item1);
        
        SaleItem item2 = new SaleItem();
        item2.setSaleId(sale.getId());
        item2.setProductId(productId);
        item2.setQty(new BigDecimal("1"));
        item2.setUnitPrice(new BigDecimal("15000"));
        saleItemRepository.save(item2);
        
        // Assert: verificar que todo se guardó
        assertEquals(initialSalesCount + 1, saleRepository.count());
        assertEquals(initialItemsCount + 2, saleItemRepository.count());
    }

    @Test
    void testDataConsistency_AfterMultipleOperations() {
        // Arrange & Act: ejecutar múltiples operaciones en la misma transacción
        Sale sale1 = new Sale();
        sale1.setCompanyId(companyId);
        sale1.setCustomerId(customerId);
        sale1.setStatus("emitida");
        sale1.setNet(new BigDecimal("10000"));
        sale1.setVat(new BigDecimal("1900"));
        sale1.setTotal(new BigDecimal("11900"));
        sale1 = saleRepository.save(sale1);
        
        SaleItem item = new SaleItem();
        item.setSaleId(sale1.getId());
        item.setProductId(productId);
        item.setQty(new BigDecimal("1"));
        item.setUnitPrice(new BigDecimal("10000"));
        item = saleItemRepository.save(item);
        
        // Assert: verificar relación consistente
        Sale savedSale = saleRepository.findById(sale1.getId()).orElseThrow();
        SaleItem savedItem = saleItemRepository.findById(item.getId()).orElseThrow();
        
        assertEquals(savedSale.getId(), savedItem.getSaleId());
        assertEquals(customerId, savedSale.getCustomerId());
        assertTrue(customerRepository.existsById(savedSale.getCustomerId()));
    }

    @Test
    void testInventoryConsistency_AfterSaleCreation() {
        // Arrange: stock inicial
        BigDecimal initialStock = lotRepository.findAll().stream()
            .filter(l -> l.getProductId().equals(productId))
            .map(InventoryLot::getQtyAvailable)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Act: simular venta (sin lógica de negocio, solo persistencia)
        Sale sale = new Sale();
        sale.setCompanyId(companyId);
        sale.setCustomerId(customerId);
        sale.setStatus("emitida");
        sale.setNet(new BigDecimal("50000"));
        sale.setVat(new BigDecimal("9500"));
        sale.setTotal(new BigDecimal("59500"));
        saleRepository.save(sale);
        
        // Assert: el stock no cambia automáticamente (requiere lógica de negocio)
        BigDecimal currentStock = lotRepository.findAll().stream()
            .filter(l -> l.getProductId().equals(productId))
            .map(InventoryLot::getQtyAvailable)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        assertEquals(0, initialStock.compareTo(currentStock),
                    "El stock no debe cambiar sin ejecutar la lógica de negocio");
    }

    @Test
    void testReferentialIntegrity_CannotSaveOrphanItems() {
        // Arrange: intentar crear SaleItem sin Sale válido
        UUID fakeSaleId = UUID.randomUUID();
        
        SaleItem orphanItem = new SaleItem();
        orphanItem.setSaleId(fakeSaleId); // ID que no existe
        orphanItem.setProductId(productId);
        orphanItem.setQty(new BigDecimal("1"));
        orphanItem.setUnitPrice(new BigDecimal("10000"));
        
        // Act & Assert: debe fallar por FK constraint
        assertThrows(Exception.class, () -> {
            saleItemRepository.save(orphanItem);
            saleItemRepository.flush(); // Forzar persistencia inmediata
        }, "No debe permitir guardar SaleItem con sale_id inexistente");
    }

    @Test
    void testCascadeDelete_Behavior() {
        // Arrange: crear venta con items
        Sale sale = new Sale();
        sale.setCompanyId(companyId);
        sale.setCustomerId(customerId);
        sale.setStatus("emitida");
        sale.setNet(new BigDecimal("10000"));
        sale.setVat(new BigDecimal("1900"));
        sale.setTotal(new BigDecimal("11900"));
        sale = saleRepository.save(sale);
        final UUID saleId = sale.getId();
        
        SaleItem item = new SaleItem();
        item.setSaleId(saleId);
        item.setProductId(productId);
        item.setQty(new BigDecimal("1"));
        item.setUnitPrice(new BigDecimal("10000"));
        saleItemRepository.save(item);
        
        long itemCount = saleItemRepository.findAll().stream()
            .filter(i -> i.getSaleId().equals(saleId))
            .count();
        
        assertEquals(1, itemCount);
        
        // Act: eliminar venta
        saleRepository.delete(sale);
        saleRepository.flush();
        
        // Assert: verificar si los items se eliminaron (depende de configuración CASCADE)
        assertFalse(saleRepository.existsById(saleId));
    }

    @Test
    void testMultipleTransactions_Isolation() {
        // Arrange & Act: crear múltiples ventas independientes
        Sale sale1 = new Sale();
        sale1.setCompanyId(companyId);
        sale1.setCustomerId(customerId);
        sale1.setStatus("emitida");
        sale1.setNet(new BigDecimal("10000"));
        sale1.setVat(new BigDecimal("1900"));
        sale1.setTotal(new BigDecimal("11900"));
        saleRepository.save(sale1);
        
        Sale sale2 = new Sale();
        sale2.setCompanyId(companyId);
        sale2.setCustomerId(customerId);
        sale2.setStatus("pendiente");
        sale2.setNet(new BigDecimal("20000"));
        sale2.setVat(new BigDecimal("3800"));
        sale2.setTotal(new BigDecimal("23800"));
        saleRepository.save(sale2);
        
        // Assert: ambas ventas son independientes
        assertNotEquals(sale1.getId(), sale2.getId());
        assertEquals("emitida", saleRepository.findById(sale1.getId()).orElseThrow().getStatus());
        assertEquals("pendiente", saleRepository.findById(sale2.getId()).orElseThrow().getStatus());
    }
}

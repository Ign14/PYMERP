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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de integración para validar la integridad de datos en ventas.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(com.datakomerz.pymes.config.TestJwtDecoderConfig.class)
public class SalesIntegrationTest {

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
        
        Customer customer = new Customer();
        customer.setCompanyId(companyId);
        customer.setName("Cliente Test");
        customer.setRut("12345678-9");
        customer.setEmail("test@test.com");
        customer = customerRepository.save(customer);
        customerId = customer.getId();
        
        Product product = new Product();
        product.setCompanyId(companyId);
        product.setName("Producto Test");
        product.setSku("TEST-001");
        product.setActive(true);
        product = productRepository.save(product);
        productId = product.getId();
        
        InventoryLot lot = new InventoryLot();
        lot.setCompanyId(companyId);
        lot.setProductId(productId);
        lot.setQtyAvailable(new BigDecimal("100"));
        lot.setCostUnit(new BigDecimal("5000"));
        lotRepository.save(lot);
    }

    @Test
    void testSaleCustomerRelationship() {
        Sale sale = new Sale();
        sale.setCompanyId(companyId);
        sale.setCustomerId(customerId);
        sale.setStatus("emitida");
        sale.setNet(new BigDecimal("20000"));
        sale.setVat(new BigDecimal("3800"));
        sale.setTotal(new BigDecimal("23800"));
        sale.setPaymentTermDays(30);
        sale = saleRepository.save(sale);
        
        Sale saved = saleRepository.findById(sale.getId()).orElseThrow();
        assertEquals(customerId, saved.getCustomerId());
    }

    @Test
    void testSaleItemRelationships() {
        Sale sale = new Sale();
        sale.setCompanyId(companyId);
        sale.setCustomerId(customerId);
        sale.setStatus("emitida");
        sale.setNet(new BigDecimal("30000"));
        sale.setVat(new BigDecimal("5700"));
        sale.setTotal(new BigDecimal("35700"));
        sale.setPaymentTermDays(30);
        sale = saleRepository.save(sale);
        
        SaleItem item = new SaleItem();
        item.setSaleId(sale.getId());
        item.setProductId(productId);
        item.setQty(new BigDecimal("2"));
        item.setUnitPrice(new BigDecimal("10000"));
        saleItemRepository.save(item);
        
        List<SaleItem> items = saleItemRepository.findBySaleId(sale.getId());
        assertEquals(1, items.size());
        assertEquals(sale.getId(), items.get(0).getSaleId());
    }

    @Test
    void testMultiTenancy() {
        Sale sale1 = new Sale();
        sale1.setCompanyId(companyId);
        sale1.setCustomerId(customerId);
        sale1.setStatus("emitida");
        sale1.setNet(new BigDecimal("10000"));
        sale1.setVat(new BigDecimal("1900"));
        sale1.setTotal(new BigDecimal("11900"));
        sale1.setPaymentTermDays(30);
        saleRepository.save(sale1);
        
        UUID company2Id = UUID.randomUUID();
        Customer customer2 = new Customer();
        customer2.setCompanyId(company2Id);
        customer2.setName("Cliente 2");
        customer2.setRut("98765432-1");
        customer2 = customerRepository.save(customer2);
        
        Sale sale2 = new Sale();
        sale2.setCompanyId(company2Id);
        sale2.setCustomerId(customer2.getId());
        sale2.setStatus("emitida");
        sale2.setNet(new BigDecimal("50000"));
        sale2.setVat(new BigDecimal("9500"));
        sale2.setTotal(new BigDecimal("59500"));
        sale2.setPaymentTermDays(30);
        saleRepository.save(sale2);
        
        long company1Sales = saleRepository.findAll().stream()
            .filter(s -> s.getCompanyId().equals(companyId))
            .count();
        
        assertEquals(1, company1Sales);
    }
}

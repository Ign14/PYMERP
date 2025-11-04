package com.datakomerz.pymes.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de integración para validar el esquema de base de datos:
 * 1. Existencia de todas las tablas principales
 * 2. Existencia de foreign keys críticas
 * 3. Tipos de datos correctos (UUID para IDs)
 * 4. Índices para multi-tenancy (companyId)
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(com.datakomerz.pymes.config.TestJwtDecoderConfig.class)
public class DatabaseSchemaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testCriticalTablesExist() {
        // Tablas críticas que deben existir
        String[] requiredTables = {
            "sales", "sale_items", "customers",
            "purchases", "purchase_items", "suppliers",
            "inventory_lots", "inventory_movements", "products",
            "companies", "users"
        };
        
        for (String tableName : requiredTables) {
            String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = 'public' AND table_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
            assertNotNull(count);
            assertEquals(1, count, "La tabla '" + tableName + "' debe existir en el esquema");
        }
    }

    @Test
    void testSalesForeignKeys() {
        // Verificar FK: sales.customer_id → customers.id
        String sql = "SELECT COUNT(*) FROM information_schema.table_constraints tc " +
                    "JOIN information_schema.key_column_usage kcu " +
                    "ON tc.constraint_name = kcu.constraint_name " +
                    "WHERE tc.constraint_type = 'FOREIGN KEY' " +
                    "AND tc.table_name = 'sales' " +
                    "AND kcu.column_name = 'customer_id'";
        
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        assertTrue(count != null && count > 0, 
                  "Debe existir FK: sales.customer_id → customers.id");
    }

    @Test
    void testSaleItemsForeignKeys() {
        // Verificar FK: sale_items.sale_id → sales.id
        String sqlSale = "SELECT COUNT(*) FROM information_schema.table_constraints tc " +
                        "JOIN information_schema.key_column_usage kcu " +
                        "ON tc.constraint_name = kcu.constraint_name " +
                        "WHERE tc.constraint_type = 'FOREIGN KEY' " +
                        "AND tc.table_name = 'sale_items' " +
                        "AND kcu.column_name = 'sale_id'";
        
        Integer countSale = jdbcTemplate.queryForObject(sqlSale, Integer.class);
        assertTrue(countSale != null && countSale > 0, 
                  "Debe existir FK: sale_items.sale_id → sales.id");
        
        // Verificar FK: sale_items.product_id → products.id
        String sqlProduct = "SELECT COUNT(*) FROM information_schema.table_constraints tc " +
                           "JOIN information_schema.key_column_usage kcu " +
                           "ON tc.constraint_name = kcu.constraint_name " +
                           "WHERE tc.constraint_type = 'FOREIGN KEY' " +
                           "AND tc.table_name = 'sale_items' " +
                           "AND kcu.column_name = 'product_id'";
        
        Integer countProduct = jdbcTemplate.queryForObject(sqlProduct, Integer.class);
        assertTrue(countProduct != null && countProduct > 0, 
                  "Debe existir FK: sale_items.product_id → products.id");
    }

    @Test
    void testPurchasesForeignKeys() {
        // Verificar FK: purchases.supplier_id → suppliers.id
        String sql = "SELECT COUNT(*) FROM information_schema.table_constraints tc " +
                    "JOIN information_schema.key_column_usage kcu " +
                    "ON tc.constraint_name = kcu.constraint_name " +
                    "WHERE tc.constraint_type = 'FOREIGN KEY' " +
                    "AND tc.table_name = 'purchases' " +
                    "AND kcu.column_name = 'supplier_id'";
        
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        assertTrue(count != null && count > 0, 
                  "Debe existir FK: purchases.supplier_id → suppliers.id");
    }

    @Test
    void testPurchaseItemsForeignKeys() {
        // Verificar FK: purchase_items.purchase_id → purchases.id
        String sqlPurchase = "SELECT COUNT(*) FROM information_schema.table_constraints tc " +
                            "JOIN information_schema.key_column_usage kcu " +
                            "ON tc.constraint_name = kcu.constraint_name " +
                            "WHERE tc.constraint_type = 'FOREIGN KEY' " +
                            "AND tc.table_name = 'purchase_items' " +
                            "AND kcu.column_name = 'purchase_id'";
        
        Integer countPurchase = jdbcTemplate.queryForObject(sqlPurchase, Integer.class);
        assertTrue(countPurchase != null && countPurchase > 0, 
                  "Debe existir FK: purchase_items.purchase_id → purchases.id");
        
        // Verificar FK: purchase_items.product_id → products.id
        String sqlProduct = "SELECT COUNT(*) FROM information_schema.table_constraints tc " +
                           "JOIN information_schema.key_column_usage kcu " +
                           "ON tc.constraint_name = kcu.constraint_name " +
                           "WHERE tc.constraint_type = 'FOREIGN KEY' " +
                           "AND tc.table_name = 'purchase_items' " +
                           "AND kcu.column_name = 'product_id'";
        
        Integer countProduct = jdbcTemplate.queryForObject(sqlProduct, Integer.class);
        assertTrue(countProduct != null && countProduct > 0, 
                  "Debe existir FK: purchase_items.product_id → products.id");
    }

    @Test
    void testInventoryLotsForeignKeys() {
        // Verificar FK: inventory_lots.product_id → products.id
        String sql = "SELECT COUNT(*) FROM information_schema.table_constraints tc " +
                    "JOIN information_schema.key_column_usage kcu " +
                    "ON tc.constraint_name = kcu.constraint_name " +
                    "WHERE tc.constraint_type = 'FOREIGN KEY' " +
                    "AND tc.table_name = 'inventory_lots' " +
                    "AND kcu.column_name = 'product_id'";
        
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        assertTrue(count != null && count > 0, 
                  "Debe existir FK: inventory_lots.product_id → products.id");
    }

    @Test
    void testUUIDColumnTypes() {
        // Verificar que las columnas ID son de tipo UUID
        String[] tablesToCheck = {
            "sales", "sale_items", "customers", "purchases", 
            "purchase_items", "suppliers", "inventory_lots", "products"
        };
        
        for (String tableName : tablesToCheck) {
            String sql = "SELECT data_type FROM information_schema.columns " +
                        "WHERE table_name = ? AND column_name = 'id'";
            
            String dataType = jdbcTemplate.queryForObject(sql, String.class, tableName);
            assertEquals("uuid", dataType, 
                        "La columna 'id' de '" + tableName + "' debe ser de tipo UUID");
        }
    }

    @Test
    void testCompanyIdColumns() {
        // Verificar que todas las tablas multi-tenant tienen company_id
        String[] multiTenantTables = {
            "sales", "purchases", "customers", "suppliers", 
            "products", "inventory_lots", "inventory_movements"
        };
        
        for (String tableName : multiTenantTables) {
            String sql = "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_name = ? AND column_name = 'company_id'";
            
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
            assertNotNull(count);
            assertEquals(1, count, 
                        "La tabla '" + tableName + "' debe tener columna company_id");
        }
    }

    @Test
    void testCompanyIdIndexes() {
        // Verificar que existen índices en company_id para performance
        String sql = "SELECT tablename, indexname FROM pg_indexes " +
                    "WHERE schemaname = 'public' AND indexdef LIKE '%company_id%'";
        
        List<Map<String, Object>> indexes = jdbcTemplate.queryForList(sql);
        
        assertFalse(indexes.isEmpty(), 
                   "Deben existir índices en company_id para optimizar queries multi-tenant");
        
        // Verificar al menos algunos índices críticos
        boolean hasSalesIndex = indexes.stream()
            .anyMatch(idx -> "sales".equals(idx.get("tablename")));
        boolean hasPurchasesIndex = indexes.stream()
            .anyMatch(idx -> "purchases".equals(idx.get("tablename")));
        
        assertTrue(hasSalesIndex || hasPurchasesIndex, 
                  "Deben existir índices en company_id en tablas críticas");
    }
}

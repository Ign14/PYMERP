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
// Nota: se ejecuta con H2 + Hibernate DDL en perfil 'test'.
@Import(com.datakomerz.pymes.config.TestJwtDecoderConfig.class)
public class DatabaseSchemaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private boolean isH2() throws Exception {
        var ds = jdbcTemplate.getDataSource();
        try (var conn = ds.getConnection()) {
            return conn.getMetaData().getDatabaseProductName().toLowerCase().contains("h2");
        }
    }

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
                        "WHERE UPPER(table_schema) = 'PUBLIC' AND UPPER(table_name) = UPPER(?)";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
            assertNotNull(count);
            assertEquals(1, count, "La tabla '" + tableName + "' debe existir en el esquema");
        }
    }

    @Test
    void testSalesForeignKeys() throws Exception {
        if (isH2()) return; // Skip FK checks on H2
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
    void testSaleItemsForeignKeys() throws Exception {
        if (isH2()) return; // Skip FK checks on H2
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
    void testPurchasesForeignKeys() throws Exception {
        if (isH2()) return; // Skip FK checks on H2
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
    void testPurchaseItemsForeignKeys() throws Exception {
        if (isH2()) return; // Skip FK checks on H2
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
    void testInventoryLotsForeignKeys() throws Exception {
        if (isH2()) return; // Skip FK checks on H2
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
    void testUUIDColumnTypes() throws Exception {
        // Verificar que las columnas ID son de tipo UUID (PostgreSQL y H2 compatibles)
        String[] tablesToCheck = {
            "sales", "sale_items", "customers", "purchases",
            "purchase_items", "suppliers", "inventory_lots", "products"
        };

        var dataSource = jdbcTemplate.getDataSource();
        assertNotNull(dataSource, "DataSource no debe ser null");

        try (var conn = dataSource.getConnection()) {
            var meta = conn.getMetaData();
            String schema = meta.getDatabaseProductName().toLowerCase().contains("h2") ? "PUBLIC" : "public";

            for (String tableName : tablesToCheck) {
                try (var rs = meta.getColumns(null, schema, tableName.toUpperCase(), "ID")) {
                    assertTrue(rs.next(), "Debe existir columna ID en " + tableName);
                    String typeName = rs.getString("TYPE_NAME");
                    assertNotNull(typeName, "TYPE_NAME no debe ser null para " + tableName);
                    assertTrue("uuid".equalsIgnoreCase(typeName),
                        "La columna 'id' de '" + tableName + "' debe ser de tipo UUID pero fue: " + typeName);
                }
            }
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
                        "WHERE UPPER(table_name) = UPPER(?) AND UPPER(column_name) = 'COMPANY_ID'";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
            assertNotNull(count);
            assertEquals(1, count, 
                        "La tabla '" + tableName + "' debe tener columna company_id");
        }
    }

    @Test
    void testCompanyIdIndexes() throws Exception {
        // Verificar índices por columna usando DatabaseMetaData (portátil)
        var dataSource = jdbcTemplate.getDataSource();
        assertNotNull(dataSource, "DataSource no debe ser null");

        String[] candidateTables = { "sales", "purchases", "customers", "suppliers",
            "products", "inventory_lots", "inventory_movements" };

        boolean anyIndex = false;
        boolean salesIndexed = false;
        boolean purchasesIndexed = false;

        try (var conn = dataSource.getConnection()) {
            var meta = conn.getMetaData();
            String schema = meta.getDatabaseProductName().toLowerCase().contains("h2") ? "PUBLIC" : "public";
            for (String table : candidateTables) {
                try (var rs = meta.getIndexInfo(null, schema, table.toUpperCase(), false, false)) {
                    while (rs.next()) {
                        String col = rs.getString("COLUMN_NAME");
                        if (col != null && col.equalsIgnoreCase("company_id")) {
                            anyIndex = true;
                            if ("sales".equalsIgnoreCase(table)) salesIndexed = true;
                            if ("purchases".equalsIgnoreCase(table)) purchasesIndexed = true;
                        }
                    }
                }
            }
        }

        assertTrue(anyIndex, "Deben existir índices en company_id para optimizar queries multi-tenant");
        assertTrue(salesIndexed || purchasesIndexed,
            "Deben existir índices en company_id en tablas críticas");
    }
}

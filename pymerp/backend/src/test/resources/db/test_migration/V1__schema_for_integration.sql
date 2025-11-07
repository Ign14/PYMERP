-- Minimal H2-compatible schema for DatabaseSchemaIntegrationTest

CREATE TABLE companies (
  id UUID PRIMARY KEY,
  name VARCHAR(100)
);

CREATE TABLE customers (
  id UUID PRIMARY KEY,
  company_id UUID NOT NULL,
  name VARCHAR(100)
);

CREATE TABLE suppliers (
  id UUID PRIMARY KEY,
  company_id UUID NOT NULL,
  name VARCHAR(100)
);

CREATE TABLE products (
  id UUID PRIMARY KEY,
  company_id UUID NOT NULL,
  name VARCHAR(120)
);

CREATE TABLE sales (
  id UUID PRIMARY KEY,
  company_id UUID NOT NULL,
  customer_id UUID,
  status VARCHAR(20)
);

CREATE TABLE sale_items (
  id UUID PRIMARY KEY,
  company_id UUID NOT NULL,
  sale_id UUID NOT NULL,
  product_id UUID
);

CREATE TABLE purchases (
  id UUID PRIMARY KEY,
  company_id UUID NOT NULL,
  supplier_id UUID
);

CREATE TABLE purchase_items (
  id UUID PRIMARY KEY,
  company_id UUID NOT NULL,
  purchase_id UUID NOT NULL,
  product_id UUID
);

CREATE TABLE inventory_lots (
  id UUID PRIMARY KEY,
  company_id UUID NOT NULL,
  product_id UUID NOT NULL
);

CREATE TABLE inventory_movements (
  id UUID PRIMARY KEY,
  company_id UUID NOT NULL,
  inventory_lot_id UUID NOT NULL
);

-- FKs expected by the integration tests
ALTER TABLE sales
  ADD CONSTRAINT fk_sales_customer
  FOREIGN KEY (customer_id) REFERENCES customers(id);

ALTER TABLE sale_items
  ADD CONSTRAINT fk_sale_items_sale
  FOREIGN KEY (sale_id) REFERENCES sales(id);

ALTER TABLE sale_items
  ADD CONSTRAINT fk_sale_items_product
  FOREIGN KEY (product_id) REFERENCES products(id);

ALTER TABLE purchases
  ADD CONSTRAINT fk_purchases_supplier
  FOREIGN KEY (supplier_id) REFERENCES suppliers(id);

ALTER TABLE purchase_items
  ADD CONSTRAINT fk_purchase_items_purchase
  FOREIGN KEY (purchase_id) REFERENCES purchases(id);

ALTER TABLE purchase_items
  ADD CONSTRAINT fk_purchase_items_product
  FOREIGN KEY (product_id) REFERENCES products(id);

ALTER TABLE inventory_lots
  ADD CONSTRAINT fk_inventory_lots_product
  FOREIGN KEY (product_id) REFERENCES products(id);

-- Performance indexes on company_id for key tables
CREATE INDEX idx_sales_company ON sales(company_id);
CREATE INDEX idx_purchases_company ON purchases(company_id);


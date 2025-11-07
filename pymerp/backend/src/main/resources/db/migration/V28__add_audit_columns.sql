-- Sprint 8: JPA Auditing - Add audit columns to entities

-- Products
ALTER TABLE products ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE products ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

UPDATE products SET created_by = 'system' WHERE created_by IS NULL;
UPDATE products SET updated_by = 'system' WHERE updated_by IS NULL;

ALTER TABLE products ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE products ALTER COLUMN updated_by SET NOT NULL;

-- Customers
ALTER TABLE customers ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

UPDATE customers SET created_by = 'system' WHERE created_by IS NULL;
UPDATE customers SET updated_by = 'system' WHERE updated_by IS NULL;

ALTER TABLE customers ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE customers ALTER COLUMN updated_by SET NOT NULL;

-- Sales
ALTER TABLE sales ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE sales ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

UPDATE sales SET created_at = COALESCE(created_at, issued_at, NOW()) WHERE created_at IS NULL;
UPDATE sales SET updated_at = COALESCE(updated_at, issued_at, NOW()) WHERE updated_at IS NULL;
UPDATE sales SET created_by = 'system' WHERE created_by IS NULL;
UPDATE sales SET updated_by = 'system' WHERE updated_by IS NULL;

ALTER TABLE sales ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE sales ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE sales ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE sales ALTER COLUMN updated_by SET NOT NULL;

-- Purchases
ALTER TABLE purchases ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE purchases ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE purchases ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;

UPDATE purchases SET updated_at = COALESCE(updated_at, created_at, NOW()) WHERE updated_at IS NULL;
UPDATE purchases SET created_by = 'system' WHERE created_by IS NULL;
UPDATE purchases SET updated_by = 'system' WHERE updated_by IS NULL;

ALTER TABLE purchases ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN updated_by SET NOT NULL;

-- Suppliers
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;

UPDATE suppliers SET updated_at = COALESCE(updated_at, created_at, NOW()) WHERE updated_at IS NULL;
UPDATE suppliers SET created_by = 'system' WHERE created_by IS NULL;
UPDATE suppliers SET updated_by = 'system' WHERE updated_by IS NULL;

ALTER TABLE suppliers ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE suppliers ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE suppliers ALTER COLUMN updated_by SET NOT NULL;

-- Fiscal Documents
ALTER TABLE fiscal_documents ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE fiscal_documents ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE fiscal_documents ADD COLUMN IF NOT EXISTS company_id UUID;

UPDATE fiscal_documents fd
SET company_id = s.company_id
FROM sales s
WHERE fd.company_id IS NULL
  AND fd.sale_id = s.id;

UPDATE fiscal_documents SET created_by = 'system' WHERE created_by IS NULL;
UPDATE fiscal_documents SET updated_by = 'system' WHERE updated_by IS NULL;

ALTER TABLE fiscal_documents ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE fiscal_documents ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE fiscal_documents ALTER COLUMN updated_by SET NOT NULL;

-- Indexes for auditing queries
CREATE INDEX IF NOT EXISTS idx_products_created_by ON products(created_by);
CREATE INDEX IF NOT EXISTS idx_customers_created_by ON customers(created_by);
CREATE INDEX IF NOT EXISTS idx_sales_created_by ON sales(created_by);
CREATE INDEX IF NOT EXISTS idx_purchases_created_by ON purchases(created_by);
CREATE INDEX IF NOT EXISTS idx_suppliers_created_by ON suppliers(created_by);
CREATE INDEX IF NOT EXISTS idx_fiscal_docs_created_by ON fiscal_documents(created_by);
CREATE INDEX IF NOT EXISTS idx_fiscal_docs_company ON fiscal_documents(company_id);

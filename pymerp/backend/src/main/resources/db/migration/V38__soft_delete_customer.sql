-- Guarantee customers table honors soft-delete flag and index for active tenants

UPDATE customers
SET active = true
WHERE active IS NULL;

ALTER TABLE customers
  ALTER COLUMN active SET DEFAULT true;

ALTER TABLE customers
  ALTER COLUMN active SET NOT NULL;

DROP INDEX IF EXISTS idx_customers_active;
CREATE INDEX idx_customers_company_active_true
  ON customers(company_id, active)
  WHERE active = true;

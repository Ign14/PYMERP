-- Add new fields to customers table
ALTER TABLE customers ADD COLUMN rut VARCHAR(20);
ALTER TABLE customers ADD COLUMN contact_person VARCHAR(120);
ALTER TABLE customers ADD COLUMN notes TEXT;
ALTER TABLE customers ADD COLUMN active BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE customers ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE customers ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW();

-- Create index for rut lookup
CREATE INDEX idx_customers_rut ON customers(company_id, rut) WHERE rut IS NOT NULL;

-- Create index for active customers
CREATE INDEX idx_customers_active ON customers(company_id, active);

-- Create index for created_at for sorting
CREATE INDEX idx_customers_created_at ON customers(company_id, created_at DESC);

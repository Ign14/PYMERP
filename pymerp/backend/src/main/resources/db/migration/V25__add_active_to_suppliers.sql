-- Add active column to suppliers table
ALTER TABLE suppliers
ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

-- Create index for filtering by active status
CREATE INDEX idx_suppliers_active ON suppliers(company_id, active);

-- Update existing records to be active
UPDATE suppliers SET active = TRUE WHERE active IS NULL;

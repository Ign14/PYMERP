-- Add customer segmentation support
ALTER TABLE customers ADD COLUMN IF NOT EXISTS segment varchar(64);
CREATE INDEX IF NOT EXISTS ix_customers_segment ON customers(segment);

-- Add received_at field to purchases table
ALTER TABLE purchases 
ADD COLUMN received_at TIMESTAMP WITH TIME ZONE;

-- Set default value for existing records to match issued_at
UPDATE purchases 
SET received_at = issued_at 
WHERE received_at IS NULL;

-- Add comment
COMMENT ON COLUMN purchases.received_at IS 'Timestamp when the purchase was physically received';

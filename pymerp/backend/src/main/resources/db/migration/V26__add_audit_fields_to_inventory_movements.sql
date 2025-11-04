-- Add audit and traceability fields to inventory_movements table
ALTER TABLE inventory_movements
ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS user_ip VARCHAR(45),
ADD COLUMN IF NOT EXISTS reason_code VARCHAR(50),
ADD COLUMN IF NOT EXISTS previous_qty DECIMAL(14,3),
ADD COLUMN IF NOT EXISTS new_qty DECIMAL(14,3);

-- Create index for audit queries
CREATE INDEX IF NOT EXISTS idx_inventory_movements_created_by ON inventory_movements(created_by);
CREATE INDEX IF NOT EXISTS idx_inventory_movements_created_at ON inventory_movements(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_inventory_movements_type ON inventory_movements(type);

-- Add comment for documentation
COMMENT ON COLUMN inventory_movements.created_by IS 'Usuario que realizó el movimiento (email o username)';
COMMENT ON COLUMN inventory_movements.user_ip IS 'Dirección IP del usuario';
COMMENT ON COLUMN inventory_movements.reason_code IS 'Código de razón del movimiento (DAMAGED, EXPIRED, ADJUSTMENT, etc.)';
COMMENT ON COLUMN inventory_movements.previous_qty IS 'Cantidad anterior del lote';
COMMENT ON COLUMN inventory_movements.new_qty IS 'Cantidad nueva del lote después del movimiento';

-- Añade trazabilidad adicional a inventory_movements
ALTER TABLE inventory_movements
  ADD COLUMN IF NOT EXISTS location_from_id UUID,
  ADD COLUMN IF NOT EXISTS location_to_id UUID,
  ADD COLUMN IF NOT EXISTS trace_id UUID;

CREATE INDEX IF NOT EXISTS idx_inventory_movements_location_from ON inventory_movements(location_from_id);
CREATE INDEX IF NOT EXISTS idx_inventory_movements_location_to ON inventory_movements(location_to_id);
CREATE INDEX IF NOT EXISTS idx_inventory_movements_trace_id ON inventory_movements(trace_id);

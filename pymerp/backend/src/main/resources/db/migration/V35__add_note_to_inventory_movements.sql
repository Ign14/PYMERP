-- V35: Agregar columna 'note' a inventory_movements
-- Fix para compatibilidad con entidad Java InventoryMovement

ALTER TABLE inventory_movements 
ADD COLUMN IF NOT EXISTS note TEXT;

-- Crear índice para búsquedas por nota (opcional, útil para auditoría)
CREATE INDEX IF NOT EXISTS idx_inventory_movements_note ON inventory_movements(note) 
WHERE note IS NOT NULL;

COMMENT ON COLUMN inventory_movements.note IS 'Nota o comentario adicional sobre el movimiento de inventario';

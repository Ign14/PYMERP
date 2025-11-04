-- Agregar columna location_id a inventory_lots
ALTER TABLE inventory_lots
ADD COLUMN location_id UUID;

-- Agregar foreign key constraint
ALTER TABLE inventory_lots
ADD CONSTRAINT fk_inventory_lot_location 
FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE SET NULL;

-- Crear índice para mejorar el rendimiento de consultas por ubicación
CREATE INDEX idx_inventory_lots_location_id ON inventory_lots(location_id);

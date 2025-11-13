-- V33: Mejorar lógica de ubicaciones con capacidad y estado
-- Permite gestionar ubicaciones con límites de capacidad y control de estado

-- Agregar campos para control de estado y capacidad usando DO blocks
DO $$
BEGIN
  -- active
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                 WHERE table_name='locations' AND column_name='active') THEN
    ALTER TABLE locations ADD COLUMN active BOOLEAN DEFAULT TRUE NOT NULL;
  END IF;
  
  -- is_blocked
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                 WHERE table_name='locations' AND column_name='is_blocked') THEN
    ALTER TABLE locations ADD COLUMN is_blocked BOOLEAN DEFAULT FALSE NOT NULL;
  END IF;
  
  -- capacity
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                 WHERE table_name='locations' AND column_name='capacity') THEN
    ALTER TABLE locations ADD COLUMN capacity DECIMAL(14,3);
  END IF;
  
  -- capacity_unit
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                 WHERE table_name='locations' AND column_name='capacity_unit') THEN
    ALTER TABLE locations ADD COLUMN capacity_unit VARCHAR(20) DEFAULT 'UNITS';
  END IF;
END $$;

-- Índices para mejorar consultas (solo si no existen)
CREATE INDEX IF NOT EXISTS idx_locations_active ON locations(active);
-- NOTA: idx_locations_type ya existe desde V20, no lo recreamos
-- NOTA: idx_locations_parent_location_id ya existe desde V20, no necesitamos otro

-- Comentarios
COMMENT ON COLUMN locations.active IS 'Indica si la ubicación está activa y puede recibir stock';
COMMENT ON COLUMN locations.is_blocked IS 'Ubicación bloqueada temporalmente (mantenimiento, inventario, etc)';
COMMENT ON COLUMN locations.capacity IS 'Capacidad máxima de la ubicación (opcional)';
COMMENT ON COLUMN locations.capacity_unit IS 'Unidad de medida de capacidad: UNITS, PALLETS, CUBIC_METERS, etc';

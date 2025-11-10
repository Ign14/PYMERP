-- V33: Mejorar lógica de ubicaciones con capacidad y estado
-- Permite gestionar ubicaciones con límites de capacidad y control de estado

-- Agregar campos para control de estado y capacidad
ALTER TABLE locations
ADD COLUMN active BOOLEAN DEFAULT TRUE NOT NULL,
ADD COLUMN is_blocked BOOLEAN DEFAULT FALSE NOT NULL,
ADD COLUMN capacity DECIMAL(14,3),
ADD COLUMN capacity_unit VARCHAR(20) DEFAULT 'UNITS';

-- Índices para mejorar consultas
CREATE INDEX idx_locations_active ON locations(active);
CREATE INDEX idx_locations_parent ON locations(parent_location_id) WHERE parent_location_id IS NOT NULL;
CREATE INDEX idx_locations_type ON locations(type);

-- Comentarios
COMMENT ON COLUMN locations.active IS 'Indica si la ubicación está activa y puede recibir stock';
COMMENT ON COLUMN locations.is_blocked IS 'Ubicación bloqueada temporalmente (mantenimiento, inventario, etc)';
COMMENT ON COLUMN locations.capacity IS 'Capacidad máxima de la ubicación (opcional)';
COMMENT ON COLUMN locations.capacity_unit IS 'Unidad de medida de capacidad: UNITS, PALLETS, CUBIC_METERS, etc';

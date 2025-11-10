-- Agregar campos de nombre fantasía y logo a la tabla companies
ALTER TABLE companies ADD COLUMN fantasy_name VARCHAR(160);
ALTER TABLE companies ADD COLUMN logo_url VARCHAR(500);

-- Crear tabla para ubicaciones padre de empresas
CREATE TABLE IF NOT EXISTS company_parent_locations (
  id UUID PRIMARY KEY,
  company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
  name VARCHAR(160) NOT NULL,
  code VARCHAR(60) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_company_parent_location_code UNIQUE (company_id, code)
);

CREATE INDEX idx_company_parent_locations_company ON company_parent_locations(company_id);

-- Agregar campo parent_location_id a locations
ALTER TABLE locations
ADD COLUMN parent_location_id UUID REFERENCES company_parent_locations(id) ON DELETE SET NULL;

CREATE INDEX idx_locations_parent ON locations(parent_location_id);

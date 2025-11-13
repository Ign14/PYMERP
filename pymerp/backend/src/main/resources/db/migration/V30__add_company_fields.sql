-- Agregar campos de nombre fantasía a la tabla companies
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                 WHERE table_name='companies' AND column_name='fantasy_name') THEN
    ALTER TABLE companies ADD COLUMN fantasy_name VARCHAR(160);
  END IF;
END $$;

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

-- NOTA: parent_location_id ya existe en locations desde V20
-- No es necesario agregarlo nuevamente

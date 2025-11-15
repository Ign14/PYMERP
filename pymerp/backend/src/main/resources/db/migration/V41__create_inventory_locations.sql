-- Requested as V38__create_inventory_locations.sql but bumped to V41 to avoid version conflicts.
CREATE TABLE inventory_locations (
  id UUID PRIMARY KEY,
  company_id UUID NOT NULL,
  code VARCHAR(50),
  name VARCHAR(100) NOT NULL,
  description VARCHAR(255),
  enabled BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_inventory_locations_company_name
  ON inventory_locations(company_id, name);

CREATE UNIQUE INDEX uq_inventory_locations_company_code
  ON inventory_locations(company_id, code)
  WHERE code IS NOT NULL;

INSERT INTO inventory_locations (
  id, company_id, code, name, description, enabled, created_at, updated_at
) SELECT DISTINCT ON (company_id, name)
  id,
  company_id,
  code,
  name,
  description,
  CASE WHEN status = 'ACTIVE' THEN true ELSE false END,
  NOW(),
  NOW()
FROM locations
ORDER BY company_id, name, id;

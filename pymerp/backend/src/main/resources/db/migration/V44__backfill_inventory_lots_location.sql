-- Backfill location_id para lotes existentes asignando ubicación default por tenant
-- Crear ubicaciones DEFAULT solo para companies que tengan lotes sin ubicación y no tengan ya un DEFAULT

INSERT INTO inventory_locations (id, company_id, code, name, description, enabled, created_at, updated_at)
SELECT 
  gen_random_uuid() AS id,
  subq.company_id,
  'DEFAULT' AS code,
  'Ubicación por defecto' AS name,
  'Creada automáticamente durante migración para lotes sin ubicación asignada' AS description,
  true AS enabled,
  NOW() AS created_at,
  NOW() AS updated_at
FROM (
  SELECT DISTINCT il.company_id
  FROM inventory_lots il
  WHERE il.location_id IS NULL
    AND NOT EXISTS (
      SELECT 1 
      FROM inventory_locations loc 
      WHERE loc.company_id = il.company_id 
        AND loc.code = 'DEFAULT'
    )
) AS subq;

-- Asignar ubicación DEFAULT a todos los lotes que no tienen ubicación
UPDATE inventory_lots il
SET location_id = (
  SELECT loc.id 
  FROM inventory_locations loc
  WHERE loc.company_id = il.company_id 
    AND loc.code = 'DEFAULT' 
  LIMIT 1
)
WHERE il.location_id IS NULL;

-- Verificación futura: ALTER TABLE inventory_lots ALTER COLUMN location_id SET NOT NULL;

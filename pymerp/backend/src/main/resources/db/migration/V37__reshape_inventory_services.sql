-- Align locations table with new domain fields
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'locations' AND column_name = 'parent_location_id'
    ) THEN
        ALTER TABLE locations DROP CONSTRAINT IF EXISTS fk_location_parent;
        ALTER TABLE locations DROP COLUMN parent_location_id;
    END IF;
END $$;

ALTER TABLE locations ADD COLUMN IF NOT EXISTS business_name VARCHAR(255);
ALTER TABLE locations ADD COLUMN IF NOT EXISTS rut VARCHAR(20);
ALTER TABLE locations ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';

DO $$
DECLARE
    has_active BOOLEAN;
    has_blocked BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'locations' AND column_name = 'active'
    ) INTO has_active;

    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'locations' AND column_name = 'is_blocked'
    ) INTO has_blocked;

    IF has_blocked THEN
        UPDATE locations SET status = 'BLOCKED' WHERE is_blocked IS TRUE;
    END IF;

    IF has_active THEN
        UPDATE locations SET status = 'BLOCKED' WHERE active IS FALSE;
    END IF;

    UPDATE locations SET status = COALESCE(status, 'ACTIVE');
END $$;

ALTER TABLE locations ALTER COLUMN status SET NOT NULL;

ALTER TABLE locations ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE locations ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

UPDATE locations SET created_by = COALESCE(created_by, 'system');
UPDATE locations SET updated_by = COALESCE(updated_by, 'system');

ALTER TABLE locations ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE locations ALTER COLUMN updated_by SET NOT NULL;

UPDATE locations SET type = 'BODEGA' WHERE type = 'WAREHOUSE';
UPDATE locations SET type = 'LOCAL' WHERE type = 'SHELF';
UPDATE locations SET type = 'CONTAINER' WHERE type = 'BIN';

ALTER TABLE locations DROP COLUMN IF EXISTS capacity;
ALTER TABLE locations DROP COLUMN IF EXISTS capacity_unit;
ALTER TABLE locations DROP COLUMN IF EXISTS active;
ALTER TABLE locations DROP COLUMN IF EXISTS is_blocked;

DROP INDEX IF EXISTS idx_locations_parent_location_id;
DROP INDEX IF EXISTS idx_locations_active;
CREATE INDEX IF NOT EXISTS idx_locations_status ON locations(status);

-- Align services table with new auditing + pricing fields
ALTER TABLE services ADD COLUMN IF NOT EXISTS category VARCHAR(120);
ALTER TABLE services ADD COLUMN IF NOT EXISTS unit_price NUMERIC(14,2) DEFAULT 0;
ALTER TABLE services ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE services ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE services ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'services' AND column_name = 'active'
    ) THEN
        UPDATE services
        SET status = CASE
            WHEN active IS FALSE THEN 'INACTIVE'
            ELSE 'ACTIVE'
        END;
    ELSE
        UPDATE services SET status = COALESCE(status, 'ACTIVE');
    END IF;
END $$;

UPDATE services SET unit_price = COALESCE(unit_price, 0);
UPDATE services SET category = COALESCE(category, 'General');
UPDATE services SET created_by = COALESCE(created_by, 'system');
UPDATE services SET updated_by = COALESCE(updated_by, 'system');

ALTER TABLE services ALTER COLUMN unit_price SET NOT NULL;
ALTER TABLE services ALTER COLUMN status SET NOT NULL;
ALTER TABLE services ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE services ALTER COLUMN updated_by SET NOT NULL;

ALTER TABLE services DROP COLUMN IF EXISTS last_purchase_date;
ALTER TABLE services DROP COLUMN IF EXISTS active;

DROP INDEX IF EXISTS idx_service_active;
CREATE INDEX IF NOT EXISTS idx_service_status ON services(status);

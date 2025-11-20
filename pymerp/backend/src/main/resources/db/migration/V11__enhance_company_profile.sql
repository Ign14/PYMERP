ALTER TABLE companies RENAME COLUMN name TO business_name;
ALTER TABLE companies RENAME COLUMN industry TO business_activity;
ALTER TABLE companies RENAME COLUMN receipt_footer TO receipt_footer_message;

ALTER TABLE companies ALTER COLUMN business_name TYPE VARCHAR(160);
ALTER TABLE companies ALTER COLUMN business_activity TYPE VARCHAR(120);
ALTER TABLE companies ALTER COLUMN rut TYPE VARCHAR(20);
ALTER TABLE companies ALTER COLUMN receipt_footer_message TYPE TEXT;

ALTER TABLE companies ADD COLUMN address VARCHAR(160);
ALTER TABLE companies ADD COLUMN commune VARCHAR(80);
ALTER TABLE companies ADD COLUMN phone VARCHAR(40);
ALTER TABLE companies ADD COLUMN email VARCHAR(160);
ALTER TABLE companies ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;

WITH formatted AS (
  SELECT
    id,
    UPPER(REGEXP_REPLACE(COALESCE(rut, ''), '[^0-9K]', '', 'g')) AS cleaned
  FROM companies
)
UPDATE companies c
SET rut = CASE
    WHEN LENGTH(f.cleaned) >= 2 THEN
      SUBSTRING(f.cleaned FROM 1 FOR LENGTH(f.cleaned) - 1) || '-' || RIGHT(f.cleaned, 1)
    ELSE NULL
  END
FROM formatted f
WHERE c.id = f.id;

DO $$
DECLARE
  rec RECORD;
  seq INTEGER := 0;
  body TEXT;
  sum INTEGER;
  factor INTEGER;
  digit INTEGER;
  modulus INTEGER;
  dv TEXT;
BEGIN
  FOR rec IN SELECT id FROM companies WHERE rut IS NULL OR BTRIM(rut) = '' LOOP
    body := LPAD((76000000 + seq)::TEXT, 8, '0');
    seq := seq + 1;
    sum := 0;
    factor := 2;
    FOR i IN REVERSE 1..LENGTH(body) LOOP
      digit := SUBSTRING(body FROM i FOR 1)::INTEGER;
      sum := sum + digit * factor;
      IF factor = 7 THEN
        factor := 2;
      ELSE
        factor := factor + 1;
      END IF;
    END LOOP;
    modulus := 11 - (sum % 11);
    IF modulus = 11 THEN
      dv := '0';
    ELSIF modulus = 10 THEN
      dv := 'K';
    ELSE
      dv := modulus::TEXT;
    END IF;
    UPDATE companies SET rut = body || '-' || dv WHERE id = rec.id;
  END LOOP;
END $$;

UPDATE companies
SET business_name = 'Compañía sin nombre'
WHERE business_name IS NULL OR BTRIM(business_name) = '';

UPDATE companies
SET updated_at = COALESCE(updated_at, created_at, NOW());

UPDATE companies
SET email = LOWER(email)
WHERE email IS NOT NULL;

ALTER TABLE companies
  ALTER COLUMN business_name SET NOT NULL,
  ALTER COLUMN rut SET NOT NULL,
  ALTER COLUMN updated_at SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_companies_rut ON companies (rut);

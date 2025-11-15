ALTER TABLE purchase_items
  ADD COLUMN IF NOT EXISTS service_id UUID;

ALTER TABLE purchase_items
  ALTER COLUMN product_id DROP NOT NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.table_constraints
    WHERE table_name = 'purchase_items'
      AND constraint_name = 'fk_purchase_items_service'
  ) THEN
    ALTER TABLE purchase_items
      ADD CONSTRAINT fk_purchase_items_service
      FOREIGN KEY (service_id) REFERENCES services(id);
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.table_constraints
    WHERE table_name = 'purchase_items'
      AND constraint_name = 'ck_purchase_item_product_or_service'
  ) THEN
    ALTER TABLE purchase_items
      ADD CONSTRAINT ck_purchase_item_product_or_service
      CHECK (
        (product_id IS NOT NULL AND service_id IS NULL)
        OR (product_id IS NULL AND service_id IS NOT NULL)
      );
  END IF;
END $$;

-- Requested as V39__add_location_to_inventory_lots.sql but advanced to V42 to honor existing migrations.
ALTER TABLE inventory_lots
  DROP CONSTRAINT IF EXISTS fk_inventory_lot_location;

ALTER TABLE inventory_lots
  ADD CONSTRAINT fk_inventory_lot_inventory_locations
  FOREIGN KEY (location_id) REFERENCES inventory_locations(id)
  ON DELETE SET NULL;

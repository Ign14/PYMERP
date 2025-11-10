-- V32: Agregar referencia de compra a lotes de inventario
-- Permite rastrear el origen de cada lote (qué factura de compra lo generó)

ALTER TABLE inventory_lots
ADD COLUMN purchase_id UUID;

-- Índice para mejorar consultas por compra
CREATE INDEX idx_inventory_lots_purchase_id ON inventory_lots(purchase_id);

-- Foreign key opcional (no todos los lotes vienen de compras, pueden ser ajustes)
ALTER TABLE inventory_lots
ADD CONSTRAINT fk_inventory_lots_purchase
FOREIGN KEY (purchase_id) REFERENCES purchases(id)
ON DELETE SET NULL;

COMMENT ON COLUMN inventory_lots.purchase_id IS 'ID de la factura de compra que originó este lote (NULL si es ajuste manual)';

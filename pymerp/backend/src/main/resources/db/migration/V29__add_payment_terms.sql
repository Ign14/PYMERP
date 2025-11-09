-- V29: Agregar términos de pago a ventas y compras
-- Autor: Sistema
-- Fecha: 2025-11-08
-- Descripción: Soporte para términos de cobro/pago en documentos de venta y compra

-- Agregar columna payment_term_days a sales
ALTER TABLE sales 
ADD COLUMN payment_term_days INTEGER NOT NULL DEFAULT 30;

-- Constraint para valores permitidos: 7, 15, 30, 60 días
ALTER TABLE sales
ADD CONSTRAINT chk_sales_payment_term_days 
CHECK (payment_term_days IN (7, 15, 30, 60));

-- Comentario explicativo
COMMENT ON COLUMN sales.payment_term_days IS 'Término de cobro en días: 7, 15, 30 o 60';

-- Agregar columna payment_term_days a purchases
ALTER TABLE purchases
ADD COLUMN payment_term_days INTEGER NOT NULL DEFAULT 30;

-- Constraint para valores permitidos: 7, 15, 30, 60 días
ALTER TABLE purchases
ADD CONSTRAINT chk_purchases_payment_term_days 
CHECK (payment_term_days IN (7, 15, 30, 60));

-- Comentario explicativo
COMMENT ON COLUMN purchases.payment_term_days IS 'Término de pago en días: 7, 15, 30 o 60';

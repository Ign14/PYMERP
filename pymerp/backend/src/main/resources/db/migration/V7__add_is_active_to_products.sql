-- Migración para agregar la columna is_active a la tabla products
ALTER TABLE products ADD is_active BOOLEAN DEFAULT true;
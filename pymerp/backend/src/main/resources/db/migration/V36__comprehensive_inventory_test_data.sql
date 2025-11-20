-- V36: Datos de prueba simplificados (proveedores, clientes, productos con stock crítico)
-- Simplificado para evitar complejidades de purchases/sales con sus documentos fiscales

DO $$
DECLARE
    v_company_id UUID;
BEGIN
    SELECT id INTO v_company_id FROM companies LIMIT 1;

    IF v_company_id IS NULL THEN
        RAISE NOTICE 'No company found';
        RETURN;
    END IF;

    -- 1. PROVEEDORES
    INSERT INTO suppliers (company_id, rut, name, email, phone, address, commune, created_by, updated_by, created_at, updated_at)
    VALUES
        (v_company_id, '76123456-7', 'Tech Supply SpA', 'ventas@techsupply.cl', '+56912345678', 'Av. Providencia 1234', 'Santiago', 'ADMIN', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        (v_company_id, '76987654-3', 'Office Solutions Ltda', 'contacto@officesol.cl', '+56987654321', 'Los Leones 890', 'Providencia', 'ADMIN', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        (v_company_id, '77555666-4', 'Importadora Global SA', 'info@impglobal.cl', '+56911223344', 'Apoquindo 3000', 'Las Condes', 'ADMIN', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

    RAISE NOTICE '✓ 3 proveedores creados';

    -- 2. CLIENTES
    INSERT INTO customers (company_id, rut, name, email, phone, address, created_by, updated_by)
    VALUES
        (v_company_id, '12345678-9', 'Juan Pérez García', 'juan.perez@email.cl', '+56911111111', 'Alameda 500, Santiago', 'ADMIN', 'ADMIN'),
        (v_company_id, '98765432-1', 'María González López', 'maria.gonzalez@email.cl', '+56922222222', 'Vitacura 2000', 'ADMIN', 'ADMIN'),
        (v_company_id, '11223344-5', 'Empresa ABC Ltda', 'contacto@abc.cl', '+56933333333', 'Av. Libertador 1500', 'ADMIN', 'ADMIN'),
        (v_company_id, '55667788-9', 'Corporación XYZ SA', 'ventas@xyz.cl', '+56944444444', 'El Bosque 100', 'ADMIN', 'ADMIN'),
        (v_company_id, '77889900-2', 'Pedro Ramírez S.', 'pedro.r@email.cl', '+56955555555', 'Los Carrera 800', 'ADMIN', 'ADMIN');

    RAISE NOTICE '✓ 5 clientes creados';

    -- 3. PRODUCTOS ADICIONALES CON STOCKS CRÍTICOS
    INSERT INTO products (company_id, sku, name, description, category, barcode, critical_stock, created_by, updated_by)
    VALUES
        (v_company_id, 'PROD-021', 'Notebook HP ProBook 450', 'Notebook empresarial i5 16GB', 'Electrónica', 'HP-PRO-450', 5, 'ADMIN', 'ADMIN'),
        (v_company_id, 'PROD-022', 'Proyector Epson EB-X05', 'Proyector 3300 lúmenes', 'Electrónica', 'EPSON-X05', 2, 'ADMIN', 'ADMIN'),
        (v_company_id, 'PROD-023', 'Pizarra Blanca 120x90', 'Pizarra magnética', 'Mobiliario', 'PIZ-120-90', 3, 'ADMIN', 'ADMIN'),
        (v_company_id, 'PROD-024', 'Resma Papel A4 75g', 'Resma 500 hojas', 'Papelería', 'RESMA-A4-500', 20, 'ADMIN', 'ADMIN'),
        (v_company_id, 'PROD-025', 'Tóner HP 85A Original', 'Tóner negro 1600 pág', 'Consumibles', 'HP-85A', 8, 'ADMIN', 'ADMIN'),
        (v_company_id, 'PROD-026', 'Silla Ergonómica Pro', 'Silla oficina con ajuste lumbar', 'Mobiliario', 'SILLA-PRO-001', 4, 'ADMIN', 'ADMIN'),
        (v_company_id, 'PROD-027', 'Monitor LG 27 4K', 'Monitor IPS 27 pulgadas 4K', 'Electrónica', 'LG-27-4K', 3, 'ADMIN', 'ADMIN'),
        (v_company_id, 'PROD-028', 'Escritorio Ejecutivo', 'Escritorio 160x80 cm caoba', 'Mobiliario', 'ESC-EXE-160', 2, 'ADMIN', 'ADMIN');

    RAISE NOTICE '✓ 8 productos con critical_stock creados';

    -- 4. PRECIOS PARA NUEVOS PRODUCTOS
    INSERT INTO price_history (product_id, price)
    SELECT id, CASE sku
        WHEN 'PROD-021' THEN 850000
        WHEN 'PROD-022' THEN 450000
        WHEN 'PROD-023' THEN 65000
        WHEN 'PROD-024' THEN 3500
        WHEN 'PROD-025' THEN 45000
        WHEN 'PROD-026' THEN 180000
        WHEN 'PROD-027' THEN 420000
        WHEN 'PROD-028' THEN 320000
    END
    FROM products
    WHERE company_id = v_company_id AND sku IN ('PROD-021','PROD-022','PROD-023','PROD-024','PROD-025','PROD-026','PROD-027','PROD-028');

    RAISE NOTICE '✓ Precios iniciales agregados';

    RAISE NOTICE '';
    RAISE NOTICE '══════════════════════════════════════════';
    RAISE NOTICE '✓ MIGRACIÓN V36 COMPLETADA';
    RAISE NOTICE '══════════════════════════════════════════';
    RAISE NOTICE '  📦 3 proveedores creados';
    RAISE NOTICE '  👥 5 clientes creados';
    RAISE NOTICE '  📊 8 productos con critical_stock';
    RAISE NOTICE '  💲 8 precios iniciales configurados';
    RAISE NOTICE '══════════════════════════════════════════';

END $$;

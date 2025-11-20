-- V34: Seed data de demostración para inventario
-- Crea productos, ubicaciones y movimientos de ejemplo para testing

DO $$
DECLARE
    v_company_id UUID;
    v_location_id UUID;
    v_product_id UUID;
    v_lot_id UUID;
    products_data TEXT[][] := ARRAY[
        ARRAY['PROD-001', 'Laptop Dell Inspiron 15', 'Computadora portátil para oficina', 'Electrónica', 'LAP-DELL-001', '750000'],
        ARRAY['PROD-002', 'Mouse Inalámbrico Logitech', 'Mouse ergonómico inalámbrico', 'Accesorios', 'MOU-LOG-001', '15000'],
        ARRAY['PROD-003', 'Teclado Mecánico Razer', 'Teclado gaming RGB', 'Accesorios', 'TEC-RAZ-001', '85000'],
        ARRAY['PROD-004', 'Monitor LG 27 pulgadas', 'Monitor Full HD IPS', 'Electrónica', 'MON-LG-001', '220000'],
        ARRAY['PROD-005', 'Cable HDMI 2.0', 'Cable HDMI 2 metros', 'Cables', 'CAB-HDMI-001', '8000'],
        ARRAY['PROD-006', 'Disco Duro SSD 500GB', 'SSD SATA III Samsung', 'Almacenamiento', 'SSD-SAM-500', '65000'],
        ARRAY['PROD-007', 'Memoria RAM DDR4 16GB', 'Memoria Kingston HyperX', 'Componentes', 'RAM-KIN-16', '55000'],
        ARRAY['PROD-008', 'Webcam Logitech C920', 'Cámara web Full HD', 'Accesorios', 'WEB-LOG-001', '75000'],
        ARRAY['PROD-009', 'Audífonos Sony WH-1000XM4', 'Audífonos con cancelación de ruido', 'Audio', 'AUD-SON-001', '320000'],
        ARRAY['PROD-010', 'Impresora HP LaserJet', 'Impresora láser monocromática', 'Impresión', 'IMP-HP-001', '180000'],
        ARRAY['PROD-011', 'Router WiFi TP-Link', 'Router dual band AC1750', 'Redes', 'ROU-TP-001', '45000'],
        ARRAY['PROD-012', 'Switch Gigabit 8 puertos', 'Switch no administrable', 'Redes', 'SWI-TP-008', '28000'],
        ARRAY['PROD-013', 'UPS APC 650VA', 'Respaldo de energía', 'Energía', 'UPS-APC-650', '95000'],
        ARRAY['PROD-014', 'Silla Ergonómica', 'Silla de oficina con soporte lumbar', 'Mobiliario', 'SIL-ERG-001', '120000'],
        ARRAY['PROD-015', 'Escritorio Regulable', 'Escritorio sit-stand eléctrico', 'Mobiliario', 'ESC-REG-001', '350000'],
        ARRAY['PROD-016', 'Lámpara LED Escritorio', 'Lámpara LED regulable', 'Iluminación', 'LAM-LED-001', '25000'],
        ARRAY['PROD-017', 'Hub USB-C 7 puertos', 'Hub USB-C con HDMI y Ethernet', 'Accesorios', 'HUB-USB-007', '38000'],
        ARRAY['PROD-018', 'Mousepad XXL', 'Mousepad gaming 90x40cm', 'Accesorios', 'PAD-XXL-001', '12000'],
        ARRAY['PROD-019', 'Filtro de Privacidad 15.6"', 'Protector de pantalla privacidad', 'Accesorios', 'FIL-PRI-156', '22000'],
        ARRAY['PROD-020', 'Soporte para Laptop', 'Soporte ajustable aluminio', 'Accesorios', 'SOP-LAP-001', '18000']
    ];
    locations_data TEXT[][] := ARRAY[
        ARRAY['BOD-001', 'Bodega Principal', 'Ubicación principal de almacenamiento', 'BODEGA'],
        ARRAY['EST-A', 'Estante A', 'Estante para productos pequeños', 'LOCAL'],
        ARRAY['EST-B', 'Estante B', 'Estante para productos medianos', 'LOCAL'],
        ARRAY['CUARENTENA', 'Cuarentena', 'Área de productos en revisión', 'CONTAINER']
    ];
    stock_levels INTEGER[] := ARRAY[5, 8, 12, 15, 20, 25, 30, 45, 60, 80, 100, 120, 150, 200];
    i INTEGER;
    j INTEGER;
    base_date TIMESTAMP WITH TIME ZONE;
    random_stock INTEGER;
BEGIN
    -- Obtener primera compañía
    SELECT id INTO v_company_id FROM companies LIMIT 1;
    
    IF v_company_id IS NULL THEN
        RAISE NOTICE 'No hay compañías en el sistema. Skipping seed data.';
        RETURN;
    END IF;
    
    -- 1. CREAR UBICACIONES
    FOR i IN 1..array_length(locations_data, 1) LOOP
        INSERT INTO locations (
            company_id,
            code,
            name,
            description,
            type
        ) VALUES (
            v_company_id,
            locations_data[i][1],
            locations_data[i][2],
            locations_data[i][3],
            locations_data[i][4]
        ) RETURNING id INTO v_location_id;
        
        RAISE NOTICE 'Creada ubicación: % (%)', locations_data[i][2], locations_data[i][1];
    END LOOP;
    
    -- Guardar ubicación principal para lotes
    SELECT id INTO v_location_id FROM locations WHERE company_id = v_company_id AND code = 'BOD-001';
    
    -- 2. CREAR PRODUCTOS CON PRECIOS
    FOR i IN 1..array_length(products_data, 1) LOOP
        -- Insertar producto
        INSERT INTO products (
            company_id,
            sku,
            name,
            description,
            category,
            barcode,
            created_by,
            updated_by
        ) VALUES (
            v_company_id,
            products_data[i][1],
            products_data[i][2],
            products_data[i][3],
            products_data[i][4],
            products_data[i][5],
            'SEED',
            'SEED'
        ) RETURNING id INTO v_product_id;
        
        -- Insertar precio actual
        INSERT INTO price_history (product_id, price)
        VALUES (v_product_id, products_data[i][6]::NUMERIC);
        
        -- Seleccionar nivel de stock aleatorio
        random_stock := stock_levels[1 + floor(random() * array_length(stock_levels, 1))::INTEGER];
        
        -- Crear lote de inventario
        INSERT INTO inventory_lots (
            company_id,
            product_id,
            qty_available,
            cost_unit,
            mfg_date,
            exp_date
        ) VALUES (
            v_company_id,
            v_product_id,
            random_stock,
            (products_data[i][6]::NUMERIC * 0.7), -- Costo = 70% del precio
            CURRENT_DATE - INTERVAL '30 days',
            CASE 
                WHEN products_data[i][4] IN ('Electrónica', 'Componentes') THEN CURRENT_DATE + INTERVAL '2 years'
                WHEN products_data[i][4] IN ('Accesorios', 'Cables') THEN CURRENT_DATE + INTERVAL '3 years'
                ELSE NULL
            END
        ) RETURNING id INTO v_lot_id;
        
        -- Crear movimiento inicial de entrada (PURCHASE)
        base_date := CURRENT_TIMESTAMP - INTERVAL '90 days';
        
        INSERT INTO inventory_movements (
            company_id,
            product_id,
            lot_id,
            type,
            qty,
            ref_type,
            created_at
        ) VALUES (
            v_company_id,
            v_product_id,
            v_lot_id,
            'PURCHASE',
            random_stock + (20 + floor(random() * 30)::INTEGER), -- Stock inicial mayor
            'PURCHASE',
            base_date
        );
        
        -- Crear algunos movimientos de salida (SALE) en los últimos 90 días
        FOR j IN 1..3 LOOP
            IF random() > 0.3 THEN -- 70% de probabilidad de tener ventas
                INSERT INTO inventory_movements (
                    company_id,
                    product_id,
                    lot_id,
                    type,
                    qty,
                    ref_type,
                    created_at
                ) VALUES (
                    v_company_id,
                    v_product_id,
                    v_lot_id,
                    'SALE',
                    -(1 + floor(random() * 10)::INTEGER), -- Salida de 1 a 10 unidades
                    'SALE',
                    base_date + (j * INTERVAL '30 days') + (random() * INTERVAL '25 days')
                );
            END IF;
        END LOOP;
        
        RAISE NOTICE 'Creado producto: % (%) con stock: %', products_data[i][2], products_data[i][1], random_stock;
    END LOOP;
    
    -- 3. CONFIGURAR INVENTORY SETTINGS
    INSERT INTO inventory_settings (company_id, low_stock_threshold)
    VALUES (v_company_id, 10)
    ON CONFLICT (company_id) DO UPDATE SET low_stock_threshold = 10;
    
    RAISE NOTICE '✓ Seed data de inventario completado exitosamente';
    RAISE NOTICE '  - 20 productos creados con precios';
    RAISE NOTICE '  - 4 ubicaciones creadas';
    RAISE NOTICE '  - 20 lotes de inventario con stock variable';
    RAISE NOTICE '  - 80+ movimientos históricos (compras y ventas)';
    RAISE NOTICE '  - Configuración de stock mínimo: 10 unidades';
END $$;

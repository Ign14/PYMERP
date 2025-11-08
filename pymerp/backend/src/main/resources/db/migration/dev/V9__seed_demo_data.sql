-- Seed demo tenant, admin user and representative business data
WITH upsert_company AS (
  INSERT INTO companies (id, name, rut, industry, open_time, close_time, receipt_footer, logo_url, created_at)
  VALUES (
    '00000000-0000-0000-0000-000000000001'::uuid,
    'PyMEs Demo',
    '76.123.456-7',
    'Retail',
    '08:30:00',
    '19:00:00',
    '¡Gracias por preferir PyMEs Demo!',
    'https://cdn.pymerp.local/demo-logo.png',
    now()
  )
  ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    rut = EXCLUDED.rut,
    industry = EXCLUDED.industry,
    open_time = EXCLUDED.open_time,
    close_time = EXCLUDED.close_time,
    receipt_footer = EXCLUDED.receipt_footer,
    logo_url = EXCLUDED.logo_url
  RETURNING id
)
INSERT INTO company_settings (company_id, timezone, currency, invoice_sequence, created_at, updated_at)
SELECT
  id,
  'America/Santiago',
  'CLP',
  1520,
  now(),
  now()
FROM upsert_company
ON CONFLICT (company_id) DO UPDATE SET
  timezone = EXCLUDED.timezone,
  currency = EXCLUDED.currency;

INSERT INTO inventory_settings (company_id, low_stock_threshold, updated_at)
VALUES (
  '00000000-0000-0000-0000-000000000001'::uuid,
  10,
  now()
)
ON CONFLICT (company_id) DO UPDATE SET
  low_stock_threshold = EXCLUDED.low_stock_threshold,
  updated_at = now();

INSERT INTO users (id, company_id, email, name, role, status, password_hash, roles, created_at)
VALUES (
  'cdc7eceb-2f8a-489c-82b7-be1641264e55'::uuid,
  '00000000-0000-0000-0000-000000000001'::uuid,
  'admin@dev.local',
  'Administradora Demo',
  'admin',
  'active',
  '$2b$10$jfH37631QthIOE4FI3ycN.Vnk80wDYgpDyarXWj4a6G.hGEzG.0zm',
  'ROLE_ADMIN,ROLE_SALES,ROLE_PURCHASES,ROLE_REPORTS,ROLE_SETTINGS',
  now()
)
ON CONFLICT (email) DO UPDATE SET
  name = EXCLUDED.name,
  status = EXCLUDED.status,
  roles = EXCLUDED.roles,
  password_hash = EXCLUDED.password_hash;

INSERT INTO suppliers (id, company_id, name, rut, created_at) VALUES
  ('3a1c0dba-ce5d-4907-b4fc-8cc80799e5aa'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'Distribuidora Central', '96.456.120-5', now()),
  ('b816ae9a-231b-46cf-b34e-abe69db0457e'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'Fábrica del Norte', '91.334.876-1', now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO customers (id, company_id, name, address, lat, lng, phone, email, segment) VALUES
  ('e9e248b6-3ebe-429a-b7ee-0fb1c28adf69'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'Panadería La Espiga', 'Av. Las Industrias 1234, Santiago', -33.456700, -70.648270, '+56 2 2456 7800', 'contacto@laespiga.cl', 'HORECA'),
  ('7cce6f92-aad7-4f29-9314-da60a7ef8bd3'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'Mini Market Las Rosas', 'Av. Providencia 2345, Providencia', -33.426310, -70.611290, '+56 2 2488 1122', 'compras@lasrosas.cl', 'RETAIL'),
  ('f29a7f34-5ec6-4ebc-8fac-543b201d7ab0'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'Servicios Andes SpA', 'Los Carrera 450, Concepción', -36.827000, -73.050310, '+56 41 223 9988', 'finanzas@andes.cl', 'B2B')
ON CONFLICT (id) DO NOTHING;

INSERT INTO products (id, company_id, sku, name, description, category, barcode, image_url, created_at, updated_at, version, is_active) VALUES
  ('131c3b0a-05c3-4815-ac96-2a4bb52a857e'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'P-0001', 'Café en grano premium 1kg', 'Tueste medio importado de Colombia.', 'Bebidas', '7800006000011', 'https://cdn.pymerp.local/products/cafe-premium.png', now() - interval '45 days', now() - interval '2 days', 2, true),
  ('b39b3f2f-a1fe-4fad-8af2-c49b508b123f'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'P-0002', 'Taza cerámica personalizada', 'Taza corporativa color blanco 350cc.', 'Merchandising', '7800006000028', 'https://cdn.pymerp.local/products/taza.png', now() - interval '32 days', now() - interval '3 days', 1, true),
  ('c691e68d-393e-4179-a776-bce015e7ecc6'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'P-0003', 'Filtro de agua industrial', 'Filtro triple etapa con capacidad 2000L/h.', 'Equipamiento', '7800006000035', 'https://cdn.pymerp.local/products/filtro.png', now() - interval '60 days', now() - interval '10 days', 3, true)
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  category = EXCLUDED.category,
  barcode = EXCLUDED.barcode,
  image_url = EXCLUDED.image_url,
  updated_at = EXCLUDED.updated_at,
  is_active = EXCLUDED.is_active;

INSERT INTO price_history (id, product_id, price, valid_from) VALUES
  ('e847a7f4-25ab-4add-9f36-17f23269fe55'::uuid, '131c3b0a-05c3-4815-ac96-2a4bb52a857e'::uuid, 7490, now() - interval '30 days'),
  ('0c2be1e8-e4bd-441c-9214-301018de960d'::uuid, 'b39b3f2f-a1fe-4fad-8af2-c49b508b123f'::uuid, 4590, now() - interval '25 days'),
  ('d0994270-04ae-4bd0-9336-7a45d37a1ebd'::uuid, 'c691e68d-393e-4179-a776-bce015e7ecc6'::uuid, 125000, now() - interval '40 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO purchases (id, company_id, supplier_id, doc_type, doc_number, status, net, vat, total, pdf_url, issued_at, created_at) VALUES
  ('2e1a9e4f-4089-4c16-b8fe-475138c0bee0'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, '3a1c0dba-ce5d-4907-b4fc-8cc80799e5aa'::uuid, 'FACTURA', 'F001-12345', 'received', 358000, 68020, 426020, NULL, now() - interval '12 days', now() - interval '11 days'),
  ('764c042f-78b7-4355-833d-f608194a9adb'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'b816ae9a-231b-46cf-b34e-abe69db0457e'::uuid, 'FACTURA', 'F002-98765', 'received', 760000, 144400, 904400, NULL, now() - interval '20 days', now() - interval '19 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO purchase_items (id, purchase_id, product_id, qty, unit_cost, vat_rate) VALUES
  ('28193a87-9807-4a2f-ad8e-bc3684d46523'::uuid, '2e1a9e4f-4089-4c16-b8fe-475138c0bee0'::uuid, '131c3b0a-05c3-4815-ac96-2a4bb52a857e'::uuid, 100, 2500, 19),
  ('9efdeef0-1839-4030-9c45-3a33335f1b4c'::uuid, '2e1a9e4f-4089-4c16-b8fe-475138c0bee0'::uuid, 'b39b3f2f-a1fe-4fad-8af2-c49b508b123f'::uuid, 60, 1800, 19),
  ('e0fe0da4-98fc-4ad8-8b8d-6e8e0a853c94'::uuid, '764c042f-78b7-4355-833d-f608194a9adb'::uuid, 'c691e68d-393e-4179-a776-bce015e7ecc6'::uuid, 8, 95000, 19)
ON CONFLICT (id) DO NOTHING;

INSERT INTO inventory_lots (id, company_id, product_id, purchase_item_id, qty_available, cost_unit, mfg_date, exp_date, created_at) VALUES
  ('7940d3ab-db2c-434d-9c7b-1f604bdf4a15'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, '131c3b0a-05c3-4815-ac96-2a4bb52a857e'::uuid, '28193a87-9807-4a2f-ad8e-bc3684d46523'::uuid, 100, 2500, now() - interval '70 days', now() + interval '6 months', now() - interval '12 days'),
  ('98499351-c4d2-41d2-8bf9-566392f5101f'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'b39b3f2f-a1fe-4fad-8af2-c49b508b123f'::uuid, '9efdeef0-1839-4030-9c45-3a33335f1b4c'::uuid, 60, 1800, now() - interval '40 days', now() + interval '18 months', now() - interval '12 days'),
  ('5a744992-845e-4771-9d9e-14ebfc7dc579'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'c691e68d-393e-4179-a776-bce015e7ecc6'::uuid, 'e0fe0da4-98fc-4ad8-8b8d-6e8e0a853c94'::uuid, 8, 95000, now() - interval '90 days', now() + interval '2 years', now() - interval '20 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO inventory_movements (id, company_id, product_id, lot_id, type, qty, ref_type, ref_id, created_at) VALUES
  ('0353df10-2cd7-4a6b-b48a-c2a4bb2b6cd7'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, '131c3b0a-05c3-4815-ac96-2a4bb52a857e'::uuid, '7940d3ab-db2c-434d-9c7b-1f604bdf4a15'::uuid, 'IN', 100, 'PURCHASE', '2e1a9e4f-4089-4c16-b8fe-475138c0bee0'::uuid, now() - interval '11 days'),
  ('245fa792-36fd-4236-a483-5220fa8235a0'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'b39b3f2f-a1fe-4fad-8af2-c49b508b123f'::uuid, '98499351-c4d2-41d2-8bf9-566392f5101f'::uuid, 'IN', 60, 'PURCHASE', '2e1a9e4f-4089-4c16-b8fe-475138c0bee0'::uuid, now() - interval '11 days'),
  ('6c03e099-4965-4ef1-8f15-dfb0ba4b0ecf'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'c691e68d-393e-4179-a776-bce015e7ecc6'::uuid, '5a744992-845e-4771-9d9e-14ebfc7dc579'::uuid, 'IN', 8, 'PURCHASE', '764c042f-78b7-4355-833d-f608194a9adb'::uuid, now() - interval '19 days'),
  ('eaf59b02-98da-4330-a640-dacaa29872d2'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, '131c3b0a-05c3-4815-ac96-2a4bb52a857e'::uuid, '7940d3ab-db2c-434d-9c7b-1f604bdf4a15'::uuid, 'OUT', 15, 'SALE', 'f99d081f-19ca-4194-b65f-9a7897f28ef2'::uuid, now() - interval '2 days'),
  ('d43b02c0-f0b6-4674-9f26-d4f28416ec03'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'b39b3f2f-a1fe-4fad-8af2-c49b508b123f'::uuid, '98499351-c4d2-41d2-8bf9-566392f5101f'::uuid, 'OUT', 10, 'SALE', 'bfbf5eff-ba1e-4cff-824e-8654ce1cdf1c'::uuid, now() - interval '1 day')
ON CONFLICT (id) DO NOTHING;

INSERT INTO sales (id, company_id, customer_id, status, net, vat, total, payment_method, issued_at, doc_type, pdf_url) VALUES
  ('f99d081f-19ca-4194-b65f-9a7897f28ef2'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'e9e248b6-3ebe-429a-b7ee-0fb1c28adf69'::uuid, 'completed', 97500, 18525, 116025, 'TRANSFERENCIA', now() - interval '2 days', 'BOLETA', NULL),
  ('bfbf5eff-ba1e-4cff-824e-8654ce1cdf1c'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, '7cce6f92-aad7-4f29-9314-da60a7ef8bd3'::uuid, 'completed', 78000, 14820, 92820, 'TARJETA', now() - interval '1 day', 'FACTURA', NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sale_items (id, sale_id, product_id, qty, unit_price, discount) VALUES
  ('6269a604-fcdf-4c8e-b35f-2e39009c3c4d'::uuid, 'f99d081f-19ca-4194-b65f-9a7897f28ef2'::uuid, '131c3b0a-05c3-4815-ac96-2a4bb52a857e'::uuid, 15, 6500, 0),
  ('7fccd8dc-f509-4c67-a0fe-6754c5d89848'::uuid, 'bfbf5eff-ba1e-4cff-824e-8654ce1cdf1c'::uuid, 'b39b3f2f-a1fe-4fad-8af2-c49b508b123f'::uuid, 10, 7800, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sale_lot_allocations (id, sale_id, product_id, lot_id, qty) VALUES
  ('b6ac6ba6-702a-4318-9b69-99bf1c8b74a8'::uuid, 'f99d081f-19ca-4194-b65f-9a7897f28ef2'::uuid, '131c3b0a-05c3-4815-ac96-2a4bb52a857e'::uuid, '7940d3ab-db2c-434d-9c7b-1f604bdf4a15'::uuid, 15),
  ('654fd1eb-70c0-419b-b783-8de629d51f1e'::uuid, 'bfbf5eff-ba1e-4cff-824e-8654ce1cdf1c'::uuid, 'b39b3f2f-a1fe-4fad-8af2-c49b508b123f'::uuid, '98499351-c4d2-41d2-8bf9-566392f5101f'::uuid, 10)
ON CONFLICT (id) DO NOTHING;

INSERT INTO payroll_runs (id, company_id, period_start, period_end, status, total_gross, total_net, created_at) VALUES
  ('8c025e43-790e-417c-840f-bd545f7db11d'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, DATE_TRUNC('month', now() - interval '1 month'), DATE_TRUNC('month', now()) - interval '1 day', 'approved', 4850000, 3925000, now() - interval '15 days'),
  ('7705499c-e186-4d74-a069-92383c0ef48f'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, DATE_TRUNC('month', now()), DATE_TRUNC('month', now()) + interval '1 month' - interval '1 day', 'in_progress', 0, 0, now() - interval '2 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO report_templates (id, company_id, name, description, last_generated_at, created_at) VALUES
  ('732a452b-6f90-4209-9e8b-a6909e313596'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'IVA mensual', 'Detalle de débito y crédito fiscal por periodo.', now() - interval '5 days', now() - interval '100 days'),
  ('73116a67-e267-473a-a27e-3576675c099c'::uuid, '00000000-0000-0000-0000-000000000001'::uuid, 'Ventas por categoría', 'Comparativo de ventas netas por línea de producto.', now() - interval '3 days', now() - interval '80 days')
ON CONFLICT (id) DO NOTHING;

-- SUPPLIERS
CREATE TABLE IF NOT EXISTS suppliers (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  company_id UUID NOT NULL REFERENCES companies(id),
  name VARCHAR(100) NOT NULL,
  rut VARCHAR(20),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_suppliers_company ON suppliers(company_id);
CREATE INDEX IF NOT EXISTS idx_suppliers_company_name ON suppliers(company_id, name);

CREATE TABLE IF NOT EXISTS supplier_contacts (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  supplier_id UUID NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
  name VARCHAR(100) NOT NULL,
  title VARCHAR(50),
  phone VARCHAR(20),
  email VARCHAR(120)
);
CREATE INDEX IF NOT EXISTS idx_supplier_contacts_supplier ON supplier_contacts(supplier_id);

-- PRODUCTS
CREATE TABLE IF NOT EXISTS products (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  company_id UUID NOT NULL REFERENCES companies(id),
  sku VARCHAR(32) NOT NULL,
  name VARCHAR(120) NOT NULL,
  description TEXT,
  category VARCHAR(64),
  barcode VARCHAR(64),
  image_url TEXT,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  deleted_at TIMESTAMP WITH TIME ZONE,
  version INT NOT NULL DEFAULT 0,
  UNIQUE (company_id, sku)
);
CREATE INDEX IF NOT EXISTS idx_products_company_sku ON products(company_id, sku);
CREATE INDEX IF NOT EXISTS idx_products_company_barcode ON products(company_id, barcode);
CREATE INDEX IF NOT EXISTS idx_products_company_name ON products(company_id, name);

-- PRICING
CREATE TABLE IF NOT EXISTS price_history (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  price NUMERIC(14,4) NOT NULL,
  valid_from TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_price_history_product ON price_history(product_id, valid_from DESC);

-- PURCHASES
CREATE TABLE IF NOT EXISTS purchases (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  company_id UUID NOT NULL REFERENCES companies(id),
  supplier_id UUID REFERENCES suppliers(id),
  doc_type VARCHAR(20),
  doc_number VARCHAR(50),
  status VARCHAR(20),
  net NUMERIC(14,2),
  vat NUMERIC(14,2),
  total NUMERIC(14,2),
  pdf_url TEXT,
  issued_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_purchases_company_date ON purchases(company_id, issued_at DESC);

CREATE TABLE IF NOT EXISTS purchase_items (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  purchase_id UUID NOT NULL REFERENCES purchases(id) ON DELETE CASCADE,
  product_id UUID NOT NULL REFERENCES products(id),
  qty NUMERIC(14,3) NOT NULL,
  unit_cost NUMERIC(14,4) NOT NULL,
  vat_rate NUMERIC(5,2)
);
CREATE INDEX IF NOT EXISTS idx_purchase_items_purchase ON purchase_items(purchase_id);

-- INVENTORY
CREATE TABLE IF NOT EXISTS inventory_lots (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  company_id UUID NOT NULL REFERENCES companies(id),
  product_id UUID NOT NULL REFERENCES products(id),
  purchase_item_id UUID REFERENCES purchase_items(id),
  qty_available NUMERIC(14,3) NOT NULL,
  cost_unit NUMERIC(14,4),
  mfg_date DATE,
  exp_date DATE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_lots_company_product ON inventory_lots(company_id, product_id);
CREATE INDEX IF NOT EXISTS idx_lots_company_created ON inventory_lots(company_id, created_at);

CREATE TABLE IF NOT EXISTS inventory_movements (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  company_id UUID NOT NULL REFERENCES companies(id),
  product_id UUID NOT NULL REFERENCES products(id),
  lot_id UUID REFERENCES inventory_lots(id),
  type VARCHAR(20) NOT NULL,
  qty NUMERIC(14,3) NOT NULL,
  ref_type VARCHAR(20),
  ref_id UUID,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_movs_company_prod ON inventory_movements(company_id, product_id, created_at DESC);

-- CUSTOMERS
CREATE TABLE IF NOT EXISTS customers (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  company_id UUID NOT NULL REFERENCES companies(id),
  name VARCHAR(120) NOT NULL,
  address TEXT,
  lat NUMERIC(10,6),
  lng NUMERIC(10,6),
  phone VARCHAR(20),
  email VARCHAR(120)
);
CREATE INDEX IF NOT EXISTS idx_customers_company_name ON customers(company_id, name);

-- SALES
CREATE TABLE IF NOT EXISTS sales (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  company_id UUID NOT NULL REFERENCES companies(id),
  customer_id UUID REFERENCES customers(id),
  status VARCHAR(20) NOT NULL,
  net NUMERIC(14,2) NOT NULL,
  vat NUMERIC(14,2) NOT NULL,
  total NUMERIC(14,2) NOT NULL,
  payment_method VARCHAR(30),
  issued_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  doc_type VARCHAR(20),
  pdf_url TEXT
);
CREATE INDEX IF NOT EXISTS idx_sales_company_date ON sales(company_id, issued_at DESC);

CREATE TABLE IF NOT EXISTS sale_items (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  sale_id UUID NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
  product_id UUID NOT NULL REFERENCES products(id),
  qty NUMERIC(14,3) NOT NULL,
  unit_price NUMERIC(14,4) NOT NULL,
  discount NUMERIC(14,4) NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_sale_items_sale ON sale_items(sale_id);

CREATE TABLE IF NOT EXISTS sale_lot_allocations (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  sale_id UUID NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
  product_id UUID NOT NULL REFERENCES products(id),
  lot_id UUID NOT NULL REFERENCES inventory_lots(id),
  qty NUMERIC(14,3) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_sale_alloc_sale ON sale_lot_allocations(sale_id);

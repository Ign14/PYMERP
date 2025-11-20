-- Create customer_segment table
CREATE TABLE customer_segment (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    color VARCHAR(7), -- Hex color format: #RRGGBB
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_id, code)
);

-- Create index for faster lookups
CREATE INDEX idx_customer_segment_company_id ON customer_segment(company_id);
CREATE INDEX idx_customer_segment_active ON customer_segment(active);

-- Insert some default segments for existing companies
INSERT INTO customer_segment (company_id, code, name, description, color)
SELECT 
    id,
    'RETAIL',
    'Retail',
    'Clientes del sector retail y comercio',
    '#3B82F6'
FROM companies
WHERE NOT EXISTS (
    SELECT 1 FROM customer_segment WHERE company_id = companies.id AND code = 'RETAIL'
);

INSERT INTO customer_segment (company_id, code, name, description, color)
SELECT 
    id,
    'CORPORATE',
    'Corporativo',
    'Clientes corporativos y empresas',
    '#8B5CF6'
FROM companies
WHERE NOT EXISTS (
    SELECT 1 FROM customer_segment WHERE company_id = companies.id AND code = 'CORPORATE'
);

INSERT INTO customer_segment (company_id, code, name, description, color)
SELECT 
    id,
    'VIP',
    'VIP',
    'Clientes premium con beneficios especiales',
    '#F59E0B'
FROM companies
WHERE NOT EXISTS (
    SELECT 1 FROM customer_segment WHERE company_id = companies.id AND code = 'VIP'
);

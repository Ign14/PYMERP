-- V22: Create services table
CREATE TABLE services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    last_purchase_date DATE,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uq_services_company_code UNIQUE (company_id, code)
);

CREATE INDEX idx_service_company ON services(company_id);
CREATE INDEX idx_service_active ON services(active);
CREATE INDEX idx_service_last_purchase ON services(last_purchase_date);

-- Crear tabla de ubicaciones (locations)
CREATE TABLE locations (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    type VARCHAR(20) NOT NULL,
    parent_location_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_location_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_location_parent FOREIGN KEY (parent_location_id) REFERENCES locations(id) ON DELETE SET NULL,
    CONSTRAINT uq_location_code_per_company UNIQUE (company_id, code)
);

-- Índices para mejorar el rendimiento de consultas
CREATE INDEX idx_locations_company_id ON locations(company_id);
CREATE INDEX idx_locations_parent_location_id ON locations(parent_location_id);
CREATE INDEX idx_locations_type ON locations(type);

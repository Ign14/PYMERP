CREATE TABLE IF NOT EXISTS account_requests (
    id UUID PRIMARY KEY,
    rut VARCHAR(20) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    address VARCHAR(200) NOT NULL,
    email VARCHAR(150) NOT NULL,
    company_name VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_account_requests_status ON account_requests(status);

${create_extension_pgcrypto}
${h2_uuid_alias}

CREATE TABLE companies (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  rut VARCHAR(20),
  industry VARCHAR(30),
  open_time TIME,
  close_time TIME,
  receipt_footer VARCHAR(200),
  logo_url TEXT,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE users (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  company_id UUID NOT NULL REFERENCES companies(id),
  email VARCHAR(120) NOT NULL UNIQUE,
  name VARCHAR(100),
  role VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX idx_users_company ON users(company_id);

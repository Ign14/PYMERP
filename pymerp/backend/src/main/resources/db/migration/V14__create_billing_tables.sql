-- Billing persistence module: core tables

CREATE TABLE IF NOT EXISTS fiscal_documents (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  sale_id UUID NOT NULL REFERENCES sales(id),
  document_type VARCHAR(16) NOT NULL,
  tax_mode VARCHAR(16) NOT NULL,
  status VARCHAR(24) NOT NULL,
  number VARCHAR(30),
  provisional_number VARCHAR(40),
  contingency_reason VARCHAR(160),
  track_id VARCHAR(80),
  provider VARCHAR(60),
  is_offline BOOLEAN NOT NULL DEFAULT FALSE,
  sync_attempts INT NOT NULL DEFAULT 0,
  last_sync_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS non_fiscal_documents (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  sale_id UUID NOT NULL REFERENCES sales(id),
  document_type VARCHAR(32) NOT NULL,
  status VARCHAR(16) NOT NULL,
  number VARCHAR(40),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS document_files (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  document_id UUID NOT NULL,
  kind VARCHAR(16) NOT NULL,
  version VARCHAR(16) NOT NULL,
  content_type VARCHAR(120) NOT NULL,
  storage_key VARCHAR(255) NOT NULL,
  checksum VARCHAR(120),
  previous_file_id UUID REFERENCES document_files(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS contingency_queue_items (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  document_id UUID NOT NULL REFERENCES fiscal_documents(id) ON DELETE CASCADE,
  idempotency_key VARCHAR(100) NOT NULL,
  provider_payload JSONB NOT NULL,
  status VARCHAR(24) NOT NULL,
  sync_attempts INT NOT NULL DEFAULT 0,
  last_sync_at TIMESTAMPTZ,
  error_detail TEXT,
  encrypted_blob BYTEA,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE contingency_queue_items
  ADD CONSTRAINT uq_contingency_idempotency UNIQUE (idempotency_key);

ALTER TABLE fiscal_documents
  DROP CONSTRAINT IF EXISTS uq_fiscal_documents_idempotency;

ALTER TABLE fiscal_documents
  ADD COLUMN IF NOT EXISTS payload_hash VARCHAR(64);

ALTER TABLE fiscal_documents
  DROP CONSTRAINT IF EXISTS uq_fiscal_documents_company_idempotency;

ALTER TABLE fiscal_documents
  ADD CONSTRAINT uq_fiscal_documents_company_idempotency
    UNIQUE (company_id, idempotency_key);

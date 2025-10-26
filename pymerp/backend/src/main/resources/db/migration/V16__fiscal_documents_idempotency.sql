ALTER TABLE fiscal_documents
  ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100);

ALTER TABLE fiscal_documents
  ADD CONSTRAINT uq_fiscal_documents_idempotency UNIQUE (idempotency_key);

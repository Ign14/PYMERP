-- Billing persistence module: indexes and constraints

-- Fiscal documents
ALTER TABLE fiscal_documents
  ADD CONSTRAINT uq_fiscal_documents_provisional UNIQUE (provisional_number);

CREATE INDEX IF NOT EXISTS idx_fiscal_documents_sale ON fiscal_documents(sale_id);
CREATE INDEX IF NOT EXISTS idx_fiscal_documents_status ON fiscal_documents(status);
CREATE INDEX IF NOT EXISTS idx_fiscal_documents_created_at ON fiscal_documents(created_at);
CREATE INDEX IF NOT EXISTS idx_fiscal_documents_track_id ON fiscal_documents(track_id);

-- Non fiscal documents
CREATE INDEX IF NOT EXISTS idx_non_fiscal_documents_sale ON non_fiscal_documents(sale_id);
CREATE INDEX IF NOT EXISTS idx_non_fiscal_documents_created_at ON non_fiscal_documents(created_at);

-- Document files
CREATE INDEX IF NOT EXISTS idx_document_files_document ON document_files(document_id, kind);
CREATE INDEX IF NOT EXISTS idx_document_files_version ON document_files(version);
CREATE INDEX IF NOT EXISTS idx_document_files_created_at ON document_files(created_at);

-- Contingency queue
CREATE INDEX IF NOT EXISTS idx_contingency_queue_status ON contingency_queue_items(status);
CREATE INDEX IF NOT EXISTS idx_contingency_queue_created_at ON contingency_queue_items(created_at);

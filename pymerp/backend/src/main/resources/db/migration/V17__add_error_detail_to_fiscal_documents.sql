ALTER TABLE fiscal_documents
ADD COLUMN IF NOT EXISTS error_detail TEXT;

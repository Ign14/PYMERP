ALTER TABLE fiscal_documents
    ADD COLUMN IF NOT EXISTS sii_type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS final_folio VARCHAR(30),
    ADD COLUMN IF NOT EXISTS resolution_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS resolution_date DATE,
    ADD COLUMN IF NOT EXISTS references_json TEXT;

UPDATE fiscal_documents
SET sii_type = CASE
    WHEN document_type = 'FACTURA' AND tax_mode = 'EXENTA' THEN 'FACTURA_NO_AFECTA_O_EXENTA'
    WHEN document_type = 'FACTURA' THEN 'FACTURA_ELECTRONICA'
    WHEN document_type = 'BOLETA' AND tax_mode = 'EXENTA' THEN 'BOLETA_NO_AFECTA_O_EXENTA'
    WHEN document_type = 'BOLETA' THEN 'BOLETA_ELECTRONICA'
    ELSE sii_type
END
WHERE sii_type IS NULL;

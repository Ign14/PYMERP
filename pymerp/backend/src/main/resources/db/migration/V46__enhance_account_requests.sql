DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'account_requests') THEN
    EXECUTE 'ALTER TABLE account_requests RENAME TO user_account_requests';
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_class WHERE relname = 'idx_account_requests_status') THEN
    EXECUTE 'ALTER INDEX idx_account_requests_status RENAME TO idx_user_account_requests_status';
  END IF;
END $$;

ALTER TABLE user_account_requests
  ADD COLUMN IF NOT EXISTS processed_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS processed_by UUID,
  ADD COLUMN IF NOT EXISTS processed_by_username VARCHAR(120),
  ADD COLUMN IF NOT EXISTS rejection_reason TEXT,
  ADD COLUMN IF NOT EXISTS notes TEXT,
  ADD COLUMN IF NOT EXISTS ip_address VARCHAR(50),
  ADD COLUMN IF NOT EXISTS user_agent TEXT;

UPDATE user_account_requests
SET status = 'APPROVED'
WHERE status = 'COMPLETED';

UPDATE user_account_requests
SET status = 'PENDING'
WHERE status = 'REVIEWING';

CREATE INDEX IF NOT EXISTS idx_user_account_requests_email ON user_account_requests(email);
CREATE INDEX IF NOT EXISTS idx_user_account_requests_requested_at ON user_account_requests(created_at DESC);

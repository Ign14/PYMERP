#!/usr/bin/env bash
set -euo pipefail

DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-55432}
DB_NAME=${DB_NAME:-pymes}
DB_USER=${DB_USER:-pymes}
DB_PASSWORD=${DB_PASSWORD:-CHANGE_ME_DB_PASSWORD}
COMPANY_ID=${COMPANY_ID:-00000000-0000-0000-0000-0000000000C0}
COMPANY_NAME=${COMPANY_NAME:-"Contingency Co"}
ADMIN_EMAIL=${ADMIN_EMAIL:-contingency-admin@example.com}
ADMIN_PASSWORD_HASH=${ADMIN_PASSWORD_HASH:-\$2b\$12\$CHANGE_ME_BCRYPT_HASH___________/.oe}
BUCKET_NAME=${BUCKET_NAME:-pymes-contingency-placeholder}
BUCKET_REGION=${BUCKET_REGION:-us-east-1}
CREATE_BUCKET=${CREATE_BUCKET:-false}

if ! command -v psql >/dev/null 2>&1; then
  echo "psql command not found. Install PostgreSQL client tools and try again." >&2
  exit 1
fi

export PGPASSWORD="${DB_PASSWORD}"

psql \
  --host="${DB_HOST}" \
  --port="${DB_PORT}" \
  --username="${DB_USER}" \
  --dbname="${DB_NAME}" \
  --set=ON_ERROR_STOP=on <<SQL
WITH upsert_company AS (
  INSERT INTO companies (id, name, created_at)
  VALUES ('${COMPANY_ID}'::uuid, '${COMPANY_NAME}', now())
  ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name
  RETURNING id
),
ensured_company_settings AS (
  INSERT INTO company_settings (company_id, timezone, currency, invoice_sequence, created_at, updated_at)
  SELECT id, 'America/Santiago', 'CLP', 1, now(), now()
  FROM upsert_company
  ON CONFLICT (company_id) DO UPDATE SET
    timezone = EXCLUDED.timezone,
    currency = EXCLUDED.currency
  RETURNING company_id
)
INSERT INTO users (company_id, email, name, role, status, password_hash, roles, created_at)
SELECT
  company_id,
  '${ADMIN_EMAIL}',
  'Contingency Admin',
  'admin',
  'active',
  '${ADMIN_PASSWORD_HASH}',
  'ROLE_ADMIN,ROLE_BILLING',
  now()
FROM ensured_company_settings
ON CONFLICT (email) DO UPDATE SET
  name = EXCLUDED.name,
  status = EXCLUDED.status,
  roles = EXCLUDED.roles,
  password_hash = EXCLUDED.password_hash;
SQL

unset PGPASSWORD

if [[ "${CREATE_BUCKET}" == "true" ]]; then
  if ! command -v aws >/dev/null 2>&1; then
    echo "AWS CLI not found. Skipping bucket creation step." >&2
    exit 0
  fi

  if [[ "${BUCKET_REGION}" == "us-east-1" ]]; then
    aws s3api create-bucket \
      --bucket "${BUCKET_NAME}" \
      --acl private
  else
    aws s3api create-bucket \
      --bucket "${BUCKET_NAME}" \
      --create-bucket-configuration "LocationConstraint=${BUCKET_REGION}" \
      --acl private
  fi

  aws s3api put-bucket-versioning \
    --bucket "${BUCKET_NAME}" \
    --versioning-configuration Status=Enabled
fi

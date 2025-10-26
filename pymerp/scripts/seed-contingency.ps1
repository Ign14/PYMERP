Param(
  [string]$DbHost = "localhost",
  [int]$DbPort = 55432,
  [string]$DbName = "pymes",
  [string]$DbUser = "pymes",
  [string]$DbPassword = "CHANGE_ME_DB_PASSWORD",
  [string]$CompanyId = "00000000-0000-0000-0000-0000000000C0",
  [string]$CompanyName = "Contingency Co",
  [string]$AdminEmail = "contingency-admin@example.com",
  [string]$AdminPasswordHash = "$2b$12$CHANGE_ME_BCRYPT_HASH___________/.oe",
  [string]$BucketName = "pymes-contingency-placeholder",
  [string]$BucketRegion = "us-east-1",
  [switch]$CreateBucket
)

if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
  Write-Error "psql command not found. Install PostgreSQL client tools and try again."
  exit 1
}

$env:PGPASSWORD = $DbPassword

$sql = @"
WITH upsert_company AS (
  INSERT INTO companies (id, name, created_at)
  VALUES ('$CompanyId'::uuid, '$CompanyName', now())
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
  '$AdminEmail',
  'Contingency Admin',
  'admin',
  'active',
  '$AdminPasswordHash',
  'ROLE_ADMIN,ROLE_BILLING',
  now()
FROM ensured_company_settings
ON CONFLICT (email) DO UPDATE SET
  name = EXCLUDED.name,
  status = EXCLUDED.status,
  roles = EXCLUDED.roles,
  password_hash = EXCLUDED.password_hash;
"@

& psql `
  "--host=$DbHost" `
  "--port=$DbPort" `
  "--username=$DbUser" `
  "--dbname=$DbName" `
  "--set=ON_ERROR_STOP=on" `
  "--command=$sql"

Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue

if ($CreateBucket) {
  if (-not (Get-Command aws -ErrorAction SilentlyContinue)) {
    Write-Warning "AWS CLI not found. Skipping bucket creation step."
    return
  }

  $createBucketArgs = @("--bucket", $BucketName, "--acl", "private")
  if ($BucketRegion -ne "us-east-1") {
    $createBucketArgs += @("--create-bucket-configuration", "LocationConstraint=$BucketRegion")
  }

  aws s3api create-bucket @createBucketArgs

  aws s3api put-bucket-versioning `
    --bucket $BucketName `
    --versioning-configuration Status=Enabled
}

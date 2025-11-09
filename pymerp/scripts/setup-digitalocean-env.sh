#!/usr/bin/env bash
#
# setup-digitalocean-env.sh - Sprint 10 Fase 4 helper to prepare App Platform env vars

set -uo pipefail
IFS=$'\n\t'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

DRY_RUN=false
FORCE=false
VALIDATE_ONLY=false

usage() {
  cat <<'EOF'
Usage: setup-digitalocean-env.sh [--dry-run] [--force] [--validate-only]

  --dry-run        Solo mostrar comandos sin ejecutarlos
  --force          Regenerar secretos aunque ya existan
  --validate-only  Ejecutar únicamente las validaciones de conectividad
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run) DRY_RUN=true ;;
    --force) FORCE=true ;;
    --validate-only) VALIDATE_ONLY=true ;;
    -h|--help) usage; exit 0 ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
  shift
done

declare -a REQUIRED_ENV_VARS=(
  "POSTGRES_HOST"
  "POSTGRES_PORT"
  "POSTGRES_DB"
  "POSTGRES_USER"
  "POSTGRES_PASSWORD"
  "REDIS_HOST"
  "REDIS_PORT"
  "REDIS_PASSWORD"
  "REDIS_SSL_ENABLED"
  "BILLING_DOCUMENTS_BASE_URL"
  "MAIL_HOST"
  "MAIL_PORT"
  "MAIL_USERNAME"
  "MAIL_PASSWORD"
  "MAIL_SMTP_AUTH"
  "MAIL_SMTP_STARTTLS_ENABLE"
  "S3_ENDPOINT"
  "STORAGE_S3_BUCKET"
  "STORAGE_S3_ACCESS_KEY"
  "STORAGE_S3_SECRET_KEY"
  "STORAGE_S3_REGION"
  "APP_CORS_ALLOWED_ORIGINS[0]"
  "APP_CORS_ALLOWED_ORIGINS[1]"
  "APP_CORS_ALLOWED_ORIGINS[2]"
)

declare -a OPTIONAL_ENV_VARS=(
  "CAPTCHA_ENABLED"
  "CAPTCHA_SECRET_KEY"
  "DB_POOL_SIZE"
  "DB_POOL_MIN_IDLE"
  "PORT"
  "POSTGRES_MIGRATION_USER"
  "POSTGRES_MIGRATION_PASSWORD"
)

declare -a AUTO_SECRET_VARS=(
  "JWT_SECRET"
  "BILLING_CRYPTO_SECRET"
  "BILLING_WEBHOOK_SECRET"
  "WEBHOOKS_HMAC_SECRET"
)

declare -a ALL_ENV_VARS=(
  "${REQUIRED_ENV_VARS[@]}"
  "${AUTO_SECRET_VARS[@]}"
  "${OPTIONAL_ENV_VARS[@]}"
)

declare -a SENSITIVE_VARS=(
  "POSTGRES_PASSWORD"
  "REDIS_PASSWORD"
  "JWT_SECRET"
  "BILLING_CRYPTO_SECRET"
  "BILLING_WEBHOOK_SECRET"
  "WEBHOOKS_HMAC_SECRET"
  "MAIL_PASSWORD"
  "STORAGE_S3_ACCESS_KEY"
  "STORAGE_S3_SECRET_KEY"
)

declare -A SECRET_GENERATORS=(
  ["JWT_SECRET"]="openssl rand -base64 64"
  ["BILLING_CRYPTO_SECRET"]="openssl rand -base64 32"
  ["BILLING_WEBHOOK_SECRET"]="openssl rand -hex 32"
  ["WEBHOOKS_HMAC_SECRET"]="openssl rand -base64 48"
)

declare -A CHECK_STATUS=()
declare -A CHECK_DETAILS=()
declare -A CHECK_LABELS=()
declare -a CHECK_ORDER=()
GLOBAL_EXIT_CODE=0

declare -a VAR_STATUS_ROWS=()

log() {
  printf '%s\n' "$1"
}

record_result() {
  local key="$1"
  local label="$2"
  local status="$3"
  local details="$4"

  if [[ -z "${CHECK_STATUS[$key]+_}" ]]; then
    CHECK_ORDER+=("$key")
    CHECK_LABELS["$key"]="$label"
  fi

  CHECK_STATUS["$key"]="$status"
  CHECK_DETAILS["$key"]="$details"

  if [[ "$status" == "❌" ]]; then
    GLOBAL_EXIT_CODE=1
  fi
}

escape_md() {
  local text="$1"
  text=${text//|/\\|}
  text=${text//$'\n'/<br>}
  printf '%s' "$text"
}

run_cmd() {
  local description="$1"
  shift
  local -a cmd=("$@")

  if $DRY_RUN; then
    printf '[dry-run] %s: %s\n' "$description" "$(printf '%q ' "${cmd[@]}")"
    return 0
  fi

  "${cmd[@]}"
}

is_auto_secret() {
  local target="$1"
  for secret in "${AUTO_SECRET_VARS[@]}"; do
    if [[ "$secret" == "$target" ]]; then
      return 0
    fi
  done
  return 1
}

is_sensitive_var() {
  local target="$1"
  for sensitive in "${SENSITIVE_VARS[@]}"; do
    if [[ "$sensitive" == "$target" ]]; then
      return 0
    fi
  done
  return 1
}

check_command() {
  local binary="$1"
  if command -v "$binary" >/dev/null 2>&1; then
    return 0
  fi
  return 1
}

check_prerequisites() {
  if ! check_command doctl; then
    record_result "prereq.doctl.bin" "doctl instalado" "❌" "doctl no está en el PATH"
  else
    record_result "prereq.doctl.bin" "doctl instalado" "✅" "doctl disponible"
  fi

  if check_command doctl && run_cmd "Validar autenticación de doctl" doctl account get >/dev/null 2>&1; then
    record_result "prereq.doctl.auth" "doctl autenticado" "✅" "Cuenta DigitalOcean disponible"
  else
    record_result "prereq.doctl.auth" "doctl autenticado" "❌" "Ejecuta 'doctl auth init' antes de continuar"
  fi

  if [[ -z "${APP_ID:-}" ]]; then
    record_result "prereq.app_id" "APP_ID definido" "❌" "Variable APP_ID ausente"
  else
    if check_command doctl && run_cmd "Validar APP_ID" doctl apps get "$APP_ID" >/dev/null 2>&1; then
      record_result "prereq.app_id" "APP_ID válido" "✅" "Aplicación $APP_ID encontrada"
    else
      record_result "prereq.app_id" "APP_ID válido" "❌" "No se pudo obtener la app $APP_ID"
    fi
  fi
}

validate_envs_pre_generation() {
  local missing_required=()
  for var in "${REQUIRED_ENV_VARS[@]}"; do
    if is_auto_secret "$var"; then
      continue
    fi
    if [[ -z "${!var:-}" ]]; then
      missing_required+=("$var")
    fi
  done

  if ((${#missing_required[@]} > 0)); then
    record_result "env.required" "Variables requeridas" "❌" "Faltan: ${missing_required[*]}"
  else
    record_result "env.required" "Variables requeridas" "✅" "Todo definido (antes de generar secretos)"
  fi

  local missing_optional=()
  for var in "${OPTIONAL_ENV_VARS[@]}"; do
    if [[ -z "${!var:-}" ]]; then
      missing_optional+=("$var")
    fi
  done

  if ((${#missing_optional[@]} > 0)); then
    record_result "env.optional" "Variables opcionales" "⚠️" "Sin valor: ${missing_optional[*]}"
  else
    record_result "env.optional" "Variables opcionales" "✅" "Todas configuradas"
  fi
}

generate_missing_secrets() {
  if $VALIDATE_ONLY; then
    record_result "secrets.skip" "Generación de secretos" "⚠️" "Omitido por --validate-only"
    return
  fi

  if ! check_command openssl; then
    record_result "secrets.openssl" "openssl disponible" "❌" "Instala openssl para generar secretos"
    return
  fi

  for secret in "${AUTO_SECRET_VARS[@]}"; do
    local current="${!secret:-}"
    local needs_generation=false
    if [[ -z "$current" || $FORCE == true ]]; then
      needs_generation=true
    fi

    if [[ "$needs_generation" == false ]]; then
      record_result "secret.$secret" "$secret" "✅" "Se mantiene valor existente"
      continue
    fi

    if $DRY_RUN; then
      export "$secret"="<dry-run-${secret,,}>"
      record_result "secret.$secret" "$secret" "⚠️" "Se generará (dry-run)"
      continue
    fi

    local cmd="${SECRET_GENERATORS[$secret]}"
    local generated
    if generated=$(eval "$cmd"); then
      export "$secret=$generated"
      record_result "secret.$secret" "$secret" "✅" "Nuevo secreto generado"
    else
      record_result "secret.$secret" "$secret" "❌" "No se pudo generar con '$cmd'"
    fi
  done
}

generate_env_payload() {
  python3 <<'PY'
import json
import os

envs = []

def add_env(key, value, scope="RUN_AND_BUILD", secret=False, allow_empty=False):
    if not value and not allow_empty:
        return
    entry = {
        "key": key,
        "value": value,
        "scope": scope,
        "type": "SECRET" if secret else "GENERAL",
    }
    envs.append(entry)

add_env("POSTGRES_HOST", "${db.HOSTNAME}", allow_empty=True)
add_env("POSTGRES_PORT", "${db.PORT}", allow_empty=True)
add_env("POSTGRES_DB", "${db.DATABASE}", allow_empty=True)
add_env("POSTGRES_USER", "${db.USERNAME}", allow_empty=True)
add_env("POSTGRES_PASSWORD", "${db.PASSWORD}", secret=True, allow_empty=True)

add_env("REDIS_HOST", "${redis.HOSTNAME}", allow_empty=True)
add_env("REDIS_PORT", "${redis.PORT}", allow_empty=True)
add_env("REDIS_PASSWORD", "${redis.PASSWORD}", secret=True, allow_empty=True)
add_env("REDIS_SSL_ENABLED", os.environ.get("REDIS_SSL_ENABLED", "true"))

add_env("JWT_SECRET", os.environ.get("JWT_SECRET", ""), scope="RUN_TIME", secret=True)
add_env("BILLING_CRYPTO_SECRET", os.environ.get("BILLING_CRYPTO_SECRET", ""), scope="RUN_TIME", secret=True)
add_env("BILLING_WEBHOOK_SECRET", os.environ.get("BILLING_WEBHOOK_SECRET", ""), scope="RUN_TIME", secret=True)
add_env("BILLING_DOCUMENTS_BASE_URL", os.environ.get("BILLING_DOCUMENTS_BASE_URL", ""))
add_env("WEBHOOKS_HMAC_SECRET", os.environ.get("WEBHOOKS_HMAC_SECRET", ""), scope="RUN_TIME", secret=True)

add_env("MAIL_HOST", os.environ.get("MAIL_HOST", ""))
add_env("MAIL_PORT", os.environ.get("MAIL_PORT", ""))
add_env("MAIL_USERNAME", os.environ.get("MAIL_USERNAME", ""), scope="RUN_TIME")
add_env("MAIL_PASSWORD", os.environ.get("MAIL_PASSWORD", ""), scope="RUN_TIME", secret=True)
add_env("MAIL_SMTP_AUTH", os.environ.get("MAIL_SMTP_AUTH", "true"))
add_env("MAIL_SMTP_STARTTLS_ENABLE", os.environ.get("MAIL_SMTP_STARTTLS_ENABLE", "true"))

add_env("S3_ENDPOINT", os.environ.get("S3_ENDPOINT", ""))
add_env("STORAGE_S3_BUCKET", os.environ.get("STORAGE_S3_BUCKET", ""))
add_env("STORAGE_S3_ACCESS_KEY", os.environ.get("STORAGE_S3_ACCESS_KEY", ""), scope="RUN_TIME", secret=True)
add_env("STORAGE_S3_SECRET_KEY", os.environ.get("STORAGE_S3_SECRET_KEY", ""), scope="RUN_TIME", secret=True)
add_env("STORAGE_S3_REGION", os.environ.get("STORAGE_S3_REGION", ""))

add_env("APP_CORS_ALLOWED_ORIGINS[0]", os.environ.get("APP_CORS_ALLOWED_ORIGINS[0]", ""))
add_env("APP_CORS_ALLOWED_ORIGINS[1]", os.environ.get("APP_CORS_ALLOWED_ORIGINS[1]", ""))
add_env("APP_CORS_ALLOWED_ORIGINS[2]", os.environ.get("APP_CORS_ALLOWED_ORIGINS[2]", ""))

add_env("CAPTCHA_ENABLED", os.environ.get("CAPTCHA_ENABLED", ""))
add_env("CAPTCHA_SECRET_KEY", os.environ.get("CAPTCHA_SECRET_KEY", ""))
add_env("DB_POOL_SIZE", os.environ.get("DB_POOL_SIZE", ""))
add_env("DB_POOL_MIN_IDLE", os.environ.get("DB_POOL_MIN_IDLE", ""))
add_env("PORT", os.environ.get("PORT", ""))
add_env("POSTGRES_MIGRATION_USER", os.environ.get("POSTGRES_MIGRATION_USER", ""))
add_env("POSTGRES_MIGRATION_PASSWORD", os.environ.get("POSTGRES_MIGRATION_PASSWORD", ""), secret=True)

print(json.dumps({"envs": envs}, indent=2))
PY
}

update_app_platform_envs() {
  if $VALIDATE_ONLY; then
    record_result "doctl.update" "App Platform" "⚠️" "Omitido por --validate-only"
    return
  fi

  if ! check_command doctl; then
    record_result "doctl.update" "App Platform" "❌" "doctl no disponible"
    return
  fi

  if ! check_command python3; then
    record_result "doctl.update" "App Platform" "❌" "python3 es requerido para generar el payload"
    return
  fi

  local env_file
  env_file="$(mktemp)"

  if ! generate_env_payload >"$env_file"; then
    record_result "doctl.update" "App Platform" "❌" "No se pudo generar el payload JSON"
    rm -f "$env_file"
    return
  fi

  if run_cmd "Actualizar variables en App Platform" doctl apps update-env "$APP_ID" --envs-file "$env_file"; then
    record_result "doctl.update" "App Platform" "✅" "Variables aplicadas vía doctl apps update-env"
  else
    record_result "doctl.update" "App Platform" "❌" "Fallo doctl apps update-env"
  fi

  rm -f "$env_file"
}

validate_postgres() {
  if ! check_command psql; then
    record_result "conn.postgres" "PostgreSQL" "⚠️" "psql no instalado"
    return
  fi

  local host="${POSTGRES_HOST:-}"
  local port="${POSTGRES_PORT:-5432}"
  local user="${POSTGRES_USER:-}"
  local db="${POSTGRES_DB:-}"
  local password="${POSTGRES_PASSWORD:-}"

  if [[ -z "$host" || -z "$user" || -z "$db" || -z "$password" ]]; then
    record_result "conn.postgres" "PostgreSQL" "❌" "Variables POSTGRES_* incompletas"
    return
  fi

  if run_cmd "Verificar PostgreSQL" env PGPASSWORD="$password" psql -h "$host" -p "$port" -U "$user" -d "$db" -c "SELECT 1" >/dev/null; then
    record_result "conn.postgres" "PostgreSQL" "✅" "SELECT 1 exitoso"
  else
    record_result "conn.postgres" "PostgreSQL" "❌" "No se pudo ejecutar SELECT 1"
  fi
}

validate_redis() {
  if ! check_command redis-cli; then
    record_result "conn.redis" "Redis" "⚠️" "redis-cli no instalado"
    return
  fi

  local host="${REDIS_HOST:-}"
  local port="${REDIS_PORT:-}"
  local password="${REDIS_PASSWORD:-}"
  local tls_flag=()
  local ssl_enabled="${REDIS_SSL_ENABLED:-true}"

  if [[ -z "$host" || -z "$port" || -z "$password" ]]; then
    record_result "conn.redis" "Redis" "❌" "Variables REDIS_* incompletas"
    return
  fi

  if [[ "${ssl_enabled,,}" == "true" ]]; then
    tls_flag+=(--tls)
  fi

  if run_cmd "Verificar Redis" redis-cli -h "$host" -p "$port" "${tls_flag[@]}" -a "$password" PING >/dev/null; then
    record_result "conn.redis" "Redis" "✅" "PING respondió PONG"
  else
    record_result "conn.redis" "Redis" "❌" "No se pudo ejecutar PING"
  fi
}

validate_s3() {
  if ! check_command aws; then
    record_result "conn.s3" "Spaces/S3" "⚠️" "aws cli no instalado"
    return
  fi

  local bucket="${STORAGE_S3_BUCKET:-}"
  local endpoint="${S3_ENDPOINT:-}"
  local access_key="${STORAGE_S3_ACCESS_KEY:-}"
  local secret_key="${STORAGE_S3_SECRET_KEY:-}"
  local region="${STORAGE_S3_REGION:-}"

  if [[ -z "$bucket" || -z "$endpoint" || -z "$access_key" || -z "$secret_key" ]]; then
    record_result "conn.s3" "Spaces/S3" "❌" "Variables S3/Spaces incompletas"
    return
  fi

  if run_cmd "Verificar Spaces" env AWS_ACCESS_KEY_ID="$access_key" AWS_SECRET_ACCESS_KEY="$secret_key" AWS_DEFAULT_REGION="$region" aws s3 ls "s3://$bucket" --endpoint-url "$endpoint" >/dev/null; then
    record_result "conn.s3" "Spaces/S3" "✅" "Acceso a bucket $bucket verificado"
  else
    record_result "conn.s3" "Spaces/S3" "❌" "aws s3 ls falló (bucket/end-point)"
  fi
}

validate_connectivity() {
  validate_postgres
  validate_redis
  validate_s3
}

create_encrypted_backup() {
  if $VALIDATE_ONLY; then
    record_result "backup.skip" "Backup encriptado" "⚠️" "Omitido por --validate-only"
    return
  fi

  if $DRY_RUN; then
    record_result "backup.dry" "Backup encriptado" "⚠️" "Dry-run: se saltó gpg --symmetric"
    return
  fi

  if ! check_command gpg; then
    record_result "backup.gpg" "Backup encriptado" "❌" "gpg no está instalado"
    return
  fi

  local backup_dir="${REPO_ROOT}/.digitalocean"
  mkdir -p "$backup_dir"

  local tmp_env
  tmp_env="$(mktemp)"
  for var in "${ALL_ENV_VARS[@]}"; do
    printf '%s=%s\n' "$var" "${!var:-}" >>"$tmp_env"
  done

  local backup_file="${backup_dir}/env-backup-$(date +%Y%m%d).enc"

  if run_cmd "Crear backup encriptado" gpg --symmetric --cipher-algo AES256 --output "$backup_file" "$tmp_env"; then
    record_result "backup.gpg" "Backup encriptado" "✅" "Archivo ${backup_file##${REPO_ROOT}/} creado"
  else
    record_result "backup.gpg" "Backup encriptado" "❌" "Error ejecutando gpg"
  fi

  rm -f "$tmp_env"
}

ensure_gitignore_entry() {
  local entry=".digitalocean/*.enc"
  local gitignore_file="${REPO_ROOT}/.gitignore"

  if grep -Fxq "$entry" "$gitignore_file" 2>/dev/null; then
    record_result "gitignore" ".gitignore" "✅" "$entry ya estaba registrado"
    return
  fi

  if $DRY_RUN; then
    record_result "gitignore" ".gitignore" "⚠️" "Dry-run: se agregaría '$entry'"
    return
  fi

  printf '\n%s\n' "$entry" >>"$gitignore_file"
  record_result "gitignore" ".gitignore" "✅" "Se agregó $entry"
}

collect_env_statuses() {
  VAR_STATUS_ROWS=()
  for var in "${ALL_ENV_VARS[@]}"; do
    local value="${!var:-}"
    local status detail
    if [[ -z "$value" ]]; then
      status="❌"
      detail="Sin valor"
    else
      status="✅"
      if is_sensitive_var "$var"; then
        detail="Definido (longitud ${#value})"
      else
        detail="$value"
      fi
    fi
    VAR_STATUS_ROWS+=("$var|$status|$detail")
  done
}

print_report() {
  echo "## DigitalOcean Setup Report"
  echo ""
  echo "| Check | Status | Detalle |"
  echo "| --- | --- | --- |"
  for key in "${CHECK_ORDER[@]}"; do
    local label="${CHECK_LABELS[$key]}"
    local status="${CHECK_STATUS[$key]}"
    local details
    details=$(escape_md "${CHECK_DETAILS[$key]}")
    printf '| %s | %s | %s |\n' "$label" "$status" "$details"
  done
  echo ""
  echo "### Variables de entorno"
  echo ""
  echo "| Variable | Status | Detalle |"
  echo "| --- | --- | --- |"
  for row in "${VAR_STATUS_ROWS[@]}"; do
    IFS='|' read -r name status detail <<<"$row"
    detail=$(escape_md "$detail")
    printf '| %s | %s | %s |\n' "$name" "$status" "$detail"
  done
  echo ""
  echo "### Siguiente paso"
  echo ""
  if [[ $GLOBAL_EXIT_CODE -eq 0 ]]; then
    echo "- Despliega/redeploy la App Platform o ejecuta el pipeline para propagar los cambios."
  else
    echo "- Corrige los ítems con ❌/⚠️, vuelve a exportar las variables y reejecuta el script."
  fi
}

main() {
  check_prerequisites
  validate_envs_pre_generation
  generate_missing_secrets
  update_app_platform_envs
  validate_connectivity
  create_encrypted_backup
  ensure_gitignore_entry
  collect_env_statuses
  print_report
  return "$GLOBAL_EXIT_CODE"
}

main
exit $?

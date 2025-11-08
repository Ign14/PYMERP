#!/usr/bin/env bash
#
# production-health-check.sh - Sprint 10 Fase 7: Production health validation
#
# Runs post-deploy health validations across HTTP endpoints, PostgreSQL,
# Flyway history, Redis, and S3/Spaces. Emits JSON so it can feed ops tooling.

set -uo pipefail

SCRIPT_NAME="$(basename "$0")"
HEALTH_URL="https://pymerp.cl/actuator/health"
SWAGGER_URL="https://pymerp.cl/swagger-ui.html"
AUTH_URL="https://pymerp.cl/api/v1/companies"

VERBOSE=false
JSON_ONLY=false
CRITICAL_ONLY=false

usage() {
  cat <<USAGE
Usage: $SCRIPT_NAME [--verbose] [--json-only] [--critical-only]

Options:
  --verbose        Show detailed command output in stderr
  --json-only      Suppress friendly logs; only emit JSON on stdout
  --critical-only  Validate only health, database, and redis checks
  -h, --help       Show this help message
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --verbose)
      VERBOSE=true
      shift
      ;;
    --json-only)
      JSON_ONLY=true
      shift
      ;;
    --critical-only)
      CRITICAL_ONLY=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

log_info() {
  if [[ "$JSON_ONLY" == "true" && "$VERBOSE" != "true" ]]; then
    return
  fi
  echo "[INFO] $1" >&2
}

log_warn() {
  echo "[WARN] $1" >&2
}

log_error() {
  echo "[ERROR] $1" >&2
}

verbose_log() {
  if [[ "$VERBOSE" == "true" ]]; then
    echo "[VERBOSE] $1" >&2
  fi
}

now_ms() {
  date +%s%3N
}

json_escape() {
  local input=${1:-}
  input=${input//\\/\\\\}
  input=${input//$'\n'/\\n}
  input=${input//$'\r'/\\r}
  input=${input//$'\t'/\\t}
  input=${input//\"/\\\"}
  echo -n "$input"
}

ALL_CHECKS=(
  "health_endpoint"
  "swagger_ui"
  "auth_protection"
  "database"
  "flyway_migrations"
  "redis"
  "s3_storage"
)
CRITICAL_CHECKS=("health_endpoint" "database" "redis")

declare -a SELECTED_CHECKS=()
if [[ "$CRITICAL_ONLY" == "true" ]]; then
  SELECTED_CHECKS=("${CRITICAL_CHECKS[@]}")
else
  SELECTED_CHECKS=("${ALL_CHECKS[@]}")
fi

declare -A CHECK_OUTPUT=()
TOTAL_CHECKS=0
PASSED=0
FAILED=0
CONFIG_ERRORS=()
TMP_FILES=()
trap 'for file in "${TMP_FILES[@]}"; do [[ -f "$file" ]] && rm -f "$file"; done' EXIT

add_config_error() {
  CONFIG_ERRORS+=("$1")
}

is_critical_check() {
  local name=$1
  for critical in "${CRITICAL_CHECKS[@]}"; do
    if [[ "$critical" == "$name" ]]; then
      return 0
    fi
  done
  return 1
}

should_run_check() {
  local name=$1
  if [[ "$CRITICAL_ONLY" == "true" ]]; then
    is_critical_check "$name"
    return $?
  fi
  return 0
}

set_check_result() {
  local name=$1
  local status=$2
  local extra=${3:-}
  local count=${4:-true}
  local fragment='"status":"'"$status"'"'
  if [[ -n "$extra" ]]; then
    fragment+=","$extra
  fi
  CHECK_OUTPUT[$name]=$fragment
  if [[ "$count" == "true" ]]; then
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    case "$status" in
      pass)
        PASSED=$((PASSED + 1))
        ;;
      fail)
        FAILED=$((FAILED + 1))
        ;;
    esac
  fi
}

add_required_command() {
  local cmd
  for cmd in "$@"; do
    REQUIRED_COMMANDS[$cmd]=1
  done
}

add_required_env() {
  local var
  for var in "$@"; do
    REQUIRED_ENV[$var]=1
  done
}

declare -A REQUIRED_COMMANDS=([timeout]=1)
declare -A REQUIRED_ENV=()
NEEDS_DATABASE=false
NEEDS_REDIS=false
NEEDS_S3=false

for check in "${SELECTED_CHECKS[@]}"; do
  case "$check" in
    health_endpoint|swagger_ui|auth_protection)
      add_required_command curl
      ;;
  esac
  case "$check" in
    database|flyway_migrations)
      add_required_command psql
      add_required_env POSTGRES_HOST POSTGRES_USER POSTGRES_DB
      NEEDS_DATABASE=true
      ;;
  esac
  case "$check" in
    redis)
      add_required_command redis-cli
      add_required_env REDIS_HOST REDIS_PORT REDIS_PASSWORD
      NEEDS_REDIS=true
      ;;
  esac
  case "$check" in
    s3_storage)
      add_required_command aws
      add_required_env STORAGE_S3_BUCKET S3_ENDPOINT AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY
      NEEDS_S3=true
      ;;
  esac
done

validate_prereqs() {
  local var value cmd
  for var in "${!REQUIRED_ENV[@]}"; do
    if [[ -z ${!var+x} ]]; then
      add_config_error "Environment variable '$var' is required"
      continue
    fi
    value="${!var}"
    if [[ -z "$value" ]]; then
      add_config_error "Environment variable '$var' is required"
    fi
  done

  if [[ "$NEEDS_DATABASE" == "true" ]]; then
    if [[ -z ${POSTGRES_PASSWORD+x} || -z "$POSTGRES_PASSWORD" ]]; then
      if [[ -z ${PGPASSWORD+x} || -z "$PGPASSWORD" ]]; then
        add_config_error "POSTGRES_PASSWORD or PGPASSWORD must be set for database checks"
      fi
    fi
  fi

  for cmd in "${!REQUIRED_COMMANDS[@]}"; do
    if ! command -v "$cmd" >/dev/null 2>&1; then
      add_config_error "Command '$cmd' is required but not installed"
    fi
  done

  if (( ${#CONFIG_ERRORS[@]} > 0 )); then
    for err in "${CONFIG_ERRORS[@]}"; do
      log_error "$err"
    done
    exit 2
  fi
}

validate_prereqs

PGPASSWORD_VALUE=""
declare -a PSQL_CONN_ARGS=()
declare -a REDIS_CLI_BASE=()
S3_BUCKET_VALUE=""
S3_ENDPOINT_VALUE=""

if [[ "$NEEDS_DATABASE" == "true" ]]; then
  PGPASSWORD_VALUE="${POSTGRES_PASSWORD:-${PGPASSWORD:-}}"
  POSTGRES_PORT_VALUE="${POSTGRES_PORT:-5432}"
  PSQL_CONN_ARGS=(-h "$POSTGRES_HOST" -U "$POSTGRES_USER" -d "$POSTGRES_DB" -p "$POSTGRES_PORT_VALUE")
fi

if [[ "$NEEDS_REDIS" == "true" ]]; then
  REDIS_CLI_BASE=(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" --tls -a "$REDIS_PASSWORD")
fi

if [[ "$NEEDS_S3" == "true" ]]; then
  S3_BUCKET_VALUE="$STORAGE_S3_BUCKET"
  S3_ENDPOINT_VALUE="$S3_ENDPOINT"
fi

check_health_endpoint() {
  local name="health_endpoint"
  log_info "Checking health endpoint ($HEALTH_URL)..."
  local start response end http_code body response_time
  start=$(now_ms)
  if ! response=$(curl -f -sS --max-time 10 -w 'HTTPSTATUS:%{http_code}' "$HEALTH_URL" 2>&1); then
    set_check_result "$name" "fail" "\"error\":\"$(json_escape "$response")\""
    return
  fi
  end=$(now_ms)
  http_code=${response##*HTTPSTATUS:}
  body=${response%HTTPSTATUS:*}
  response_time=$((end - start))
  local compact_body="$(echo "$body" | tr -d '\r' | tr -s '\n' ' ')"
  verbose_log "Health endpoint response (HTTP $http_code): $compact_body"
  if [[ "$http_code" != "200" ]]; then
    set_check_result "$name" "fail" "\"error\":\"$(json_escape "Unexpected HTTP status $http_code")\""
    return
  fi
  if [[ "$body" != *'"status":"UP"'* ]]; then
    set_check_result "$name" "fail" "\"error\":\"$(json_escape 'Response missing status="UP"')\""
    return
  fi
  set_check_result "$name" "pass" "\"response_time_ms\":$response_time"
}

check_swagger_ui() {
  local name="swagger_ui"
  log_info "Checking Swagger UI ($SWAGGER_URL)..."
  local response http_code
  if ! response=$(curl -I -sS --max-time 5 -w 'HTTPSTATUS:%{http_code}' "$SWAGGER_URL" 2>&1); then
    set_check_result "$name" "fail" "\"error\":\"$(json_escape "$response")\""
    return
  fi
  http_code=${response##*HTTPSTATUS:}
  verbose_log "Swagger UI HTTP status: $http_code"
  if [[ "$http_code" != "200" ]]; then
    set_check_result "$name" "fail" "\"error\":\"$(json_escape "Expected HTTP 200, got $http_code")\""
    return
  fi
  set_check_result "$name" "pass" ""
}

check_auth_protection() {
  local name="auth_protection"
  log_info "Checking auth protection on $AUTH_URL ..."
  local response http_code
  if ! response=$(curl -I -sS --max-time 10 -w 'HTTPSTATUS:%{http_code}' "$AUTH_URL" 2>&1); then
    set_check_result "$name" "fail" "\"error\":\"$(json_escape "$response")\""
    return
  fi
  http_code=${response##*HTTPSTATUS:}
  verbose_log "Auth endpoint HTTP status: $http_code"
  if [[ "$http_code" != "401" ]]; then
    set_check_result "$name" "fail" "\"error\":\"$(json_escape "Expected HTTP 401, got $http_code")\""
    return
  fi
  set_check_result "$name" "pass" ""
}

run_psql() {
  local sql="$1"
  PGPASSWORD="$PGPASSWORD_VALUE" timeout 10s psql "${PSQL_CONN_ARGS[@]}" --no-align --tuples-only -c "$sql"
}

check_database() {
  local name="database"
  log_info "Checking PostgreSQL connectivity..."
  local start output end latency sanitized
  start=$(now_ms)
  if ! output=$(run_psql "SELECT 1;" 2>&1); then
    set_check_result "$name" "fail" "\"error\":\"$(json_escape "$output")\""
    return
  fi
  end=$(now_ms)
  latency=$((end - start))
  sanitized=$(echo "$output" | tr -d '[:space:]')
  verbose_log "Database SELECT 1 output: $sanitized"
  if [[ "$sanitized" != "1" ]]; then
    set_check_result "$name" "fail" "\"error\":\"$(json_escape "Unexpected response: $sanitized")\""
    return
  fi
  set_check_result "$name" "pass" "\"latency_ms\":$latency"
}

check_flyway_migrations() {
  local name="flyway_migrations"
  log_info "Checking Flyway migration history..."
  local query="SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"
  local output
  if ! output=$(run_psql "$query" 2>&1); then
    set_check_result "$name" "fail" "\"error\":\"$(json_escape "$output")\""
    return
  fi
  if [[ -z "$output" ]]; then
    set_check_result "$name" "fail" "\"error\":\"$(json_escape "No migrations returned")\""
    return
  fi
  local row all_success=true last_version="" line success normalized rows=0
  while IFS='|' read -r rank version description success; do
    [[ -z "$rank" ]] && continue
    ((rows++))
    if [[ -z "$last_version" ]]; then
      last_version="$version"
    fi
    normalized="${success,,}"
    if [[ "$normalized" != "t" && "$normalized" != "true" && "$normalized" != "1" ]]; then
      all_success=false
    fi
  done <<< "$output"

  local total_applied_raw total_applied=0
  if ! total_applied_raw=$(run_psql "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE;" 2>&1); then
    set_check_result "$name" "fail" "\"error\":\"$(json_escape "$total_applied_raw")\""
    return
  fi
  total_applied=$(echo "$total_applied_raw" | tr -d '[:space:]')
  [[ -z "$total_applied" ]] && total_applied=0
  verbose_log "Flyway rows checked: $rows | last version: $last_version | all success: $all_success"
  local escaped_version="$(json_escape "${last_version:-unknown}")"
  local extras="\"last_version\":\"$escaped_version\",\"total_applied\":$total_applied"
  if [[ "$all_success" == "true" ]]; then
    set_check_result "$name" "pass" "$extras"
  else
    set_check_result "$name" "fail" "$extras"
  fi
}

check_redis() {
  local name="redis"
  log_info "Checking Redis availability..."
  local ping_output
  if ! ping_output=$(timeout 10s "${REDIS_CLI_BASE[@]}" PING 2>&1); then
    set_check_result "$name" "fail" "\"error\":\"$(json_escape "$ping_output")\""
    return
  fi
  local trimmed=$(echo "$ping_output" | tr -d '[:space:]')
  verbose_log "Redis PING output: $trimmed"
  if [[ "$trimmed" != "PONG" ]]; then
    set_check_result "$name" "fail" "\"error\":\"$(json_escape "Expected PONG, got $trimmed")\""
    return
  fi
  local key="healthcheck:test:$(date +%s)"
  if ! timeout 10s "${REDIS_CLI_BASE[@]}" SET "$key" "ok" EX 60 >/dev/null 2>&1; then
    set_check_result "$name" "fail" "\"error\":\"$(json_escape "Failed to SET test key")\""
    return
  fi
  local value
  if ! value=$(timeout 10s "${REDIS_CLI_BASE[@]}" GET "$key" 2>&1); then
    set_check_result "$name" "fail" "\"error\":\"$(json_escape "$value")\""
    return
  fi
  local value_trimmed=$(echo "$value" | tr -d '[:space:]')
  if [[ "$value_trimmed" != "ok" ]]; then
    set_check_result "$name" "fail" "\"error\":\"$(json_escape "Unexpected GET response: $value_trimmed")\""
    return
  fi
  set_check_result "$name" "pass" ""
}

check_s3_storage() {
  local name="s3_storage"
  log_info "Checking S3/Spaces uploads..."
  local payload="health-check-$(date +%s)"
  local tmp_file
  if ! tmp_file=$(mktemp /tmp/pymerp-health-XXXXXX); then
    set_check_result "$name" "fail" "\"error\":\"$(json_escape "Unable to create temp file")\""
    return
  fi
  TMP_FILES+=("$tmp_file")
  echo "$payload" > "$tmp_file"
  local destination="s3://$S3_BUCKET_VALUE/health-checks/$payload.txt"
  local upload_output
  if ! upload_output=$(timeout 10s aws s3 cp "$tmp_file" "$destination" --endpoint-url "$S3_ENDPOINT_VALUE" 2>&1); then
    set_check_result "$name" "fail" "\"error\":\"$(json_escape "$upload_output")\""
    return
  fi
  verbose_log "S3 upload output: $upload_output"
  set_check_result "$name" "pass" "\"object\":\"$(json_escape "$destination")\""
}

run_checks() {
  local check
  for check in "${ALL_CHECKS[@]}"; do
    if ! should_run_check "$check"; then
      set_check_result "$check" "skipped" "\"reason\":\"disabled via --critical-only\"" false
      continue
    fi
    case "$check" in
      health_endpoint)
        check_health_endpoint
        ;;
      swagger_ui)
        check_swagger_ui
        ;;
      auth_protection)
        check_auth_protection
        ;;
      database)
        check_database
        ;;
      flyway_migrations)
        check_flyway_migrations
        ;;
      redis)
        check_redis
        ;;
      s3_storage)
        check_s3_storage
        ;;
    esac
  done
}

print_checks_json() {
  local total=${#ALL_CHECKS[@]}
  local idx
  for idx in "${!ALL_CHECKS[@]}"; do
    local name=${ALL_CHECKS[$idx]}
    local data=${CHECK_OUTPUT[$name]:-'"status":"unknown"'}
    local comma=","
    if [[ $idx -eq $((total - 1)) ]]; then
      comma=""
    fi
    printf '    "%s": {%s}%s\n' "$name" "$data" "$comma"
  done
}

run_checks

SUMMARY_STATUS="healthy"
if (( FAILED > 0 )); then
  if (( PASSED == 0 )); then
    SUMMARY_STATUS="down"
  else
    SUMMARY_STATUS="degraded"
  fi
fi

timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
cat <<EOF
{
  "timestamp": "$timestamp",
  "status": "$SUMMARY_STATUS",
  "checks": {
$(print_checks_json)
  },
  "summary": {
    "total": $TOTAL_CHECKS,
    "passed": $PASSED,
    "failed": $FAILED
  }
}

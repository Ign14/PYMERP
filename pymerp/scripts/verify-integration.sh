#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
errors=()

log() {
  printf "  • %s\n" "$1"
}

check_pattern() {
  local description=$1
  shift
  if "$@"; then
    log "$description"
  else
    errors+=("$description")
  fi
}

log "🔎 Validando criterios A–F antes de ejecutar los comandos"
check_pattern "A1 Secrets validation en production.yml" \
  grep -q "app\\.security\\.secrets-validation\\.enabled: true" "$ROOT/backend/src/main/resources/application-prod.yml"
check_pattern "A2 SecretsValidator cubre JWT y Billing" \
  grep -q "class SecretsValidator" "$ROOT/backend/src/main/java/com/datakomerz/pymes/config/SecretsValidator.java"
if grep -Eiq "change-me|default" "$ROOT/.env.example"; then
  errors+=("A3 .env.example contiene placeholders con change-me/default")
else
  log "A3 .env.example sin change-me/default"
fi
for var in POSTGRES_HOST POSTGRES_PORT POSTGRES_DB POSTGRES_USER POSTGRES_PASSWORD \
  JWT_SECRET BILLING_WEBHOOK_SECRET BILLING_CRYPTO_SECRET STORAGE_S3_BUCKET STORAGE_S3_ACCESS_KEY \
  STORAGE_S3_SECRET_KEY REDIS_HOST REDIS_PORT REDIS_PASSWORD WEBHOOKS_HMAC_SECRET; do
  check_pattern "A4 .env.example define $var" \
    grep -q "^$var=" "$ROOT/.env.example"
done

check_pattern "B1 Customer soft delete + ValueNormalizer" \
  grep -q "@SQLDelete" "$ROOT/backend/src/main/java/com/datakomerz/pymes/customers/Customer.java"
check_pattern "B2 CustomerService usa ValueNormalizer" \
  grep -q "valueNormalizer" "$ROOT/backend/src/main/java/com/datakomerz/pymes/customers/CustomerService.java"
check_pattern "B3 API incluye flag includeInactive" \
  grep -q "includeInactive" "$ROOT/backend/src/main/java/com/datakomerz/pymes/customers/api/CustomerController.java"
check_pattern "B4 Prueba de ciclo de clientes" \
  grep -q "lifecycleCreateListUpdateDeleteRespectsSoftDeleteFilters" "$ROOT/backend/src/test/java/com/datakomerz/pymes/customers/integration/CustomersIT.java"

check_pattern "C1 BillingController exige Idempotency-Key" \
  grep -q "Idempotency-Key" "$ROOT/backend/src/main/java/com/datakomerz/pymes/billing/api/BillingController.java"
check_pattern "C2 Idempotency store y test declarados" \
  grep -q "class BillingIdempotencyIT" "$ROOT/backend/src/test/java/com/datakomerz/pymes/billing/integration/BillingIdempotencyIT.java"

check_pattern "D1 Header X-Company-Id unificado" \
  grep -q "X-Company-Id" "$ROOT/backend/src/main/java/com/datakomerz/pymes/multitenancy/TenantInterceptor.java"
check_pattern "D2 MultitenancyIT asegura 403 cross-tenant" \
  grep -q "class MultitenancyIT" "$ROOT/backend/src/test/java/com/datakomerz/pymes/customers/integration/MultitenancyIT.java"

check_pattern "E1 Pipeline CI usa jacoco y cobertura backend" \
  grep -q "jacocoTestCoverageVerification" "$ROOT/.github/workflows/ci.yml"
check_pattern "E2 UI Vitest tiene umbrales" \
  grep -q "coverage:" "$ROOT/ui/vite.config.ts"
check_pattern "E3 Flutter coverage script está disponible" \
  test -f "$ROOT/scripts/check_flutter_coverage.py"

check_pattern "F1 AbstractIT + containers" \
  grep -q "class AbstractIT" "$ROOT/backend/src/test/java/com/datakomerz/pymes/integration/AbstractIT.java"

if [ "${#errors[@]}" -ne 0 ]; then
  echo "🚨 Faltan los siguientes chequeos de patrones:"
  for failed in "${errors[@]}"; do
    printf "    - %s\n" "$failed"
  done
  exit 1
fi

echo "✅ Criterios A–F validados, iniciando comandos de verificación"

(
  cd "$ROOT/backend"
  ./gradlew clean test
)

(
  cd "$ROOT/ui"
  npm ci
  npm run test -- --coverage
  npm run build
)

(
  cd "$ROOT/app_flutter"
  flutter pub get
  flutter test --coverage
)

python3 "$ROOT/scripts/check_flutter_coverage.py" "$ROOT/app_flutter/coverage/lcov.info" 50

cleanup() {
  docker compose down -v >/dev/null 2>&1 || true
}

trap cleanup EXIT

docker compose up -d

probe() {
  local url=$1
  for attempt in $(seq 1 30); do
    if curl -sf "$url" >/dev/null; then
      echo "✔ $url listo"
      return 0
    fi
    sleep 2
  done
  return 1
}

probe http://localhost:8081/actuator/health
probe http://localhost:8081/actuator/info
probe http://localhost:8081/api/v1/ping
if ! curl -sf http://localhost:8081/swagger-ui.html && ! curl -sf http://localhost:8081/swagger-ui/index.html; then
  echo "❌ Swagger UI no respondió"
  exit 1
fi

cleanup
trap - EXIT

echo "✅ Comandos de verificación ejecutados correctamente"

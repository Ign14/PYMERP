#!/bin/bash
# validate-env.sh - Validates all required environment variables for production

set -e

echo "🔍 Validating production environment variables..."

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Required variables
REQUIRED_VARS=(
  "POSTGRES_HOST"
  "POSTGRES_PORT"
  "POSTGRES_DB"
  "POSTGRES_USER"
  "POSTGRES_PASSWORD"
  "REDIS_HOST"
  "REDIS_PORT"
  "REDIS_PASSWORD"
  "JWT_SECRET"
  "BILLING_CRYPTO_SECRET"
  "BILLING_WEBHOOK_SECRET"
  "BILLING_DOCUMENTS_BASE_URL"
  "WEBHOOKS_HMAC_SECRET"
  "MAIL_HOST"
  "MAIL_PORT"
  "MAIL_USERNAME"
  "MAIL_PASSWORD"
  "S3_ENDPOINT"
  "STORAGE_S3_BUCKET"
  "STORAGE_S3_ACCESS_KEY"
  "STORAGE_S3_SECRET_KEY"
)

# Optional but recommended
RECOMMENDED_VARS=(
  "APP_CORS_ALLOWED_ORIGINS[0]"
  "STORAGE_S3_REGION"
  "REDIS_SSL_ENABLED"
)

missing=()
weak_secrets=()

# Check required variables
for var in "${REQUIRED_VARS[@]}"; do
  # Remove array notation for checking
  var_name="${var%%\[*\]}"
  
  if [ -z "${!var_name}" ]; then
    missing+=("$var")
  else
    # Check if secret is too weak (< 32 chars for sensitive vars)
    if [[ "$var_name" =~ (SECRET|PASSWORD|KEY) ]]; then
      value="${!var_name}"
      if [ ${#value} -lt 32 ]; then
        weak_secrets+=("$var_name (${#value} chars, recommended: 32+)")
      fi
    fi
  fi
done

# Check recommended variables
missing_recommended=()
for var in "${RECOMMENDED_VARS[@]}"; do
  var_name="${var%%\[*\]}"
  if [ -z "${!var_name}" ]; then
    missing_recommended+=("$var")
  fi
done

# Report results
echo ""
if [ ${#missing[@]} -eq 0 ] && [ ${#weak_secrets[@]} -eq 0 ]; then
  echo -e "${GREEN}✅ All required environment variables are properly set${NC}"
else
  if [ ${#missing[@]} -gt 0 ]; then
    echo -e "${RED}❌ Missing required environment variables:${NC}"
    printf '   - %s\n' "${missing[@]}"
    echo ""
  fi
  
  if [ ${#weak_secrets[@]} -gt 0 ]; then
    echo -e "${YELLOW}⚠️  Weak secrets detected (use stronger values):${NC}"
    printf '   - %s\n' "${weak_secrets[@]}"
    echo ""
  fi
fi

if [ ${#missing_recommended[@]} -gt 0 ]; then
  echo -e "${YELLOW}ℹ️  Recommended variables not set (using defaults):${NC}"
  printf '   - %s\n' "${missing_recommended[@]}"
  echo ""
fi

# Validate JWT_SECRET strength
if [ -n "$JWT_SECRET" ]; then
  jwt_length=${#JWT_SECRET}
  if [ $jwt_length -lt 64 ]; then
    echo -e "${RED}❌ JWT_SECRET is too short ($jwt_length chars). Minimum recommended: 64 chars${NC}"
    echo "   Generate new secret: openssl rand -base64 64"
    exit 1
  else
    echo -e "${GREEN}✅ JWT_SECRET meets security requirements ($jwt_length chars)${NC}"
  fi
fi

# Validate CORS configuration
if [ -z "${APP_CORS_ALLOWED_ORIGINS[0]}" ]; then
  echo -e "${RED}❌ CORS origins not configured. Application will reject all cross-origin requests.${NC}"
  echo "   Set: APP_CORS_ALLOWED_ORIGINS[0]=https://yourdomain.com"
  exit 1
fi

# Final check
if [ ${#missing[@]} -gt 0 ] || [ ${#weak_secrets[@]} -gt 0 ]; then
  echo -e "${RED}❌ Environment validation FAILED${NC}"
  exit 1
fi

echo ""
echo -e "${GREEN}✅ Environment validation PASSED${NC}"
echo "   All required variables are set with strong values"
exit 0

#!/usr/bin/env bash
#
# pre-deploy-checklist.sh - Sprint 10 Fase 5: Comprehensive pre-deployment validation
#
# Verifies all prerequisites are met before deploying to production:
# - GitHub secrets configured
# - DigitalOcean infrastructure provisioned
# - DNS propagated
# - Environment variables configured
# - Dockerfile builds successfully
# - CI pipeline passed
# - Database backups exist (if production has data)

set -euo pipefail
IFS=$'\n\t'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Counters
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0
WARNING_CHECKS=0

# Report file
REPORT_FILE="${REPO_ROOT}/docs/pre-deploy-report-$(date +%Y%m%d-%H%M%S).md"

# Helper functions
log_info() {
  echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
  echo -e "${GREEN}✅ $1${NC}"
  ((PASSED_CHECKS++)) || true
}

log_fail() {
  echo -e "${RED}❌ $1${NC}"
  ((FAILED_CHECKS++)) || true
}

log_warning() {
  echo -e "${YELLOW}⚠️  $1${NC}"
  ((WARNING_CHECKS++)) || true
}

check_command() {
  local cmd=$1
  local name=${2:-$cmd}
  
  ((TOTAL_CHECKS++)) || true
  if command -v "$cmd" &> /dev/null; then
    log_success "$name is installed"
    return 0
  else
    log_fail "$name is not installed"
    return 1
  fi
}

# Initialize report
init_report() {
  cat > "$REPORT_FILE" <<EOF
# Pre-Deployment Validation Report

**Generated**: $(date '+%Y-%m-%d %H:%M:%S')  
**Branch**: $(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")  
**Commit**: $(git rev-parse --short HEAD 2>/dev/null || echo "unknown")  
**Environment**: Production (pymerp.cl)

---

## Summary

EOF
}

# Finalize report
finalize_report() {
  cat >> "$REPORT_FILE" <<EOF

---

## Final Status

- **Total Checks**: $TOTAL_CHECKS
- **Passed**: $PASSED_CHECKS ✅
- **Failed**: $FAILED_CHECKS ❌
- **Warnings**: $WARNING_CHECKS ⚠️

EOF

  if [ $FAILED_CHECKS -eq 0 ]; then
    cat >> "$REPORT_FILE" <<EOF
### ✅ **READY TO DEPLOY**

All critical checks passed. You can proceed with deployment:

\`\`\`bash
# Merge to main and trigger CD pipeline
git checkout main
git merge feature/sprint-10-production-deploy
git push origin main

# Or manually deploy
doctl apps create --spec do-app-spec.yml
\`\`\`

EOF
  else
    cat >> "$REPORT_FILE" <<EOF
### ❌ **NOT READY TO DEPLOY**

$FAILED_CHECKS critical check(s) failed. Please resolve the issues above before deploying.

EOF
  fi

  echo ""
  log_info "Report saved to: $REPORT_FILE"
  
  if [ $FAILED_CHECKS -eq 0 ]; then
    log_success "All checks passed! Ready to deploy 🚀"
    exit 0
  else
    log_fail "$FAILED_CHECKS check(s) failed. Review the report."
    exit 1
  fi
}

# Check 1: Required CLI tools
check_cli_tools() {
  log_info "Checking required CLI tools..."
  echo "" >> "$REPORT_FILE"
  echo "## 1. CLI Tools" >> "$REPORT_FILE"
  echo "" >> "$REPORT_FILE"
  
  local all_installed=true
  
  if check_command "git" "Git"; then
    echo "- ✅ Git: $(git --version)" >> "$REPORT_FILE"
  else
    echo "- ❌ Git: Not installed" >> "$REPORT_FILE"
    all_installed=false
  fi
  
  if check_command "docker" "Docker"; then
    echo "- ✅ Docker: $(docker --version)" >> "$REPORT_FILE"
  else
    echo "- ❌ Docker: Not installed" >> "$REPORT_FILE"
    all_installed=false
  fi
  
  if check_command "doctl" "DigitalOcean CLI"; then
    echo "- ✅ doctl: $(doctl version)" >> "$REPORT_FILE"
  else
    echo "- ❌ doctl: Not installed" >> "$REPORT_FILE"
    all_installed=false
  fi
  
  if check_command "curl" "curl"; then
    echo "- ✅ curl: $(curl --version | head -n1)" >> "$REPORT_FILE"
  else
    echo "- ❌ curl: Not installed" >> "$REPORT_FILE"
    all_installed=false
  fi
  
  if ! $all_installed; then
    echo "" >> "$REPORT_FILE"
    echo "**Action**: Install missing tools before proceeding." >> "$REPORT_FILE"
  fi
}

# Check 2: GitHub repository status
check_github_status() {
  log_info "Checking GitHub repository status..."
  echo "" >> "$REPORT_FILE"
  echo "## 2. GitHub Repository" >> "$REPORT_FILE"
  echo "" >> "$REPORT_FILE"
  
  ((TOTAL_CHECKS++)) || true
  
  # Check if on correct branch
  local current_branch=$(git rev-parse --abbrev-ref HEAD)
  if [ "$current_branch" = "feature/sprint-10-production-deploy" ]; then
    log_success "On deployment branch"
    echo "- ✅ Branch: $current_branch" >> "$REPORT_FILE"
  else
    log_warning "Not on deployment branch (current: $current_branch)"
    echo "- ⚠️ Branch: $current_branch (expected: feature/sprint-10-production-deploy)" >> "$REPORT_FILE"
  fi
  
  # Check for uncommitted changes
  ((TOTAL_CHECKS++)) || true
  if git diff-index --quiet HEAD --; then
    log_success "No uncommitted changes"
    echo "- ✅ Working tree: Clean" >> "$REPORT_FILE"
  else
    log_fail "Uncommitted changes detected"
    echo "- ❌ Working tree: Uncommitted changes detected" >> "$REPORT_FILE"
    echo "  \`\`\`" >> "$REPORT_FILE"
    git status --short >> "$REPORT_FILE"
    echo "  \`\`\`" >> "$REPORT_FILE"
  fi
  
  # Check if branch is pushed
  ((TOTAL_CHECKS++)) || true
  if git rev-parse --abbrev-ref --symbolic-full-name @{u} &> /dev/null; then
    local local_commit=$(git rev-parse HEAD)
    local remote_commit=$(git rev-parse @{u})
    
    if [ "$local_commit" = "$remote_commit" ]; then
      log_success "Branch is up-to-date with remote"
      echo "- ✅ Remote sync: Up-to-date" >> "$REPORT_FILE"
    else
      log_fail "Local commits not pushed to remote"
      echo "- ❌ Remote sync: Local commits need to be pushed" >> "$REPORT_FILE"
    fi
  else
    log_fail "Branch not tracking remote"
    echo "- ❌ Remote sync: Branch not tracking remote" >> "$REPORT_FILE"
  fi
}

# Check 3: GitHub Secrets
check_github_secrets() {
  log_info "Checking GitHub secrets configuration..."
  echo "" >> "$REPORT_FILE"
  echo "## 3. GitHub Secrets" >> "$REPORT_FILE"
  echo "" >> "$REPORT_FILE"
  
  # Note: We can't directly check if secrets exist, but we can check if they're documented
  local required_secrets=(
    "DIGITALOCEAN_ACCESS_TOKEN"
    "DIGITALOCEAN_APP_ID"
  )
  
  local optional_secrets=(
    "SLACK_WEBHOOK_URL"
    "TEST_BILLING_CRYPTO_SECRET"
    "TEST_BILLING_WEBHOOK_SECRET"
  )
  
  echo "### Required Secrets" >> "$REPORT_FILE"
  echo "" >> "$REPORT_FILE"
  for secret in "${required_secrets[@]}"; do
    echo "- ⚠️ $secret (verify in GitHub Settings → Secrets)" >> "$REPORT_FILE"
  done
  
  echo "" >> "$REPORT_FILE"
  echo "### Optional Secrets" >> "$REPORT_FILE"
  echo "" >> "$REPORT_FILE"
  for secret in "${optional_secrets[@]}"; do
    echo "- ℹ️ $secret (optional)" >> "$REPORT_FILE"
  done
  
  ((TOTAL_CHECKS++)) || true
  log_warning "Verify secrets manually at: https://github.com/Ign14/PYMERP/settings/secrets/actions"
  ((WARNING_CHECKS++)) || true
  
  echo "" >> "$REPORT_FILE"
  echo "**Action**: Verify all required secrets are configured in GitHub repository settings." >> "$REPORT_FILE"
}

# Check 4: DigitalOcean Infrastructure
check_digitalocean_infra() {
  log_info "Checking DigitalOcean infrastructure..."
  echo "" >> "$REPORT_FILE"
  echo "## 4. DigitalOcean Infrastructure" >> "$REPORT_FILE"
  echo "" >> "$REPORT_FILE"
  
  # Check doctl authentication
  ((TOTAL_CHECKS++)) || true
  if doctl account get &> /dev/null; then
    log_success "doctl is authenticated"
    local account_email=$(doctl account get --format Email --no-header 2>/dev/null || echo "unknown")
    echo "- ✅ doctl authenticated (account: $account_email)" >> "$REPORT_FILE"
  else
    log_fail "doctl not authenticated"
    echo "- ❌ doctl not authenticated" >> "$REPORT_FILE"
    echo "  **Action**: Run \`doctl auth init\`" >> "$REPORT_FILE"
    return
  fi
  
  # Check if APP_ID is set
  ((TOTAL_CHECKS++)) || true
  if [ -n "${APP_ID:-}" ]; then
    log_info "Checking App Platform app (ID: $APP_ID)..."
    
    if doctl apps get "$APP_ID" &> /dev/null; then
      log_success "App Platform app exists"
      local app_name=$(doctl apps get "$APP_ID" --format Spec.Name --no-header 2>/dev/null || echo "unknown")
      local app_status=$(doctl apps get "$APP_ID" --format ActiveDeployment.Phase --no-header 2>/dev/null || echo "unknown")
      echo "- ✅ App Platform: $app_name (status: $app_status)" >> "$REPORT_FILE"
    else
      log_fail "App Platform app not found"
      echo "- ❌ App Platform: App ID $APP_ID not found" >> "$REPORT_FILE"
    fi
  else
    log_warning "APP_ID not set in environment"
    echo "- ⚠️ App Platform: APP_ID not set (skip check)" >> "$REPORT_FILE"
    ((WARNING_CHECKS++)) || true
  fi
  
  # Check Container Registry
  ((TOTAL_CHECKS++)) || true
  if doctl registry get &> /dev/null; then
    log_success "Container Registry exists"
    local registry_name=$(doctl registry get --format Name --no-header 2>/dev/null || echo "unknown")
    echo "- ✅ Container Registry: $registry_name" >> "$REPORT_FILE"
  else
    log_warning "Container Registry not found or not accessible"
    echo "- ⚠️ Container Registry: Not found or not accessible" >> "$REPORT_FILE"
    ((WARNING_CHECKS++)) || true
  fi
  
  # Check databases (requires database cluster name)
  echo "" >> "$REPORT_FILE"
  echo "### Managed Databases" >> "$REPORT_FILE"
  echo "" >> "$REPORT_FILE"
  echo "⚠️ Verify manually that PostgreSQL and Redis clusters are provisioned:" >> "$REPORT_FILE"
  echo "\`\`\`bash" >> "$REPORT_FILE"
  echo "doctl databases list" >> "$REPORT_FILE"
  echo "\`\`\`" >> "$REPORT_FILE"
}

# Check 5: DNS Configuration
check_dns() {
  log_info "Checking DNS configuration..."
  echo "" >> "$REPORT_FILE"
  echo "## 5. DNS Configuration" >> "$REPORT_FILE"
  echo "" >> "$REPORT_FILE"
  
  local domain="pymerp.cl"
  
  # Check apex domain
  ((TOTAL_CHECKS++)) || true
  if dig +short "$domain" | grep -q .; then
    local dns_result=$(dig +short "$domain" | head -n1)
    log_success "DNS resolves for $domain"
    echo "- ✅ $domain → $dns_result" >> "$REPORT_FILE"
  else
    log_fail "DNS does not resolve for $domain"
    echo "- ❌ $domain: No DNS records found" >> "$REPORT_FILE"
  fi
  
  # Check www subdomain
  ((TOTAL_CHECKS++)) || true
  if dig +short "www.$domain" | grep -q .; then
    local dns_result=$(dig +short "www.$domain" | head -n1)
    log_success "DNS resolves for www.$domain"
    echo "- ✅ www.$domain → $dns_result" >> "$REPORT_FILE"
  else
    log_warning "DNS does not resolve for www.$domain"
    echo "- ⚠️ www.$domain: No DNS records found" >> "$REPORT_FILE"
    ((WARNING_CHECKS++)) || true
  fi
  
  # Check app subdomain
  ((TOTAL_CHECKS++)) || true
  if dig +short "app.$domain" | grep -q .; then
    local dns_result=$(dig +short "app.$domain" | head -n1)
    log_success "DNS resolves for app.$domain"
    echo "- ✅ app.$domain → $dns_result" >> "$REPORT_FILE"
  else
    log_warning "DNS does not resolve for app.$domain"
    echo "- ⚠️ app.$domain: No DNS records found" >> "$REPORT_FILE"
    ((WARNING_CHECKS++)) || true
  fi
}

# Check 6: Environment Variables
check_environment_variables() {
  log_info "Checking environment variables..."
  echo "" >> "$REPORT_FILE"
  echo "## 6. Environment Variables" >> "$REPORT_FILE"
  echo "" >> "$REPORT_FILE"
  
  local required_vars=(
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
    "STORAGE_S3_BUCKET"
    "STORAGE_S3_ACCESS_KEY"
    "STORAGE_S3_SECRET_KEY"
  )
  
  local missing_vars=()
  
  for var in "${required_vars[@]}"; do
    ((TOTAL_CHECKS++)) || true
    if [ -n "${!var:-}" ]; then
      echo "- ✅ $var (set)" >> "$REPORT_FILE"
    else
      missing_vars+=("$var")
      echo "- ❌ $var (missing)" >> "$REPORT_FILE"
    fi
  done
  
  if [ ${#missing_vars[@]} -eq 0 ]; then
    log_success "All required environment variables are set"
  else
    log_fail "${#missing_vars[@]} required environment variables are missing"
    echo "" >> "$REPORT_FILE"
    echo "**Action**: Set missing variables using \`scripts/setup-digitalocean-env.sh\`" >> "$REPORT_FILE"
  fi
}

# Check 7: Dockerfile Build
check_dockerfile_build() {
  log_info "Checking Dockerfile build..."
  echo "" >> "$REPORT_FILE"
  echo "## 7. Dockerfile Build" >> "$REPORT_FILE"
  echo "" >> "$REPORT_FILE"
  
  ((TOTAL_CHECKS++)) || true
  
  local dockerfile="${REPO_ROOT}/backend/Dockerfile"
  
  if [ ! -f "$dockerfile" ]; then
    log_fail "Dockerfile not found"
    echo "- ❌ Dockerfile not found at: $dockerfile" >> "$REPORT_FILE"
    return
  fi
  
  log_info "Testing Docker build (this may take a few minutes)..."
  
  if docker build -t pymerp-backend-test:latest -f "$dockerfile" "${REPO_ROOT}/backend" &> /tmp/docker-build.log; then
    log_success "Dockerfile builds successfully"
    echo "- ✅ Docker build: Success" >> "$REPORT_FILE"
    
    # Get image size
    local image_size=$(docker images pymerp-backend-test:latest --format "{{.Size}}" 2>/dev/null || echo "unknown")
    echo "  - Image size: $image_size" >> "$REPORT_FILE"
    
    # Cleanup
    docker rmi pymerp-backend-test:latest &> /dev/null || true
  else
    log_fail "Dockerfile build failed"
    echo "- ❌ Docker build: Failed" >> "$REPORT_FILE"
    echo "  \`\`\`" >> "$REPORT_FILE"
    tail -n 20 /tmp/docker-build.log >> "$REPORT_FILE"
    echo "  \`\`\`" >> "$REPORT_FILE"
  fi
}

# Check 8: CI Pipeline Status
check_ci_pipeline() {
  log_info "Checking CI pipeline status..."
  echo "" >> "$REPORT_FILE"
  echo "## 8. CI Pipeline" >> "$REPORT_FILE"
  echo "" >> "$REPORT_FILE"
  
  ((TOTAL_CHECKS++)) || true
  
  # Check if .github/workflows/ci.yml exists
  local ci_workflow="${REPO_ROOT}/.github/workflows/ci.yml"
  
  if [ ! -f "$ci_workflow" ]; then
    log_fail "CI workflow not found"
    echo "- ❌ CI workflow not found at: $ci_workflow" >> "$REPORT_FILE"
    return
  fi
  
  log_success "CI workflow file exists"
  echo "- ✅ CI workflow: .github/workflows/ci.yml exists" >> "$REPORT_FILE"
  
  # Note: We can't check GitHub Actions status from CLI without GitHub CLI
  echo "- ⚠️ CI status: Verify manually at https://github.com/Ign14/PYMERP/actions" >> "$REPORT_FILE"
  log_warning "Verify CI passed manually at: https://github.com/Ign14/PYMERP/actions"
  ((WARNING_CHECKS++)) || true
}

# Check 9: Production Configuration
check_production_config() {
  log_info "Checking production configuration files..."
  echo "" >> "$REPORT_FILE"
  echo "## 9. Production Configuration" >> "$REPORT_FILE"
  echo "" >> "$REPORT_FILE"
  
  local files=(
    "backend/src/main/resources/application-prod.yml"
    "backend/Dockerfile"
    "do-app-spec.yml"
    "docs/ENV_VARIABLES.md"
    ".github/workflows/ci.yml"
    ".github/workflows/deploy.yml"
  )
  
  for file in "${files[@]}"; do
    ((TOTAL_CHECKS++)) || true
    local full_path="${REPO_ROOT}/${file}"
    
    if [ -f "$full_path" ]; then
      log_success "$file exists"
      echo "- ✅ $file" >> "$REPORT_FILE"
    else
      log_fail "$file not found"
      echo "- ❌ $file (missing)" >> "$REPORT_FILE"
    fi
  done
  
  # Validate application-prod.yml doesn't have insecure defaults
  ((TOTAL_CHECKS++)) || true
  local app_prod="${REPO_ROOT}/backend/src/main/resources/application-prod.yml"
  
  if [ -f "$app_prod" ]; then
    if grep -q "password123\|changeme\|secret123" "$app_prod"; then
      log_fail "application-prod.yml contains insecure defaults"
      echo "- ❌ Security: Insecure defaults found in application-prod.yml" >> "$REPORT_FILE"
    else
      log_success "application-prod.yml has no insecure defaults"
      echo "- ✅ Security: No insecure defaults in application-prod.yml" >> "$REPORT_FILE"
    fi
  fi
}

# Check 10: Database Backup
check_database_backup() {
  log_info "Checking database backup status..."
  echo "" >> "$REPORT_FILE"
  echo "## 10. Database Backup" >> "$REPORT_FILE"
  echo "" >> "$REPORT_FILE"
  
  # This is only relevant if production database already has data
  # For initial deployment, this is not critical
  
  ((TOTAL_CHECKS++)) || true
  log_warning "Database backup check skipped (initial deployment)"
  echo "- ⚠️ Database backup: Skipped (initial deployment)" >> "$REPORT_FILE"
  echo "  - For future deployments, ensure backups exist before deploying" >> "$REPORT_FILE"
  echo "  - DigitalOcean managed databases have automatic daily backups" >> "$REPORT_FILE"
  ((WARNING_CHECKS++)) || true
}

# Main execution
main() {
  echo ""
  log_info "🚀 Pre-Deployment Validation Checklist"
  log_info "========================================"
  echo ""
  
  init_report
  
  check_cli_tools
  check_github_status
  check_github_secrets
  check_digitalocean_infra
  check_dns
  check_environment_variables
  check_dockerfile_build
  check_ci_pipeline
  check_production_config
  check_database_backup
  
  finalize_report
}

# Run main
main "$@"

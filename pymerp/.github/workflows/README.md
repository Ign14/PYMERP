# GitHub Actions Workflows

This directory contains CI/CD pipelines for automated testing and deployment.

## Workflows

### 1. CI Pipeline (`ci.yml`)
**Trigger**: Push to any branch, PRs to main/develop

**Steps**:
- ✅ Checkout code
- ✅ Setup JDK 21 (Eclipse Temurin)
- ✅ Validate Gradle wrapper
- ✅ Build project (`./gradlew clean build`)
- ✅ Run unit tests (`./gradlew test`)
- ✅ Code style check (Checkstyle)
- ✅ Code formatting check (Spotless)
- ✅ Security scan (Trivy)
- ✅ Upload test reports and artifacts

**Required Secrets** (optional, use test defaults if not set):
- `TEST_BILLING_CRYPTO_SECRET`
- `TEST_BILLING_WEBHOOK_SECRET`

### 2. CD Pipeline (`deploy.yml`)
**Trigger**: Push to main branch (manual dispatch available)

**Steps**:
- ✅ Checkout code
- ✅ Install doctl (DigitalOcean CLI)
- ✅ Build Docker image with multi-stage optimization
- ✅ Push to DigitalOcean Container Registry
- ✅ Deploy to App Platform
- ✅ Wait for startup (60s)
- ✅ Health check (10 retries)
- ✅ Rollback on failure
- ✅ Slack notification
- ✅ Smoke tests (health, info, swagger, auth)

**Required Secrets**:
```bash
# DigitalOcean
DIGITALOCEAN_ACCESS_TOKEN=dop_v1_xxxxx...  # API token
DIGITALOCEAN_APP_ID=your-app-id-uuid       # App Platform app ID

# Slack (optional)
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/xxx  # Deployment notifications
```

## Setting Up Secrets

### GitHub Repository Secrets
1. Go to: **Settings → Secrets and variables → Actions**
2. Click **New repository secret**
3. Add each secret from the list above

### DigitalOcean Access Token
```bash
# Create token in DigitalOcean dashboard
1. Go to API → Tokens/Keys
2. Generate New Token
3. Name: "github-actions-pymerp"
4. Scopes: read + write
5. Copy token (shown only once!)
6. Add to GitHub as DIGITALOCEAN_ACCESS_TOKEN
```

### Get App Platform ID
```bash
# Install doctl locally
brew install doctl  # macOS
# or download from: https://github.com/digitalocean/doctl/releases

# Authenticate
doctl auth init

# List apps to find your app ID
doctl apps list --format ID,Spec.Name
```

### Slack Webhook (Optional)
```bash
1. Go to: https://api.slack.com/apps
2. Create New App → From scratch
3. Enable Incoming Webhooks
4. Add New Webhook to Workspace
5. Select channel (e.g., #deployments)
6. Copy webhook URL
7. Add to GitHub as SLACK_WEBHOOK_URL
```

## Manual Deployment

Trigger deployment without pushing to main:

```bash
1. Go to: Actions → CD - Deploy to Production
2. Click "Run workflow"
3. Select branch: main
4. Click "Run workflow"
```

## Monitoring Workflows

### View Workflow Runs
- **All runs**: `https://github.com/YOUR_ORG/PYMERP/actions`
- **Specific workflow**: Click workflow name → See all runs
- **Live logs**: Click on running workflow → Click job name

### Artifacts
- Test reports: Available for 7 days after run
- Build JARs: Available for 7 days after successful build
- Download: Actions → Workflow run → Artifacts section

## Troubleshooting

### CI Fails: "Command not found: ./gradlew"
**Fix**: Ensure `gradlew` has execute permissions:
```bash
git update-index --chmod=+x backend/gradlew
git commit -m "fix: Make gradlew executable"
```

### CI Fails: Tests fail with environment variables
**Fix**: Add test secrets or update `.github/workflows/ci.yml` defaults

### CD Fails: "doctl: command not found"
**Fix**: Check `digitalocean/action-doctl@v2` version compatibility

### CD Fails: Health check timeout
**Fix**: 
1. Check App Platform logs: `doctl apps logs $APP_ID`
2. Increase timeout in `deploy.yml` (currently 10 attempts × 10s)
3. Verify all environment variables are set in App Platform

### CD Fails: Rollback doesn't work
**Fix**: Ensure at least 2 successful deployments exist before rollback attempts

## Best Practices

1. **Never commit secrets** - Always use GitHub Secrets
2. **Test locally first** - Run `./gradlew clean build test` before pushing
3. **Use feature branches** - CI runs on all branches, CD only on main
4. **Monitor deployments** - Check Slack notifications or GitHub Actions UI
5. **Review logs** - Download artifacts for failed builds

## Environment-Specific Configs

### Development
- Profile: `dev`
- Database: H2 in-memory
- Flyway: Includes dev seed data
- CORS: localhost:5173

### Production
- Profile: `prod`
- Database: DigitalOcean PostgreSQL
- Flyway: Excludes dev seed data
- CORS: pymerp.cl, www.pymerp.cl, app.pymerp.cl

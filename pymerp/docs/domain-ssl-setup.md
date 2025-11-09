# Domain and SSL Configuration Guide

This guide covers DNS configuration, SSL/TLS setup, and domain verification for deploying PYMERP to production at `pymerp.cl`.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [DNS Configuration](#dns-configuration)
3. [SSL/TLS Setup](#ssltls-setup)
4. [HTTP to HTTPS Redirects](#http-to-https-redirects)
5. [CORS Validation](#cors-validation)
6. [Health Check Endpoints](#health-check-endpoints)
7. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Access
- ✅ Domain registrar account (where `pymerp.cl` is registered)
- ✅ DigitalOcean App Platform deployed (from Phase 2)
- ✅ DNS management access (registrar or Cloudflare)

### Tools
```bash
# Install dig (DNS lookup)
# Ubuntu/Debian
sudo apt-get install dnsutils

# macOS (already included)
# Windows: https://www.isc.org/download/

# Install openssl (SSL verification)
# Usually pre-installed on Linux/macOS
# Windows: https://slproweb.com/products/Win32OpenSSL.html
```

---

## DNS Configuration

### 1. Get App Platform Public IP

```bash
# Get your App Platform app ID (from Phase 2)
export APP_ID="your-app-id-from-phase-2"

# Get the default domain assigned by DigitalOcean
doctl apps get $APP_ID --format DefaultIngress

# Example output: pymerp-backend-abc123.ondigitalocean.app
```

DigitalOcean App Platform uses **automatic routing**. You don't need a static IP - instead, use a **CNAME** record pointing to the default DigitalOcean domain.

### 2. Configure DNS Records

**Option A: Using Domain Registrar DNS**

Add these records in your domain registrar's DNS management panel:

| Type  | Name         | Value                                      | TTL  |
|-------|--------------|---------------------------------------------|------|
| CNAME | `@` (apex)   | `pymerp-backend-abc123.ondigitalocean.app.` | 3600 |
| CNAME | `www`        | `pymerp.cl.`                                | 3600 |
| CNAME | `app`        | `pymerp.cl.`                                | 3600 |

⚠️ **Note**: Some registrars don't support CNAME on apex domain (`@`). If this fails, use **Option B** (Cloudflare) or DigitalOcean DNS.

**Option B: Using DigitalOcean DNS (Recommended)**

```bash
# Create DNS zone in DigitalOcean
doctl compute domain create pymerp.cl

# Add CNAME for apex (using ALIAS-like functionality)
doctl compute domain records create pymerp.cl \
  --record-type CNAME \
  --record-name @ \
  --record-data pymerp-backend-abc123.ondigitalocean.app. \
  --record-ttl 3600

# Add CNAME for www
doctl compute domain records create pymerp.cl \
  --record-type CNAME \
  --record-name www \
  --record-data pymerp.cl. \
  --record-ttl 3600

# Add CNAME for app
doctl compute domain records create pymerp.cl \
  --record-type CNAME \
  --record-name app \
  --record-data pymerp.cl. \
  --record-ttl 3600

# Verify DNS records
doctl compute domain records list pymerp.cl
```

**Then update your domain registrar's nameservers** to point to DigitalOcean:
- `ns1.digitalocean.com`
- `ns2.digitalocean.com`
- `ns3.digitalocean.com`

### 3. Add Custom Domain to App Platform

```bash
# Add custom domain to your app
doctl apps update $APP_ID --spec - <<EOF
name: pymerp-backend
domains:
  - domain: pymerp.cl
    type: PRIMARY
  - domain: www.pymerp.cl
    type: ALIAS
  - domain: app.pymerp.cl
    type: ALIAS
EOF

# Verify domain configuration
doctl apps get $APP_ID --format Domains
```

### 4. Verify DNS Propagation

```bash
# Check apex domain
dig pymerp.cl +short
# Expected: CNAME pointing to ondigitalocean.app

# Check www subdomain
dig www.pymerp.cl +short
# Expected: CNAME to pymerp.cl

# Check app subdomain
dig app.pymerp.cl +short
# Expected: CNAME to pymerp.cl

# Check from external DNS server (Google)
dig @8.8.8.8 pymerp.cl +short

# Windows: use nslookup
nslookup pymerp.cl
nslookup www.pymerp.cl
nslookup app.pymerp.cl
```

**DNS propagation typically takes 5-60 minutes**, but can take up to 48 hours globally.

---

## SSL/TLS Setup

DigitalOcean App Platform provides **automatic SSL/TLS certificates** via Let's Encrypt.

### 1. Enable Automatic SSL

SSL is **enabled by default** when you add a custom domain. App Platform will:
1. Detect your custom domain
2. Wait for DNS to propagate
3. Request Let's Encrypt certificate
4. Auto-renew every 90 days

### 2. Verify SSL Certificate

```bash
# Check SSL certificate details
openssl s_client -connect pymerp.cl:443 -servername pymerp.cl </dev/null 2>/dev/null | \
  openssl x509 -noout -dates -subject -issuer

# Expected output:
# notBefore=Nov  8 12:00:00 2025 GMT
# notAfter=Feb  6 12:00:00 2026 GMT
# subject=CN=pymerp.cl
# issuer=C=US, O=Let's Encrypt, CN=R3

# Check SSL grade (requires online tool)
# https://www.ssllabs.com/ssltest/analyze.html?d=pymerp.cl
# Expected: A or A+
```

### 3. Force HTTPS Redirect

App Platform automatically redirects HTTP → HTTPS. Verify:

```bash
# Test HTTP redirect
curl -I http://pymerp.cl
# Expected: HTTP/1.1 301 Moved Permanently
#           Location: https://pymerp.cl/

# Test HTTPS works
curl -I https://pymerp.cl/actuator/health
# Expected: HTTP/2 200
```

### 4. SSL Configuration in Application

The backend application doesn't need SSL configuration - App Platform handles TLS termination. The app receives plain HTTP on port 8080 internally.

However, ensure `application-prod.yml` uses secure cookies:

```yaml
server:
  servlet:
    session:
      cookie:
        secure: true      # Only send cookies over HTTPS
        http-only: true   # Prevent JavaScript access
        same-site: strict # CSRF protection
```

This is already configured in `backend/src/main/resources/application-prod.yml`.

---

## HTTP to HTTPS Redirects

### 1. Apex Domain Redirect

Both `http://pymerp.cl` and `https://pymerp.cl` should work, with HTTP redirecting to HTTPS.

```bash
# Test apex redirect
curl -L -I http://pymerp.cl/actuator/health
# Should follow: http://pymerp.cl → https://pymerp.cl → 200 OK
```

### 2. WWW Redirect

Decide on canonical domain: `pymerp.cl` (apex) or `www.pymerp.cl`.

**Recommendation**: Use apex (`pymerp.cl`) as canonical, redirect `www` to apex.

Configure in App Platform:

```bash
# Update app spec to redirect www → apex
doctl apps update $APP_ID --spec - <<EOF
name: pymerp-backend
domains:
  - domain: pymerp.cl
    type: PRIMARY
  - domain: www.pymerp.cl
    type: ALIAS
    redirect_to: pymerp.cl  # Redirect www to apex
  - domain: app.pymerp.cl
    type: ALIAS
EOF
```

Test:

```bash
# Test www redirect
curl -L -I https://www.pymerp.cl/actuator/health
# Should follow: www.pymerp.cl → pymerp.cl → 200 OK
```

---

## CORS Validation

Verify CORS configuration allows production domains only.

### 1. Check Application Configuration

Already configured in `backend/src/main/resources/application-prod.yml`:

```yaml
app:
  cors:
    allowed-origins:
      - https://pymerp.cl
      - https://www.pymerp.cl
      - https://app.pymerp.cl
    allowed-methods:
      - GET
      - POST
      - PUT
      - DELETE
      - PATCH
    allowed-headers:
      - "*"
    allow-credentials: true
```

### 2. Test CORS Headers

```bash
# Test preflight request (OPTIONS)
curl -X OPTIONS https://pymerp.cl/api/v1/companies \
  -H "Origin: https://app.pymerp.cl" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Authorization" \
  -v 2>&1 | grep -i "access-control"

# Expected headers:
# Access-Control-Allow-Origin: https://app.pymerp.cl
# Access-Control-Allow-Credentials: true
# Access-Control-Allow-Methods: GET, POST, PUT, DELETE, PATCH
# Access-Control-Allow-Headers: *

# Test actual request
curl https://pymerp.cl/api/v1/companies \
  -H "Origin: https://app.pymerp.cl" \
  -v 2>&1 | grep -i "access-control-allow-origin"

# Expected: Access-Control-Allow-Origin: https://app.pymerp.cl
```

### 3. Test CORS Rejection (Invalid Origin)

```bash
# Should reject requests from localhost (not in allowed-origins)
curl -X OPTIONS https://pymerp.cl/api/v1/companies \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: GET" \
  -v 2>&1 | grep -i "access-control"

# Expected: No Access-Control-Allow-Origin header (CORS blocked)
```

---

## Health Check Endpoints

### 1. Public Health Endpoints

These endpoints should be accessible without authentication:

```bash
# Health check (used by App Platform, monitoring tools)
curl https://pymerp.cl/actuator/health
# Expected: {"status":"UP"}

# Info endpoint (application metadata)
curl https://pymerp.cl/actuator/info
# Expected: {"app":{"name":"PYMERP","version":"1.0.0",...}}

# Prometheus metrics (if monitoring is enabled)
curl https://pymerp.cl/actuator/prometheus
# Expected: Prometheus text format metrics
```

### 2. Protected Actuator Endpoints

These require authentication (configured in `application-prod.yml`):

```bash
# Should return 401 Unauthorized
curl https://pymerp.cl/actuator/env
curl https://pymerp.cl/actuator/beans
curl https://pymerp.cl/actuator/mappings

# Expected: HTTP 401 (authentication required)
```

### 3. API Swagger Documentation

```bash
# Swagger UI (public access)
curl -I https://pymerp.cl/swagger-ui.html
# Expected: HTTP/2 200

# OpenAPI spec
curl https://pymerp.cl/v3/api-docs | jq '.info'
# Expected: {"title":"PYMERP API","version":"1.0.0",...}
```

---

## Troubleshooting

### DNS Not Resolving

**Symptom**: `dig pymerp.cl` returns `NXDOMAIN` or no results.

**Solutions**:
1. Verify DNS records are created:
   ```bash
   doctl compute domain records list pymerp.cl
   ```

2. Check nameservers are updated:
   ```bash
   dig pymerp.cl NS +short
   # Should show: ns1.digitalocean.com, ns2.digitalocean.com, ns3.digitalocean.com
   ```

3. Wait for propagation (up to 48 hours):
   ```bash
   # Check propagation status
   https://www.whatsmydns.net/#CNAME/pymerp.cl
   ```

4. Clear local DNS cache:
   ```bash
   # macOS
   sudo dscacheutil -flushcache; sudo killall -HUP mDNSResponder
   
   # Windows
   ipconfig /flushdns
   
   # Linux
   sudo systemd-resolve --flush-caches
   ```

### SSL Certificate Not Issued

**Symptom**: `curl https://pymerp.cl` returns SSL error.

**Solutions**:
1. Verify DNS is propagated (SSL requires valid DNS):
   ```bash
   dig pymerp.cl +short
   # Must return CNAME to ondigitalocean.app
   ```

2. Check App Platform domain status:
   ```bash
   doctl apps get $APP_ID --format Domains
   # Status should be: "ACTIVE" and "Certificate: VALID"
   ```

3. Wait for certificate issuance (5-15 minutes after DNS propagation)

4. Check Let's Encrypt rate limits:
   - Max 50 certificates per domain per week
   - If exceeded, wait 7 days or use staging environment

5. Manually trigger certificate renewal:
   ```bash
   # Remove and re-add domain to force new certificate
   doctl apps update $APP_ID --spec do-app-spec.yml
   ```

### CORS Errors in Browser

**Symptom**: Browser console shows `CORS policy: No 'Access-Control-Allow-Origin' header`.

**Solutions**:
1. Verify origin is in `allowed-origins`:
   ```yaml
   # application-prod.yml
   app:
     cors:
       allowed-origins:
         - https://app.pymerp.cl  # Must match exactly
   ```

2. Check for protocol mismatch (http vs https):
   ```bash
   # Origin must be HTTPS in production
   # http://app.pymerp.cl → REJECTED
   # https://app.pymerp.cl → ALLOWED
   ```

3. Verify preflight request succeeds:
   ```bash
   curl -X OPTIONS https://pymerp.cl/api/v1/companies \
     -H "Origin: https://app.pymerp.cl" \
     -H "Access-Control-Request-Method: POST" \
     -v
   ```

4. Check for trailing slashes or subdomain mismatches

### Health Check Failing

**Symptom**: `curl https://pymerp.cl/actuator/health` returns 5xx or timeout.

**Solutions**:
1. Check App Platform deployment status:
   ```bash
   doctl apps get $APP_ID --format ActiveDeployment.Phase
   # Should be: ACTIVE
   ```

2. Check application logs:
   ```bash
   doctl apps logs $APP_ID --type RUN
   # Look for startup errors, database connection failures
   ```

3. Verify database connectivity:
   ```bash
   # Check if DB credentials are set
   doctl apps get $APP_ID --format Spec.Envs
   ```

4. Test health endpoint directly from container:
   ```bash
   # Get container shell (if possible)
   doctl apps logs $APP_ID --type RUN --follow
   # Look for: "Started PymerBackendApplication in X seconds"
   ```

5. Increase health check timeout in App Platform spec:
   ```yaml
   health_check:
     http_path: /actuator/health
     initial_delay_seconds: 30  # Increase if app is slow to start
     timeout_seconds: 10
   ```

### Redirect Loop

**Symptom**: Browser shows "Too many redirects" error.

**Solutions**:
1. Verify only ONE domain is marked as `PRIMARY`:
   ```bash
   doctl apps get $APP_ID --format Domains
   # Only pymerp.cl should have type=PRIMARY
   ```

2. Check for conflicting redirect rules in Cloudflare (if using)

3. Disable HTTPS redirect temporarily to debug:
   ```yaml
   # In do-app-spec.yml (temporary)
   domains:
     - domain: pymerp.cl
       type: PRIMARY
       redirect_https: false  # Disable to debug
   ```

---

## Validation Checklist

Before proceeding to Phase 4, verify:

- [ ] DNS resolves correctly (`dig pymerp.cl` returns CNAME)
- [ ] SSL certificate is valid (`openssl s_client` shows Let's Encrypt)
- [ ] HTTP redirects to HTTPS (`curl -I http://pymerp.cl` → 301)
- [ ] WWW redirects to apex (`curl -I https://www.pymerp.cl` → 301 → pymerp.cl)
- [ ] Health check works (`curl https://pymerp.cl/actuator/health` → 200)
- [ ] CORS allows production domains (`curl -H "Origin: https://app.pymerp.cl"`)
- [ ] CORS blocks localhost (`curl -H "Origin: http://localhost:5173"` → no CORS headers)
- [ ] Swagger UI accessible (`https://pymerp.cl/swagger-ui.html`)
- [ ] SSL grade A or A+ (https://www.ssllabs.com/ssltest/)

---

## Next Steps

Once all validations pass, proceed to **Phase 4: Environment Variables Configuration**.

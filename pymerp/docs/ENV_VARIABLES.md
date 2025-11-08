# Variables de Entorno - PYMERP Producción

## 🔐 Secretos Críticos (REQUERIDOS)

### Base de Datos PostgreSQL
```bash
POSTGRES_HOST=your-postgres-host.db.ondigitalocean.com
POSTGRES_PORT=25060
POSTGRES_DB=pymes
POSTGRES_USER=doadmin
POSTGRES_PASSWORD=<strong-password>
```

### Redis Cache
```bash
REDIS_HOST=your-redis-host.db.ondigitalocean.com
REDIS_PORT=25061
REDIS_PASSWORD=<strong-password>
REDIS_SSL_ENABLED=true
```

### JWT Authentication
```bash
# Generate with: openssl rand -base64 64
JWT_SECRET=<your-jwt-secret-min-64-chars>
```

### Billing (Facturación Electrónica)
```bash
BILLING_CRYPTO_SECRET=<billing-encryption-key>
BILLING_WEBHOOK_SECRET=<webhook-validation-secret>
BILLING_DOCUMENTS_BASE_URL=https://pymerp.cl/api/v1/billing/documents
```

### Webhooks
```bash
# Generate with: openssl rand -hex 32
WEBHOOKS_HMAC_SECRET=<webhooks-hmac-secret>
```

---

## 📧 Configuración de Email (REQUERIDO)

```bash
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=<sendgrid-api-key>
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
```

**Alternativas de SMTP:**
- **SendGrid:** smtp.sendgrid.net:587
- **AWS SES:** email-smtp.us-east-1.amazonaws.com:587
- **Mailgun:** smtp.mailgun.org:587

---

## 🗄️ Almacenamiento S3/MinIO (REQUERIDO)

```bash
S3_ENDPOINT=https://nyc3.digitaloceanspaces.com
STORAGE_S3_BUCKET=pymerp-prod
STORAGE_S3_ACCESS_KEY=<spaces-access-key>
STORAGE_S3_SECRET_KEY=<spaces-secret-key>
STORAGE_S3_REGION=us-east-1
```

**Nota:** DigitalOcean Spaces es compatible con S3.

---

## 🌐 CORS (REQUERIDO)

```bash
# Dominios permitidos (separados por coma)
APP_CORS_ALLOWED_ORIGINS[0]=https://pymerp.cl
APP_CORS_ALLOWED_ORIGINS[1]=https://www.pymerp.cl
APP_CORS_ALLOWED_ORIGINS[2]=https://app.pymerp.cl
```

**⚠️ IMPORTANTE:** No usar wildcard `*` en producción por seguridad.

---

## 🔢 Configuración Opcional

### CAPTCHA (Google reCAPTCHA v2)
```bash
CAPTCHA_ENABLED=false
CAPTCHA_SECRET_KEY=<recaptcha-secret-key>
```

### Pool de Conexiones
```bash
DB_POOL_SIZE=10
DB_POOL_MIN_IDLE=2
```

### Puerto del Servidor
```bash
PORT=8080
```

---

## 🛠️ Migraciones Flyway (Opcional)

Si necesitas usuario específico para migraciones:

```bash
POSTGRES_MIGRATION_USER=flyway_user
POSTGRES_MIGRATION_PASSWORD=<migration-password>
```

---

## 📋 Plantilla .env para Development

Crea archivo `.env.prod.template`:

```bash
# Database
POSTGRES_HOST=
POSTGRES_PORT=25060
POSTGRES_DB=pymes
POSTGRES_USER=
POSTGRES_PASSWORD=

# Redis
REDIS_HOST=
REDIS_PORT=25061
REDIS_PASSWORD=
REDIS_SSL_ENABLED=true

# JWT
JWT_SECRET=

# Billing
BILLING_CRYPTO_SECRET=
BILLING_WEBHOOK_SECRET=
BILLING_DOCUMENTS_BASE_URL=https://pymerp.cl/api/v1/billing/documents

# Webhooks
WEBHOOKS_HMAC_SECRET=

# Email
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=

# S3/Spaces
S3_ENDPOINT=https://nyc3.digitaloceanspaces.com
STORAGE_S3_BUCKET=pymerp-prod
STORAGE_S3_ACCESS_KEY=
STORAGE_S3_SECRET_KEY=
STORAGE_S3_REGION=us-east-1

# CORS
APP_CORS_ALLOWED_ORIGINS[0]=https://pymerp.cl
APP_CORS_ALLOWED_ORIGINS[1]=https://www.pymerp.cl
```

---

## 🔒 Generación de Secretos

### JWT Secret (mínimo 64 caracteres)
```bash
openssl rand -base64 64
```

### HMAC Secrets
```bash
openssl rand -hex 32
```

### Passwords fuertes
```bash
openssl rand -base64 32
```

---

## ✅ Validación de Variables

Script para validar que todas las variables requeridas están configuradas:

```bash
#!/bin/bash
# validate-env.sh

REQUIRED_VARS=(
  "POSTGRES_HOST"
  "POSTGRES_PASSWORD"
  "REDIS_HOST"
  "REDIS_PASSWORD"
  "JWT_SECRET"
  "BILLING_CRYPTO_SECRET"
  "MAIL_PASSWORD"
  "STORAGE_S3_ACCESS_KEY"
)

missing=()
for var in "${REQUIRED_VARS[@]}"; do
  if [ -z "${!var}" ]; then
    missing+=("$var")
  fi
done

if [ ${#missing[@]} -gt 0 ]; then
  echo "❌ Missing required environment variables:"
  printf '   - %s\n' "${missing[@]}"
  exit 1
fi

echo "✅ All required environment variables are set"
```

---

## 🚀 DigitalOcean App Platform

Configurar en Settings → App-Level Environment Variables:

1. Ir a: https://cloud.digitalocean.com/apps
2. Seleccionar app → Settings → Environment Variables
3. Añadir cada variable (marcar como "Encrypted" para secretos)
4. Guardar y re-deploy

---

## 📝 Notas de Seguridad

1. **NUNCA** commitear archivos `.env` a Git
2. Usar secretos encriptados en GitHub Actions
3. Rotar secretos cada 90 días
4. Usar passwords de mínimo 32 caracteres
5. Habilitar 2FA en cuentas de servicios (SendGrid, DigitalOcean, etc.)

---

**Última actualización:** 7 de noviembre de 2025  
**Mantenido por:** PYMERP DevOps Team

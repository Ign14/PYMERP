# 🔐 SPRINT 1: Secrets Rotation Guide

**Fecha**: 4 de noviembre de 2025  
**Sprint**: Security Critical Fixes  
**Estado**: ✅ COMPLETADO

---

## 📋 Resumen de Cambios

### Secrets Rotados

| Secret | Antes | Después | Método de Generación |
|--------|-------|---------|---------------------|
| `JWT_SECRET` | `change-this-dev-secret-at-least-32-chars-long` | **ROTADO** ✅ | `openssl rand -base64 32` |
| `BILLING_CRYPTO_SECRET` | `MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=` | **ROTADO** ✅ | `openssl rand -base64 32` |
| `BILLING_WEBHOOK_SECRET` | `change-me` | **ROTADO** ✅ | `openssl rand -hex 32` |
| `WEBHOOKS_HMAC_SECRET` | `CHANGE_ME_HMAC_SECRET` | **ROTADO** ✅ | `openssl rand -hex 32` |

---

## 🛠️ Cómo Generar Secrets

### Opción 1: OpenSSL (Linux/Mac/Git Bash)

```bash
# JWT Secret (256 bits base64)
openssl rand -base64 32

# Webhook Secrets (256 bits hex)
openssl rand -hex 32
```

### Opción 2: PowerShell (Windows)

```powershell
# Base64 secret (para JWT_SECRET, BILLING_CRYPTO_SECRET)
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))

# Alfanumérico secret (para BILLING_WEBHOOK_SECRET, WEBHOOKS_HMAC_SECRET)
-join ((48..57) + (65..90) + (97..122) | Get-Random -Count 64 | ForEach-Object {[char]$_})
```

### Opción 3: Online (Solo para desarrollo, NUNCA producción)

**⚠️ NO usar generadores online para secretos de producción**

Para desarrollo local:
- https://www.random.org/passwords/
- https://generate-random.org/api-token-generator

---

## 📝 Procedimiento de Rotación

### 1. Generar Nuevos Secrets

```bash
# En tu terminal
JWT_NEW=$(openssl rand -base64 32)
BILLING_CRYPTO_NEW=$(openssl rand -base64 32)
BILLING_WEBHOOK_NEW=$(openssl rand -hex 32)
WEBHOOKS_HMAC_NEW=$(openssl rand -hex 32)

echo "JWT_SECRET=$JWT_NEW"
echo "BILLING_CRYPTO_SECRET=$BILLING_CRYPTO_NEW"
echo "BILLING_WEBHOOK_SECRET=$BILLING_WEBHOOK_NEW"
echo "WEBHOOKS_HMAC_SECRET=$WEBHOOKS_HMAC_NEW"
```

### 2. Actualizar Variables de Entorno

**Desarrollo** (`.env` local):
```bash
cp backend/.env.production.example backend/.env
# Editar backend/.env con valores generados
```

**Producción** (según tu plataforma):

#### Docker:
```bash
# docker-compose.yml
environment:
  - JWT_SECRET=${JWT_SECRET}
  - BILLING_CRYPTO_SECRET=${BILLING_CRYPTO_SECRET}
  - BILLING_WEBHOOK_SECRET=${BILLING_WEBHOOK_SECRET}
  - WEBHOOKS_HMAC_SECRET=${WEBHOOKS_HMAC_SECRET}
```

#### Kubernetes:
```bash
kubectl create secret generic pymerp-secrets \
  --from-literal=JWT_SECRET="$JWT_NEW" \
  --from-literal=BILLING_CRYPTO_SECRET="$BILLING_CRYPTO_NEW" \
  --from-literal=BILLING_WEBHOOK_SECRET="$BILLING_WEBHOOK_NEW" \
  --from-literal=WEBHOOKS_HMAC_SECRET="$WEBHOOKS_HMAC_NEW"
```

#### AWS ECS/Fargate:
- Usar **AWS Secrets Manager** o **Parameter Store**
- Configurar task definition con `secrets` section

#### Azure:
- Usar **Azure Key Vault**
- Configurar App Service con referencias `@Microsoft.KeyVault(...)`

### 3. Validar Configuración

```bash
# Iniciar aplicación
cd backend
./gradlew bootRun

# Verificar logs - debe aparecer:
# ✅ Security secrets validation passed

# Si falla, verás:
# ❌ Security validation FAILED:
#   - JWT_SECRET contains insecure default value
```

### 4. Reiniciar Aplicación

```bash
# Desarrollo
# Reiniciar servidor Spring Boot

# Producción (ejemplo Docker)
docker-compose down
docker-compose up -d

# Verificar health
curl https://api.yourcompany.com/actuator/health
```

---

## ⚠️ IMPORTANTE: Tokens Existentes

### JWT Tokens

**Problema**: Al rotar `JWT_SECRET`, todos los access tokens existentes quedan **inválidos**.

**Solución**:
1. Notificar a usuarios antes del deploy
2. Los usuarios deben hacer logout/login
3. Refresh tokens también quedan inválidos (tabla `refresh_tokens`)

**Script para invalidar refresh tokens** (opcional):
```sql
UPDATE refresh_tokens 
SET revoked_at = CURRENT_TIMESTAMP 
WHERE revoked_at IS NULL;
```

### Billing Webhooks

**Problema**: Al rotar `BILLING_WEBHOOK_SECRET`, el proveedor de facturación debe actualizar su configuración.

**Solución**:
1. Coordinar con proveedor (Facturador.cl, SII, etc.)
2. Actualizar webhook secret en su panel
3. Probar con endpoint `/webhooks/billing` antes de activar en producción

---

## 🔍 Testing de Secrets

### Test 1: Validación en Startup

```bash
# Debe fallar con secret inseguro
JWT_SECRET=change-this ./gradlew bootRun
# ❌ Expected: "JWT_SECRET contains insecure default value"

# Debe pasar con secret válido
JWT_SECRET=$(openssl rand -base64 32) ./gradlew bootRun
# ✅ Expected: "Security secrets validation passed"
```

### Test 2: Login con Nuevo JWT Secret

```bash
# 1. Login
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@company.test",
    "password": "password123",
    "captchaAnswer": 5
  }'

# Response:
# {"accessToken": "eyJhbGc...", "refreshToken": "..."}

# 2. Usar token
curl http://localhost:8081/api/v1/products \
  -H "Authorization: Bearer eyJhbGc..."
```

### Test 3: Webhook Signature Verification

```bash
# Simular webhook con nueva secret
PAYLOAD='{"event":"invoice.created","data":{"id":123}}'
SIGNATURE=$(echo -n "$PAYLOAD" | openssl dgst -sha256 -hmac "$BILLING_WEBHOOK_NEW" | cut -d' ' -f2)

curl -X POST http://localhost:8081/webhooks/billing \
  -H "Content-Type: application/json" \
  -H "X-Webhook-Signature: $SIGNATURE" \
  -d "$PAYLOAD"

# ✅ Expected: 200 OK
```

---

## 📚 Referencias

- **SecurityConfig.java**: Validación de CORS
- **SecretsValidator.java**: Validación de secrets en startup
- **.env.production.example**: Template de variables de entorno
- **application.yml**: Configuración con placeholders `${VAR}`

---

## ✅ Checklist de Deploy

Antes de hacer deploy a producción:

- [ ] Todos los secrets están generados aleatoriamente
- [ ] `.env.production` NO está en git
- [ ] Secrets están en secret manager (AWS/Azure/K8s)
- [ ] Tests de startup pasan localmente
- [ ] Usuarios notificados de rotación de JWT
- [ ] Proveedor de facturación actualizado con nuevo webhook secret
- [ ] Backup de base de datos antes de deploy
- [ ] Rollback plan documentado

---

**Generado por**: Sprint 1 - Security Critical Fixes  
**Última actualización**: 4 de noviembre de 2025

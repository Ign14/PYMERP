# Deploy PYMERP vía GitHub Actions

## ✅ Ventajas del Deploy Automático

- **Zero-downtime**: Rolling updates con health checks
- **Rollback automático**: Si falla health check, vuelve a versión anterior
- **Trazabilidad**: Cada deploy tiene commit SHA, logs, y timestamp
- **CI/CD completo**: Tests → Build → Deploy en un solo push

## 📋 Configuración Inicial (Una sola vez)

### PASO 1: Aprovisionar Infraestructura DigitalOcean

**Opción A: Script Automatizado (RECOMENDADO)**
```powershell
# En PowerShell en tu máquina local
.\scripts\provision-digitalocean.ps1
```

**Opción B: Manual (siguiendo docs/digitalocean-setup.md)**
```bash
# 1. PostgreSQL 16
doctl databases create pymerp-db-cluster \
  --engine pg --version 16 --size db-s-1vcpu-1gb \
  --region nyc3 --num-nodes 1

# 2. Redis 7
doctl databases create pymerp-redis-cluster \
  --engine redis --version 7 --size db-s-1vcpu-512mb \
  --region nyc3 --eviction-policy allkeys-lru

# 3. Container Registry
doctl registry create pymerp

# 4. Spaces (vía Web Console)
# https://cloud.digitalocean.com/spaces
# Name: pymes-prod, Region: NYC3, ACL: Private
```

Al finalizar, guarda:
- Database Cluster IDs
- Connection strings (host, port, user, password)
- Spaces Access/Secret Keys

---

### PASO 2: Configurar GitHub Secrets

Ir a: https://github.com/Ign14/PYMERP/settings/secrets/actions

**Click "New repository secret" y agregar:**

#### 🔑 Secrets Requeridos:

| Secret Name | Dónde obtenerlo | Ejemplo |
|------------|-----------------|---------|
| `DIGITALOCEAN_ACCESS_TOKEN` | [DigitalOcean API Tokens](https://cloud.digitalocean.com/account/api/tokens) | `dop_v1_abc123...` |
| `DIGITALOCEAN_APP_ID` | Después de crear App Platform (ver PASO 3) | `12345678-1234-...` |

#### 📧 Secrets Opcionales:

| Secret Name | Para qué sirve | Ejemplo |
|------------|----------------|---------|
| `SLACK_WEBHOOK_URL` | Notificaciones de deploy | `https://hooks.slack.com/...` |

**Cómo crear el Access Token:**
1. Ir a: https://cloud.digitalocean.com/account/api/tokens
2. Click "Generate New Token"
3. Name: `PYMERP GitHub Actions`
4. Scopes: **Read + Write** (necesita crear/modificar resources)
5. Expiration: `No expiry` (o 90 días y renovar)
6. Copiar token y guardarlo en GitHub Secrets

---

### PASO 3: Crear App Platform

**ANTES de crear App Platform**, debes tener:
- ✅ PostgreSQL cluster ONLINE
- ✅ Redis cluster ONLINE
- ✅ Spaces bucket `pymes-prod` creado
- ✅ Container Registry `pymerp` creado

**Actualizar `do-app-spec.yml` con valores reales:**

```bash
# Obtener Database IDs
doctl databases list --format ID,Name

# Ejemplo de salida:
# ID                                      Name
# 12345678-1234-1234-1234-123456789012   pymerp-db-cluster
# 87654321-4321-4321-4321-210987654321   pymerp-redis-cluster
```

Editar `do-app-spec.yml` y reemplazar placeholders:

```yaml
# Línea 87-95 (PostgreSQL)
- key: POSTGRES_HOST
  value: ${db-12345678-1234-1234-1234-123456789012.HOSTNAME}
- key: POSTGRES_PORT
  value: ${db-12345678-1234-1234-1234-123456789012.PORT}
- key: POSTGRES_DB
  value: ${db-12345678-1234-1234-1234-123456789012.DATABASE}
- key: POSTGRES_USER
  value: ${db-12345678-1234-1234-1234-123456789012.USERNAME}
- key: POSTGRES_PASSWORD
  value: ${db-12345678-1234-1234-1234-123456789012.PASSWORD}

# Línea 97-102 (Redis)
- key: REDIS_HOST
  value: ${db-87654321-4321-4321-4321-210987654321.HOSTNAME}
- key: REDIS_PORT
  value: ${db-87654321-4321-4321-4321-210987654321.PORT}
- key: REDIS_PASSWORD
  value: ${db-87654321-4321-4321-4321-210987654321.PASSWORD}
```

**Crear App Platform:**

```bash
# Crear app desde spec
doctl apps create --spec do-app-spec.yml

# Output incluirá App ID, guardarlo:
# ID: 12345678-1234-1234-1234-123456789012
# Name: pymerp-backend
# Status: creating

# Guardar App ID en GitHub Secrets como DIGITALOCEAN_APP_ID
```

**Verificar creación:**
```bash
doctl apps list
doctl apps get <APP_ID>

# Esperar hasta que Status = ACTIVE
```

---

### PASO 4: Configurar Variables de Entorno Secretas

Algunas variables NO pueden estar en `do-app-spec.yml` porque son secretas.

**Ejecutar script de setup:**
```bash
# Exportar variables de connection strings (del PASO 1)
export POSTGRES_HOST="..."
export POSTGRES_PORT="..."
export POSTGRES_USER="..."
export POSTGRES_PASSWORD="..."
export POSTGRES_DB="..."

export REDIS_HOST="..."
export REDIS_PORT="..."
export REDIS_PASSWORD="..."

export STORAGE_S3_BUCKET="pymes-prod"
export STORAGE_S3_ACCESS_KEY="..."  # From Spaces settings
export STORAGE_S3_SECRET_KEY="..."  # From Spaces settings

# Ejecutar setup (genera secrets automáticamente)
bash scripts/setup-digitalocean-env.sh

# Output:
# ✅ JWT_SECRET generado
# ✅ BILLING_CRYPTO_KEY generado
# ✅ BILLING_WEBHOOK_SECRET generado
# ✅ WEBHOOKS_HMAC_SECRET generado
# 🔐 Backup encriptado en: .digitalocean/secrets-backup-20251108.env.gpg
```

El script te mostrará comandos `doctl apps update` para agregar las variables a App Platform.

**Ejecutar los comandos sugeridos:**
```bash
doctl apps update <APP_ID> --spec do-app-spec.yml
```

---

### PASO 5: Configurar Dominio y SSL

**Si tienes acceso al dominio pymerp.cl:**

Ver guía completa: `docs/domain-ssl-setup.md`

```bash
# 1. Crear DNS zone
doctl compute domain create pymerp.cl

# 2. Agregar CNAME records
doctl compute domain records create pymerp.cl \
  --record-type CNAME \
  --record-name @ \
  --record-data <APP_URL>.ondigitalocean.app. \
  --record-ttl 3600

# Repetir para www y app subdomains

# 3. Actualizar nameservers en el registrar
ns1.digitalocean.com
ns2.digitalocean.com
ns3.digitalocean.com

# 4. Esperar propagación DNS (5-60 min)
# 5. App Platform detectará dominio y provisionará SSL automáticamente
```

**Si NO tienes dominio todavía:**

La app estará disponible en:
```
https://pymerp-backend-xxxxx.ondigitalocean.app
```

Puedes deployar sin dominio custom y agregarlo después.

---

### PASO 6: Pre-Deploy Checklist

**Ejecutar validación final:**
```bash
bash scripts/pre-deploy-checklist.sh
```

Debe pasar **todas** las validaciones:
- ✅ CLI Tools (git, docker, doctl)
- ✅ GitHub Secrets configurados
- ✅ DigitalOcean infrastructure online
- ✅ DNS configurado (o skip si usas *.ondigitalocean.app)
- ✅ Environment variables válidas
- ✅ Dockerfile builds exitosamente
- ✅ CI pipeline configurado

Si retorna exit code 0:
```
✅ READY TO DEPLOY
```

Entonces estás listo para el primer deploy.

---

## 🚀 Deploy Automático (Cada vez que hagas push)

### Merge a `main` dispara deploy automático:

```bash
# 1. Asegurarte que todos los tests pasen
git checkout feature/sprint-10-production-deploy
./gradlew test  # O la task que corresponda

# 2. Merge a main
git checkout main
git merge feature/sprint-10-production-deploy

# 3. Push a GitHub (esto dispara el deploy)
git push origin main
```

### Qué sucede automáticamente:

```
1. GitHub Actions recibe push a main
2. Ejecuta .github/workflows/ci.yml:
   - Setup JDK 21
   - ./gradlew clean test (todos los tests)
   - Checkstyle, Spotless
   - Trivy security scan
   
3. Si CI pasa, ejecuta .github/workflows/deploy.yml:
   - Build Docker image (multi-stage)
   - Tag: registry.digitalocean.com/pymerp/backend:latest
   - Push a Container Registry
   - Deploy a App Platform (rolling update)
   - Wait hasta Phase = ACTIVE
   - Health check: curl https://pymerp.cl/actuator/health
   - Si health check falla → ROLLBACK automático
   
4. Notificación (si configuraste Slack):
   - ✅ Deploy SUCCESS
   - ❌ Deploy FAILED (con logs)
```

### Monitorear deploy en tiempo real:

**GitHub Actions:**
```
https://github.com/Ign14/PYMERP/actions
```

**DigitalOcean Logs:**
```bash
doctl apps logs <APP_ID> --type RUN --follow
```

**Metrics Dashboard:**
```
https://cloud.digitalocean.com/apps/<APP_ID>/metrics
```

---

## 🔍 Post-Deploy Validation

### PASO 1: Verificar Health Endpoint

```bash
curl https://pymerp.cl/actuator/health

# Expected:
# {"status":"UP"}
```

### PASO 2: Ejecutar Health Check Completo

```bash
bash scripts/production-health-check.sh --verbose

# Valida:
# ✅ Health endpoint (200 OK)
# ✅ Swagger UI (/swagger-ui/index.html)
# ✅ Auth protection (/api/customers → 401)
# ✅ PostgreSQL connection (SELECT 1)
# ✅ Flyway migrations (última versión)
# ✅ Redis connection (PING + SET/GET)
# ✅ S3/Spaces (upload test file)
```

### PASO 3: Verificar Flyway Migrations

```bash
# Conectar a PostgreSQL
psql "postgresql://$POSTGRES_USER:$POSTGRES_PASSWORD@$POSTGRES_HOST:$POSTGRES_PORT/$POSTGRES_DB?sslmode=require"

# Verificar migrations
SELECT installed_rank, version, description, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 10;

# Verificar tablas creadas
\dt

# Salir
\q
```

### PASO 4: Smoke Tests

```bash
# Test 1: Login (debería fallar con 401 - correcto, no hay usuarios)
curl -X POST https://pymerp.cl/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'

# Expected: 401 Unauthorized

# Test 2: Swagger accesible
curl -I https://pymerp.cl/swagger-ui/index.html

# Expected: 200 OK

# Test 3: Actuator endpoints
curl https://pymerp.cl/actuator/info
curl https://pymerp.cl/actuator/health
```

---

## 📊 Monitoreo Continuo

### Logs en Tiempo Real

```bash
# Application logs
doctl apps logs <APP_ID> --type RUN --follow

# Deployment logs
doctl apps logs <APP_ID> --type DEPLOY --follow

# Build logs
doctl apps logs <APP_ID> --type BUILD --follow
```

### Metrics Dashboard

```
https://cloud.digitalocean.com/apps/<APP_ID>/metrics
```

Métricas disponibles:
- CPU usage (%)
- Memory usage (MB)
- Request rate (req/s)
- Response time (ms)
- Error rate (%)

### Alertas Configuradas (en do-app-spec.yml)

```yaml
alerts:
  - rule: CPU_UTILIZATION
    threshold: 80
  - rule: MEM_UTILIZATION
    threshold: 85
  - rule: RESTART_COUNT
    threshold: 5
```

---

## 🔄 Rollback Manual (Si es necesario)

### Opción 1: Via DigitalOcean Console

```
1. Ir a: https://cloud.digitalocean.com/apps/<APP_ID>
2. Tab "Deployments"
3. Click en deployment anterior (Working)
4. Click "Rollback to this deployment"
```

### Opción 2: Via doctl CLI

```bash
# Listar deployments
doctl apps list-deployments <APP_ID>

# Rollback a deployment específico
doctl apps create-deployment <APP_ID> --deployment-id <DEPLOYMENT_ID>
```

### Opción 3: Via Git Revert

```bash
# Revertir último commit en main
git checkout main
git revert HEAD
git push origin main

# Esto dispara nuevo deploy con versión anterior
```

---

## 🛑 Troubleshooting

Ver guía completa: `docs/RUNBOOK.md`

### Problema 1: Deploy falla con "502 Bad Gateway"

**Diagnóstico:**
```bash
doctl apps logs <APP_ID> --type RUN | tail -50
```

**Causas comunes:**
- App no inicia (ver logs de Java)
- Health check falla (/actuator/health)
- Puerto incorrecto (debe ser 8080)

**Solución:**
```bash
# Verificar variables de entorno
doctl apps spec get <APP_ID> | grep -A 2 "PORT"

# Verificar Dockerfile EXPOSE
cat backend/Dockerfile | grep EXPOSE
```

### Problema 2: "Database connection failed"

**Diagnóstico:**
```bash
# Verificar PostgreSQL está online
doctl databases list

# Test connection
psql "postgresql://$POSTGRES_USER:$POSTGRES_PASSWORD@$POSTGRES_HOST:$POSTGRES_PORT/$POSTGRES_DB?sslmode=require"
```

**Causas comunes:**
- Database cluster offline
- Firewall bloqueando App Platform IP
- Credenciales incorrectas en variables

**Solución:**
```bash
# Ver trusted sources del database
doctl databases firewalls list <DB_CLUSTER_ID>

# Agregar App Platform si no está
doctl databases firewalls append <DB_CLUSTER_ID> --rule app:<APP_ID>
```

### Problema 3: "Redis connection timeout"

**Diagnóstico:**
```bash
# Verificar Redis está online
doctl databases list | grep redis

# Test connection (desde app)
redis-cli -h $REDIS_HOST -p $REDIS_PORT --tls --askpass
# Password: $REDIS_PASSWORD
> PING
```

**Solución:** Similar a PostgreSQL, verificar firewall rules.

### Problema 4: "S3/Spaces upload failed"

**Diagnóstico:**
```bash
# Verificar bucket existe
doctl compute cdn list

# Test con AWS CLI
aws s3 ls s3://pymes-prod \
  --endpoint-url https://nyc3.digitaloceanspaces.com \
  --region nyc3
```

**Solución:**
- Verificar Access/Secret Keys
- Verificar CORS configurado en Spaces
- Verificar región correcta (nyc3)

---

## 💰 Costos Mensuales

| Recurso | Plan | Costo |
|---------|------|-------|
| PostgreSQL 16 | db-s-1vcpu-1gb | $15/mes |
| Redis 7 | db-s-1vcpu-512mb | $15/mes |
| Container Registry | Starter | $5/mes (500MB) |
| App Platform | Basic (1 instancia) | $5/mes base + $5/instancia |
| Spaces | Standard | $5/mes (250GB) + $0.01/GB transfer |
| **TOTAL** | | **~$50-70/mes** |

**Notas:**
- Auto-scaling puede aumentar costos (hasta 3 instancias = +$10/mes)
- Transfer out over 1TB = $0.01/GB adicional
- Snapshots database = $0.05/GB/mes (opcional)

---

## 📋 Checklist Final

Antes del primer deploy a producción:

- [ ] PostgreSQL cluster creado y ONLINE
- [ ] Redis cluster creado y ONLINE
- [ ] Container Registry creado
- [ ] Spaces bucket `pymes-prod` creado
- [ ] App Platform creado con `do-app-spec.yml`
- [ ] GitHub Secrets configurados (DIGITALOCEAN_ACCESS_TOKEN, APP_ID)
- [ ] Variables de entorno secretas configuradas en App Platform
- [ ] DNS configurado (o usando *.ondigitalocean.app)
- [ ] SSL provisionado (Let's Encrypt automático)
- [ ] `scripts/pre-deploy-checklist.sh` retorna exit 0
- [ ] Todos los tests pasan: `./gradlew test`
- [ ] Merge a `main` y push

Después del primer deploy:

- [ ] Health endpoint responde 200 OK
- [ ] `scripts/production-health-check.sh` retorna exit 0
- [ ] Flyway migrations aplicadas correctamente
- [ ] Smoke tests pasan (login, swagger, protected endpoints)
- [ ] Logs no muestran errores críticos
- [ ] Metrics dashboard muestra app saludable
- [ ] Documentar en `docs/deployment-log.md`

---

## 📞 Soporte

### Tier 1 - Logs y Health Checks
```bash
doctl apps logs <APP_ID> --type RUN --follow
bash scripts/production-health-check.sh --verbose
```

### Tier 2 - Rollback y Restart
```bash
doctl apps list-deployments <APP_ID>
doctl apps create-deployment <APP_ID> --force-rebuild
```

### Tier 3 - DigitalOcean Support
```
https://cloud.digitalocean.com/support
```

**Horarios:** 24/7 (Plan Pro+), Business hours (Plan Basic)

---

## 🎯 Resumen

1. **Provisionar infraestructura** (una vez): PostgreSQL, Redis, Registry, Spaces, App Platform
2. **Configurar GitHub Secrets** (una vez): DIGITALOCEAN_ACCESS_TOKEN, APP_ID
3. **Configurar variables secretas** (una vez): JWT, BILLING, Webhooks secrets
4. **Configurar dominio y SSL** (una vez, opcional)
5. **Deploy automático**: `git push origin main` → GitHub Actions hace el resto
6. **Monitoreo continuo**: Health checks, logs, metrics
7. **Rollback si es necesario**: Automático o manual

✅ **Zero-downtime deployments**  
✅ **Rollback automático**  
✅ **Health checks en cada deploy**  
✅ **Trazabilidad completa**  
✅ **Escalamiento automático**  

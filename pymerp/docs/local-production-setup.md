# PyMERP - Configuración Local con Dominio

Este documento explica cómo montar PyMERP localmente apuntando al dominio `pymerp.cl` con HTTPS.

## 📋 Prerequisitos

- Docker Desktop instalado
- Node.js 18+ y npm
- PowerShell (Windows) o Bash (Linux/Mac)
- 4GB RAM libre mínimo

## 🚀 Setup Rápido

### Windows (PowerShell)

```powershell
# 1. Ejecutar script de setup
.\scripts\setup-local-prod.ps1
```

### Linux/Mac (Bash)

```bash
# 1. Generar certificado SSL
mkdir -p nginx/ssl
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout nginx/ssl/pymerp.key \
  -out nginx/ssl/pymerp.crt \
  -subj "/CN=pymerp.cl/O=PyMERP Local/C=CL"

# 2. Agregar entrada en /etc/hosts (requiere sudo)
echo "127.0.0.1 pymerp.cl www.pymerp.cl" | sudo tee -a /etc/hosts

# 3. Crear archivo .env
cp .env.local-prod.example .env.local-prod

# 4. Build frontend
cd ui && npm ci && npm run build && cd ..

# 5. Iniciar servicios
docker-compose -f docker-compose.local-prod.yml --env-file .env.local-prod up -d --build
```

## 📊 Arquitectura Local

```
┌─────────────────────────────────────────────────┐
│           NAVEGADOR (pymerp.cl)                 │
└─────────────────┬───────────────────────────────┘
                  │ HTTPS (443)
                  ▼
┌─────────────────────────────────────────────────┐
│           NGINX Reverse Proxy                   │
│  - Termina SSL                                  │
│  - Sirve React SPA                             │
│  - Proxy a /api/* → backend:8081               │
└─────┬───────────────────────────────────────────┘
      │
      ├─────────────────────────────────────────┐
      │                                         │
      ▼                                         ▼
┌─────────────┐  ┌──────────┐  ┌────────┐  ┌──────┐
│ Spring Boot │  │PostgreSQL│  │ Redis  │  │MinIO │
│   (8081)    │→ │  (5432)  │  │ (6379) │  │(9000)│
└─────────────┘  └──────────┘  └────────┘  └──────┘
```

## 🔧 Configuración Manual

### 1. Certificado SSL

#### Opción A: PowerShell (Windows)
```powershell
$cert = New-SelfSignedCertificate `
    -DnsName "pymerp.cl", "www.pymerp.cl", "localhost" `
    -CertStoreLocation "Cert:\CurrentUser\My" `
    -NotAfter (Get-Date).AddYears(1)

# Exportar y convertir (requiere OpenSSL)
Export-PfxCertificate -Cert $cert -FilePath nginx/ssl/pymerp.pfx
openssl pkcs12 -in nginx/ssl/pymerp.pfx -out nginx/ssl/pymerp.crt -clcerts -nokeys
openssl pkcs12 -in nginx/ssl/pymerp.pfx -out nginx/ssl/pymerp.key -nocerts -nodes
```

#### Opción B: OpenSSL (Linux/Mac/Windows con Git Bash)
```bash
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout nginx/ssl/pymerp.key \
  -out nginx/ssl/pymerp.crt \
  -subj "/CN=pymerp.cl/O=PyMERP/C=CL" \
  -addext "subjectAltName=DNS:pymerp.cl,DNS:www.pymerp.cl,DNS:localhost"
```

### 2. Archivo Hosts

#### Windows
```powershell
# Requiere PowerShell como Administrador
Add-Content -Path C:\Windows\System32\drivers\etc\hosts -Value "127.0.0.1 pymerp.cl www.pymerp.cl"
```

#### Linux/Mac
```bash
sudo sh -c 'echo "127.0.0.1 pymerp.cl www.pymerp.cl" >> /etc/hosts'
```

### 3. Variables de Entorno

Crear archivo `.env.local-prod`:

```env
POSTGRES_PASSWORD=PymesProd2024!
REDIS_PASSWORD=RedisSecure2024!
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=MinioSecure2024!
JWT_SECRET=your-super-secret-jwt-key-min-64-chars
```

### 4. Build y Deploy

```bash
# Build frontend
cd ui
npm ci
VITE_API_URL=https://pymerp.cl npm run build
cd ..

# Iniciar stack
docker-compose -f docker-compose.local-prod.yml --env-file .env.local-prod up -d --build

# Ver logs
docker-compose -f docker-compose.local-prod.yml logs -f
```

## 🌐 URLs Disponibles

| Servicio | URL | Descripción |
|----------|-----|-------------|
| Frontend | https://pymerp.cl | React SPA (producción local) |
| Backend API | https://pymerp.cl/api/v1 | Spring Boot REST API |
| Health Check | http://localhost:8081/actuator/health | Estado del backend |
| MinIO Console | http://localhost:9001 | S3-compatible storage UI |
| MailHog | http://localhost:8025 | Email testing interface |
| PostgreSQL | localhost:5432 | Base de datos principal |
| Redis | localhost:6379 | Cache y sesiones |

## 🔐 Credenciales por Defecto

### Aplicación
- **Usuario**: `admin@dev.local`
- **Contraseña**: `Admin1234`
- **Compañía**: Dev Company
- **RUT**: 76.000.000-0

### MinIO
- **Access Key**: `minioadmin`
- **Secret Key**: `MinioSecure2024!`

### PostgreSQL
- **Usuario**: `pymes`
- **Contraseña**: `PymesProd2024!`
- **Base de datos**: `pymes`

### Redis
- **Contraseña**: `RedisSecure2024!`

## 🛠️ Comandos Útiles

```bash
# Ver todos los servicios
docker-compose -f docker-compose.local-prod.yml ps

# Ver logs de un servicio específico
docker-compose -f docker-compose.local-prod.yml logs -f backend
docker-compose -f docker-compose.local-prod.yml logs -f nginx
docker-compose -f docker-compose.local-prod.yml logs -f postgres

# Reiniciar un servicio
docker-compose -f docker-compose.local-prod.yml restart backend

# Reconstruir backend
docker-compose -f docker-compose.local-prod.yml up -d --build backend

# Detener todo
docker-compose -f docker-compose.local-prod.yml down

# Eliminar TODO (incluyendo volúmenes)
docker-compose -f docker-compose.local-prod.yml down -v
```

## 🐛 Troubleshooting

### Advertencia de Certificado en Navegador

**Normal**: El navegador mostrará advertencia porque el certificado es autofirmado.

**Solución**: Click en "Avanzado" → "Continuar a pymerp.cl (inseguro)"

### Error "Cannot connect to backend"

```bash
# Verificar que backend esté corriendo
docker-compose -f docker-compose.local-prod.yml ps backend

# Ver logs del backend
docker-compose -f docker-compose.local-prod.yml logs backend

# Verificar health
curl http://localhost:8081/actuator/health
```

### Puerto 443 o 80 en uso

```powershell
# Ver qué proceso usa el puerto
netstat -ano | findstr :443
netstat -ano | findstr :80

# Detener IIS si está corriendo
net stop was /y
```

### PostgreSQL no inicia

```bash
# Verificar logs
docker-compose -f docker-compose.local-prod.yml logs postgres

# Recrear volumen (ELIMINA DATOS)
docker-compose -f docker-compose.local-prod.yml down -v
docker-compose -f docker-compose.local-prod.yml up -d
```

### Frontend no actualiza cambios

```bash
# Rebuild frontend
cd ui
npm run build
cd ..

# Reiniciar nginx
docker-compose -f docker-compose.local-prod.yml restart nginx
```

## 📊 Monitoreo

### Health Checks

```bash
# Backend health
curl http://localhost:8081/actuator/health

# PostgreSQL
docker-compose -f docker-compose.local-prod.yml exec postgres pg_isready -U pymes

# Redis
docker-compose -f docker-compose.local-prod.yml exec redis redis-cli ping
```

### Métricas

```bash
# Actuator metrics
curl http://localhost:8081/actuator/metrics

# Docker stats
docker stats pymerp-backend pymerp-postgres pymerp-redis
```

## 🔒 Seguridad

### Para Producción Real

1. **Cambiar todas las contraseñas** en `.env.local-prod`
2. **Usar certificado SSL válido** (Let's Encrypt, etc.)
3. **Configurar firewall** para exponer solo puertos necesarios
4. **Activar OIDC** con Keycloak/Auth0
5. **Configurar backups** de PostgreSQL
6. **Habilitar logs centralizados**
7. **Configurar rate limiting** más estricto

### Variables Sensibles

**NUNCA** commitear el archivo `.env.local-prod` al repositorio.

Ya está en `.gitignore`:
```
.env.local-prod
nginx/ssl/*.key
nginx/ssl/*.pfx
```

## 📝 Diferencias con Entorno de Desarrollo

| Aspecto | Desarrollo | Local-Prod |
|---------|-----------|------------|
| Puerto Frontend | 5173 | 443 (HTTPS) |
| Hot Reload | Sí | No |
| SSL | No | Sí (autofirmado) |
| Nginx | No | Sí |
| Build Frontend | dev | producción |
| JVM Memory | 512MB | 1GB |
| Dominio | localhost | pymerp.cl |

## 🚀 Próximos Pasos

1. **Integrar CI/CD** para builds automáticos
2. **Configurar backups** automáticos de PostgreSQL
3. **Implementar monitoreo** con Prometheus + Grafana
4. **Agregar logs centralizados** con ELK Stack
5. **Deploy a nube** (DigitalOcean, AWS, Azure)

## 📞 Soporte

Para problemas o preguntas:
1. Revisar logs: `docker-compose -f docker-compose.local-prod.yml logs -f`
2. Verificar health checks
3. Consultar documentación en `docs/`

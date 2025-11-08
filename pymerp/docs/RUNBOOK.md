# RUNBOOK Operacional PYMERP

## 1. Información General
- Aplicación: **PYMERP ERP** (Sprint 10 · Fase 7 monitoreo y operaciones)
- URL Producción: https://pymerp.cl
- Infraestructura: DigitalOcean App Platform (región **nyc3**)
- Stack: Spring Boot 3.3.3 + PostgreSQL 16 + Redis 7 + DigitalOcean Spaces (S3-compatible)
- Repositorio: https://github.com/Ign14/PYMERP
- Branch principal: `main`
- Artefacto backend: `pymerp/backend` publicado en Container Registry de DigitalOcean

## 2. Arquitectura de Producción
Componentes activos en DigitalOcean y su interacción:
```
                +-------------------------------+
                |  DigitalOcean App Platform    |
                |  Service: pymerp-backend      |
                |  (1-3 instancias auto-scale)  |
                +-------------------------------+
                            |  HTTPS (Let’s Encrypt)
                            v
+----------------+     +-----------------+     +--------------------+
| DO Container   | --> | PostgreSQL 16   | --> | DigitalOcean Spaces|
| Registry       |     | Managed (db-s-1)|     | Bucket: pymes-prod |
| pymerp/backend |     | backups diarios |     | privado, versionado|
+----------------+     +-----------------+     +--------------------+
                            ^                       ^
                            |                       |
                            | Redis over TLS        |
                            | (db-s-1 512MB)        |
                            +-----------------------+
```
- **PostgreSQL Managed**: plan `db-s-1vcpu-1gb`, backups diarios automáticos, SSL requerido.
- **Redis Managed**: plan `db-s-1vcpu-512mb`, política de eviction `allkeys-lru`, modo cache-only.
- **Spaces / S3**: bucket `pymes-prod`, carpeta `health-checks/` reservada para validaciones.

## 3. Accesos y Credenciales
- DigitalOcean Console: https://cloud.digitalocean.com
- GitHub Repository: https://github.com/Ign14/PYMERP
- Secrets principales: GitHub → Settings → Secrets and variables → Actions (contienen POSTGRES_*, REDIS_*, STORAGE_S3_*, AWS_*).
- SSH/Shell: **No disponible** (App Platform es totalmente gestionado).
- Para manipular recursos usar `doctl` autenticado con token del equipo.

Comandos útiles:
```bash
# Ver app ID y servicios
doctl apps list

# Logs en tiempo real
doctl apps logs <APP_ID> --type RUN --follow

# Despliegues recientes
doctl apps list-deployments <APP_ID>

# Estado de bases de datos
doctl databases list
```

## 4. Procedimientos de Rollback y Recuperación
### 4.1 Rollback de App Platform (último deployment estable)
```bash
# 1. Listar deployments (últimos 10)
doctl apps list-deployments <APP_ID> --format ID,Cause,Phase,Progress --no-header | head -n 10

# 2. Identificar deployment estable anterior (Phase=ACTIVE, Progress=100%)
PREVIOUS_DEPLOYMENT_ID=<deployment-activo-anterior>

# 3. Crear nuevo deployment usando el anterior
doctl apps create-deployment <APP_ID> --deployment-id $PREVIOUS_DEPLOYMENT_ID

# 4. Monitorear rollback
doctl apps logs <APP_ID> --type RUN --follow

# 5. Validar health
curl https://pymerp.cl/actuator/health
./scripts/production-health-check.sh --critical-only
```
- **Criterio de éxito**: endpoints sanos, health script `status=healthy`.
- **Notas**: rollback reutiliza la imagen Docker previa, no toca base de datos.

### 4.2 Rollback manual desde `do-app-spec.yml`
1. Editar `do-app-spec.yml` fijando `source_dir`/imagen deseada.
2. Confirmar variables críticas (`POSTGRES_*`, `REDIS_*`, `STORAGE_S3_*`).
3. Ejecutar `doctl apps update <APP_ID> --spec do-app-spec.yml`.
4. Validar logs RUN y BUILD (buscar errores de arranque).
5. Correr `./scripts/production-health-check.sh --verbose`.

### 4.3 Rollback de Base de Datos (uso restringido)
- DigitalOcean mantiene **backups diarios** (retención 7 días).
- Proceso:
  1. Identificar cluster: `doctl databases list` → `ID`.
  2. Listar backups: `doctl databases backups list <DB_CLUSTER_ID>`.
  3. Restaurar desde consola DigitalOcean (opción “Restore from backup”). **Advertencia**: reemplaza el cluster actual; bloquear tráfico antes.
  4. Actualizar cadenas de conexión si cambia el endpoint.
  5. Ejecutar migraciones faltantes: `./backend/scripts/flyway-migrate.sh --env=prod` (si aplica).

## 5. Troubleshooting Común
### 5.1 502 Bad Gateway
- **Síntomas**: https://pymerp.cl devuelve 502.
- **Causas posibles**: aplicación caida, timeout de arranque, OOM.
- **Diagnóstico**:
  ```bash
  doctl apps logs <APP_ID> --type RUN --tail 100
  doctl apps get <APP_ID> --format ActiveDeployment.Phase,ActiveDeployment.Progress
  curl -v https://pymerp.cl/actuator/health
  ```
- **Resolución**: ajustar `health_check.initial_delay_seconds` en `do-app-spec.yml` si tarda en iniciar, inspeccionar stacktrace y variables, escalar a `instance_size_slug` superior si hay OOM.

### 5.2 Database Connection Failures
- **Síntomas**: `Connection refused`, `Authentication failed` en logs.
- **Diagnóstico**:
  ```bash
  doctl apps get <APP_ID> --format Spec.Envs | grep POSTGRES
  doctl databases get <DB_CLUSTER_ID> --format Status
  psql "postgresql://$POSTGRES_USER:$POSTGRES_PASSWORD@$POSTGRES_HOST:$POSTGRES_PORT/$POSTGRES_DB?sslmode=require" -c "SELECT 1"
  ```
- **Resolución**: validar `POSTGRES_PASSWORD`, revisar firewall/maintenance window, contactar soporte DO si cluster down.

### 5.3 Redis Timeout / Connection Errors
- **Síntomas**: `RedisConnectionException`, `PING timeout`.
- **Diagnóstico**:
  ```bash
  redis-cli -h $REDIS_HOST -p $REDIS_PORT --tls -a $REDIS_PASSWORD PING
  doctl apps get <APP_ID> --format Spec.Envs | grep REDIS_SSL_ENABLED
  ```
- **Resolución**: asegurar `REDIS_SSL_ENABLED=true`, validar password, aumentar `maxmemory` si eviction agresivo.

### 5.4 S3/Spaces Upload Failures
- **Síntomas**: errores al subir PDFs/adjuntos.
- **Diagnóstico**:
  ```bash
  aws s3 ls s3://$STORAGE_S3_BUCKET --endpoint-url $S3_ENDPOINT
  aws s3 cp test.txt s3://$STORAGE_S3_BUCKET/test/ --endpoint-url $S3_ENDPOINT
  ```
- **Resolución**: verificar `STORAGE_S3_ACCESS_KEY/SECRET_KEY`, existencia del bucket (`doctl compute cdn list`), revisar CORS para `app.pymerp.cl`.

### 5.5 Flyway Migration Failed
- **Síntomas**: despliegue falla con `FlywayException`.
- **Diagnóstico**:
  ```bash
  psql -h $POSTGRES_HOST -U $POSTGRES_USER -d $POSTGRES_DB -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 10"
  psql -h $POSTGRES_HOST -U $POSTGRES_USER -d $POSTGRES_DB -c "SELECT * FROM flyway_schema_history WHERE success = false"
  ```
- **Resolución**: revisar SQL en `backend/src/main/resources/db/migration`, corregir y redeploy. Para rollback manual borrar fila problemática: `DELETE FROM flyway_schema_history WHERE version='VXX';` antes de redeployar.

### 5.6 SSL Certificate Expired / Invalid
- **Síntomas**: navegador muestra `NET::ERR_CERT_DATE_INVALID`.
- **Diagnóstico**:
  ```bash
  openssl s_client -connect pymerp.cl:443 -servername pymerp.cl </dev/null 2>/dev/null | openssl x509 -noout -dates
  doctl apps get <APP_ID> --format Domains
  ```
- **Resolución**: App Platform renueva automáticamente. Si falla, remover/reagregar dominio en `do-app-spec.yml` y forzar redeploy (`doctl apps create-deployment <APP_ID>`).

## 6. Logs y Monitoreo
- **Application Logs**:
  ```bash
  doctl apps logs <APP_ID> --type RUN --follow        # streaming
  doctl apps logs <APP_ID> --type RUN --tail 100      # últimos eventos
  doctl apps logs <APP_ID> --type BUILD               # fallas de build
  ```
- **Database Logs**: DigitalOcean Console → Databases → `pymerp-db-cluster` → Logs (slow queries, auth).
- **CI/CD Logs**: GitHub Actions https://github.com/Ign14/PYMERP/actions (tests, despliegues).
- **Health Check Logs**:
  ```bash
  ./scripts/production-health-check.sh --verbose
  # Si se ejecuta vía cron: revisar /var/log/pymerp-health.log (servidor de monitoreo)
  ```
- **Métricas objetivo**: response time `/actuator/health` <200 ms, latency DB <50 ms, CPU <80 %, RAM <90 %, error rate <1 %, uptime mensual >99.9 %.
- **Alertas configuradas (do-app-spec.yml)**: `DEPLOYMENT_FAILED`, `CPU_UTILIZATION>85%`, `MEM_UTILIZATION>90%`, `RESTART_COUNT>3/5min` → email + Slack `#pymerp-alerts`.

## 7. Contactos y Escalación
- **Tier 1 – Development Team**: L-V 09:00-18:00 CLT · Slack `#pymerp-alerts` · dev@pymerp.cl · SLA 30 min.
- **Tier 2 – DevOps / Infrastructure**: 24/7 para P1/P2 · Slack `#infrastructure-emergencies` · Pager +56 X XXXX XXXX · SLA 15 min (P1) / 1 h (P2).
- **Tier 3 – DigitalOcean Support**: https://cloud.digitalocean.com/support/tickets · Plan Business ($100/mes) · Para fallas de plataforma (DB down, networking).
- **Severidad**:
  - P1: sitio caído / pérdida de datos → escalar directo a Tier 2 + rollback inmediato.
  - P2: funcionalidad crítica degradada → Tier 1 horario hábil, Tier 2 fuera de horario.
  - P3: funcionalidad no crítica → resolver siguiente día laboral.
  - P4: mejoras / documentación → backlog del sprint.

## 8. Comandos de Emergencia
```bash
# Estado general de la app
doctl apps get <APP_ID> --format Spec.Name,ActiveDeployment.Phase,ActiveDeployment.Progress

# Escalamiento horizontal (más instancias)
# Editar do-app-spec.yml -> instance_count: 3 y aplicar
doctl apps update <APP_ID> --spec do-app-spec.yml

# Escalamiento vertical (más CPU/RAM)
# Cambiar instance_size_slug (ej. basic-s) en do-app-spec.yml y actualizar

doctl apps update <APP_ID> --spec do-app-spec.yml

# Rebuild sin cambios de código (refresh)
doctl apps create-deployment <APP_ID>

# Ver uso de recursos configurado
doctl apps get <APP_ID> --format Spec.Services[0].InstanceCount,Spec.Services[0].InstanceSizeSlug
```
- **Pausar tráfico**: no disponible en App Platform (solo eliminar y recrear servicio).

## 9. Checklist Post-Incidente
1. Documentar en `docs/deployment-log.md` (timestamp, causa raíz, resolución, responsables).
2. Actualizar `docs/RUNBOOK.md` si el caso aporta nuevo troubleshooting.
3. Crear issue en GitHub con label `incident` + milestone `post-mortem`.
4. Agendar post-mortem ≤48 h posteriores.
5. Implementar acciones preventivas (alertas, validaciones, pruebas automatizadas).
6. Ajustar `scripts/production-health-check.sh` si se detectan brechas.

## 10. Mantenimiento Programado
- **Backups**: PostgreSQL automático diario (retención 7 días). Redis no persistente (cache only). Spaces con versionado habilitado.
- **Variables de entorno**: respaldos encriptados en `.digitalocean/env-backup-*.enc` (almacenar en cofre seguro).
- **Actualizaciones**:
  - Spring Boot: revisar CVE mensualmente, aplicar parches menores.
  - PostgreSQL y Redis Managed: minor upgrades automáticos (16.x / 7.x).
  - Dependencias: Renovate crea PRs automáticas.
- **Ventanas de mantenimiento**: martes 02:00-04:00 CLT (bajo tráfico). Notificar stakeholders con 48 h de anticipación.
- **Rollback plan**: mantener deployment previo listo para fallback y confirmar backups antes de cambios mayores.
- **Validación**: revisar este RUNBOOK mensualmente y después de cada incidente mayor.

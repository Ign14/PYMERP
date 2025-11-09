# DigitalOcean – Infra Sprint 10 · Fase 2

Guía paso a paso para levantar todos los componentes requeridos en DigitalOcean usando `doctl`. Asume que la región primaria será `nyc3`; ajusta las variables según tu proyecto/entorno.

## 0. Prerrequisitos

```bash
# Instalar/actualizar doctl (Linux x86_64)
curl -sL https://github.com/digitalocean/doctl/releases/latest/download/doctl-$(uname | tr '[:upper:]' '[:lower:]')-amd64.tar.gz \
  | tar -xz -C /usr/local/bin doctl

# Autenticación y contexto
doctl auth init --context pymerp-prod
doctl auth switch --context pymerp-prod

# Variables comunes
export DO_REGION=nyc3
export PROJECT_TAG=pymerp
```

> **Tip:** agrupa los recursos dentro del mismo proyecto en el panel de DigitalOcean para heredar etiquetado y políticas compartidas.

## 1. PostgreSQL Managed Database

**Objetivo:** PostgreSQL 16.x, 1 GB RAM (plan `db-s-1vcpu-1gb`), backups diarios habilitados por defecto.

```bash
# Crear clúster y capturar el ID
POSTGRES_ID=$(doctl databases create pymerp-postgres \
  --engine pg \
  --version 16 \
  --region "$DO_REGION" \
  --size db-s-1vcpu-1gb \
  --num-nodes 1 \
  --tag-names "$PROJECT_TAG" \
  --format ID --no-header)

# Forzar ventana de mantenimiento (domingo 04:00 UTC)
doctl databases maintenance-window update "$POSTGRES_ID" \
  --day sunday \
  --hour 04:00

# Verificar backups automáticos (retención diaria)
doctl databases backups list "$POSTGRES_ID"
```

- **Usuarios/DB iniciales:** `doctl databases db create "$POSTGRES_ID" pymerp_app` y `doctl databases user create "$POSTGRES_ID" backend`.
- **Conexión privada:** `doctl databases connection "$POSTGRES_ID" --format URI --no-header`.
- **Costo estimado:** **USD 15/mes** (plan básico 1 GB con backups incluidos).

## 2. Redis Managed Database

**Objetivo:** Redis 7.x administrado, 512 MB máximo (plan mínimo disponible es 1 GB; usa políticas de expiración `allkeys-lru` para cumplir el comportamiento requerido).

```bash
# Crear clúster Redis y capturar el ID
REDIS_ID=$(doctl databases create pymerp-redis \
  --engine redis \
  --version 7 \
  --region "$DO_REGION" \
  --size db-s-1vcpu-1gb \
  --num-nodes 1 \
  --tag-names "$PROJECT_TAG" \
  --format ID --no-header)

# Configurar política de eviction global
doctl databases configuration update "$REDIS_ID" \
  --engine redis \
  --config-json '{"redis_maxmemory_policy":"allkeys-lru"}'

# (Opcional) Definir timeout de conexión y persistencia
doctl databases configuration update "$REDIS_ID" \
  --engine redis \
  --config-json '{"redis_timeout":600,"redis_persistence":"rdb"}'
```

- **Costo estimado:** **USD 15/mes** (plan básico Redis 1 GB; usar límites de memoria + eviction para comportarse como 512 MB efectivos).

## 3. Container Registry (DOCR)

**Objetivo:** Registro `pymerp` en plan Basic (sin costo hasta 1 GiB almacenado).

```bash
# Crear el registro
doctl registry create pymerp \
  --region "$DO_REGION" \
  --subscription-tier basic

# Iniciar sesión para subir imágenes
doctl registry login

# Marcar políticas de expiración de tags (ejemplo: conservar 30 tags por servicio)
doctl registry garbage-collection start pymerp \
  --include-untagged-manifests=false
```

- **Costo estimado:** **USD 0/mes** en plan Basic (≥1 GiB o transferencia adicional cambia a USD 20/mes plan Standard).

## 4. App Platform (pymerp-backend)

**Objetivo:** Servicio App Platform con 1 vCPU / 1 GB (slug `basic-s`), auto-escalado 1–3 instancias, origen DOCR `pymerp`.

1. Guardar el siguiente spec en `do-app-spec.yml`:

```yaml
name: pymerp-backend
region: ${DO_REGION}
services:
  - name: pymerp-backend
    http_port: 8080
    environment_slug: container
    instance_size_slug: basic-s        # 1 vCPU, 1 GB RAM
    instance_count: 1
    autoscaling:
      min_instance_count: 1
      max_instance_count: 3
      metrics:
        cpu:
          percent: 70
    image:
      registry_type: DOCR
      repository: pymerp/backend
      tag: latest
    envs:
      - key: SPRING_PROFILES_ACTIVE
        value: prod
      - key: POSTGRES_URL
        scope: RUN_AND_BUILD_TIME
        value: ${POSTGRES_URL}
      - key: REDIS_URL
        scope: RUN_AND_BUILD_TIME
        value: ${REDIS_URL}
```

2. Desplegar / actualizar:

```bash
export POSTGRES_URL=$(doctl databases connection "$POSTGRES_ID" --format URI --no-header)
export REDIS_URL=$(doctl databases connection "$REDIS_ID" --format URI --no-header)

doctl apps create --spec do-app-spec.yml
# Para cambios posteriores:
# doctl apps update <APP_ID> --spec do-app-spec.yml
```

- **Costo estimado:** **USD 24/mes** por instancia básica (1 vCPU/1 GB). Con autoescalado 1–3, planifica **USD 24–72/mes** según carga real.

## 5. Spaces / S3 (`pymes-prod`, ACL privada)

`doctl` aún no expone un subcomando para crear buckets de Spaces, pero sí permite generar claves de acceso. Usa la compatibilidad S3 (AWS CLI o s3cmd) una vez tengas las credenciales.

```bash
# Generar access key / secret para operadores de almacenamiento
doctl spaces keys create pymes-prod-uploader --region "$DO_REGION"

# Usa las claves anteriores con AWS CLI
export SPACES_KEY=<ACCESS_KEY>
export SPACES_SECRET=<SECRET_KEY>
aws --endpoint-url https://$DO_REGION.digitaloceanspaces.com \
  s3api create-bucket --bucket pymes-prod --acl private \
  --region "$DO_REGION"
```

- Activa versionado y lifecycle policies (s3api) para eliminar objetos viejos si fuese necesario.
- **Costo estimado:** **USD 5/mes** (incluye 250 GB + 1 TB de transferencia saliente). Almacenamiento adicional USD 0.02/GB y egresos adicionales USD 0.01/GB.

## 6. Resumen de costos mensuales

| Recurso                               | Plan / Detalle                 | USD/mes aprox. |
|---------------------------------------|--------------------------------|---------------:|
| PostgreSQL Managed DB (1 GB)          | `db-s-1vcpu-1gb`, 1 nodo       | 15 |
| Redis Managed DB (1 GB, LRU 512 MB)   | `db-s-1vcpu-1gb`, 1 nodo       | 15 |
| App Platform pymerp-backend           | `basic-s`, 1–3 instancias      | 24–72 |
| Container Registry (Basic)            | Hasta 1 GiB                    | 0 |
| Spaces pymes-prod (250 GB incluidos)  | Plan estándar                  | 5 |
| **Total estimado**                    |                                | **59–107** |

> **Notas de control de costos**
> - App Platform domina el gasto: monitorea el auto-escalado (CPU objetivo 70 %). Ajusta a `basic-xs` o `basic-m` según consumo real.
> - Configura alertas de uso en `Billing → Cost Alerts` para USD 80 y USD 100.
> - Usa `doctl databases metrics` y `doctl apps logs` para vigilar saturación antes de escalar verticalmente.

## 7. Checklist posterior al aprovisionamiento

- [ ] Añadir reglas de firewall / VPC para que App Platform se conecte solo por redes privadas a PostgreSQL/Redis.
- [ ] Rotar credenciales (`doctl databases user reset` y `doctl spaces keys delete`) previo a pasar a producción.
- [ ] Exportar las URIs consolidadas en `docs/ENV_VARIABLES.md` para mantener una sola fuente de verdad.

Con estos pasos la infraestructura mínima solicitada queda automatizada y documentada.

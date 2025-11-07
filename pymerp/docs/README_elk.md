# ELK Monitoring - PYMERP

Sistema de monitoreo centralizado usando Elasticsearch, Logstash y Kibana (ELK Stack) para logs estructurados, análisis de errores, performance y métricas de negocio.

## 📋 Tabla de Contenidos

- [Arquitectura](#arquitectura)
- [Inicio Rápido](#inicio-rápido)
- [Configuración](#configuración)
- [Dashboards](#dashboards)
- [Queries Útiles](#queries-útiles)
- [Troubleshooting](#troubleshooting)
- [Producción](#producción)

---

## 🏗️ Arquitectura

```
┌─────────────────┐
│  Spring Boot    │  Logs en formato JSON (LogstashEncoder)
│  Application    │  MDC: traceId, userId, companyId, requestUri, method
└────────┬────────┘
         │ logs/pymerp.log (rotación diaria, max 100MB)
         │
         ↓
┌─────────────────┐
│   Logstash      │  Pipeline: input → filter → output
│                 │  - Parse JSON logs
│                 │  - Enrich with metadata
│                 │  - Extract performance metrics
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│ Elasticsearch   │  Indices: logs-pymerp-YYYY.MM.dd
│                 │  Almacenamiento: 30 días
│                 │  Búsqueda full-text
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│    Kibana       │  UI: http://localhost:5601
│                 │  Dashboards, visualizaciones, alertas
└─────────────────┘
```

---

## 🚀 Inicio Rápido

### 1. Iniciar Stack ELK

```bash
# Desde la raíz del proyecto
docker-compose -f docker-compose.elk.yml up -d
```

**Servicios:**
- **Elasticsearch:** http://localhost:9200
- **Logstash:** http://localhost:9600
- **Kibana:** http://localhost:5601

### 2. Verificar servicios

```bash
# Elasticsearch health
curl http://localhost:9200/_cluster/health

# Logstash stats
curl http://localhost:9600/_node/stats/pipelines

# Kibana status
curl http://localhost:5601/api/status
```

### 3. Iniciar aplicación en modo producción

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=prod'
```

Los logs se generarán en formato JSON en:
- **Console:** Output JSON a stdout (capturado por Logstash)
- **File:** `backend/logs/pymerp.log` (rotación automática)

### 4. Configurar Kibana

1. Abrir http://localhost:5601
2. Ir a **Management → Stack Management → Index Patterns**
3. Click "Create index pattern"
4. Index pattern: `logs-pymerp-*`
5. Time field: `@timestamp`
6. Click "Create index pattern"

---

## ⚙️ Configuración

### Logback (Spring Boot)

**Archivo:** `backend/src/main/resources/logback-spring.xml`

**Profiles:**
- **dev:** Logs legibles en consola (desarrollo local)
- **prod:** Logs JSON + archivo con rotación (producción)

**MDC Fields:**
- `traceId`: UUID único por request (tracking distribuido)
- `userId`: Usuario autenticado (desde SecurityContext)
- `companyId`: Compañía en contexto multitenancy
- `requestUri`: URI del request HTTP
- `method`: Método HTTP (GET, POST, etc.)

**Header X-Trace-ID:**
Cada response incluye header `X-Trace-ID` con el traceId para debugging.

### Logstash Pipeline

**Archivo:** `logstash/pipeline/logstash.conf`

**Input:**
- TCP port 5000 (logs en tiempo real)
- File `/var/log/pymerp/*.log` (logs desde archivos)

**Filter:**
- Parse timestamp ISO8601
- Tag errores (`level:ERROR`)
- Extract performance metrics (`duration_ms`)
- Enrich con tenant info (`companyId`)

**Output:**
- Elasticsearch index: `logs-pymerp-YYYY.MM.dd`
- Console (debugging, comentar en prod)

### Elasticsearch

**Configuración:**
- Single node (desarrollo)
- Sin seguridad (`xpack.security.enabled=false`)
- Memoria: 512MB heap

**Producción:**
- Cluster multi-node
- Habilitar seguridad (TLS + authentication)
- Aumentar heap según volumen de logs

### Kibana

**Dashboards incluidos:**
- Error Monitoring (errores por hora, top errores, stack traces)
- Performance (tiempos de respuesta, requests/min, endpoints lentos)
- Authentication (login attempts, failures, success rate)
- Business Metrics (ventas, compras, clientes, actividad por compañía)

Ver detalles en: `kibana/dashboards/README.md`

---

## 📊 Dashboards

### 1. Error Monitoring

**URL:** http://localhost:5601/app/dashboards

**Visualizaciones:**
- Errores por hora (últimas 24h)
- Top 10 errores más frecuentes
- Stack traces recientes
- Errores por compañía (multitenancy)

### 2. Performance Monitoring

**Visualizaciones:**
- Tiempos de respuesta (percentiles 50, 95, 99)
- Requests por minuto
- Endpoints más lentos (avg, max duration)
- Requests por método HTTP (GET, POST, etc.)

### 3. Authentication & Security

**Visualizaciones:**
- Login attempts (últimas 24h)
- Login failures
- Failed attempts by user
- Success rate (gauge)

### 4. Business Metrics

**Visualizaciones:**
- Ventas creadas (últimas 7 días)
- Compras creadas (últimas 7 días)
- Nuevos clientes registrados (últimos 30 días)
- Actividad por compañía
- Operaciones por usuario

---

## 🔍 Queries Útiles

### Errores últimas 24h
```
level:ERROR AND @timestamp:[now-24h TO now]
```

### Buscar por usuario
```
userId:"admin@company.com"
```

### Buscar por compañía
```
companyId:1
```

### Performance lento (>1s)
```
duration_ms:>1000
```

### Excepciones con stack trace
```
stack_trace:* AND level:ERROR
```

### Requests a endpoint específico
```
requestUri:/api/v1/sales*
```

### Logs de autenticación fallida
```
requestUri:/api/v1/auth* AND (level:ERROR OR level:WARN)
```

### Actividad de compañía en últimos 7 días
```
companyId:1 AND @timestamp:[now-7d TO now]
```

### Buscar por traceId (debugging request completo)
```
traceId:"550e8400-e29b-41d4-a716-446655440000"
```

### Logs de un usuario en rango de tiempo
```
userId:"john@example.com" AND @timestamp:[2025-11-01 TO 2025-11-07]
```

---

## 🛠️ Troubleshooting

### Elasticsearch no arranca

**Síntoma:** Container `pymerp-elasticsearch` en estado `Restarting`

**Solución:**
```bash
# Ver logs
docker logs pymerp-elasticsearch

# Verificar permisos del volumen
docker volume inspect pymerp_elasticsearch-data

# Si es problema de permisos (Linux):
sudo chown -R 1000:1000 /path/to/elasticsearch-data/
```

### Logstash no procesa logs

**Síntoma:** Logs no aparecen en Kibana

**Diagnóstico:**
```bash
# Verificar pipeline Logstash
curl -X GET "localhost:9600/_node/stats/pipelines"

# Ver logs de Logstash
docker logs pymerp-logstash

# Verificar que existe archivo de logs
ls -lh backend/logs/pymerp.log
```

**Solución:**
- Verificar que aplicación está en profile `prod`
- Verificar que archivo `backend/logs/pymerp.log` existe
- Reiniciar Logstash: `docker restart pymerp-logstash`

### Kibana no conecta con Elasticsearch

**Síntoma:** Kibana muestra "Kibana server is not ready yet"

**Diagnóstico:**
```bash
# Verificar health de Elasticsearch
curl http://localhost:9200/_cluster/health

# Ver logs de Kibana
docker logs pymerp-kibana
```

**Solución:**
- Esperar a que Elasticsearch termine de iniciar (health check)
- Verificar que containers están en la misma red `elk`
- Reiniciar Kibana: `docker restart pymerp-kibana`

### Logs no tienen campos MDC

**Síntoma:** Logs no muestran `traceId`, `userId`, `companyId`

**Diagnóstico:**
- Verificar que `LoggingContextFilter` está registrado en `SecurityConfig`
- Verificar profile activo: `prod` (no `dev`)
- Buscar en Kibana: `traceId:*` (debe retornar resultados)

**Solución:**
```bash
# Verificar orden de filtros en SecurityConfig
# LoggingContextFilter debe ir ANTES de JwtAuthenticationFilter

# Reiniciar aplicación con profile prod
./gradlew bootRun --args='--spring.profiles.active=prod'
```

### Disco lleno por logs

**Síntoma:** Elasticsearch usa mucho espacio

**Diagnóstico:**
```bash
# Ver tamaño de índices
curl -X GET "localhost:9200/_cat/indices/logs-pymerp-*?v&s=store.size:desc"

# Ver uso de disco
docker system df
```

**Solución:**
```bash
# Eliminar índices antiguos (mayores a 30 días)
curl -X DELETE "localhost:9200/logs-pymerp-2025.10.*"

# Configurar ILM (Index Lifecycle Management) para auto-delete
# Ver documentación: https://www.elastic.co/guide/en/elasticsearch/reference/current/index-lifecycle-management.html
```

### Performance de Elasticsearch degradada

**Síntoma:** Búsquedas lentas, alta latencia

**Diagnóstico:**
```bash
# Ver stats de cluster
curl -X GET "localhost:9200/_cluster/stats?human&pretty"

# Ver hot threads
curl -X GET "localhost:9200/_nodes/hot_threads"
```

**Solución:**
- Aumentar heap de Elasticsearch (`ES_JAVA_OPTS=-Xms1g -Xmx1g`)
- Reducir retención de logs (eliminar índices antiguos)
- Considerar cluster multi-node para producción

---

## 🏭 Producción

### Checklist de Seguridad

- [ ] Habilitar autenticación en Elasticsearch (`xpack.security.enabled=true`)
- [ ] Configurar TLS para comunicación cluster
- [ ] Usar contraseñas seguras (no defaults)
- [ ] Limitar acceso por firewall (puertos 9200, 5601 solo internos)
- [ ] Habilitar audit logging en Elasticsearch
- [ ] Configurar roles y permisos en Kibana

### Escalabilidad

**Elasticsearch Cluster:**
- **Master nodes:** 3 (quorum, alta disponibilidad)
- **Data nodes:** 3+ (distribución de shards)
- **Coordinating nodes:** 2+ (balanceo de carga)

**Logstash:**
- **Workers:** Configurar según CPU (`pipeline.workers: 4`)
- **Batch size:** Ajustar según throughput (`pipeline.batch.size: 125`)
- **Multiple pipelines:** Separar por tipo de log

**Configuración recomendada:**
```yaml
# docker-compose.elk.prod.yml
version: '3.8'
services:
  elasticsearch-master-1:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    environment:
      - node.name=es-master-1
      - cluster.name=pymerp-cluster
      - discovery.seed_hosts=es-master-2,es-master-3
      - cluster.initial_master_nodes=es-master-1,es-master-2,es-master-3
      - node.roles=master
      - xpack.security.enabled=true
      - "ES_JAVA_OPTS=-Xms2g -Xmx2g"
    volumes:
      - es-master-1-data:/usr/share/elasticsearch/data
  
  # ... (más nodos)
```

### Retención de Logs

**Estrategia recomendada:**
- **Hot tier (SSD):** Últimos 7 días (búsquedas rápidas)
- **Warm tier (HDD):** 8-30 días (búsquedas ocasionales)
- **Delete:** >30 días (cumplimiento GDPR/compliance)

**Configurar ILM Policy:**
```bash
curl -X PUT "localhost:9200/_ilm/policy/pymerp-logs-policy" -H 'Content-Type: application/json' -d'
{
  "policy": {
    "phases": {
      "hot": {
        "actions": {
          "rollover": {
            "max_size": "50GB",
            "max_age": "1d"
          }
        }
      },
      "warm": {
        "min_age": "7d",
        "actions": {
          "allocate": {
            "number_of_replicas": 1
          }
        }
      },
      "delete": {
        "min_age": "30d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}
'
```

### Backup y Restore

**Snapshot repository:**
```bash
# Configurar snapshot repo (S3, NFS, etc.)
curl -X PUT "localhost:9200/_snapshot/pymerp-backups" -H 'Content-Type: application/json' -d'
{
  "type": "fs",
  "settings": {
    "location": "/mnt/backups/elasticsearch"
  }
}
'

# Crear snapshot
curl -X PUT "localhost:9200/_snapshot/pymerp-backups/snapshot-$(date +%Y%m%d)"

# Restore
curl -X POST "localhost:9200/_snapshot/pymerp-backups/snapshot-20251107/_restore"
```

### Monitoreo del Stack ELK

**Herramientas:**
- **Metricbeat:** Métricas de Elasticsearch, Logstash, Kibana
- **Filebeat:** Logs de containers ELK
- **APM:** Application Performance Monitoring

**Alertas críticas:**
1. Elasticsearch cluster health != green
2. Disco >85% lleno
3. JVM heap >90% usado
4. No logs recibidos en 5 minutos

---

## 📚 Referencias

- [Elasticsearch Documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Logstash Documentation](https://www.elastic.co/guide/en/logstash/current/index.html)
- [Kibana Documentation](https://www.elastic.co/guide/en/kibana/current/index.html)
- [Logstash Logback Encoder](https://github.com/logfellow/logstash-logback-encoder)
- [Spring Boot Logging](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.logging)

---

## 📝 Notas

- Los logs en `dev` profile usan formato legible (no JSON) para facilitar debugging local
- El header `X-Trace-ID` permite correlacionar requests distribuidos
- MDC se limpia automáticamente en `LoggingContextFilter.finally` para evitar memory leaks
- Logstash procesa logs en tiempo real (TCP) y desde archivos (rotación)
- Elasticsearch indices se crean automáticamente con patrón `logs-pymerp-YYYY.MM.dd`

---

**Versión:** 1.0  
**Última actualización:** 7 de noviembre de 2025  
**Mantenido por:** PYMERP Development Team

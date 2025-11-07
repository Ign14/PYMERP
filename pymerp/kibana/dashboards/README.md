# Configuración de Kibana Dashboards para PYMERP

## 1. Crear Index Pattern

1. Ir a **Management → Stack Management → Index Patterns**
2. Click en "Create index pattern"
3. Index pattern: `logs-pymerp-*`
4. Time field: `@timestamp`
5. Click "Create index pattern"

## 2. Dashboard: Error Monitoring

**Nombre:** PYMERP - Error Monitoring

**Visualizaciones:**

### 2.1. Errores por hora (últimas 24h)
- **Tipo:** Line chart
- **Query:** `level:ERROR`
- **Y-axis:** Count
- **X-axis:** @timestamp (hourly)
- **Time range:** Last 24 hours

### 2.2. Top 10 Errores más frecuentes
- **Tipo:** Data table
- **Query:** `level:ERROR`
- **Columns:** logger, message, count
- **Sort:** Count (descending)
- **Rows:** 10

### 2.3. Stack Traces recientes
- **Tipo:** Logs
- **Query:** `level:ERROR AND stack_trace:*`
- **Columns:** @timestamp, userId, companyId, message, stack_trace
- **Rows:** 20

### 2.4. Errores por compañía (multitenancy)
- **Tipo:** Pie chart
- **Query:** `level:ERROR`
- **Slice by:** companyId
- **Size:** Count

## 3. Dashboard: Performance Monitoring

**Nombre:** PYMERP - Performance

**Visualizaciones:**

### 3.1. Tiempos de respuesta (percentiles)
- **Tipo:** Line chart
- **Query:** `duration_ms:*`
- **Y-axis:** Percentiles (50, 95, 99) of duration_ms
- **X-axis:** @timestamp (5 min intervals)
- **Time range:** Last 1 hour

### 3.2. Requests por minuto
- **Tipo:** Line chart
- **Query:** `*`
- **Y-axis:** Count
- **X-axis:** @timestamp (1 min intervals)
- **Time range:** Last 1 hour

### 3.3. Endpoints más lentos
- **Tipo:** Data table
- **Query:** `duration_ms:*`
- **Columns:** requestUri, method, avg(duration_ms), max(duration_ms), count
- **Sort:** avg(duration_ms) (descending)
- **Rows:** 10

### 3.4. Requests por método HTTP
- **Tipo:** Pie chart
- **Query:** `*`
- **Slice by:** method (GET, POST, PUT, DELETE)
- **Size:** Count

## 4. Dashboard: Authentication & Security

**Nombre:** PYMERP - Authentication

**Visualizaciones:**

### 4.1. Login attempts (últimas 24h)
- **Tipo:** Line chart
- **Query:** `requestUri:/api/v1/auth/login`
- **Y-axis:** Count
- **X-axis:** @timestamp (hourly)
- **Time range:** Last 24 hours

### 4.2. Login failures
- **Tipo:** Line chart
- **Query:** `requestUri:/api/v1/auth/login AND (level:ERROR OR level:WARN)`
- **Y-axis:** Count
- **X-axis:** @timestamp (hourly)
- **Time range:** Last 24 hours

### 4.3. Failed login attempts by user
- **Tipo:** Data table
- **Query:** `requestUri:/api/v1/auth/login AND level:ERROR`
- **Columns:** @timestamp, message, count
- **Sort:** count (descending)
- **Rows:** 10

### 4.4. Success rate
- **Tipo:** Gauge
- **Query success:** `requestUri:/api/v1/auth/login AND level:INFO`
- **Query total:** `requestUri:/api/v1/auth/login`
- **Formula:** (success / total) * 100
- **Ranges:** 0-50 (red), 50-90 (yellow), 90-100 (green)

## 5. Dashboard: Business Metrics

**Nombre:** PYMERP - Business Metrics

**Visualizaciones:**

### 5.1. Ventas creadas (últimas 7 días)
- **Tipo:** Line chart
- **Query:** `logger:*SaleService AND message:*created*`
- **Y-axis:** Count
- **X-axis:** @timestamp (daily)
- **Time range:** Last 7 days

### 5.2. Compras creadas (últimas 7 días)
- **Tipo:** Line chart
- **Query:** `logger:*PurchaseService AND message:*created*`
- **Y-axis:** Count
- **X-axis:** @timestamp (daily)
- **Time range:** Last 7 days

### 5.3. Nuevos clientes registrados
- **Tipo:** Metric
- **Query:** `logger:*CustomerService AND message:*created*`
- **Aggregation:** Count
- **Time range:** Last 30 days

### 5.4. Actividad por compañía
- **Tipo:** Bar chart
- **Query:** `*`
- **Y-axis:** Count
- **X-axis:** companyId
- **Time range:** Last 24 hours

### 5.5. Operaciones por usuario
- **Tipo:** Data table
- **Query:** `userId:* AND (message:*created* OR message:*updated* OR message:*deleted*)`
- **Columns:** userId, companyId, count
- **Sort:** count (descending)
- **Rows:** 20

## 6. Queries Útiles

### Buscar errores últimas 24h
```
level:ERROR AND @timestamp:[now-24h TO now]
```

### Buscar por usuario específico
```
userId:"admin@company.com"
```

### Buscar por compañía
```
companyId:1
```

### Performance lento (>1 segundo)
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

### Actividad de una compañía en período específico
```
companyId:1 AND @timestamp:[now-7d TO now]
```

## 7. Alertas Recomendadas

### Alert 1: Error Rate Threshold
- **Condición:** level:ERROR count > 100 in 5 minutes
- **Acción:** Send email to ops team
- **Severidad:** High

### Alert 2: Performance Degradation
- **Condición:** avg(duration_ms) > 2000 in 10 minutes
- **Acción:** Send Slack notification
- **Severidad:** Medium

### Alert 3: Failed Login Attempts
- **Condición:** requestUri:/api/v1/auth/login AND level:ERROR count > 10 in 5 minutes
- **Acción:** Send email to security team
- **Severidad:** High

### Alert 4: No Logs Received
- **Condición:** * count = 0 in 5 minutes
- **Acción:** Send email to ops team
- **Severidad:** Critical

## 8. Exportar/Importar Dashboards

### Exportar
```bash
# Desde Kibana UI:
Management → Stack Management → Saved Objects → Export
# Seleccionar todos los dashboards y visualizaciones
# Guardar en kibana/dashboards/pymerp-dashboards.ndjson
```

### Importar
```bash
# Desde Kibana UI:
Management → Stack Management → Saved Objects → Import
# Seleccionar archivo kibana/dashboards/pymerp-dashboards.ndjson
```

---

**Nota:** Los dashboards se pueden crear manualmente desde la UI de Kibana siguiendo estas especificaciones, o se pueden importar si se exporta una configuración existente.

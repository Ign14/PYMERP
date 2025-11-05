# Guía de Auditoría y Logging

## 🎯 Objetivo

Sistema de auditoría completo para registrar todas las acciones críticas en PYMERP, cumpliendo con estándares de compliance (ISO 27001, SOC 2, GDPR).

## 📋 Características

### Eventos Auditados

1. **CRUD en entidades críticas**:
   - CREATE, READ, UPDATE, DELETE en: Customer, Product, Sale, Purchase, Supplier, Invoice, etc.
   - Captura automática con `@Audited` annotation

2. **Eventos de autenticación**:
   - LOGIN (exitoso)
   - FAILED_LOGIN (credenciales inválidas)
   - LOGOUT

3. **Eventos de autorización**:
   - ACCESS_DENIED (403 Forbidden)
   - Intentos de acceso sin permisos

### Información Capturada

Cada audit log contiene 16 campos:

| Campo | Descripción | Ejemplo |
|-------|-------------|---------|
| `timestamp` | Timestamp UTC | `2025-01-24T10:30:00Z` |
| `username` | Usuario que ejecutó la acción | `admin@company.com` |
| `userRoles` | Roles al momento de la acción | `ROLE_ADMIN,ROLE_ERP_USER` |
| `action` | Acción ejecutada | `DELETE`, `LOGIN`, `ACCESS_DENIED` |
| `entityType` | Tipo de entidad afectada | `Customer`, `Product` |
| `entityId` | ID de la entidad | `123` |
| `httpMethod` | Método HTTP | `DELETE`, `POST` |
| `endpoint` | Endpoint llamado | `/api/v1/customers/123` |
| `ipAddress` | IP del cliente | `192.168.1.1` |
| `userAgent` | Navegador/cliente | `Mozilla/5.0...` |
| `companyId` | ID de la empresa (multi-tenancy) | `1` |
| `statusCode` | Código de respuesta HTTP | `200`, `403`, `500` |
| `errorMessage` | Mensaje de error (si hubo) | `Insufficient permissions` |
| `requestBody` | JSON del request (opcional) | `{"name":"John"}` |
| `responseTimeMs` | Tiempo de respuesta | `152` (ms) |

## 🚀 Uso

### 1. Marcar Endpoints para Auditar

Usa la anotación `@Audited` en métodos de controllers:

```java
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'ERP_USER')")
  @Audited(action = "DELETE", entityType = "Customer")
  public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
    customerService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'ERP_USER')")
  @Audited(action = "CREATE", entityType = "Customer", captureRequestBody = true)
  public ResponseEntity<CustomerDTO> createCustomer(@RequestBody CustomerReq req) {
    // ...
  }
}
```

**Parámetros de @Audited**:

- `action`: Acción ejecutada (CREATE, READ, UPDATE, DELETE, etc.). Si se omite, se infiere del método HTTP.
- `entityType`: Tipo de entidad (Customer, Product, etc.). **Obligatorio**.
- `captureRequestBody`: `true` para capturar el JSON del request. **Default: false** (por privacidad).

### 2. Eventos de Autenticación Automáticos

El `SecurityEventListener` captura automáticamente:

- **LOGIN**: Cuando un usuario se autentica exitosamente
- **FAILED_LOGIN**: Cuando falla la autenticación (401 Unauthorized)
- **ACCESS_DENIED**: Cuando un usuario intenta acceder sin permisos (403 Forbidden)

No requiere configuración adicional.

### 3. Consultar Audit Logs (REST API)

Todos los endpoints requieren rol **ADMIN**:

#### 3.1. Obtener todos los logs (paginado)

```http
GET /api/v1/audit/logs?page=0&size=20
Authorization: Bearer <token>
```

**Respuesta**:
```json
{
  "content": [
    {
      "id": 1,
      "timestamp": "2025-01-24T10:30:00Z",
      "username": "admin@company.com",
      "userRoles": "ROLE_ADMIN,ROLE_ERP_USER",
      "action": "DELETE",
      "entityType": "Customer",
      "entityId": 123,
      "httpMethod": "DELETE",
      "endpoint": "/api/v1/customers/123",
      "ipAddress": "192.168.1.1",
      "companyId": 1,
      "statusCode": 200,
      "responseTimeMs": 152
    }
  ],
  "pageable": { ... },
  "totalElements": 1500,
  "totalPages": 75
}
```

#### 3.2. Filtrar por usuario

```http
GET /api/v1/audit/logs/user/admin@company.com?page=0&size=20
```

#### 3.3. Filtrar por acción

```http
GET /api/v1/audit/logs/action/ACCESS_DENIED?page=0&size=20
```

#### 3.4. Obtener solo requests fallidos (status >= 400)

```http
GET /api/v1/audit/logs/failed?page=0&size=20
```

#### 3.5. Filtrar por rango de fechas

```http
GET /api/v1/audit/logs/range?startDate=2025-01-01T00:00:00Z&endDate=2025-01-31T23:59:59Z&page=0&size=50
```

#### 3.6. Detectar ataques de fuerza bruta

```http
GET /api/v1/audit/security/failed-attempts/hacker@evil.com
```

**Respuesta**:
```json
15
```

Si el contador es alto (ej: >10 en 24 horas), indica posible ataque de fuerza bruta.

## 🔧 Configuración

### Exclusiones del Interceptor

El `AuditInterceptor` NO audita:

- `/api/v1/auth/login` → Redundante con `SecurityEventListener`
- `/api/v1/auth/refresh` → Tokens no son acciones críticas
- `/actuator/**` → Endpoints de monitoreo interno

Para agregar más exclusiones, edita `WebConfig.java`:

```java
registry.addInterceptor(auditInterceptor)
    .addPathPatterns("/api/**")
    .excludePathPatterns(
        "/api/v1/auth/**",
        "/actuator/**",
        "/api/v1/public/**"  // Agregar aquí
    );
```

### Async Logging

Los audit logs se guardan de forma **asíncrona** para no afectar el rendimiento:

```java
@Async
@Transactional
public void logAction(AuditLog auditLog) {
    auditLogRepository.save(auditLog);
}
```

La aplicación ya tiene `@EnableAsync` habilitado en `PymesApplication.java`.

### Retención de Datos

**Recomendación**: Implementar política de retención de 90 días para cumplir con GDPR.

Agregar un scheduled job en el futuro:

```java
@Scheduled(cron = "0 0 2 * * ?") // 2am diariamente
public void deleteOldLogs() {
    Instant cutoff = Instant.now().minus(90, ChronoUnit.DAYS);
    auditLogRepository.deleteByTimestampBefore(cutoff);
}
```

## 📊 Índices de Base de Datos

La migración `V27__create_audit_logs.sql` crea 6 índices para optimizar consultas:

1. `idx_audit_timestamp` → Orden cronológico descendente
2. `idx_audit_username` → Filtro por usuario
3. `idx_audit_company` → Multi-tenancy
4. `idx_audit_action` → Filtro por acción
5. `idx_audit_entity_type` → Filtro por tipo de entidad
6. `idx_audit_ip` → Investigación por IP

## ✅ Compliance

### ISO 27001 (A.12.4)

> **A.12.4.1**: Registrar eventos de acceso, uso y administración del sistema.

✅ **Cumplimiento**: Todos los eventos CRUD, LOGIN, ACCESS_DENIED se registran con timestamp, usuario, IP.

### SOC 2 (CC7.2)

> **CC7.2**: Monitoreo de actividades y alertas de seguridad.

✅ **Cumplimiento**: Endpoint `/api/v1/audit/security/failed-attempts/{username}` permite detectar ataques de fuerza bruta.

### GDPR (Art. 30)

> **Artículo 30**: Registro de actividades de tratamiento de datos personales.

✅ **Cumplimiento**: Campos `username`, `companyId`, `timestamp`, `action`, `entityType` permiten auditoría completa de operaciones sobre datos de clientes.

## 🧪 Testing

### Unit Tests

- **AuditServiceTest**: 6 tests para servicio de auditoría
- **AuditInterceptorTest**: 3 tests para captura de requests

Ejecutar:
```bash
./gradlew test --tests "com.datakomerz.pymes.audit.*"
```

### Integration Tests

Crear tests end-to-end para verificar flujo completo:

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AuditFlowIT {

  @Test
  void deleteCustomer_ShouldGenerateAuditLog() {
    // DELETE /api/v1/customers/123
    // Verificar que se creó 1 audit log con action=DELETE
  }
}
```

## 🔍 Troubleshooting

### Los logs no se guardan

1. **Verificar que `@EnableAsync` está habilitado** en `PymesApplication.java`
2. **Verificar que el endpoint tiene `@Audited`** annotation
3. **Ver logs de aplicación** para excepciones:
   ```
   ERROR AuditService - Error al guardar audit log: ...
   ```

### Logs se guardan con `companyId = null`

El JWT del usuario debe contener el claim `company_id`:

```json
{
  "sub": "admin@company.com",
  "company_id": 1,
  "realm_access": {
    "roles": ["ROLE_ADMIN"]
  }
}
```

Verificar configuración de Keycloak (mappers).

### Performance degradado

Si el volumen de audit logs es muy alto (>100k/día):

1. **Habilitar particionado de tabla** por mes:
   ```sql
   CREATE TABLE audit_logs_2025_01 PARTITION OF audit_logs
   FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
   ```

2. **Agregar índice compuesto** para queries frecuentes:
   ```sql
   CREATE INDEX idx_audit_company_timestamp_action
   ON audit_logs(company_id, timestamp DESC, action);
   ```

## 📚 Recursos Adicionales

- **ISO/IEC 27001:2022** - Anexo A.12.4 (Logging and monitoring)
- **SOC 2 Trust Services Criteria** - CC7.2 (Security)
- **GDPR Article 30** - Records of processing activities
- **Spring Security Events**: https://docs.spring.io/spring-security/reference/servlet/authentication/events.html

---

**Última actualización**: Sprint 4 - Audit & Logging Complete (Enero 2025)

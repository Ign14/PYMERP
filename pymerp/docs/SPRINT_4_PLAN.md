# Sprint 4: Auditoría y Logging Avanzado

**Fecha Inicio**: 4 de noviembre 2025  
**Duración Estimada**: 12-15 horas  
**Branch**: `security/sprint-4-audit-logging`  
**Prioridad**: Alta (Compliance & Security)

---

## 🎯 Objetivos

### Objetivo Principal
Implementar sistema de auditoría completo para registrar todas las acciones sensibles con contexto de seguridad (usuario, rol, IP, companyId, timestamp).

### Objetivos Específicos
1. ✅ Crear entidad `AuditLog` con todos los campos necesarios
2. ✅ Implementar interceptor de Spring Security para capturar eventos
3. ✅ Registrar automáticamente acciones CRUD en endpoints críticos
4. ✅ Crear API REST para consultar logs de auditoría
5. ✅ Implementar filtros y búsquedas avanzadas
6. ✅ Crear tests de auditoría
7. ✅ Documentar sistema de auditoría

---

## 📋 Tareas Detalladas

### Fase 1: Modelo de Datos (2h)

#### Task 4.1: Entidad AuditLog
**Archivo**: `backend/src/main/java/com/datakomerz/pymes/audit/AuditLog.java`

**Campos**:
```java
- id: Long (PK)
- timestamp: Instant (indexed)
- username: String (indexed)
- userRole: String (roles separados por coma)
- action: String (CREATE, READ, UPDATE, DELETE, LOGIN, LOGOUT, ACCESS_DENIED)
- entityType: String (Customer, Product, Sale, etc.)
- entityId: Long (nullable)
- httpMethod: String (GET, POST, PUT, DELETE)
- endpoint: String (/api/v1/customers/123)
- ipAddress: String (indexed)
- userAgent: String
- companyId: Long (indexed - multi-tenancy)
- statusCode: Integer (200, 403, 401, etc.)
- errorMessage: String (nullable)
- requestBody: String (JSON, nullable, max 4000 chars)
- responseTime: Long (milliseconds)
```

**Índices**:
- timestamp DESC
- username
- companyId
- action
- entityType

#### Task 4.2: Repository
**Archivo**: `backend/src/main/java/com/datakomerz/pymes/audit/AuditLogRepository.java`

**Queries**:
```java
- findByCompanyIdOrderByTimestampDesc(Long companyId, Pageable)
- findByUsernameAndCompanyId(String username, Long companyId, Pageable)
- findByActionAndCompanyId(String action, Long companyId, Pageable)
- findByEntityTypeAndCompanyId(String entityType, Long companyId, Pageable)
- findByTimestampBetweenAndCompanyId(Instant from, Instant to, Long companyId, Pageable)
- findByStatusCodeAndCompanyId(Integer statusCode, Long companyId, Pageable)
```

---

### Fase 2: Interceptor de Auditoría (4h)

#### Task 4.3: AuditInterceptor
**Archivo**: `backend/src/main/java/com/datakomerz/pymes/audit/AuditInterceptor.java`

**Funcionalidad**:
- Implementar `HandlerInterceptor` de Spring MVC
- `preHandle()`: Capturar request (timestamp inicio, username, roles, IP, endpoint, method)
- `afterCompletion()`: Guardar log (calcular responseTime, capturar statusCode, error si aplica)
- Filtrar endpoints a auditar (solo endpoints de negocio, excluir /actuator, /health)

**Configuración**:
```java
@Configuration
public class AuditConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditInterceptor)
            .addPathPatterns("/api/v1/**")
            .excludePathPatterns("/api/v1/auth/login", "/actuator/**");
    }
}
```

#### Task 4.4: Annotation @Audited
**Archivo**: `backend/src/main/java/com/datakomerz/pymes/audit/Audited.java`

**Uso**:
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
    String action(); // CREATE, UPDATE, DELETE
    String entityType(); // Customer, Product, etc.
}
```

**Aplicar a controladores**:
```java
@PostMapping
@PreAuthorize("hasAnyRole('ADMIN', 'SETTINGS')")
@Audited(action = "CREATE", entityType = "Customer")
public ResponseEntity<?> createCustomer(@RequestBody CustomerDto dto) { ... }
```

---

### Fase 3: Service y Aspect (3h)

#### Task 4.5: AuditService
**Archivo**: `backend/src/main/java/com/datakomerz/pymes/audit/AuditService.java`

**Métodos**:
```java
- logAction(AuditLogDto dto): void
- getAuditLogs(AuditFilterDto filter, Pageable pageable): Page<AuditLog>
- getAuditLogsByUser(String username, Long companyId, Pageable): Page<AuditLog>
- getAuditLogsByAction(String action, Long companyId, Pageable): Page<AuditLog>
- getAuditLogsByDateRange(Instant from, Instant to, Long companyId, Pageable): Page<AuditLog>
- getFailedAccessAttempts(Long companyId, Pageable): Page<AuditLog>
```

#### Task 4.6: Security Event Listener
**Archivo**: `backend/src/main/java/com/datakomerz/pymes/audit/SecurityEventListener.java`

**Eventos a capturar**:
```java
@EventListener
public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
    // Registrar LOGIN exitoso
}

@EventListener
public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
    // Registrar LOGIN fallido
}

@EventListener
public void onAccessDenied(AuthorizationDeniedEvent event) {
    // Registrar intentos 403 Forbidden
}
```

---

### Fase 4: API REST (2h)

#### Task 4.7: AuditLogController
**Archivo**: `backend/src/main/java/com/datakomerz/pymes/audit/AuditLogController.java`

**Endpoints**:
```java
GET /api/v1/audit/logs
  - Query params: username, action, entityType, from, to, page, size
  - Roles: ADMIN only
  - Returns: Page<AuditLogDto>

GET /api/v1/audit/logs/user/{username}
  - Roles: ADMIN only
  - Returns: Page<AuditLogDto>

GET /api/v1/audit/logs/failed-access
  - Roles: ADMIN only
  - Returns: Page<AuditLogDto> (solo 403/401)

GET /api/v1/audit/stats
  - Roles: ADMIN only
  - Returns: AuditStatsDto (total logs, por acción, por usuario)
```

---

### Fase 5: Tests (3h)

#### Task 4.8: Tests Unitarios
**Archivos**:
- `AuditServiceTest.java`: Validar lógica de filtros
- `AuditInterceptorTest.java`: Validar captura de requests
- `SecurityEventListenerTest.java`: Validar eventos de Spring Security

#### Task 4.9: Tests de Integración
**Archivos**:
- `AuditLogControllerIT.java`: Validar endpoints de auditoría
- `AuditFlowIT.java`: Validar flujo completo (acción → log guardado → consulta)

**Casos de prueba**:
1. ✅ CREATE customer → AuditLog con action=CREATE, entityType=Customer
2. ✅ DELETE product → AuditLog con action=DELETE, entityType=Product
3. ✅ Login exitoso → AuditLog con action=LOGIN
4. ✅ Access denied (403) → AuditLog con action=ACCESS_DENIED, statusCode=403
5. ✅ Filtrar logs por usuario
6. ✅ Filtrar logs por fecha
7. ✅ Solo ADMIN puede consultar logs

---

### Fase 6: Documentación (1h)

#### Task 4.10: Documentación Técnica
**Archivos**:
- `docs/AUDIT_GUIDE.md`: Guía completa de auditoría
- `docs/SPRINT_4_SUMMARY.md`: Resumen del sprint
- Actualizar `README_dev.md` con sección de auditoría

---

## 📊 Métricas de Éxito

### Cobertura
- ✅ Todos los endpoints críticos con auditoría
- ✅ Eventos de Spring Security capturados
- ✅ Tests: 15+ tests de auditoría

### Compliance
- ✅ ISO 27001: Registro de accesos (A.12.4)
- ✅ SOC 2: Logging and Monitoring (CC7.2)
- ✅ GDPR: Trazabilidad de accesos a datos personales (Art. 30)

### Performance
- ✅ Auditoría asíncrona (no bloquea requests)
- ✅ Índices en BD para queries rápidas
- ✅ Paginación en consultas

---

## 🚀 Estrategia de Implementación

### Orden de Ejecución
1. **Task 4.1-4.2**: Modelo de datos (entity + repository)
2. **Task 4.3**: AuditInterceptor básico
3. **Task 4.5**: AuditService
4. **Task 4.4**: Annotation @Audited
5. **Task 4.6**: Security Event Listener
6. **Task 4.7**: API REST
7. **Task 4.8-4.9**: Tests
8. **Task 4.10**: Documentación

### Commits Strategy
```
feat(audit): Add AuditLog entity and repository
feat(audit): Implement audit interceptor
feat(audit): Add @Audited annotation
feat(audit): Implement AuditService
feat(audit): Add security event listener
feat(audit): Create audit REST API
test(audit): Add audit tests (15 tests)
docs(audit): Add audit documentation
```

---

## 🏆 Beneficios Esperados

### Seguridad
- ✅ Detectar intentos de acceso no autorizado
- ✅ Trazabilidad completa de operaciones
- ✅ Identificar actividad sospechosa

### Compliance
- ✅ Cumplir requisitos de auditoría (ISO 27001, SOC 2, GDPR)
- ✅ Evidencia para auditorías externas
- ✅ Reportes de actividad por usuario/rol

### Operación
- ✅ Debugging de issues reportados por usuarios
- ✅ Análisis de uso de la aplicación
- ✅ Detección de patrones anómalos

---

## ⚠️ Consideraciones Técnicas

### Performance
- Auditoría en thread separado (no bloquear requests)
- Usar `@Async` en AuditService
- Cleanup de logs antiguos (retention policy: 90 días)

### Almacenamiento
- Estimación: ~500 bytes por log
- 1000 requests/día → 500 KB/día → 15 MB/mes
- Particionamiento de tabla por mes (opcional)

### Seguridad de Logs
- Logs de auditoría inmutables (no se pueden modificar/eliminar)
- Solo ADMIN puede consultar
- No almacenar passwords o tokens en requestBody

---

## 📅 Próximos Pasos

Después de Sprint 4:
- **Sprint 5**: Rate Limiting por rol
- **Sprint 6**: Field-Level Security
- **Sprint 7**: Tests E2E con Testcontainers

---

**Creado**: 4 de noviembre 2025  
**Autor**: Sistema de Desarrollo PYMERP  
**Estado**: 🚧 EN PROGRESO

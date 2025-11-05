# Sprint 5: Multi-tenancy Avanzado - Plan de Implementación

**Fecha inicio**: 5 de noviembre de 2025  
**Prioridad**: Alta  
**Estimación**: 14-16 horas  
**Prerequisitos**: Sprint 3 (RBAC) ✅, Sprint 4 (Audit) ✅

---

## 🎯 Objetivos del Sprint

### Problema Actual
El sistema actual tiene soporte básico de multi-tenancy:
- ✅ Campo `company_id` en entidades
- ✅ Header `X-Company-Id` en requests
- ⚠️ **Filtrado manual** en cada query
- ⚠️ **Sin protección automática** contra acceso cross-tenant
- ⚠️ **Código repetitivo** en todos los repositories

### Solución Propuesta
Implementar **Multi-tenancy Automático** con:
1. **JPA Filters** - Filtrado automático a nivel de Hibernate
2. **Tenant Context** - Thread-local para tenant actual
3. **Tenant Interceptor** - Validación automática de headers
4. **Tenant Validator** - Protección contra acceso cross-tenant
5. **Tenant Metrics** - Estadísticas por empresa

---

## 📋 Fases de Implementación

### **Fase 1: Tenant Context & Infrastructure** (3h)

#### 1.1. TenantContext - Thread-local storage
```java
@Component
public class TenantContext {
    private static final ThreadLocal<UUID> currentTenant = new ThreadLocal<>();
    
    public static void setTenantId(UUID tenantId) {
        currentTenant.set(tenantId);
    }
    
    public static UUID getTenantId() {
        return currentTenant.get();
    }
    
    public static void clear() {
        currentTenant.remove();
    }
    
    public static UUID require() {
        UUID tenant = currentTenant.get();
        if (tenant == null) {
            throw new TenantNotFoundException("No tenant context available");
        }
        return tenant;
    }
}
```

#### 1.2. TenantNotFoundException
```java
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TenantNotFoundException extends RuntimeException {
    public TenantNotFoundException(String message) {
        super(message);
    }
}
```

#### 1.3. CrossTenantAccessException
```java
@ResponseStatus(HttpStatus.FORBIDDEN)
public class CrossTenantAccessException extends RuntimeException {
    public CrossTenantAccessException(String message) {
        super(message);
    }
}
```

**Archivos a crear**:
- `backend/src/main/java/com/datakomerz/pymes/multitenancy/TenantContext.java`
- `backend/src/main/java/com/datakomerz/pymes/multitenancy/TenantNotFoundException.java`
- `backend/src/main/java/com/datakomerz/pymes/multitenancy/CrossTenantAccessException.java`

---

### **Fase 2: Tenant Interceptor & Filter** (3h)

#### 2.1. TenantInterceptor - Extract tenant from header
```java
@Component
public class TenantInterceptor implements HandlerInterceptor {
    
    private static final String TENANT_HEADER = "X-Company-Id";
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        String tenantId = request.getHeader(TENANT_HEADER);
        
        if (tenantId == null || tenantId.isBlank()) {
            // Permitir endpoints públicos (auth, actuator)
            String path = request.getRequestURI();
            if (isPublicPath(path)) {
                return true;
            }
            throw new TenantNotFoundException("Header " + TENANT_HEADER + " is required");
        }
        
        try {
            UUID tenant = UUID.fromString(tenantId);
            TenantContext.setTenantId(tenant);
            return true;
        } catch (IllegalArgumentException e) {
            throw new TenantNotFoundException("Invalid tenant ID format: " + tenantId);
        }
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, 
                               HttpServletResponse response, 
                               Object handler, 
                               Exception ex) {
        TenantContext.clear();
    }
    
    private boolean isPublicPath(String path) {
        return path.startsWith("/api/v1/auth/") || 
               path.startsWith("/actuator/");
    }
}
```

#### 2.2. Registrar TenantInterceptor en WebConfig
```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    // TenantInterceptor PRIMERO (establece contexto)
    registry.addInterceptor(tenantInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns("/api/v1/auth/**", "/actuator/**")
        .order(1);
    
    // AuditInterceptor SEGUNDO (usa contexto)
    registry.addInterceptor(auditInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns("/api/v1/auth/login", "/api/v1/auth/refresh", "/actuator/**")
        .order(2);
}
```

**Archivos a modificar/crear**:
- Crear: `backend/src/main/java/com/datakomerz/pymes/multitenancy/TenantInterceptor.java`
- Modificar: `backend/src/main/java/com/datakomerz/pymes/config/WebConfig.java`

---

### **Fase 3: JPA Tenant Filter** (4h)

#### 3.1. @TenantFiltered annotation
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantFiltered {
    String tenantColumn() default "company_id";
}
```

#### 3.2. HibernateConfig - Register tenant filter
```java
@Configuration
public class HibernateConfig {
    
    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilter() {
        FilterRegistrationBean<TenantFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TenantFilter());
        registrationBean.addUrlPatterns("/api/*");
        return registrationBean;
    }
}
```

#### 3.3. Actualizar entidades con @TenantFiltered
```java
@Entity
@Table(name = "products")
@TenantFiltered
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = "uuid"))
@Filter(name = "tenantFilter", condition = "company_id = :tenantId")
public class Product {
    // ... existing code
}
```

**Entidades a actualizar** (agregar @TenantFiltered + @FilterDef + @Filter):
- Product
- Customer
- Sale
- Purchase
- Supplier
- Service
- Inventory
- Location
- Finance
- Pricing
- CustomerSegment
- AccountRequest
- BillingDocument

#### 3.4. EntityManager Listener
```java
@Component
public class TenantFilterEnabler {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @EventListener(ApplicationReadyEvent.class)
    public void enableFilter() {
        Session session = entityManager.unwrap(Session.class);
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            session.enableFilter("tenantFilter")
                   .setParameter("tenantId", tenantId);
        }
    }
}
```

**Archivos a crear/modificar**:
- Crear: `backend/src/main/java/com/datakomerz/pymes/multitenancy/TenantFiltered.java`
- Crear: `backend/src/main/java/com/datakomerz/pymes/config/HibernateConfig.java`
- Crear: `backend/src/main/java/com/datakomerz/pymes/multitenancy/TenantFilterEnabler.java`
- Modificar: 13 entidades (agregar anotaciones)

---

### **Fase 4: Tenant Validator & Protection** (2h)

#### 4.1. @ValidateTenant annotation
```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateTenant {
    String entityParam() default "id";
    Class<?> entityClass();
}
```

#### 4.2. TenantValidationAspect
```java
@Aspect
@Component
public class TenantValidationAspect {
    
    @Autowired
    private EntityManager entityManager;
    
    @Before("@annotation(validateTenant)")
    public void validateTenantAccess(JoinPoint joinPoint, ValidateTenant validateTenant) {
        UUID currentTenant = TenantContext.require();
        
        // Extraer ID de la entidad del parámetro
        Object[] args = joinPoint.getArgs();
        Object entityId = extractEntityId(args, validateTenant.entityParam());
        
        if (entityId == null) {
            return; // No validation needed
        }
        
        // Verificar que la entidad pertenece al tenant actual
        Object entity = entityManager.find(validateTenant.entityClass(), entityId);
        if (entity == null) {
            throw new EntityNotFoundException("Entity not found");
        }
        
        UUID entityTenant = extractTenantId(entity);
        if (!currentTenant.equals(entityTenant)) {
            throw new CrossTenantAccessException(
                "Access denied: Entity belongs to different tenant"
            );
        }
    }
    
    private UUID extractTenantId(Object entity) {
        try {
            Method getter = entity.getClass().getMethod("getCompanyId");
            return (UUID) getter.invoke(entity);
        } catch (Exception e) {
            throw new IllegalStateException("Entity must have getCompanyId() method");
        }
    }
}
```

**Archivos a crear**:
- `backend/src/main/java/com/datakomerz/pymes/multitenancy/ValidateTenant.java`
- `backend/src/main/java/com/datakomerz/pymes/multitenancy/TenantValidationAspect.java`

---

### **Fase 5: Refactoring Repositories** (2h)

#### 5.1. Eliminar filtros manuales de companyId
**Antes**:
```java
@Query("SELECT p FROM Product p WHERE p.companyId = :companyId AND p.deletedAt IS NULL")
Page<Product> findByCompanyId(@Param("companyId") UUID companyId, Pageable pageable);
```

**Después** (con @TenantFiltered automático):
```java
Page<Product> findByDeletedAtIsNull(Pageable pageable);
```

#### 5.2. Actualizar Controllers para usar TenantContext
**Antes**:
```java
UUID companyId = companyContext.require();
productRepository.findByCompanyIdAndDeletedAtIsNull(companyId, pageable);
```

**Después**:
```java
// TenantContext ya está configurado por TenantInterceptor
productRepository.findByDeletedAtIsNull(pageable);
```

**Archivos a modificar**:
- Todos los repositories que tengan queries con `company_id`
- Todos los controllers que usen `companyContext.require()`

---

### **Fase 6: Testing** (3h)

#### 6.1. TenantContextTest
```java
@SpringBootTest
class TenantContextTest {
    
    @Test
    void shouldSetAndGetTenantId() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        assertEquals(tenantId, TenantContext.getTenantId());
        TenantContext.clear();
    }
    
    @Test
    void shouldThrowWhenNoTenant() {
        TenantContext.clear();
        assertThrows(TenantNotFoundException.class, TenantContext::require);
    }
}
```

#### 6.2. TenantInterceptorTest
```java
@SpringBootTest
@AutoConfigureMockMvc
class TenantInterceptorTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldRejectRequestWithoutTenantHeader() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void shouldAcceptRequestWithValidTenantHeader() throws Exception {
        mockMvc.perform(get("/api/v1/products")
            .header("X-Company-Id", UUID.randomUUID().toString()))
            .andExpect(status().isOk());
    }
}
```

#### 6.3. TenantFilterTest
```java
@SpringBootTest
@Transactional
class TenantFilterTest {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Test
    void shouldFilterByTenant() {
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();
        
        // Setup: Create products for 2 tenants
        createProduct("Product 1", tenant1);
        createProduct("Product 2", tenant2);
        
        // Test: Set tenant context and verify filtering
        TenantContext.setTenantId(tenant1);
        List<Product> products = productRepository.findAll();
        assertEquals(1, products.size());
        assertEquals("Product 1", products.get(0).getName());
        
        TenantContext.clear();
    }
}
```

**Archivos a crear**:
- `backend/src/test/java/com/datakomerz/pymes/multitenancy/TenantContextTest.java`
- `backend/src/test/java/com/datakomerz/pymes/multitenancy/TenantInterceptorTest.java`
- `backend/src/test/java/com/datakomerz/pymes/multitenancy/TenantFilterTest.java`

---

### **Fase 7: Documentación** (1h)

#### 7.1. MULTITENANCY_GUIDE.md
- Arquitectura de multi-tenancy
- Cómo funciona el filtrado automático
- Uso de TenantContext
- Migración de código legacy
- Best practices

#### 7.2. Actualizar README_dev.md
- Agregar sección de Multi-tenancy
- Explicar header X-Company-Id obligatorio

**Archivos a crear/modificar**:
- `docs/MULTITENANCY_GUIDE.md`
- `README_dev.md`

---

## 📊 Estimación de Tiempo

| Fase | Descripción | Tiempo |
|------|-------------|--------|
| 1 | Tenant Context & Infrastructure | 3h |
| 2 | Tenant Interceptor & Filter | 3h |
| 3 | JPA Tenant Filter | 4h |
| 4 | Tenant Validator & Protection | 2h |
| 5 | Refactoring Repositories | 2h |
| 6 | Testing | 3h |
| 7 | Documentación | 1h |
| **TOTAL** | | **18h** |

---

## ✅ Criterios de Aceptación

### Funcionales
- [ ] TenantContext establece y recupera tenant actual
- [ ] TenantInterceptor valida header X-Company-Id
- [ ] JPA Filter filtra automáticamente por company_id
- [ ] Validación automática contra acceso cross-tenant
- [ ] Repositories eliminan filtrado manual

### No Funcionales
- [ ] Sin impacto en performance (< 5ms overhead)
- [ ] Thread-safe (ThreadLocal)
- [ ] Backward compatible con código existente
- [ ] 100% de tests pasando

### Documentación
- [ ] MULTITENANCY_GUIDE.md completo
- [ ] README_dev.md actualizado
- [ ] Javadoc en componentes nuevos

---

## 🎯 Beneficios Esperados

### Para Desarrolladores
- ✅ Menos código repetitivo
- ✅ Protección automática contra bugs
- ✅ Queries más simples

### Para el Sistema
- ✅ Aislamiento garantizado de datos
- ✅ Imposible acceso cross-tenant
- ✅ Base para SaaS multi-cliente

### Para Compliance
- ✅ GDPR - Aislamiento de datos por empresa
- ✅ SOC 2 - Control de acceso granular
- ✅ ISO 27001 - Segregación de información

---

## 🚀 Siguientes Pasos

Después de Sprint 5, las opciones son:

1. **Sprint 6: Notificaciones Push** - WebSockets + SSE
2. **Sprint 7: Dashboard Analytics** - Métricas y reportes
3. **Sprint 8: API Versioning** - Versionado semántico

---

## 📝 Notas Técnicas

### Compatibilidad con Código Existente
- `CompanyContext` se mantiene por ahora (deprecated)
- Migración gradual de repositories
- Tests existentes siguen funcionando con mocks

### Performance Considerations
- JPA Filter aplica a nivel SQL (WHERE clause)
- ThreadLocal limpieza garantizada en afterCompletion
- Sin queries N+1 adicionales

### Seguridad
- TenantInterceptor ejecuta ANTES que AuditInterceptor
- Validación doble: interceptor + JPA filter
- Cross-tenant access bloqueado a nivel AOP

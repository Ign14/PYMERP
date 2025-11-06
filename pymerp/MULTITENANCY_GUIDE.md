# Guía de Multitenencia

Esta guía resume los componentes introducidos para soportar multitenencia en la
plataforma PYMERP y explica cómo utilizarlos correctamente en controladores,
servicios, repositorios y pruebas.

## 1. Flujo general del request
- `TenantInterceptor` lee el encabezado `X-Company-Id` y pobla `TenantContext`
  antes de llegar al controlador. El contexto se limpia en `afterCompletion`.
- Cualquier bean ejecutado dentro del request puede recuperar el tenant activo
  mediante `TenantContext` o su envoltorio `CompanyContext`.
- Si el encabezado falta o no es un UUID válido, se lanza
  `TenantNotFoundException` y el request termina con 400.

## 2. TenantContext y CompanyContext
- **Obtener tenant actual**: usa `companyContext.require()` cuando necesitas el
  valor (por ejemplo para rutas de almacenamiento o auditoría). Lanza excepción
  si no existe tenant, lo que evita trabajar en modo “sin aislamiento”.
- **Usos de conveniencia**: `companyContext.current()` devuelve `Optional<UUID>`
  cuando el tenant puede ser opcional (jobs batch, eventos, etc.).
- **Fuera del ciclo HTTP** (tests, schedulers) establece el tenant manualmente
  con `TenantContext.setTenantId(...)` y recuerda limpiar con `TenantContext.clear()`.

## 3. Filtro de Hibernate
- Marca entidades que pertenecen a un tenant con `@TenantFiltered` y define
  `@FilterDef/@Filter` apuntando a la columna `company_id`.
- `TenantFilterAspect` habilita automáticamente el filtro `tenantFilter` antes
  de cada llamada a repositorios Spring Data JPA. No es necesario habilitarlo a
  mano en servicios comunes.
- Para escenarios especiales (consultas nativas, Batch) usa directamente
  `TenantFilterEnabler`:
  ```java
  filterEnabler.enableTenantFilter(entityManager);
  // ... consulta
  filterEnabler.disableTenantFilter(entityManager);
  ```

## 4. Validación explícita con @ValidateTenant
- Aplica `@ValidateTenant` a métodos de controladores o servicios que reciben un
  identificador de entidad (`@PathVariable`, `UUID productId`, etc.). Ejemplos:
  - `ProductController#update(...)`, `#delete(...)`
  - `PricingService#history(UUID productId, ...)`
- El aspecto `TenantValidationAspect` verifica que la entidad existe y pertenece
  al tenant del contexto. Si está en otro tenant se lanza
  `CrossTenantAccessException` (HTTP 403).
- Parámetros clave:
  - `entityClass`: clase JPA a validar (obligatorio).
  - `entityParam` (por defecto `id`) o `entityParamIndex` para indicar dónde se
    encuentra el identificador cuando el nombre del parámetro cambia
    (`productId`, `entityId`, etc.).
  - Usa `entityParamIndex = 0` cuando quieras evitar depender de que el bytecode
    conserve nombres de parámetros.
- Métodos sin `@ValidateTenant` deben revisar explícitamente la pertenencia o
  mantenerse en rutas de sólo lectura (el filtro de Hibernate sigue aplicando).

## 5. Patrones para repositorios
- **No** incluir `companyId` en firmas de métodos Spring Data. Con el filtro
  activo, `findById`, `findByDeletedAtIsNullAndNameContainingIgnoreCase(...)`
  y equivalentes ya devuelven sólo datos del tenant.
- Mantén los nombres semánticos (`findByDeletedAtIsNull...` sustituyendo
  `findByCompanyIdAndDeletedAtIsNull...`).
- Si necesitas consultar varias entidades del tenant, usa
  `findByIdIn(Collection<UUID> ids)`; el filtro eliminará automáticamente
  registros de otros tenants.
- Únicamente utiliza el `companyId` explícito cuando sea necesario persistir un
  nuevo registro (`entity.setCompanyId(companyContext.require())`).

## 6. Pruebas automatizadas
- Incluye el encabezado `X-Company-Id` en peticiones `MockMvc` para que el
  interceptor cargue el tenant.
- Cuando una prueba mockea repositorios pero invoca un endpoint anotado con
  `@ValidateTenant`, reemplaza la dependencia por un `@MockBean TenantValidationAspect`
  o inserta datos en una base de datos embebida para que el `EntityManager`
  resuelva la entidad.
- Configura las autoridades acordes con las reglas de `@PreAuthorize` del
  controlador. Por ejemplo, para `/products/{id}/inventory-alert` es necesario
  simular `ROLE_SETTINGS` o `ROLE_ADMIN`.

## 7. Checklist al añadir nuevas funcionalidades
1. ¿El endpoint recibe IDs de entidades sensibles? → añade `@ValidateTenant`.
2. ¿El repositorio nuevo filtra por `companyId`? → elimina el parámetro y confía
   en el filtro automático.
3. ¿Se accede al tenant fuera de un request HTTP? → establece y limpia
   `TenantContext` manualmente.
4. ¿Las pruebas cubren el flujo multitenant? → agrega header, rol válido y
   decide entre mockear o persistir datos para la validación del aspecto.

Seguir estas pautas permite mantener el aislamiento entre empresas y reduce el
riesgo de accesos cruzados en nuevas funcionalidades.

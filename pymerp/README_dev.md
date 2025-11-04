# Pruebas rápidas — Backend protegido (Keycloak + Spring Boot)

## Configuración de red

- **Backend**: Se ejecuta en `http://localhost:8081`
- **Frontend**: Se ejecuta en `http://172.29.0.1:5173/app` (configurado para WSL/Docker)
- **Keycloak**: Se ejecuta en `http://localhost:8082`

El backend ya está configurado para aceptar requests CORS desde `http://172.29.0.1:5173`.

## Requisitos de Java (LTS)

- JDK 21 (LTS). El proyecto ya está configurado con Gradle Toolchains para Java 21 (`java.toolchain = 21`), por lo que el propio Gradle puede descargar un JDK compatible automáticamente durante el build.
- Opcional: si prefieres usar tu JDK local, asegúrate de que `JAVA_HOME` apunte a una instalación de Java 21 y que `java -version` muestre 21.x.

No es necesario instalar manualmente otra versión si usas el wrapper (`gradlew.bat` / `./gradlew`), ya que gestionará el toolchain.

Pasos rápidos

1. Copia `.env.sample` a `.env` en la raíz del proyecto y completa valores si es necesario:

   ```powershell
   copy .env.sample .env
   ```

2. Asegúrate de que el backend esté corriendo en `http://localhost:8081` (ejecutado en otra terminal):

   ```powershell
   .\gradlew bootRun --args="--spring.profiles.active=dev"
   ```

3. Desde VS Code puedes usar la extensión REST Client abriendo `requests.http` y ejecutando las peticiones en orden (Get Token -> Health -> Products). El `access_token` será capturado en la variable `access_token`.

4. O usa la Task de VS Code `Test Protected Endpoint` (Ctrl+Shift+P -> Tasks: Run Task -> "Test Protected Endpoint") que ejecuta `scripts/test-protected.ps1`.

Criterios de aceptación

- `GET /actuator/health` → 200 con `{"status":"UP"}` o similar.
- `GET /api/v1/products` → 200 y lista de productos si el token tiene los roles/claims apropiados.
- Si `GET /api/v1/products` responde 401 o 403, el script/report debe imprimir los claims principales del JWT (sub, preferred_username, scope, realm_access.roles, resource_access) y una recomendación precisa de ajuste.

Troubleshooting común

- Puerto incorrecto: asegúrate de que `BACKEND_BASE` en `.env` apunte a `http://localhost:8081`.
- Falta header `X-Company-Id`: este header es obligatorio para multitenancy; el script lo añade desde `COMPANY_ID` en `.env`.
- Mapeo de roles: si recibes 401/403 y en los claims `realm_access.roles` aparece `erp_user` pero la aplicación espera `ROLE_ERP_USER`, ajusta `OidcRoleMapper` para normalizar roles a `ROLE_*` o actualiza `SecurityConfig` para aceptar roles sin prefijo.

Recomendaciones de ajuste (si detectas 401/403)

- Normalizar roles: en `OidcRoleMapper` convertir `realm_access.roles` -> `ROLE_<upper>` y retornar `new SimpleGrantedAuthority("ROLE_...")`.
- O usar scopes: si la app usa scopes, mapear `scope` claim a `SCOPE_...` authorities o ajustar `JwtAuthenticationConverter`.

Reporte automático (qué imprime el script)

- HTTP status y body para `/actuator/health` y `/api/v1/products`.
- Si el endpoint protegido falla (401/403), imprime los claims principales del JWT y una recomendación de ajuste en SecurityConfig/OidcRoleMapper.

Notas finales

- No comitees `.env` con secretos; `.env.sample` contiene placeholders.
- Los scripts son idempotentes y seguros para ejecuciones repetidas en entorno de desarrollo.

## Autorización RBAC (Role-Based Access Control)

### Roles Definidos

El sistema implementa 5 roles con permisos específicos:

| Rol | Descripción | Acceso | Endpoints |
|-----|-------------|--------|-----------|
| **ADMIN** | Administrador completo | Acceso total (100%) | 106/106 endpoints |
| **SETTINGS** | Gestión de catálogos | Lectura + Configuración (71%) | 75/106 endpoints |
| **ERP_USER** | Operaciones diarias | Lectura + Operaciones (90%) | 95/106 endpoints |
| **READONLY** | Solo lectura | Consultas (57%) | 60/106 endpoints |
| **ACTUATOR_ADMIN** | Monitoreo DevOps | Solo actuator | /actuator/** |

### Matriz de Permisos por Operación

#### Endpoints de Catálogo (Customers, Products, Suppliers, etc.)
| Operación | Roles Permitidos | Status |
|-----------|-----------------|--------|
| `GET` | ADMIN, SETTINGS, ERP_USER, READONLY | 200 OK |
| `POST` | ADMIN, SETTINGS | 200/201 |
| `PUT` | ADMIN, SETTINGS | 200 OK |
| `DELETE` | ADMIN | 204 No Content |
| Otros roles | - | **403 Forbidden** |

#### Endpoints Operacionales (Sales, Purchases, Inventory, Billing)
| Operación | Roles Permitidos | Status |
|-----------|-----------------|--------|
| `GET` | ADMIN, SETTINGS, ERP_USER, READONLY | 200 OK |
| `POST` | ADMIN, ERP_USER | 200/201 |
| `PUT` | ADMIN, ERP_USER | 200 OK |
| `DELETE` | ADMIN | 204 No Content |
| Otros roles | - | **403 Forbidden** |

### Ejemplos de Uso

#### Obtener Token con Rol Específico (Keycloak)
```bash
# Token con rol ERP_USER
curl -X POST http://localhost:8082/realms/pymes/protocol/openid-connect/token \
  -d "client_id=pymes-api" \
  -d "client_secret=YOUR_SECRET" \
  -d "grant_type=password" \
  -d "username=user@example.com" \
  -d "password=password"
```

#### Request Exitoso con ERP_USER
```http
GET http://localhost:8081/api/v1/products
Authorization: Bearer <token_with_erp_user_role>
X-Company-Id: 00000000-0000-0000-0000-000000000001
```
**Response**: `200 OK` - Usuario puede listar productos

#### Request Denegado con READONLY
```http
POST http://localhost:8081/api/v1/products
Authorization: Bearer <token_with_readonly_role>
X-Company-Id: 00000000-0000-0000-0000-000000000001
Content-Type: application/json

{"name": "New Product"}
```
**Response**: `403 Forbidden` - READONLY no puede crear productos

#### Request Exitoso con SETTINGS
```http
POST http://localhost:8081/api/v1/products
Authorization: Bearer <token_with_settings_role>
X-Company-Id: 00000000-0000-0000-0000-000000000001
Content-Type: application/json

{"name": "New Product"}
```
**Response**: `200 OK` - SETTINGS puede gestionar catálogos

### Configuración de Roles en Keycloak

1. **Crear Roles en Realm `pymes`**:
   - Ir a: Realm Settings → Roles → Create Role
   - Crear: `ADMIN`, `SETTINGS`, `ERP_USER`, `READONLY`, `ACTUATOR_ADMIN`

2. **Asignar Roles a Usuarios**:
   - Ir a: Users → Select User → Role Mappings
   - En "Realm Roles": asignar roles apropiados (ej: `ERP_USER`)

3. **Configurar Client Scope** (opcional):
   - Ir a: Client Scopes → roles → Mappers
   - Verificar mapper `realm roles` esté activo
   - El claim `realm_access.roles` debe incluir roles asignados

4. **Verificar Token**:
   ```bash
   # Decodificar JWT en https://jwt.io
   # Verificar claim: "realm_access": { "roles": ["ERP_USER", "SETTINGS"] }
   ```

### Códigos de Estado HTTP

- **200/201**: Token válido con rol apropiado + header `X-Company-Id` válido
- **401 Unauthorized**: 
  - No hay token en header `Authorization: Bearer`
  - Token expirado o inválido
  - Firma del token no coincide con clave pública
- **403 Forbidden**:
  - Token válido pero sin rol apropiado para la operación
  - Usuario con rol `READONLY` intenta `POST/PUT/DELETE`
  - Usuario con rol `ERP_USER` intenta eliminar (`DELETE`)
- **400 Bad Request**:
  - Falta header `X-Company-Id`
  - Header `X-Company-Id` no es UUID válido

### Troubleshooting RBAC

#### Problema: 403 Forbidden con token válido
**Causa**: El token no incluye el rol requerido o el claim `realm_access.roles` está vacío.

**Solución**:
1. Verificar roles asignados al usuario en Keycloak
2. Decodificar JWT y verificar `realm_access.roles`
3. Verificar mapper de roles en Client Scope
4. Verificar `OidcRoleMapper` convierte roles a `ROLE_*` prefix

#### Problema: 401 Unauthorized
**Causa**: Token inválido, expirado o clave pública incorrecta.

**Solución**:
1. Verificar `app.security.jwt.oidc-enabled=true` en `application.yml`
2. Verificar `spring.security.oauth2.resourceserver.jwt.issuer-uri` apunta a Keycloak
3. Verificar conectividad a Keycloak (`http://localhost:8082`)
4. Regenerar token (puede estar expirado)

#### Problema: Claims no se mapean correctamente
**Causa**: `JwtAuthenticationConverter` no está configurado o `OidcRoleMapper` no normaliza roles.

**Solución**:
1. Verificar `SecurityConfig.jwtAuthenticationConverter()` está registrado
2. Verificar `OidcRoleMapper.mapRolesFromClaims()` convierte `erp_user` → `ROLE_ERP_USER`
3. Agregar logs en `OidcRoleMapper` para debug:
   ```java
   logger.debug("Mapped roles: {}", authorities);
   ```

### Documentación Adicional

- **RBAC_MATRIX.md**: Matriz completa de permisos (411 líneas)
- **SPRINT_3_TESTS_GUIDE.md**: Guía de tests de autorización (287 líneas)
- **TROUBLESHOOTING_RBAC.md**: Casos comunes y soluciones

---

## Autorización Efectiva (Legacy)

Matriz rápida:

- **200**: token válido que incluye `realm_access.roles` con roles apropiados (ej: `erp_user` mapeado a `ROLE_ERP_USER`) y header `X-Company-Id` válido.
- **403**: token válido pero sin roles requeridos para la operación.
- **400**: falta `X-Company-Id` o no es UUID válido.
- **401**: no hay token o el token es inválido/expirado.

Ejemplo (requests.http extra):

### Products con scope explícito (si el token no trae roles)
GET {{BACKEND_BASE}}/api/v1/products
Authorization: Bearer {{access_token}}
X-Company-Id: 00000000-0000-0000-0000-000000000001


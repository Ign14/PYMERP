# Troubleshooting RBAC - Guía de Resolución de Problemas

## 🚨 Códigos de Error HTTP

### 401 Unauthorized

**Significado**: No hay autenticación válida o el token ha expirado.

#### Causas Comunes

1. **Token no proporcionado**
   ```http
   GET /api/v1/products
   X-Company-Id: 00000000-0000-0000-0000-000000000001
   ```
   ❌ Falta header `Authorization: Bearer <token>`

2. **Token expirado**
   ```json
   {
     "exp": 1704067200,  // Timestamp de expiración
     "iat": 1704063600   // Emitido hace más de 1 hora
   }
   ```
   ❌ Token expiró (`exp` < tiempo actual)

3. **Token con firma inválida**
   ```
   JWT signature does not match locally computed signature
   ```
   ❌ Clave pública en backend no coincide con clave privada de Keycloak

4. **Issuer incorrecto**
   ```json
   {
     "iss": "http://localhost:8082/realms/wrong-realm"
   }
   ```
   ❌ `spring.security.oauth2.resourceserver.jwt.issuer-uri` esperaba `pymes`

#### Soluciones

✅ **Verificar configuración de Keycloak**:
```yaml
# application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8082/realms/pymes  # Debe coincidir con issuer en JWT
          jwk-set-uri: http://localhost:8082/realms/pymes/protocol/openid-connect/certs
```

✅ **Regenerar token**:
```bash
curl -X POST http://localhost:8082/realms/pymes/protocol/openid-connect/token \
  -d "client_id=pymes-api" \
  -d "client_secret=YOUR_SECRET" \
  -d "grant_type=password" \
  -d "username=user@example.com" \
  -d "password=password"
```

✅ **Verificar conectividad a Keycloak**:
```bash
curl http://localhost:8082/realms/pymes/.well-known/openid-configuration
```
Debe retornar JSON con `issuer`, `authorization_endpoint`, `jwks_uri`.

✅ **Decodificar token para debug**:
- Ir a https://jwt.io
- Pegar token en "Encoded"
- Verificar claims: `iss`, `exp`, `aud`, `realm_access.roles`

---

### 403 Forbidden

**Significado**: Token válido pero el usuario no tiene permisos para esta operación.

#### Causas Comunes

1. **Rol insuficiente**
   ```http
   POST /api/v1/products
   Authorization: Bearer <token_with_READONLY_role>
   ```
   ❌ READONLY no puede crear productos (requiere ADMIN o SETTINGS)

2. **Claim `realm_access.roles` vacío**
   ```json
   {
     "sub": "user123",
     "realm_access": {
       "roles": []  // No hay roles asignados
     }
   }
   ```
   ❌ Usuario sin roles en Keycloak

3. **Roles sin prefijo `ROLE_`**
   ```json
   {
     "realm_access": {
       "roles": ["erp_user"]  // Minúscula, sin prefijo
     }
   }
   ```
   ⚠️ `@PreAuthorize("hasRole('ERP_USER')")` espera `ROLE_ERP_USER`

4. **DELETE sin rol ADMIN**
   ```http
   DELETE /api/v1/products/123
   Authorization: Bearer <token_with_SETTINGS_role>
   ```
   ❌ DELETE solo permitido para ADMIN

#### Soluciones

✅ **Verificar roles del usuario en Keycloak**:
1. Ir a: Keycloak Admin → Users → Select User
2. Tab "Role Mappings" → "Realm Roles"
3. Verificar que tenga roles asignados (ej: `ADMIN`, `ERP_USER`)

✅ **Asignar roles apropiados**:
```
Usuario operativo → Rol: ERP_USER
Usuario de configuración → Rol: SETTINGS
Usuario administrador → Roles: ADMIN, ERP_USER, SETTINGS
Usuario de consulta → Rol: READONLY
```

✅ **Verificar mapper de roles en Client Scope**:
1. Ir a: Client Scopes → `roles` → Mappers
2. Verificar mapper `realm roles` esté activo
3. Token Claim Name debe ser: `realm_access.roles`

✅ **Verificar `OidcRoleMapper` normaliza roles**:
```java
// En OidcRoleMapper.java
List<GrantedAuthority> authorities = roles.stream()
  .map(role -> role.toUpperCase()) // erp_user → ERP_USER
  .map(role -> "ROLE_" + role)     // ERP_USER → ROLE_ERP_USER
  .map(SimpleGrantedAuthority::new)
  .collect(Collectors.toList());
```

✅ **Tabla de permisos requeridos**:
| Operación | Endpoint Ejemplo | Roles Permitidos |
|-----------|------------------|------------------|
| GET | `/api/v1/products` | Todos (ADMIN, SETTINGS, ERP_USER, READONLY) |
| POST catalog | `/api/v1/products` | ADMIN, SETTINGS |
| POST operational | `/api/v1/sales` | ADMIN, ERP_USER |
| PUT catalog | `/api/v1/products/{id}` | ADMIN, SETTINGS |
| PUT operational | `/api/v1/sales/{id}` | ADMIN, ERP_USER |
| DELETE | `/api/v1/products/{id}` | ADMIN |

---

### 400 Bad Request

**Significado**: Request malformado o faltan headers requeridos.

#### Causas Comunes

1. **Falta header `X-Company-Id`**
   ```http
   GET /api/v1/products
   Authorization: Bearer <valid_token>
   ```
   ❌ Header `X-Company-Id` es obligatorio para multi-tenancy

2. **`X-Company-Id` no es UUID válido**
   ```http
   GET /api/v1/products
   Authorization: Bearer <valid_token>
   X-Company-Id: invalid-uuid
   ```
   ❌ Debe ser formato UUID: `00000000-0000-0000-0000-000000000001`

3. **Body JSON inválido**
   ```http
   POST /api/v1/products
   Content-Type: application/json

   {"name": "Product  // JSON incompleto
   ```
   ❌ JSON malformado

#### Soluciones

✅ **Siempre incluir `X-Company-Id`**:
```http
GET /api/v1/products
Authorization: Bearer <token>
X-Company-Id: 00000000-0000-0000-0000-000000000001
```

✅ **Usar UUID válido**:
```bash
# Generar UUID en Linux/Mac
uuidgen

# En PowerShell
[guid]::NewGuid()
```

✅ **Validar JSON con herramientas**:
- Visual Studio Code: Formatear documento (Alt+Shift+F)
- jsonlint.com
- Postman: Tab "Body" → raw → JSON

---

## 🔧 Problemas de Configuración

### Problema: `JwtAuthenticationConverter` no registrado

**Síntoma**:
```
org.springframework.security.access.AccessDeniedException: Access is denied
```
A pesar de tener token válido con roles correctos.

**Causa**: `SecurityConfig.jwtAuthenticationConverter()` no está configurado en `.oauth2ResourceServer()`.

**Solución**:
```java
// SecurityConfig.java
http.oauth2ResourceServer(oauth2 -> 
  oauth2.jwt(jwt -> 
    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
  )
);

private JwtAuthenticationConverter jwtAuthenticationConverter() {
  JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
  converter.setJwtGrantedAuthoritiesConverter(jwt -> {
    Map<String, Object> claims = jwt.getClaims();
    return OidcRoleMapper.mapRolesFromClaims(claims);
  });
  return converter;
}
```

---

### Problema: Roles no se mapean desde Keycloak

**Síntoma**: Token tiene `realm_access.roles: ["erp_user"]` pero `@PreAuthorize` falla.

**Causa**: `OidcRoleMapper` no convierte roles a formato `ROLE_*` uppercase.

**Solución**:
```java
// OidcRoleMapper.java
public static List<GrantedAuthority> mapRolesFromClaims(Map<String, Object> claims) {
  Object realmAccess = claims.get("realm_access");
  if (!(realmAccess instanceof Map)) {
    return List.of();
  }
  
  Object rolesObj = ((Map<?, ?>) realmAccess).get("roles");
  if (!(rolesObj instanceof List)) {
    return List.of();
  }
  
  List<String> roles = (List<String>) rolesObj;
  return roles.stream()
    .map(String::toUpperCase)        // erp_user → ERP_USER
    .map(role -> "ROLE_" + role)     // ERP_USER → ROLE_ERP_USER
    .map(SimpleGrantedAuthority::new)
    .collect(Collectors.toList());
}
```

---

### Problema: Multi-tenancy no valida Company

**Síntoma**: Puedo acceder a datos de otra compañía cambiando `X-Company-Id`.

**Causa**: `CompanyContext.require()` no valida que el usuario pertenece a la compañía.

**Solución**:
```java
// En cada controller
UUID companyId = companyContext.require();
// Validar que el usuario autenticado pertenece a esta compañía
User user = getCurrentUser();
if (!user.getCompanyId().equals(companyId)) {
  throw new AccessDeniedException("User does not belong to this company");
}
```

---

## 🧪 Testing

### Ejecutar tests de autorización

```bash
cd backend
.\gradlew.bat test --tests "*AuthTest"
```

**Output esperado**:
```
CustomerControllerAuthTest > testCreateCustomer_ReadonlyRole_Forbidden() PASSED
CustomerControllerAuthTest > testDeleteCustomer_SettingsRole_Forbidden() PASSED
...
47 tests, 47 passed
```

### Debug de tests fallidos

**Test falla con 200 en lugar de 403**:
```java
@Test
@WithMockUser(roles = "READONLY")
void testCreate_ShouldForbid() {
  mockMvc.perform(post("/api/v1/products")...)
    .andExpect(status().isForbidden()); // Falla: actual 200
}
```

**Causa**: `@PreAuthorize` no está aplicado o está mal configurado.

**Solución**:
1. Verificar `@PreAuthorize("hasAnyRole('ADMIN', 'SETTINGS')")` en método del controller
2. Verificar `@EnableMethodSecurity(prePostEnabled = true)` en `SecurityConfig`
3. Agregar logs:
   ```java
   @PreAuthorize("hasAnyRole('ADMIN', 'SETTINGS')")
   public Product createProduct(@RequestBody Product product) {
     logger.debug("Creating product with user roles: {}", 
       SecurityContextHolder.getContext().getAuthentication().getAuthorities());
     // ...
   }
   ```

---

## 📊 Herramientas de Debug

### 1. Logs de Spring Security

Habilitar logs detallados:
```yaml
# application-dev.yml
logging:
  level:
    org.springframework.security: DEBUG
    com.datakomerz.pymes.security: DEBUG
```

**Output útil**:
```
DEBUG o.s.s.a.i.a.MethodSecurityInterceptor : Authorizing method invocation
DEBUG o.s.s.access.vote.AffirmativeBased   : Voter: RoleVoter, returned: 0
DEBUG o.s.s.access.vote.AffirmativeBased   : Voter: AuthenticatedVoter, returned: 1
```

### 2. Endpoint de actuator (solo desarrollo)

```yaml
# application-dev.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env
```

**Consultar configuración**:
```bash
curl -H "Authorization: Bearer <admin_token>" \
  http://localhost:8081/actuator/env | jq '.propertySources[] | select(.name == "security")'
```

### 3. JWT Debugger

Decodificar token JWT:
```bash
# Extraer payload del JWT
echo "<token>" | cut -d'.' -f2 | base64 -d | jq
```

**Output esperado**:
```json
{
  "sub": "user@example.com",
  "realm_access": {
    "roles": ["ERP_USER", "SETTINGS"]
  },
  "exp": 1704067200,
  "iat": 1704063600,
  "iss": "http://localhost:8082/realms/pymes"
}
```

---

## 🎯 Checklist de Verificación

### Backend (Spring Boot)

- [ ] `@EnableMethodSecurity(prePostEnabled = true)` en `SecurityConfig`
- [ ] `jwtAuthenticationConverter()` registrado en `.oauth2ResourceServer()`
- [ ] `OidcRoleMapper` convierte roles a `ROLE_*` uppercase
- [ ] Todos los endpoints tienen `@PreAuthorize` o están en `.permitAll()`
- [ ] `application.yml` tiene `spring.security.oauth2.resourceserver.jwt.issuer-uri`
- [ ] Logs de seguridad habilitados en desarrollo

### Keycloak

- [ ] Realm `pymes` creado
- [ ] Client `pymes-api` configurado
- [ ] Roles `ADMIN`, `SETTINGS`, `ERP_USER`, `READONLY`, `ACTUATOR_ADMIN` creados
- [ ] Usuarios tienen roles asignados en "Role Mappings"
- [ ] Client Scope `roles` incluye mapper `realm roles`
- [ ] Token generado incluye claim `realm_access.roles`

### Requests HTTP

- [ ] Header `Authorization: Bearer <token>` presente
- [ ] Header `X-Company-Id: <uuid>` presente (multi-tenancy)
- [ ] Token no expirado (verificar claim `exp`)
- [ ] Rol apropiado para la operación (ver matriz de permisos)

---

## 📚 Referencias

- **RBAC_MATRIX.md**: Matriz completa de permisos (106 endpoints × 5 roles)
- **README_dev.md**: Configuración de Keycloak y ejemplos de requests
- **SPRINT_3_TESTS_GUIDE.md**: Guía de tests de autorización
- **Spring Security Docs**: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- **Keycloak Docs**: https://www.keycloak.org/docs/latest/server_admin/

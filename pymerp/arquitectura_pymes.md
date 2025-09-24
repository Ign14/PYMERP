# Arquitectura de Software - Sistema de Gestion PyMEs

## 1. Vision General
- Monolito modular en Spring Boot 3 (Java 21) con capas: config, core (tenancy, excepciones), dominio (companies, products, suppliers, purchases, sales, inventory).
- Frontend web en React 18 + Vite + React Query.
- Postgres como base relacional, Flyway para versionado, Redis disponible para cache/eventos.
- MinIO pensado para archivos y S3 compatible (no usado aun en codigo).

## 2. Multitenencia y Seguridad
- Tenancy por `company_id` (UUID). El filtro `CompanyContextFilter` resuelve el tenant desde header `X-Company-Id` o desde la propiedad `app.tenancy.default-company-id`.
- Seguridad basada en JWT + refresh tokens; sesi�n autom�tica en UI, proveedor dev `admin@dev.local / Admin1234`, claims con `companyId`, `roles` y soporte a empaquetado desktop.
- Endpoint `/api/v1/auth/refresh` exige refresh token en cabecera `X-Refresh-Token` (o cookie `refresh_token`) y valida estado del usuario y `company_id` antes de emitir nuevos tokens.
- Manejo centralizado de errores con respuestas RFC 7807 (`ProblemDetail`).

## 3. API REST v1
- Prefijo: `/api/v1`.
- Controladores expuestos: `CompanyController`, `ProductController`, `SupplierController`, `PurchaseController`, `SalesController`, `HealthController`.
- Validacion con `jakarta.validation` en DTOs, mapeos a entidades en servicios.

## 4. Dominio principal (actual)
- **Companies**: CRUD basico; guarda datos de negocio (rut, horarios, logos).
- **Products**: Catalogo, soft delete reservado via campo `deleted_at` (no expuesto aun). Consultas paginadas por nombre/sku/barcode.
- **Suppliers**: Proveedores y contactos, vinculados al tenant.
- **Customers**: Clientes con datos de contacto y soporte para georreferenciacion.
- **Pricing**: Historial de precios por producto y API para fijar nuevos valores.
- **Purchases**: Registra compras y genera lotes + movimientos de inventario.
- **Inventory**: Lotes y movimientos, consumo FIFO durante ventas.
- **Sales**: Crea ventas, calcula totales y descuenta stock.

## 5. Sincronizacion y estados
- Campos `created_at`, `updated_at`, `version` en tablas criticas permiten historicos y sincronizacion futura.
- Operaciones criticas (`PurchaseService`, `SalesService`, `InventoryService`) declaradas transaccionales.

## 6. Frontend
- React + TypeScript con Axios y React Query; el contexto de autenticacion gestiona el JWT y aplica los headers Authorization y X-Company-Id.
- React Query para cache/estado de peticiones (health, companies).
- CustomersCard usa scroll infinito paginado (GET /v1/customers?page=&size=&sort=createdAt,desc) evitando duplicados y solicitando nuevos lotes mientras hasNext sea verdadero.
- Componentes base: layout LayoutShell, PageHeader, cards reutilizables (HealthCard, CompaniesCard, ProductsCard, SuppliersCard, CustomersCard), paginas dedicadas (Ventas, Compras, Inventario, Finanzas, Reportes, Configuracion).
- Variables de entorno (.env.*) definen la base de la API; las credenciales y compania se resuelven tras autenticacion.

## 7. Roadmap sugerido
1. Integrar proveedor de identidad centralizado (Keycloak/Auth0) y gestion avanzada de usuarios.
2. CRUD de clientes y listas de precios.
3. Reportes y paneles (ventas, inventario, compras).
4. Integracion con MinIO (archivos) y envio de comprobantes.
5. Mecanismos offline / sync (app Flutter).

## 8. Integración OIDC (Keycloak / Auth0) — guía rápida

Esta sección describe cómo integrar un proveedor OIDC (por ejemplo Keycloak o Auth0) como Identity Provider centralizado para PyMEs.

Objetivo
- Permitir que en entornos de producción la API valide tokens emitidos por un IdP (OIDC). En desarrollo la aplicación mantiene el flujo local de JWT/refresh tokens por defecto.

Requisitos
- Tener un servidor OIDC disponible (Keycloak en contenedor, Auth0 tenant, etc.).
- Configurar un cliente (application) en el IdP cuyo `client_id` y `jwk-set-uri` o `issuer` estén accesibles.

Resumen de la solución en la aplicación
- El backend soporta dos modos (toggle):
	- Modo local (por defecto): uso de la implementación interna de JWT + refresh tokens (dev friendly).
	- Modo OIDC: activar `app.security.jwt.oidc-enabled=true` y proveer la URL de JWK/Issuer para que Spring Security valide los JWT del proveedor.

Archivos relevantes
- `backend/src/main/resources/application-dev.properties` — propiedad toggle: `app.security.jwt.oidc-enabled` y ejemplo `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`.
- `backend/src/main/java/com/datakomerz/pymes/config/SecurityConfig.java` — configuración condicional que habilita `oauth2ResourceServer().jwt()` cuando `app.security.jwt.oidc-enabled=true`.
- `backend/src/main/java/com/datakomerz/pymes/config/AppProperties.java` — binder que contiene `oidcEnabled` (boolean).

Configuración recomendada en Keycloak (pasos resumidos)
1. Crear un Realm (si no existe) y un Client, por ejemplo `pymerp-backend`.
2. Tipo de cliente: `confidential` (si la API necesita usar client secret para token introspection) o `public` si sólo se usa redirección desde frontend.
3. En Settings del client: establecer `Valid Redirect URIs` para la UI, y guardar `Client ID` / `Client secret` si aplica.
4. Crear Roles (realm roles) y asignarlos a usuarios o groups.
5. Crear un Mapper (Client Scope) para exponer roles en el claim `realm_access.roles` o un claim personalizado `roles` (según preferencia).

Ejemplo de `application-dev.properties` (fragmento)

```properties
# Activar el modo OIDC (false = modo local)
app.security.jwt.oidc-enabled=true

# URL JWKS del proveedor OIDC (p. ej. Keycloak):
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://<keycloak-host>/auth/realms/<realm>/protocol/openid-connect/certs

# (Opcional) si usas spring OAuth2 client para flows en el frontend:
spring.security.oauth2.client.registration.keycloak.client-id=pymerp-frontend
spring.security.oauth2.client.registration.keycloak.client-secret=<secret>
spring.security.oauth2.client.provider.keycloak.issuer-uri=https://<keycloak-host>/auth/realms/<realm>
```

Mapeo de claims -> roles
- Spring Security no asume de forma automática dónde están tus roles en el JWT. En Keycloak los roles suelen llegar dentro de `realm_access.roles` o `resource_access.<client>.roles`.
- Implementación recomendada (en `SecurityConfig`): crear un `JwtAuthenticationConverter` que extraiga los roles desde `realm_access.roles` o desde el claim elegido y los transforme a GrantedAuthorities con prefijo `ROLE_`.

Ejemplo (snippet Java para documentación)

```java
// Ejemplo ilustrativo - ya existe lógica condicional en SecurityConfig.java
JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
converter.setJwtGrantedAuthoritiesConverter(jwt -> {
	Collection<GrantedAuthority> authorities = new ArrayList<>();
	Object realmAccess = jwt.getClaim("realm_access");
	if (realmAccess instanceof Map) {
		Object roles = ((Map) realmAccess).get("roles");
		if (roles instanceof Collection) {
			for (Object r : (Collection) roles) {
				authorities.add(new SimpleGrantedAuthority("ROLE_" + r.toString().toUpperCase()));
			}
		}
	}
	return authorities;
});

http.oauth2ResourceServer().jwt().jwtAuthenticationConverter(converter);
```

Pruebas y verificación
1. Obtener un access token válido del IdP (Keycloak Admin Console → Users → Login as user, o usar el endpoint `/protocol/openid-connect/token` con password grant / client credentials según la configuración).
2. Llamar a la API protegida incluyendo el header `Authorization: Bearer <ACCESS_TOKEN>` y `X-Company-Id: <UUID-compania>` (el backend sigue exigiendo `company_id` vía header para tenancy).

Ejemplo curl (usar token real):

```bash
curl -X GET "http://localhost:8081/api/v1/products" \
	-H "Authorization: Bearer <ACCESS_TOKEN>" \
	-H "X-Company-Id: 00000000-0000-0000-0000-000000000001"
```

Fallback y desarrollo
- Si `app.security.jwt.oidc-enabled=false`, la aplicación mantiene el flujo interno de autenticación (endpoints `/api/v1/auth/**`) y el dev user `admin@dev.local / Admin1234` sigue disponible.
- Recomendación: activar OIDC en entornos staging/producción y mantener el modo local sólo en `dev` para facilitar pruebas.

Consideraciones adicionales
- Si el IdP devuelve `email_verified` y `companyId` en claims, la aplicación puede mapearlos para validar tenancy. De lo contrario el frontend debe enviar `X-Company-Id` en cada petición.
- Para SSO en la UI + backend: configurar un client OIDC para la UI y usar el flujo Authorization Code + PKCE; el backend validará el JWT en cada petición.

Siguientes pasos sugeridos
1. Probar manualmente: crear un realm Keycloak local y un client `pymerp-backend`, habilitar `app.security.jwt.oidc-enabled=true` y probar con un token.
2. Implementar tests e2e que obtengan tokens de un Keycloak de pruebas y validen rutas protegidas.
3. Documentar en detalle el mapeo de roles para cada proveedor (Keycloak / Auth0) si se requiere comportamiento avanzado.











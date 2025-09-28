# Arquitectura de Software - Sistema de Gestión PyMEs

## 1. Resumen del monorepo
- Repositorio unificado con tres clientes principales:
  - **Backend** en Spring Boot 3 / Java 21 dentro de `backend/` (Gradle, Checkstyle y Spotless).
  - **Frontend web** en React 18 + Vite + TypeScript en `ui/` con React Query, React Router y Tailwind opcional.
  - **Cliente Flutter** experimental (`app_flutter/`) orientado a escenarios offline/web.
- Packaging adicional mediante Tauri en `desktop/` para distribuir la UI como app de escritorio (ver `docs/windows-desktop.md`).
- Scripts de la raíz (`package.json`, `scripts/run-ui.js`) permiten ejecutar la UI desde el workspace npm; Makefiles simplifican tareas recurrentes.
- `docker-compose.yml` orquesta Postgres, Redis, backend, frontend, MinIO y Mailhog para un entorno local completo.

## 2. Backend (Spring Boot)
### 2.1 Capas y configuración
- Proyecto Java 21 con Gradle y Spring Boot 3.3.3, dependencias para web, seguridad, OAuth2/OIDC, JPA, Redis, mail, Flyway, OpenAPI y SDK S3. 【F:pymerp/backend/build.gradle†L1-L74】
- Configuración tipada vía `AppProperties` expone CORS, multitenencia (`app.tenancy.default-company-id`), JWT/refresh tokens, captcha y banderas OIDC. 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/config/AppProperties.java†L9-L103】
- `application-dev.properties` conecta con Postgres y Redis, habilita Actuator, define secreto JWT, captcha simple y parámetros OIDC opcionales. 【F:pymerp/backend/src/main/resources/application-dev.properties†L1-L40】

### 2.2 Multitenencia
- `CompanyContextFilter` exige el header `X-Company-Id` en toda ruta `/api/**`, valida UUID y almacena el tenant en un contexto `ThreadLocal`, liberándolo al terminar la petición. 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/core/tenancy/CompanyContextFilter.java†L1-L65】
- Repositorios y servicios reciben el `companyId` desde `CompanyContext.require()` para aislar datos por inquilino.

### 2.3 Seguridad
- Seguridad por defecto con JWT internos y refresh tokens (stateless) usando `JwtAuthenticationFilter` y un `DaoAuthenticationProvider`. 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/config/SecurityConfig.java†L39-L79】
- Alternativa OIDC: al habilitar `app.security.jwt.oidc-enabled`, se activa `oauth2ResourceServer().jwt()` y se mapean roles/alcances con `OidcRoleMapper` para admitir `realm_access.roles`, `resource_access` y `scope`. 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/config/SecurityConfig.java†L48-L75】【F:pymerp/backend/src/main/java/com/datakomerz/pymes/security/OidcRoleMapper.java†L1-L67】
- CORS configurable, sesiones deshabilitadas, endpoints públicos limitados a `/actuator/**`, `/api/v1/auth/**` y `/api/v1/requests/**`.
- Flujo de solicitud de cuenta protegido con captcha aritmético (`SimpleCaptchaValidationService`) y notificación por correo (`AccountRequestNotifier`). 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/requests/application/AccountRequestService.java†L18-L62】【F:pymerp/backend/src/main/java/com/datakomerz/pymes/requests/application/AccountRequestNotifier.java†L1-L55】

### 2.4 Persistencia e infraestructura
- Postgres 16 como base de datos principal; migraciones Flyway (`backend/src/main/resources/db`).
- Redis 7 disponible para cache/eventos y deshabilitado en health local cuando no está presente. 【F:pymerp/backend/src/main/resources/application-dev.properties†L13-L25】
- Servicio de archivos `StorageService` con implementación local (`LocalStorageService`) para imágenes y QR de productos, configurable vía `app.storage.*`. 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/storage/LocalStorageService.java†L1-L108】【F:pymerp/backend/src/main/java/com/datakomerz/pymes/storage/StorageProperties.java†L1-L33】
- SDK AWS S3 incluido para futura integración con MinIO/S3.
- Actuator expone health/metrics; ZXing genera códigos QR.

### 2.5 Dominios implementados
- **Autenticación (`auth`)**: login, refresh, emisión de módulos habilitados y expiraciones; soporte para proveedor dev.
- **Compañías (`company`)**: CRUD extendido con campos de razón social, contacto y pie de boleta; migraciones Flyway aplican nuevas columnas.【F:pymerp/README.md†L24-L33】
- **Productos (`products`)**: búsqueda por nombre/SKU/código de barras, creación/edición multipart con validaciones de imagen (PNG/JPEG/WebP ≤1 MB), activación/desactivación, alertas de inventario, generación/descarga de QR y consulta de lotes. 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/products/ProductController.java†L40-L170】
- **Pricing (`pricing`)**: historial de precios por producto, registro de cambios y consulta del último precio vigente. 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/pricing/PricingService.java†L19-L70】
- **Inventario (`inventory`)**: lotes FIFO, movimientos, alertas de stock, ajustes manuales (incremento/decremento), configuración de umbral y resumen de valor/alertas/productos. 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/inventory/InventoryController.java†L1-L53】【F:pymerp/backend/src/main/java/com/datakomerz/pymes/inventory/InventoryService.java†L19-L128】
- **Ventas (`sales`)**: creación/actualización/cancelación, métricas diarias, detalle completo y listado paginado con filtros por estado, documento, método de pago y rango de fechas; integra consumo/reverso de stock. 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/sales/api/SalesController.java†L1-L97】【F:pymerp/backend/src/main/java/com/datakomerz/pymes/inventory/InventoryService.java†L30-L83】
- **Compras (`purchases`)**: alta/edición/cancelación, listado paginado con filtros y métricas diarias para paneles analíticos. 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/purchases/PurchaseController.java†L1-L77】
- **Clientes (`customers`)**: CRUD completo, segmentación, conteo por segmentos y paginación con filtros por texto/segmento. 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/customers/api/CustomerController.java†L1-L86】
- **Proveedores (`suppliers`)**: gestión de proveedor y contactos asociados al tenant. 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/suppliers/SupplierController.java†L1-L70】
- **Solicitudes de cuenta (`requests`)**: endpoint público `/api/v1/requests` que persiste solicitudes, valida RUT, confirma contraseñas y envía notificaciones. 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/requests/api/AccountRequestController.java†L1-L26】【F:pymerp/backend/src/main/java/com/datakomerz/pymes/requests/application/AccountRequestService.java†L33-L62】

### 2.6 Calidad y pruebas
- `./gradlew test` ejecuta pruebas JUnit5 con soporte de Spring Security; Checkstyle/Spotless aplican reglas sobre capas de aplicación. 【F:pymerp/backend/build.gradle†L76-L101】
- `./gradlew flywayMigrate` disponible para preparar la base de datos antes de ejecutar servicios. 【F:pymerp/README.md†L24-L31】

## 3. API REST v1 (principales rutas)
- `/api/v1/auth/**`: autenticación (login, refresh). 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/auth/api/AuthController.java†L16-L46】
- `/api/v1/companies`: mantenimiento de compañías. 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/company/CompanyController.java†L21-L51】
- `/api/v1/products`: catálogo, imágenes, QR, stock y ajustes de estado/inventario. 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/products/ProductController.java†L40-L170】
- `/api/v1/pricing`: historial de precios y registro de cambios. 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/pricing/PricingController.java†L21-L49】
- `/api/v1/inventory`: alertas, resumen, configuración y ajustes manuales. 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/inventory/InventoryController.java†L1-L53】
- `/api/v1/sales`: CRUD, métricas y detalle de ventas. 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/sales/api/SalesController.java†L1-L97】
- `/api/v1/purchases`: gestión y métricas de compras. 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/purchases/PurchaseController.java†L1-L77】
- `/api/v1/customers`: CRUD, segmentos y resumen de clientes. 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/customers/api/CustomerController.java†L33-L86】
- `/api/v1/suppliers`: proveedores y contactos. 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/suppliers/SupplierController.java†L19-L61】
- `/api/v1/requests`: solicitudes de cuenta + captcha (público). 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/requests/api/AccountRequestController.java†L18-L26】
- `/api/v1/health`: health check extendido. 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/api/HealthController.java†L1-L10】

## 4. Frontend web (Vite + React)
- `App.tsx` define un shell protegido con React Router, LayoutShell y navegación por módulos (`dashboard`, `sales`, `purchases`, `inventory`, `customers`, `suppliers`, `finances`, `reports`, `settings`). 【F:pymerp/ui/src/App.tsx†L6-L64】
- `AuthContext` persiste la sesión JWT/refresh en `localStorage`, programa refrescos automáticos, limpia sesión expirada y propaga `modules` para el control de accesos por vista. 【F:pymerp/ui/src/context/AuthContext.tsx†L1-L96】
- `services/client.ts` centraliza Axios, añade encabezados `Authorization` y `X-Company-Id`, y ofrece **modo offline**: ante fallos de red genera datos demo para compañías, inventario, ventas/compras diarias y documentos. 【F:pymerp/ui/src/services/client.ts†L1-L111】【F:pymerp/ui/src/services/client.ts†L520-L611】
- La experiencia de landing combina login y solicitud de cuenta con captcha local, validación de RUT y manejo de cool-down por múltiples fallos. 【F:pymerp/ui/src/pages/LandingExperience.tsx†L1-L120】
- React Query maneja cache de peticiones, loaders y mutaciones (login, account request, módulos). Las pruebas usan Vitest + Testing Library (`npm run test`). 【F:pymerp/ui/package.json†L6-L34】

## 5. Clientes adicionales
- **Flutter (`app_flutter/`)**: consume endpoints de clientes (`/v1/customers`) con paginación infinita, formularios con lat/lng opcional, botón "Usar ubicación actual", cabeceras `Authorization`/`X-Company-Id` y pruebas de serialización/widgets. Falta sincronización offline y edición avanzada. 【F:pymerp/app_flutter/README.md†L1-L37】
- **Desktop (`desktop/`)**: base Tauri para empaquetar la UI, documentado en `docs/windows-desktop.md`.

## 6. Infraestructura local
- `docker-compose.yml` levanta backend Gradle, Postgres 16, Redis 7, frontend Vite, MinIO y Mailhog; variables de entorno inyectan credenciales demo y compañía por defecto. 【F:pymerp/docker-compose.yml†L1-L79】
- Scripts recomendados: `docker compose up --build`, `cd backend && ./gradlew bootRun --args='--spring.profiles.active=dev'`, `npm run dev` para frontend o `make run-web` en Flutter. 【F:pymerp/README.md†L15-L23】【F:pymerp/app_flutter/README.md†L1-L17】

## 7. Roadmap sugerido (actualizado)
1. Completar integración con MinIO/S3 para servir imágenes y documentos desde almacenamiento objeto (hoy opera modo local).
2. Automatizar pipelines de pruebas (Gradle + Vitest) y quality gates (Checkstyle/Spotless) en CI.
3. Expandir el motor offline (web y Flutter) con sincronización bidireccional y colas de reintento.
4. Implementar reportes avanzados (ventas, compras, inventario) con filtros dinámicos y descargas.
5. Incorporar gestión avanzada de usuarios/roles vía IdP OIDC y panel administrativo.

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
- `backend/src/main/java/com/datakomerz/pymes/config/AppProperties.java` — binder que contiene `oidcEnabled` (boolean) y opciones de captcha.
- `backend/src/main/java/com/datakomerz/pymes/security/OidcRoleMapper.java` — normaliza claims de roles (`realm_access.roles`, `resource_access`, `roles`) y scopes (`scope`).

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
- Implementación recomendada: `OidcRoleMapper.mapRolesFromClaims` extrae roles/alcances y los transforma a GrantedAuthorities con prefijo `ROLE_` o `SCOPE_`. 【F:pymerp/backend/src/main/java/com/datakomerz/pymes/security/OidcRoleMapper.java†L1-L67】

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

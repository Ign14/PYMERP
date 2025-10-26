# Arquitectura de Software - Sistema de Gestión PyMEs

## 1. Visión general
- Monorepo centrado en PyMEs que agrupa backend, frontend web, cliente Flutter y empaquetado desktop en un único workspace.
- Arquitectura multicliente con API Spring Boot 3 que expone dominios de ventas, compras, inventario, pricing, clientes y proveedores.
- Capas de presentación basadas en React 18 (Vite) y Flutter con capacidades offline y reutilización del mismo backend.
- Infraestructura local reproducible con `docker compose` (Postgres, Redis, MinIO, Mailhog) y scripts Gradle/NPM para desarrollo y QA.

## 2. Estructura del monorepo
| Ruta | Descripción |
| --- | --- |
| `backend/` | API REST en Spring Boot 3 (Java 21, Gradle) con módulos DDD, seguridad JWT/OIDC, migraciones Flyway y pruebas automatizadas. |
| `ui/` | Frontend web en Vite + React 18 + TypeScript con React Query, React Router y modo offline controlado. |
| `app_flutter/` | Cliente Flutter orientado a escenarios móviles/offline con autenticación JWT y sincronización progresiva. |
| `desktop/` | Proyecto Tauri que empaqueta la build de `ui/` como instalador de escritorio Windows. |
| `docs/` | Documentación complementaria (por ejemplo `docs/windows-desktop.md`). |
| `scripts/`, `package.json`, `Makefile` | Scripts Node y targets Make para orquestar tareas (build, lint, ejecutar clientes). |
| `docker-compose.yml` | Stack local con Postgres 16, Redis 7, backend, frontend, MinIO y Mailhog. |

## 3. Vista lógica de alto nivel
```text
                    Usuarios
                       |
        +--------------+-----------------------------+
        |                                            |
 React 18 (ui/) & Tauri (desktop/)          Flutter (app_flutter/)
        |                                            |
        +---------------------+----------------------+
                              |
                 Spring Boot API (backend/)
                              |
     +------------+-----------+---------------------+
     |            |                                 |
Postgres 16   Redis 7                      Almacenamiento local
(Flyway, JPA) (cache y locks)      (imágenes, QR) → MinIO/S3*
                              |
                        Servicios auxiliares
             (Mailhog SMTP, captcha, métricas Actuator)
                              |
                 IdP OIDC opcional (Keycloak/Auth0)
```
\* MinIO/S3 está disponible como futuro backend de objetos; hoy se usa `LocalStorageService`.

## 4. Backend (Spring Boot)

### 4.1 Stack base
- Java 21 + Spring Boot 3.3.x administrado con Gradle (`backend/build.gradle`) incorporando starters web, security, OAuth2 resource server, data JPA, Redis, mail, Flyway y OpenAPI.
- Configuración externa tipada mediante `AppProperties` (`backend/src/main/java/com/datakomerz/pymes/config/AppProperties.java`) para multitenencia, JWT/refresh tokens, captcha y toggles OIDC.
- Perfiles `application-dev.properties` y `application-test.properties` (`backend/src/main/resources/`) definen conexiones a Postgres/Redis, credenciales demo, URLs de MinIO y parámetros SMTP.

### 4.2 Organización por capas
- Controladores REST viven en `com.datakomerz.pymes.<dominio>.api` o directamente en el paquete del dominio (`ProductController`, `SalesController`, etc.) cuidando DTOs y validaciones.
- Servicios de aplicación (`…/<dominio>/application` o `…/<dominio>/<Dominio>Service`) encapsulan reglas de negocio, coordinan transacciones y delegan en repositorios.
- Capa de dominio modelada con entidades JPA, agregados y repositorios (Spring Data) en `com.datakomerz.pymes.<dominio>.domain`.
- Adaptadores de infraestructura (`core/tenancy`, `storage`, `config`) resuelven preocupaciones transversales (contexto de compañía, almacenamiento de archivos, captcha, notificaciones).

### 4.3 Multitenencia
- `CompanyContextFilter` (`backend/src/main/java/com/datakomerz/pymes/core/tenancy/CompanyContextFilter.java`) exige el header `X-Company-Id`, valida UUID y almacena el tenant en un `ThreadLocal`.
- `CompanyContext.require()` se emplea desde repositorios y servicios para asegurar aislamiento por compañía.
- Seeder y perfiles dev crean una compañía demo `00000000-0000-0000-0000-000000000001` utilizada por clientes durante desarrollo.

### 4.4 Seguridad y autenticación
- Flujo por defecto con JWT internos: autenticación en `/api/v1/auth/login`, emisión de refresh tokens y `JwtAuthenticationFilter` para proteger rutas (`backend/src/main/java/com/datakomerz/pymes/config/SecurityConfig.java`).
- Toggle OIDC (`app.security.jwt.oidc-enabled`) habilita `oauth2ResourceServer().jwt()` y delega la validación de tokens a un IdP externo; `OidcRoleMapper` (`backend/src/main/java/com/datakomerz/pymes/security/OidcRoleMapper.java`) mapea claims (`realm_access`, `resource_access`, `scope`) a authorities.
- `AccountRequestService` y `AccountRequestNotifier` gestionan solicitudes de cuenta públicas con captcha aritmético y envío de email (`backend/src/main/java/com/datakomerz/pymes/requests/application`).
- CORS configurable vía `AppProperties`, sesiones deshabilitadas y endpoints públicos acotados a `/actuator/**`, `/api/v1/auth/**` y `/api/v1/requests/**`.

### 4.5 Persistencia e integraciones
- Postgres 16 como base principal; migraciones Flyway en `backend/src/main/resources/db` para versionar esquema y seeds.
- Redis 7 como cache auxiliar y soporte para locks/eventos (se deshabilita automáticamente cuando no está disponible en local).
- `StorageService` y `LocalStorageService` (`backend/src/main/java/com/datakomerz/pymes/storage/`) manejan archivos e imágenes; existe configuración para apuntar a MinIO/S3.
- Librerías complementarias: ZXing para códigos QR, JavaMailSender para notificaciones y actuator/metrics para observabilidad.

### 4.6 Dominios implementados
- **Autenticación (`auth`)**: login, refresh, módulos habilitados por usuario, expiraciones controladas (`backend/src/main/java/com/datakomerz/pymes/auth/api/AuthController.java`).
- **Compañías (`company`)**: CRUD extendido con datos legales, contacto y pie de boleta (`backend/src/main/java/com/datakomerz/pymes/company/CompanyController.java`).
- **Productos (`products`)**: búsqueda por nombre/SKU/código de barras, carga de imágenes multipart, estados y generación de QR (`backend/src/main/java/com/datakomerz/pymes/products/ProductController.java`).
- **Inventario (`inventory`)**: lotes FIFO, alertas, ajustes manuales y métricas (`backend/src/main/java/com/datakomerz/pymes/inventory`).
- **Ventas (`sales`)** y **Compras (`purchases`)**: CRUD con métricas diarias, filtros avanzados y conciliación de stock (`backend/src/main/java/com/datakomerz/pymes/sales/api/SalesController.java`, `backend/src/main/java/com/datakomerz/pymes/purchases/PurchaseController.java`).
- **Pricing (`pricing`)**: historial de precios y auditoría de cambios (`backend/src/main/java/com/datakomerz/pymes/pricing`).
- **Clientes (`customers`)** y **Proveedores (`suppliers`)**: segmentación, conteos y filtros (`backend/src/main/java/com/datakomerz/pymes/customers/api`, `backend/src/main/java/com/datakomerz/pymes/suppliers`).
- **Solicitudes de cuenta (`requests`)**: endpoint público con validación de RUT, captcha y notificación (`backend/src/main/java/com/datakomerz/pymes/requests/api/AccountRequestController.java`).

### 4.7 Calidad y pruebas
- `./gradlew test` ejecuta suites JUnit 5 con soporte Spring Security; se incluyen pruebas de integración por dominio.
- Checkstyle y Spotless aplican reglas de estilo, y Flyway asegura consistencia de schema antes de subir ambientes (`backend/build.gradle`).
- Gradle tasks adicionales: `bootRun` con perfil `dev`, `flywayMigrate`, `spotlessApply`.

## 5. API REST v1 (rutas principales)
| Ruta | Uso principal | Implementación |
| --- | --- | --- |
| `/api/v1/auth/**` | Login y refresh tokens | `backend/src/main/java/com/datakomerz/pymes/auth/api/AuthController.java` |
| `/api/v1/companies` | Gestión de compañías | `backend/src/main/java/com/datakomerz/pymes/company/CompanyController.java` |
| `/api/v1/products` | Catálogo, imágenes, QR, lotes | `backend/src/main/java/com/datakomerz/pymes/products/ProductController.java` |
| `/api/v1/pricing` | Historial y mutaciones de precios | `backend/src/main/java/com/datakomerz/pymes/pricing/PricingController.java` |
| `/api/v1/inventory` | Alertas y ajustes de inventario | `backend/src/main/java/com/datakomerz/pymes/inventory/InventoryController.java` |
| `/api/v1/sales` | CRUD de ventas y métricas | `backend/src/main/java/com/datakomerz/pymes/sales/api/SalesController.java` |
| `/api/v1/purchases` | Compras y métricas | `backend/src/main/java/com/datakomerz/pymes/purchases/PurchaseController.java` |
| `/api/v1/customers` | Clientes, segmentos y resumen | `backend/src/main/java/com/datakomerz/pymes/customers/api/CustomerController.java` |
| `/api/v1/suppliers` | Proveedores y contactos | `backend/src/main/java/com/datakomerz/pymes/suppliers/SupplierController.java` |
| `/api/v1/requests` | Solicitudes de cuenta públicas | `backend/src/main/java/com/datakomerz/pymes/requests/api/AccountRequestController.java` |
| `/api/v1/health` | Health check extendido | `backend/src/main/java/com/datakomerz/pymes/api/HealthController.java` |

## 6. Frontend web (Vite + React)

### 6.1 Stack y estructura
- Vite + React 18 + TypeScript (`ui/`) con alias organizados por módulos (`features`, `components`, `pages`, `context`).
- `App.tsx` define el shell autenticado, layout lateral y routing por módulos (`ui/src/App.tsx`).
- Los formularios usan React Hook Form y componentes reutilizables, mientras que tablas y listados se apoyan en TanStack Table.

### 6.2 Estado, datos y modo offline
- `AuthContext` (`ui/src/context/AuthContext.tsx`) persiste tokens en `localStorage`, programa refresh automático y propaga `modules` para control de acceso.
- `services/client.ts` centraliza el cliente HTTP (Axios), añade encabezados `Authorization` / `X-Company-Id` y expone un modo offline que genera datos demo cuando falla la red (`ui/src/services/client.ts`).
- React Query gestiona cache, revalidaciones y loaders; mutaciones cubren login, solicitudes de cuenta, módulos y entidades de negocio.

### 6.3 Pruebas, build y linting
- Vitest + Testing Library (`ui/package.json`) para pruebas unitarias/componentes (`npm run test`).
- `npm run dev` levanta Vite con proxy a `/api`; `npm run build` produce artefactos listos para Tauri o deploy estático.
- Roadmap: activar ESLint + Prettier compartidos en el workspace y agregar pruebas e2e (Playwright/Cypress).

## 7. Clientes adicionales
- **Flutter (`app_flutter/`)**: consume `/api/v1/customers` con paginación infinita, formularios que integran geolocalización y manejo de `Authorization`/`X-Company-Id` en `lib/data/remote`. Incluye scripts `make run-web` / `make test` y pruebas de serialización/widgets.
- **Desktop (`desktop/`)**: proyecto Tauri que empaqueta la build de `ui/` en instaladores `.msi`. La guía `docs/windows-desktop.md` detalla requisitos, firma y distribución interna.

## 8. Infraestructura y DevOps
- `docker-compose.yml` levanta Postgres 16, Redis 7, backend (perfil `dev`), frontend (Vite), MinIO y Mailhog; variables `.env` controlan credenciales demo y rutas de archivos.
- Scripts recomendados: `docker compose up --build`, `cd backend && ./gradlew bootRun --args='--spring.profiles.active=dev'`, `npm run dev`, `make run-web`.
- Observabilidad: Actuator expone `/actuator/health`, `/actuator/metrics` y `/actuator/env`; logs estructurados se emiten a consola con niveles configurables vía `application-*.properties`.
- Lineamientos CI/CD: Gradle + Vitest tests, Checkstyle/Spotless como quality gates y verificación de migraciones Flyway antes de despliegues.

## 9. Integración OIDC (Keycloak / Auth0)

### 9.1 Objetivo y modo de ejecución
- Permitir que entornos de staging/producción validen tokens emitidos por un IdP OIDC; en `dev` se mantiene el flujo JWT interno.
- Toggle clave: `app.security.jwt.oidc-enabled=true` en las propiedades del backend, más `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` o `issuer-uri`.

### 9.2 Pasos en el IdP
1. Crear un Realm/tenant y un Client (p. ej. `pymerp-backend`).
2. Definir URIs válidas de redirect (para la UI) y obtener `client_id` / `client_secret` si aplica.
3. Crear roles (realm roles o client roles) y asignarlos a usuarios/grupos.
4. Agregar un mapper que exponga los roles en `realm_access.roles`, `resource_access.<client>.roles` o un claim `roles`.

### 9.3 Configuración de la aplicación
```properties
# backend/src/main/resources/application-dev.properties
app.security.jwt.oidc-enabled=true
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://<host>/realms/<realm>/protocol/openid-connect/certs

# (Opcional) si se usa el cliente OAuth2 de Spring para flujos web:
spring.security.oauth2.client.registration.keycloak.client-id=pymerp-frontend
spring.security.oauth2.client.registration.keycloak.client-secret=<secret>
spring.security.oauth2.client.provider.keycloak.issuer-uri=https://<host>/realms/<realm>
```

- `SecurityConfig` habilita automáticamente `oauth2ResourceServer().jwt()` cuando el toggle está activo (`backend/src/main/java/com/datakomerz/pymes/config/SecurityConfig.java`).
- `OidcRoleMapper` transforma claims en authorities `ROLE_*` / `SCOPE_*`, garantizando compatibilidad con Keycloak y Auth0 (`backend/src/main/java/com/datakomerz/pymes/security/OidcRoleMapper.java`).

### 9.4 Validación y pruebas
1. Obtener un access token válido (Keycloak Admin Console → Users → Impersonate / Auth0 Test tab / `password` grant).
2. Invocar la API con encabezados `Authorization: Bearer <TOKEN>` y `X-Company-Id: <UUID-compania>`.
```bash
curl -X GET "http://localhost:8081/api/v1/products" \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "X-Company-Id: 00000000-0000-0000-0000-000000000001"
```
3. Si `app.security.jwt.oidc-enabled=false`, el backend vuelve al flujo interno (`admin@dev.local` / `Admin1234`).

## 10. Roadmap sugerido
1. Completar integración con MinIO/S3 para servir imágenes y documentos desde almacenamiento de objetos.
2. Automatizar pipelines de pruebas (Gradle + Vitest) y quality gates (Checkstyle, Spotless) en CI/CD.
3. Expandir el modo offline (web y Flutter) con sincronización bidireccional y colas de reintento.
4. Implementar reportes analíticos avanzados (ventas, compras, inventario) con filtros dinámicos y exportaciones.
5. Incorporar gestión avanzada de usuarios/roles administrables desde el IdP y la UI (provisionamiento SCIM, auditoría).


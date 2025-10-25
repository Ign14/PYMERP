# Sistema de Gestion PyMEs - Monorepo

## Estructura general del repositorio

| Directorio / archivo | Descripción |
| --- | --- |
| `backend/` | API REST multitenant construida con Spring Boot 3 (Java 21) y Gradle; incluye migraciones Flyway, autenticación JWT/OIDC opcional, capas de dominio (ventas, compras, inventario, pricing, clientes, proveedores) y servicios de almacenamiento local/S3. |
| `ui/` | Frontend web en Vite + React 18 + TypeScript. Usa React Query, React Router, contexto de autenticación con refresco automático de tokens y un modo offline demo para datos críticos. |
| `app_flutter/` | Cliente Flutter orientado a escenarios móviles/offline con autenticación JWT, paginación infinita de clientes y formularios con geolocalización opcional. |
| `desktop/` | Paquete Tauri que permite distribuir la UI web como aplicación de escritorio; guía en `docs/windows-desktop.md`. |
| `docs/` | Documentación auxiliar (por ejemplo, empaquetado desktop en Windows). |
| `scripts/`, `package.json` | Scripts Node para orquestar tareas comunes del workspace (ejecutar UI, lint, helpers). |
| `docker-compose.yml` | Orquestación local con Postgres 16, Redis 7, backend, frontend, MinIO y Mailhog. |

## Funcionalidades principales

### Backend (Spring Boot)
- Autenticación con JWT propios (refresh tokens) y opción OIDC mediante `OidcRoleMapper` para mapear roles/alcances.
- Multitenencia via header `X-Company-Id`, filtros y contexto `ThreadLocal` para aislar datos por compañía.
- Dominios cubiertos: compañías, productos, pricing, inventario con lotes FIFO, ventas, compras, clientes, proveedores y solicitudes de cuenta con captcha.
- Integraciones: almacenamiento local/S3, generación de códigos QR, Actuator/metrics y semillas de datos demo.

### Frontend web (React)
- Shell protegido con sidebar/topbar, navegación modular (dashboard, ventas, compras, inventario, clientes, proveedores, finanzas, reportes, configuración).
- Persistencia de sesión en `localStorage`, refresco automático de tokens y controles de acceso por módulos.
- Formularios y tablas reactivas con React Query; landing unifica login + solicitud de cuenta con captcha y validación de RUT.
- Modo offline simulado para compañías, inventario y métricas diarias cuando no hay red.

### Cliente Flutter
- Consume endpoints `/api/v1/customers` con paginación, búsqueda y formularios que capturan ubicación actual.
- Reutiliza encabezados `Authorization` y `X-Company-Id`; pruebas de serialización/widgets y comandos `make run-web`/`make test`.

### Paquete Desktop (Tauri)
- Empaqueta la UI de `ui/` como ejecutable Windows (`.msi`) siguiendo los pasos de `docs/windows-desktop.md`.
- Ideal para despliegues internos sin navegador dedicado.

## Perfiles recomendados
- Backend `dev`: puerto 8081, autenticacion JWT (`admin@dev.local` / `Admin1234`), compañia por defecto `00000000-0000-0000-0000-000000000001`.
- Base de datos de desarrollo incluye semilla con proveedores, clientes, productos, compras, ventas, planillas y reportes para el tenant demo.
- Frontend `development`: Vite proxy hacia `/api`; iniciar sesion en `/login` con las credenciales anteriores.

## Pasos rapidos

1. Levanta todo el stack (backend, frontend y dependencias) con `docker compose up --build`.
   - Backend expone `http://localhost:8081` con perfil `dev` conectado a Postgres y Redis del compose.
   - Frontend corre Vite en `http://localhost:5173` apuntando al backend interno (`http://backend:8081/api`).
2. Si prefieres ejecutar los servicios manualmente:
   - Backend: `cd backend && ./gradlew bootRun --args='--spring.profiles.active=dev'`. Si el puerto 8081 está ocupado, el backend
     buscará automáticamente un puerto libre e informará el valor en la consola.
   - Frontend: `npm install` (desde la raíz instala dependencias del workspace) y luego `npm run dev`.
4. Build desktop (ver `docs/windows-desktop.md`): empaquetar UI con Tauri para generar instalador `.msi` listo para distribución interna.

## Scripts
- Backend pruebas: `cd backend && ./gradlew test`
- Frontend build: `cd ui && npm run build`
- Lint/format (pendiente): integrar `eslint` + `prettier`.

## Actualización Compañías

- Ejecuta la nueva migración Flyway (`cd backend && ./gradlew flywayMigrate`) antes de iniciar el backend para que se apliquen las columnas `business_name`, `business_activity`, `address`, `commune`, `phone`, `email`, `receipt_footer_message` y `updated_at`.
- Para validar el backend después del cambio: `cd backend && ./gradlew test --tests com.datakomerz.pymes.company.CompanyControllerIT`.
- El frontend incorpora validaciones y CRUD extendido para compañías; puedes ejecutar `cd ui && npm run test -- CreateCompanyForm` para verificar los formularios.

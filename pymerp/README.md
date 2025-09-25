# Sistema de Gestion PyMEs - Monorepo

- `backend/`: Spring Boot (Java 21, Gradle)
- `ui/`: Vite + React 18 + TypeScript
- `docs/windows-desktop.md`: empaquetado Tauri/Electron para Windows
- `docker-compose.yml`: Servicios locales (Postgres, Redis, MinIO, Mailhog)

## Perfiles recomendados
- Backend `dev`: puerto 8081, autenticacion JWT (`admin@dev.local` / `Admin1234`), compañia por defecto `00000000-0000-0000-0000-000000000001`.
- Base de datos de desarrollo incluye semilla con proveedores, clientes, productos, compras, ventas, planillas y reportes para el tenant demo.
- Frontend `development`: Vite proxy hacia `/api`; iniciar sesion en `/login` con las credenciales anteriores.

## UI Principal
- Layout responsive con sidebar, topbar y páginas: Panel, Ventas, Compras, Inventario, Clientes, Proveedores, Finanzas, Reportes, Configuración.
- Dashboards con KPI, tablas reactivas (`react-query`) y componentes reutilizables (`card`, `table`, `status`).
- Auto-refresh de tokens (JWT + refresh) y persistencia en localStorage.

## Pasos rapidos
1. Levanta todo el stack (backend, frontend y dependencias) con `docker compose up --build`.
   - Backend expone `http://localhost:8081` con perfil `dev` conectado a Postgres y Redis del compose.
   - Frontend corre Vite en `http://localhost:5173` apuntando al backend interno (`http://backend:8081/api`).
2. Si prefieres ejecutar los servicios manualmente:
   - Backend: `cd backend && ./gradlew bootRun --args='--spring.profiles.active=dev'`. Si el puerto 8081 está ocupado, el backend
     buscará automáticamente un puerto libre e informará el valor en la consola.
   - Frontend: `npm install` (desde la raíz instala dependencias del workspace) y luego `npm run dev`.
4. Build desktop (ver `docs/windows-desktop.md`): empaquetar UI con Tauri para generar instalador `.msi`.

## Scripts
- Backend pruebas: `cd backend && ./gradlew test`
- Frontend build: `cd ui && npm run build`
- Lint/format (pendiente): integrar `eslint` + `prettier`.

# Ventas - Dashboard conectado a datos reales

El panel de ventas ahora consume los nuevos reportes del backend (`/api/v1/reports/sales/summary` y `/api/v1/reports/sales/timeseries`).

## Cómo validar los datos

1. **Levanta el entorno completo**

   ```bash
   docker compose up --build
   ```

   Esto inicia Postgres, Redis, backend y frontend (Vite).

2. **Crea documentos de ventas reales**

   - Usa la UI (`http://localhost:5173`) o envía `POST /api/v1/sales` contra el backend (`http://localhost:8081`).
   - Asegúrate de incluir ventas en distintos días y con estados mixtos para verificar la exclusión de canceladas.

3. **Verifica los endpoints de reportes**

   - `GET http://localhost:8081/api/v1/reports/sales/summary?days=14`
   - `GET http://localhost:8081/api/v1/reports/sales/timeseries?days=14&bucket=day`

   Los totales deben coincidir con la suma de las ventas emitidas en los últimos 14 días.

4. **Revisa el dashboard**

   - Ingresa a la página de Ventas en la UI.
   - Cambia de tenant (selector `X-Company-Id`) si es necesario y confirma que las tarjetas y el gráfico se actualizan.
   - El gráfico siempre mostrará 14 puntos (uno por día) y los días sin ventas quedarán en cero.

5. **Pruebas unitarias (frontend)**

   ```bash
   cd ui
   npm install
   npm run test -- src/components/sales/__tests__/SalesDashboardOverview.test.tsx
   ```

## Notas

- El formato de moneda usa CLP por defecto. Si el tenant expone otra moneda, ajusta el `createCurrencyFormatter` en `src/utils/currency.ts`.
- El backend excluye automáticamente ventas con estado `cancelled` y completa los días sin datos con cero.

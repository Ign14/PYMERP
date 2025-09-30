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
- La tarjeta "Número de documentos" ya no muestra una previsualización; al hacer clic abre el modal que carga la lista completa de documentos de venta.

## Registrar venta: productos frecuentes del cliente

- En la pantalla **Registrar venta** activa la casilla **Productos frecuentes del cliente** para ver un panel con los artículos más comprados por el cliente seleccionado.
- El panel aparece habilitado solo cuando existe un cliente elegido. Cambiar de cliente limpia el filtro y recarga los datos.
- Puedes filtrar rápidamente por nombre o SKU desde la barra de búsqueda. Cada fila muestra SKU, fecha de la última compra, veces compradas, cantidad promedio y el precio usado en la última venta.
- Haz clic (o presiona Enter/Espacio) sobre un producto para agregarlo de inmediato a la venta con la última cantidad conocida y el precio vigente.
- Si el cliente no tiene historial se mostrará un mensaje de estado. En caso de error puedes reintentar desde el botón dentro del panel.

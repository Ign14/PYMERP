# UI/UX Blueprint - PyMEs Management Suite

## 1. Personas & Objetivos
- **Due�o**: visi�n global de ventas, compras, stock, cobranzas.
- **Administrador Operativo**: ejecuta compras, ventas, inventario.
- **Contador**: reportes fiscales, conciliaciones, exportaciones.
- **Vendedor**: registra ventas r�pidas, consulta clientes.

## 2. Informaci�n Clave por Persona
| Persona | M�tricas clave | Acciones diarias |
| --- | --- | --- |
| Due�o | Ingresos, m�rgenes, stock cr�tico | Revisar panel, aprobar compras, ver balances |
| Admin | �rdenes pendientes, inventario, proveedores | Registrar compras/ventas, actualizar cat�logo |
| Contador | IVA, flujo caja, documentos | Exportar reportes, validar facturas |
| Vendedor | Cat�logo, precio, clientes frecuentes | Consulta disponibilidad, registra venta |

## 3. IA de Navegaci�n
```
+------------------------------------------+
�  Topbar                                   �
+-------------------------------------------�
� Sidebar     �  Page Content               �
�  - Dashboard�  - Widgets / Tables         �
�  - Ventas   �                             �
�  - Compras  �                             �
�  - Inventario                              �
�  - Clientes                               �
�  - Proveedores                            �
�  - Finanzas                               �
�  - Reportes                               �
�  - Configuraci�n                          �
+-------------------------------------------+
```

## 4. Layout Responsivo
- **Desktop = 1200px**: sidebar fijo + grid 3 columnas.
- **Tablet 768-1199px**: sidebar colapsable, contenidos en 2 columnas.
- **Mobile = 767px**: nav drawer, stack vertical, acciones principales flotantes.
- Utilizar CSS Grid + Flex, `@media` en `App.css`.

## 5. M�dulos y Vistas
1. **Dashboard Ejecutivo**
   - KPI cards (Ingresos d�a/semana, Stock cr�tico, Clientes nuevos).
   - Gr�ficos (l�nea ingresos vs gastos, barras categor�a).
   - Tareas pendientes (�rdenes, pagos).

2. **Ventas**
   - Tabla facturas (status, total, cliente).
   - Acciones: registrar venta, aplicar descuentos, generar comprobante.
   - Resumen ticket promedio, top productos.

3. **Compras**
   - Tabla compras, filtros proveedor/estado.
   - Form modal para crear compra, adjuntar PDF, cargar �tems.

4. **Inventario**
   - Tab: Productos, Lotes, Movimientos.
   - Tabla responsive con inline editing (stock m�nimo, ubicaci�n).
   - Alertas de vencimiento (productos perecibles).

5. **Clientes y Proveedores**
   - Tablas con buscador y filtros.
   - Perfil lateral: datos contacto, historial transacciones.
   - Geolocalizaci�n (mapa mini, lat/lng).

6. **Finanzas**
   - Flujos de caja, cuentas por cobrar/pagar.
   - Integraci�n con bancos (futuro, placeholder).

7. **Reportes**
   - Selecci�n de per�odo, exportaci�n PDF/Excel.
   - Widgets para IVA, ventas regionales.

8. **Configuraci�n**
   - Datos empresa, usuarios/roles, cat�logos auxiliares.
   - Integraciones (S3, correo, facturaci�n).

## 6. Componentes Reutilizables
- `LayoutShell`: topbar + sidebar + drawer mobile.
- `NavMenu` con datos declarativos (icono, ruta, permisos).
- `StatCard`, `TrendChart`, `DataTable`, `Timeline`, `Badge`.
- Formularios con `react-hook-form` (futuro), `Dialog` para modales.

## 7. Plugins / Librer�as a Evaluar
- **UI**: `@headlessui/react` + Tailwind para accesibilidad.
- **Charts**: `recharts` o `nivo`.
- **Tables**: `@tanstack/react-table` para sorting/pagination.
- **Forms**: `react-hook-form` + `zod` validaciones.
- **Maps**: `leaflet` con `react-leaflet` para geolocalizaciones.

## 8. Flujo de Trabajo (Frontend)
1. Login -> Dashboard (pre-cargado con `react-query` + cache).
2. Side navigation -> subm�dulos.
3. Modales para CRUD con optimistic updates y toasts (`react-hot-toast`).
4. Responsividad testeada con `@media` + `ResizeObserver`.

## 9. Windows Desktop (Roadmap)
- Empaquetar UI existente con **Tauri** (Rust) o **Electron**.
- Tauri preferido (ligero, webview2):
  1. `cargo init --bin pymes-desktop` en nueva carpeta `desktop/`.
  2. Configurar Tauri para servir build de Vite (`dist/`).
  3. IPC para acceder APIs locales (futuro: sincronizaci�n offline).
  4. `tauri build` genera `.msi` instalador Windows.
- Alternativa: `electron-builder` con auto-update.

## 10. M�tricas de Impacto
- Panel �nico reduce tiempo de decisi�n (~40%).
- Alertas proactivas bajan rotura stock (~25%).
- Escritorio Windows permite operar sin navegador en cajas.
- Arquitectura modular habilita agregados futuros (mobile Flutter, BI).

## 11. Pr�ximos Entregables
- Implementar `LayoutShell`, `SideNav`, `DashboardPage` con widgets.
- Crear p�ginas base para Ventas/Compras/Inventario (placeholders).
- Incorporar tema oscuro/claro con CSS variables.
- Automatizar empaquetado desktop (Tauri + CI).

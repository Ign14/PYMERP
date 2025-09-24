# UI/UX Blueprint - PyMEs Management Suite

## 1. Personas & Objetivos
- **Dueño**: visión global de ventas, compras, stock, cobranzas.
- **Administrador Operativo**: ejecuta compras, ventas, inventario.
- **Contador**: reportes fiscales, conciliaciones, exportaciones.
- **Vendedor**: registra ventas rápidas, consulta clientes.

## 2. Información Clave por Persona
| Persona | Métricas clave | Acciones diarias |
| --- | --- | --- |
| Dueño | Ingresos, márgenes, stock crítico | Revisar panel, aprobar compras, ver balances |
| Admin | Órdenes pendientes, inventario, proveedores | Registrar compras/ventas, actualizar catálogo |
| Contador | IVA, flujo caja, documentos | Exportar reportes, validar facturas |
| Vendedor | Catálogo, precio, clientes frecuentes | Consulta disponibilidad, registra venta |

## 3. IA de Navegación
```
+------------------------------------------+
¦  Topbar                                   ¦
+-------------------------------------------¦
¦ Sidebar     ¦  Page Content               ¦
¦  - Dashboard¦  - Widgets / Tables         ¦
¦  - Ventas   ¦                             ¦
¦  - Compras  ¦                             ¦
¦  - Inventario                              ¦
¦  - Clientes                               ¦
¦  - Proveedores                            ¦
¦  - Finanzas                               ¦
¦  - Reportes                               ¦
¦  - Configuración                          ¦
+-------------------------------------------+
```

## 4. Layout Responsivo
- **Desktop = 1200px**: sidebar fijo + grid 3 columnas.
- **Tablet 768-1199px**: sidebar colapsable, contenidos en 2 columnas.
- **Mobile = 767px**: nav drawer, stack vertical, acciones principales flotantes.
- Utilizar CSS Grid + Flex, `@media` en `App.css`.

## 5. Módulos y Vistas
1. **Dashboard Ejecutivo**
   - KPI cards (Ingresos día/semana, Stock crítico, Clientes nuevos).
   - Gráficos (línea ingresos vs gastos, barras categoría).
   - Tareas pendientes (órdenes, pagos).

2. **Ventas**
   - Tabla facturas (status, total, cliente).
   - Acciones: registrar venta, aplicar descuentos, generar comprobante.
   - Resumen ticket promedio, top productos.

3. **Compras**
   - Tabla compras, filtros proveedor/estado.
   - Form modal para crear compra, adjuntar PDF, cargar ítems.

4. **Inventario**
   - Tab: Productos, Lotes, Movimientos.
   - Tabla responsive con inline editing (stock mínimo, ubicación).
   - Alertas de vencimiento (productos perecibles).

5. **Clientes y Proveedores**
   - Tablas con buscador y filtros.
   - Perfil lateral: datos contacto, historial transacciones.
   - Geolocalización (mapa mini, lat/lng).

6. **Finanzas**
   - Flujos de caja, cuentas por cobrar/pagar.
   - Integración con bancos (futuro, placeholder).

7. **Reportes**
   - Selección de período, exportación PDF/Excel.
   - Widgets para IVA, ventas regionales.

8. **Configuración**
   - Datos empresa, usuarios/roles, catálogos auxiliares.
   - Integraciones (S3, correo, facturación).

## 6. Componentes Reutilizables
- `LayoutShell`: topbar + sidebar + drawer mobile.
- `NavMenu` con datos declarativos (icono, ruta, permisos).
- `StatCard`, `TrendChart`, `DataTable`, `Timeline`, `Badge`.
- Formularios con `react-hook-form` (futuro), `Dialog` para modales.

## 7. Plugins / Librerías a Evaluar
- **UI**: `@headlessui/react` + Tailwind para accesibilidad.
- **Charts**: `recharts` o `nivo`.
- **Tables**: `@tanstack/react-table` para sorting/pagination.
- **Forms**: `react-hook-form` + `zod` validaciones.
- **Maps**: `leaflet` con `react-leaflet` para geolocalizaciones.

## 8. Flujo de Trabajo (Frontend)
1. Login -> Dashboard (pre-cargado con `react-query` + cache).
2. Side navigation -> submódulos.
3. Modales para CRUD con optimistic updates y toasts (`react-hot-toast`).
4. Responsividad testeada con `@media` + `ResizeObserver`.

## 9. Windows Desktop (Roadmap)
- Empaquetar UI existente con **Tauri** (Rust) o **Electron**.
- Tauri preferido (ligero, webview2):
  1. `cargo init --bin pymes-desktop` en nueva carpeta `desktop/`.
  2. Configurar Tauri para servir build de Vite (`dist/`).
  3. IPC para acceder APIs locales (futuro: sincronización offline).
  4. `tauri build` genera `.msi` instalador Windows.
- Alternativa: `electron-builder` con auto-update.

## 10. Métricas de Impacto
- Panel único reduce tiempo de decisión (~40%).
- Alertas proactivas bajan rotura stock (~25%).
- Escritorio Windows permite operar sin navegador en cajas.
- Arquitectura modular habilita agregados futuros (mobile Flutter, BI).

## 11. Próximos Entregables
- Implementar `LayoutShell`, `SideNav`, `DashboardPage` con widgets.
- Crear páginas base para Ventas/Compras/Inventario (placeholders).
- Incorporar tema oscuro/claro con CSS variables.
- Automatizar empaquetado desktop (Tauri + CI).

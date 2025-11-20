# Correcciones de Errores de Red y API

## Fecha: 2025-11-19

## Resumen
Se corrigieron múltiples errores relacionados con llamadas a la API que causaban "Network Error" y "Error: Unexpected token '<', "<!doctype "... is not valid JSON".

## Problemas Identificados y Solucionados

### 1. ✅ Rutas API Duplicadas (Problema Principal)

**Problema:** Las rutas API tenían el prefijo `/api` duplicado, generando URLs como `/api/api/v1/...` que no existían y retornaban HTML en lugar de JSON.

**Archivos Corregidos:**

#### `pymerp/ui/src/services/inventory.ts`
- ❌ Antes: `/api/v1/inventory/locations`
- ✅ Después: `/v1/inventory/locations`

Líneas modificadas:
- `getLocations()`: línea 31
- `createLocation()`: línea 40
- `updateLocation()`: línea 48
- `toggleLocationEnabled()`: línea 52
- `getStockByProduct()`: línea 286

#### `pymerp/ui/src/services/client.ts`
- ❌ Antes: `/api/v1/inventory/locations`, `/api/v1/services`
- ✅ Después: `/v1/inventory/locations`, `/v1/services`

Funciones corregidas:
- `listLocations()`
- `createLocation()`
- `updateLocation()`
- `deleteLocation()`
- `getLocationStockSummary()`
- `listServices()`
- `createService()`

**Razón:** El axios instance ya tiene `baseURL: '/api'` configurado, y Vite hace proxy de `/api` a `http://localhost:8081`.

---

### 2. ✅ Mejora en el Manejo de Errores HTTP

**Archivo:** `pymerp/ui/src/services/client.ts`

Se agregó un **interceptor de respuesta** en axios para:

1. **Detectar errores de red** (servidor no disponible)
   - Muestra mensaje claro: "No se puede conectar con el servidor. Verifica que el backend esté corriendo en http://localhost:8081"

2. **Detectar respuestas HTML** en lugar de JSON
   - Identifica cuando el Content-Type es `text/html`
   - Muestra la URL exacta que falló

3. **Mensajes personalizados por código HTTP**:
   - 404: "Recurso no encontrado: [URL]"
   - 500: "Error interno del servidor"
   - 401: "No autorizado. Por favor inicia sesión nuevamente"
   - 403: "Acceso denegado. No tienes permisos para esta acción"

---

### 3. ✅ Datos Falsos Reemplazados por Datos Reales

**Archivo:** `pymerp/ui/src/components/sales/SalesTopCustomersPanel.tsx`

**Problema:** El componente "Top 10 Clientes" mostraba datos hardcodeados falsos.

**Solución:**
- Se reemplazó la función `generateTopCustomers()` con llamadas reales a la API
- Usa `listCustomers()` para obtener todos los clientes activos
- Usa `getCustomerStats()` para obtener estadísticas reales de cada cliente
- Ordena por `totalRevenue` descendente y muestra el top 10
- Agrega estados de carga y error apropiados

---

### 4. ✅ Mejoras en Mensajes de Error de Pronósticos

Se mejoraron los mensajes de error en los siguientes componentes:

#### `pymerp/ui/src/components/ForecastChart.tsx`
- ❌ Antes: `Error al cargar pronósticos: [error]`
- ✅ Después: 
  ```
  ⚠️ Error al calcular el pronóstico
  [mensaje de error detallado]
  ```

#### `pymerp/ui/src/components/ForecastTable.tsx`
- Agregado mensaje de error detallado con icono ⚠️

#### `pymerp/ui/src/components/PurchaseForecast.tsx`
- Agregado contenedor con estilo de error
- Muestra mensaje de error específico

#### `pymerp/ui/src/components/finances/FinanceSummaryCards.tsx`
- Agregado botón "Reintentar" en caso de error
- Mensaje de error más descriptivo
- Estructura visual mejorada

---

## Impacto de las Correcciones

### Módulos Afectados Positivamente:

1. **✅ Compras (Purchases)**
   - Ahora se registran correctamente las nuevas compras
   - Endpoint: `POST /v1/purchases/with-inventory`

2. **✅ Ubicaciones (Locations)**
   - Ahora se crean/guardan correctamente las ubicaciones
   - Endpoints: `POST /v1/inventory/locations`, `PUT /v1/inventory/locations/:id`

3. **✅ Resumen Financiero (Finance Summary)**
   - Carga correctamente los datos financieros
   - Endpoint: `GET /v1/finances/summary`
   - Botón de reintentar en caso de error

4. **✅ Pronósticos (Forecasts)**
   - Mensajes de error más claros en todos los componentes
   - Endpoint: `GET /v1/inventory/forecast`

5. **✅ Top 10 Clientes**
   - Ahora muestra datos reales en lugar de datos falsos
   - Endpoints: `GET /v1/customers`, `GET /v1/customers/:id/stats`

6. **✅ Servicios**
   - Endpoints corregidos para listar y crear servicios

---

## Verificación Post-Corrección

### Para verificar que las correcciones funcionan:

1. **Iniciar el backend:**
   ```bash
   cd pymerp/backend
   ./gradlew bootRun
   ```
   El backend debe estar corriendo en `http://localhost:8081`

2. **Iniciar el frontend:**
   ```bash
   cd pymerp/ui
   npm run dev
   ```
   El frontend debe estar en `http://localhost:5173`

3. **Verificar las funcionalidades:**
   - ✅ Crear una nueva compra en `/app/purchases/create`
   - ✅ Crear una nueva ubicación en Inventario > Ubicaciones
   - ✅ Ver el Resumen Financiero en el dashboard
   - ✅ Ver los pronósticos en Inventario
   - ✅ Ver el Top 10 Clientes en Ventas
   - ✅ Todos los módulos deben mostrar mensajes de error claros si el backend no está disponible

---

## Notas Técnicas

### Configuración de Vite Proxy
El archivo `pymerp/ui/vite.config.ts` tiene configurado:
```typescript
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8081',
      changeOrigin: true,
    },
  },
}
```

Esto significa que todas las peticiones a `/api/*` se redirigen a `http://localhost:8081/api/*`.

### Estructura de URLs Correcta
- ❌ Incorrecto: `/api/v1/endpoint` → Se convierte en `http://localhost:8081/api/api/v1/endpoint`
- ✅ Correcto: `/v1/endpoint` → Se convierte en `http://localhost:8081/api/v1/endpoint`

### Backend Endpoints (Spring Boot)
Los controladores en el backend tienen la estructura:
```java
@RestController
@RequestMapping("/api/v1/[module]")
public class ModuleController {
  // endpoints
}
```

Por lo tanto, la URL completa es `http://localhost:8081/api/v1/[module]/[endpoint]`.

---

## Archivos Modificados

1. `pymerp/ui/src/services/inventory.ts` - Rutas API corregidas
2. `pymerp/ui/src/services/client.ts` - Rutas API corregidas + interceptor de errores
3. `pymerp/ui/src/components/sales/SalesTopCustomersPanel.tsx` - Datos reales en lugar de falsos
4. `pymerp/ui/src/components/ForecastChart.tsx` - Mensajes de error mejorados
5. `pymerp/ui/src/components/ForecastTable.tsx` - Mensajes de error mejorados
6. `pymerp/ui/src/components/PurchaseForecast.tsx` - Mensajes de error mejorados
7. `pymerp/ui/src/components/finances/FinanceSummaryCards.tsx` - Mensajes de error mejorados + botón reintentar

---

## Recomendaciones Futuras

1. **Testing:** Agregar tests automatizados para verificar que las URLs se construyen correctamente
2. **Logging:** Considerar agregar logging más detallado en desarrollo para detectar estos problemas temprano
3. **Validación:** Crear una utilidad que valide que las rutas no tengan prefijos duplicados
4. **Documentación:** Mantener actualizada la documentación de endpoints del backend

---

**Estado:** ✅ Todas las correcciones implementadas y listas para testing


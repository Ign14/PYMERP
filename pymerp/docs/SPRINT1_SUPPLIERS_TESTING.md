# 🧪 Guía de Testing - Sprint 1 Proveedores

**Fecha**: 3 de noviembre de 2025  
**Módulo**: Proveedores  
**Endpoints**: `/api/v1/suppliers/{id}/metrics`, `/api/v1/suppliers/alerts`

---

## 🚀 **Instrucciones de Inicio**

### **1. Iniciar Backend**

```cmd
cd backend
gradlew.bat bootRun --args="--spring.profiles.active=dev --server.port=8081"
```

O usar la tarea de VS Code: `bootRun:dev (sin debug)`

### **2. Iniciar Frontend**

En otra terminal:
```cmd
cd ui
npm run dev
```

Debería abrir en: http://localhost:5173

---

## ✅ **Checklist de Testing**

### **Backend - Endpoints**

#### ✅ **GET /api/v1/suppliers/alerts**
```bash
# Método: GET
# URL: http://localhost:8081/api/v1/suppliers/alerts
# Headers: Authorization: Bearer <token>
```

**Resultado esperado**:
```json
[
  {
    "supplierId": null,
    "supplierName": "Concentración de Compras",
    "type": "HIGH_CONCENTRATION",
    "severity": "WARNING",
    "message": "XX.X% de las compras concentradas en 3 proveedores - Riesgo de dependencia",
    "actionLabel": "Diversificar proveedores",
    "daysWithoutPurchases": null,
    "concentrationPercentage": 75.5
  },
  {
    "supplierId": "uuid-proveedor",
    "supplierName": "Nombre Proveedor",
    "type": "NO_RECENT_PURCHASES",
    "severity": "WARNING",
    "message": "Sin compras hace XX días",
    "actionLabel": "Contactar proveedor",
    "daysWithoutPurchases": 120,
    "concentrationPercentage": null
  }
]
```

**Verificar**:
- [ ] Retorna 200 OK
- [ ] Array de alertas (puede estar vacío si no hay alertas)
- [ ] Alertas tienen severidad: INFO, WARNING o CRITICAL
- [ ] Tipos válidos: NO_RECENT_PURCHASES, INACTIVE_SUPPLIER, HIGH_CONCENTRATION, SINGLE_SOURCE

#### ✅ **GET /api/v1/suppliers/{id}/metrics**
```bash
# Método: GET
# URL: http://localhost:8081/api/v1/suppliers/{supplier-id}/metrics
# Headers: Authorization: Bearer <token>
# Reemplazar {supplier-id} con UUID real
```

**Resultado esperado**:
```json
{
  "totalPurchases": 15,
  "totalAmount": 12500000,
  "averageOrderValue": 833333.33,
  "lastPurchaseDate": "2025-10-15T10:30:00Z",
  "purchasesLastMonth": 3,
  "amountLastMonth": 2500000,
  "purchasesPreviousMonth": 2,
  "amountPreviousMonth": 1800000
}
```

**Verificar**:
- [ ] Retorna 200 OK
- [ ] totalPurchases >= 0
- [ ] totalAmount es BigDecimal
- [ ] averageOrderValue calculado correctamente (total/cantidad)
- [ ] lastPurchaseDate es ISO 8601 o null
- [ ] purchasesLastMonth y purchasesPreviousMonth son números
- [ ] Si no tiene compras, todos los valores son 0 o null

---

### **Frontend - Componentes**

#### ✅ **SuppliersStatsCard** (Modernizado)

**Navegación**: http://localhost:5173/app/suppliers

**Verificar**:
- [ ] Se muestra el card "📊 Estadísticas de Proveedores"
- [ ] KPIs muestran:
  - [ ] Total de proveedores
  - [ ] Activos (con %, fondo verde)
  - [ ] Inactivos (con %, fondo gris)
- [ ] Si hay alertas críticas/advertencias:
  - [ ] Se muestra badge "X alerta(s)" arriba
  - [ ] Máximo 2 alertas visibles con iconos 🔴/⚠️
- [ ] Si hay más inactivos que activos:
  - [ ] Aparece alerta naranja "Más proveedores inactivos que activos"
- [ ] Top 5 por comuna se muestra con badges
- [ ] Top 5 por actividad se muestra con badges
- [ ] Dark theme aplicado (fondo negro, bordes grises)

#### ✅ **SupplierPerformancePanel** (Nuevo)

**Ubicación**: Segunda fila, columna izquierda

**Verificar**:
- [ ] Se muestra el card "📈 Desempeño de Proveedores"
- [ ] Selector de proveedor funciona
- [ ] Al seleccionar un proveedor:
  - [ ] Muestra loading mientras carga
  - [ ] Muestra error si falla
  - [ ] Muestra métricas:
    - [ ] Total Compras
    - [ ] Monto Total (con formato CLP)
    - [ ] Promedio por Orden (AOV)
    - [ ] Última Compra (fecha + días transcurridos)
  - [ ] Sección "Actividad Reciente":
    - [ ] Último mes (cantidad + monto)
    - [ ] Mes anterior (cantidad + monto)
    - [ ] Tendencia % (↑ verde o ↓ rojo) si hay cambio
- [ ] Si no hay compras en 90+ días:
  - [ ] Alerta amarilla "Sin compras hace X días"
- [ ] Si el proveedor no tiene compras:
  - [ ] Info azul "Este proveedor no tiene compras registradas aún"
- [ ] Dark theme aplicado

#### ✅ **SupplierAlertsPanel** (Nuevo)

**Ubicación**: Segunda fila, columna derecha

**Verificar**:
- [ ] Se muestra el card "🔔 Alertas de Proveedores"
- [ ] Badge con total de alertas arriba
- [ ] Si no hay alertas:
  - [ ] Icono ✅ verde
  - [ ] Mensaje "No hay alertas pendientes"
- [ ] Si hay alertas:
  - [ ] Agrupadas por severidad:
    - [ ] 🔴 Críticas (rojo)
    - [ ] ⚠️ Advertencias (amarillo)
    - [ ] ℹ️ Informativas (azul)
  - [ ] Cada alerta muestra:
    - [ ] Badge con tipo
    - [ ] Mensaje descriptivo
    - [ ] Nombre del proveedor (excepto concentración)
    - [ ] Días sin compras (si aplica)
    - [ ] % de concentración (si aplica)
    - [ ] Botón de acción sugerido
  - [ ] Informativas muestran máximo 3, resto "+X más..."
- [ ] Dark theme aplicado

#### ✅ **SuppliersPage** (Layout)

**Verificar**:
- [ ] Layout 2x2 en desktop:
  - [ ] Fila 1: SuppliersCard | SuppliersStatsCard
  - [ ] Fila 2: SupplierPerformancePanel | SupplierAlertsPanel
- [ ] Layout responsive en móvil (columna única)
- [ ] Funcionalidad existente intacta:
  - [ ] Botón "Exportar CSV" funciona
  - [ ] Botón "Importar CSV" funciona
  - [ ] Botón "+ Nuevo proveedor" abre diálogo
  - [ ] CRUD de proveedores funciona
  - [ ] Gestión de contactos funciona
  - [ ] Filtros (Activos/Inactivos) funcionan
  - [ ] Búsqueda funciona

---

## 🧪 **Escenarios de Prueba**

### **Escenario 1: Sin proveedores ni compras**
**Datos**: Base de datos vacía

**Resultado esperado**:
- SuppliersStatsCard: Total=0, Activos=0, Inactivos=0
- SupplierPerformancePanel: Selector vacío
- SupplierAlertsPanel: ✅ "No hay alertas pendientes"

### **Escenario 2: Proveedores sin compras**
**Datos**:
- 5 proveedores activos
- 0 compras

**Resultado esperado**:
- SuppliersStatsCard: Total=5, Activos=5, sin alertas
- SupplierPerformancePanel: Al seleccionar proveedor → Info azul "sin compras"
- SupplierAlertsPanel: 5 alertas INFO "Proveedor activo sin compras registradas"

### **Escenario 3: Proveedor con compras antiguas**
**Datos**:
- 1 proveedor activo
- Última compra hace 120 días

**Resultado esperado**:
- SupplierPerformancePanel: Alerta amarilla "Sin compras hace 120 días"
- SupplierAlertsPanel: 1 alerta WARNING "Sin compras hace 120 días"

### **Escenario 4: Alta concentración de compras**
**Datos**:
- 10 proveedores
- Top 3 concentran 75% de las compras

**Resultado esperado**:
- SuppliersStatsCard: Alerta "75.0% de las compras concentradas en 3 proveedores"
- SupplierAlertsPanel: 1 alerta WARNING "Alta concentración - Riesgo de dependencia"

### **Escenario 5: Proveedor con tendencia positiva**
**Datos**:
- Proveedor con 2 compras mes anterior
- 5 compras último mes

**Resultado esperado**:
- SupplierPerformancePanel:
  - Último mes: 5 compras
  - Mes anterior: 2 compras
  - Tendencia: ↑ 150% (verde)

---

## 🐛 **Problemas Conocidos y Soluciones**

### **Backend no inicia**
```
Error: Aplicación ya corriendo en puerto 8081
```
**Solución**: Matar proceso o cambiar puerto:
```cmd
gradlew.bat bootRun --args="--spring.profiles.active=dev --server.port=8082"
```

### **Frontend no carga datos**
```
Error: Network Error / 401 Unauthorized
```
**Solución**:
1. Verificar backend corriendo en http://localhost:8081
2. Verificar token de autenticación válido
3. Revisar consola del navegador para errores CORS

### **Alertas no aparecen**
**Posibles causas**:
- No hay proveedores registrados → Crear proveedores
- No hay compras registradas → Crear compras en módulo Compras
- Criterios de alerta no se cumplen → Revisar lógica en `SupplierService.java`

### **Métricas en 0**
**Causas**:
- Proveedor sin compras → Normal, verificar con otro proveedor
- `supplierId` en Purchase no coincide → Revisar base de datos

---

## 📊 **Datos de Prueba Sugeridos**

### **Crear Proveedores**
```
1. Distribuidora Norte (Activo)
   - Comuna: Santiago
   - Actividad: Distribución

2. Logística Express (Activo)
   - Comuna: Providencia
   - Actividad: Transporte

3. Suministros Chile (Inactivo)
   - Comuna: Las Condes
   - Actividad: Retail
```

### **Crear Compras**
```
1. Distribuidora Norte
   - Última compra: Hace 10 días
   - Total: $5,000,000

2. Logística Express
   - Última compra: Hace 150 días
   - Total: $2,000,000

3. Distribuidora Norte
   - Última compra: Hace 30 días
   - Total: $3,500,000
```

**Resultado esperado con estos datos**:
- Total proveedores: 3 (2 activos, 1 inactivo)
- Alertas:
  - WARNING: "Sin compras hace 150 días" (Logística Express)
  - INFO: "Proveedor inactivo con datos..." (Suministros Chile)
- Concentración: ~70% en Distribuidora Norte → posible alerta

---

## ✅ **Criterios de Aceptación**

### **Funcionalidad**
- [ ] Todos los endpoints retornan 200 OK con datos válidos
- [ ] Frontend carga sin errores en consola
- [ ] Todos los componentes se visualizan correctamente
- [ ] Alertas se generan según criterios definidos
- [ ] Métricas se calculan correctamente
- [ ] CSV import/export sigue funcionando

### **UX/UI**
- [ ] Dark theme consistente en todos los componentes
- [ ] Responsive design funciona en móvil
- [ ] Badges, colores y iconos correctos
- [ ] Sin errores TypeScript/Java
- [ ] Tiempos de carga < 2 segundos

### **Integración**
- [ ] Backend y frontend se comunican correctamente
- [ ] Datos de Purchase se integran con Supplier
- [ ] Queries optimizadas (sin N+1 problems)
- [ ] Fallbacks offline funcionan

---

## 🎯 **Resultado Esperado Final**

Al terminar el testing, deberías poder:

1. ✅ Ver dashboard de proveedores modernizado
2. ✅ Consultar métricas de cualquier proveedor
3. ✅ Recibir alertas automáticas de riesgos
4. ✅ Comparar desempeño mes a mes
5. ✅ Mantener funcionalidad CSV intacta
6. ✅ Navegar sin errores en consola

**Si todo funciona**: Sprint 1 completado exitosamente 🎉

**Si hay problemas**: Revisar logs del backend y consola del navegador para detalles.

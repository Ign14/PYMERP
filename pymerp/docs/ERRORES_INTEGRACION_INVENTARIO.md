# 🔍 Errores de Integración Backend-BD en Página de Inventario

**Fecha de Análisis**: 3 de noviembre de 2025  
**Módulo**: Inventario (`/app/inventory`)

---

## ❌ **Problemas Identificados**

### 1. **Inconsistencia en Rutas de API - LocationStockSummary**

#### **Problema**:
El frontend llama a una ruta incorrecta para obtener el resumen de stock por ubicación.

#### **Frontend** (`ui/src/services/client.ts` línea 3302):
```typescript
const { data } = await api.get<LocationStockSummary[]>("/api/locations/stock-summary");
```

#### **Backend** (`LocationController.java` línea 53):
```java
@GetMapping("/stock-summary")  // ✅ CORRECTO
@RequestMapping("/api/locations")  // Ruta base
```

#### **Estado**: ✅ **RUTA CORRECTA** - No hay error aquí

---

### 2. **Posible Error en Tipo de Datos - Product**

#### **Problema**:
Los componentes de análisis (`InventoryRotationAnalysis.tsx`) intentan acceder a propiedades `cost` y `price` que no existen en el tipo `Product`.

#### **Ubicación**: 
- `InventoryRotationAnalysis.tsx` líneas 20-21
- Workaround actual: `@ts-ignore` (solución temporal)

#### **Código Problemático**:
```typescript
// @ts-ignore - cost/price existen pero no en tipo base
const cost = Number(p.cost ?? 0);
// @ts-ignore
const price = Number(p.price ?? 0);
```

#### **Causa Raíz**:
El tipo `Product` en `client.ts` probablemente no incluye estas propiedades, pero el backend SÍ las devuelve.

#### **Solución Requerida**:
Verificar/actualizar la interfaz `Product` en `client.ts`:
```typescript
export type Product = {
  id: string;
  sku: string;
  name: string;
  active: boolean;
  category?: string;
  stock?: number;
  criticalStock?: number;
  currentPrice?: number;  // ← Agregar si falta
  cost?: number;          // ← Agregar si falta
  price?: number;         // ← Agregar si falta
  imageUrl?: string;
  // ... otros campos
};
```

---

### 3. **Datos Simulados en Componentes de Análisis**

#### **Problema**:
Los nuevos componentes de análisis usan datos simulados en lugar de consumir endpoints reales del backend.

#### **Componentes Afectados**:

##### **A. InventoryRotationAnalysis.tsx**
```typescript
// Línea 23: Rotación SIMULADA
const rotation = Math.random() * 20; // ⚠️ Ventas simuladas
```

**Endpoint Faltante en Backend**:
```java
// NECESARIO: GET /api/v1/inventory/rotation-metrics
// Debería retornar:
{
  "productId": "uuid",
  "salesLast30Days": 45,
  "avgDailySales": 1.5,
  "rotationRate": 12.3
}
```

##### **B. InventoryValuationChart.tsx**
```typescript
// Línea 17: Evolución histórica SIMULADA
const historicalData = useMemo(() => {
  return [
    { month: "Hace 3m", value: totalValue * 0.75 },  // ⚠️ Simulado
    { month: "Hace 2m", value: totalValue * 0.85 },
    // ...
  ];
}, [totalValue]);
```

**Endpoint Faltante en Backend**:
```java
// NECESARIO: GET /api/v1/inventory/valuation-history?months=4
// Debería retornar:
[
  { "month": "2025-08", "totalValue": 125000 },
  { "month": "2025-09", "totalValue": 142000 },
  // ...
]
```

##### **C. InventoryReplenishmentPanel.tsx**
```typescript
// Líneas 14-17: Consumo diario SIMULADO
const dailyConsumption = Math.random() * 5 + 1;  // ⚠️ Simulado
const leadTimeDays = 7; // Simulado
const safetyStock = dailyConsumption * 3;
const reorderPoint = dailyConsumption * leadTimeDays + safetyStock;
```

**Endpoint Faltante en Backend**:
```java
// NECESARIO: GET /api/v1/inventory/replenishment-analysis
// Debería retornar:
[
  {
    "productId": "uuid",
    "avgDailyConsumption": 4.2,
    "leadTimeDays": 7,
    "safetyStockDays": 3,
    "reorderPoint": 33.6,
    "suggestedOrderQty": 126
  }
]
```

##### **D. InventoryEfficiencyMetrics.tsx**
```typescript
// Líneas 31-35: Métricas SIMULADAS
const dailyConsumption = 15; // Simulado
const adjustments = 12; // Simulado
const totalMovements = 150; // Simulado
```

**Endpoint Faltante en Backend**:
```java
// NECESARIO: GET /api/v1/inventory/efficiency-metrics
// Debería retornar:
{
  "daysOfCoverage": 28.5,
  "stockOutRate": 8.2,
  "inventoryAccuracy": 96.3,
  "monthlyStorageCost": 4500,
  "totalMovementsLast30Days": 247,
  "adjustmentsLast30Days": 15
}
```

---

## 🔧 **Correcciones Prioritarias**

### **Prioridad ALTA** 🔴

#### 1. Actualizar Tipo `Product` en Frontend
**Archivo**: `ui/src/services/client.ts`

Verificar que incluya:
```typescript
cost?: number;
price?: number;
```

Si faltan, buscar en backend el DTO correcto y actualizar.

---

### **Prioridad MEDIA** 🟡

#### 2. Crear Endpoints de Análisis en Backend

Estos endpoints mejorarían significativamente la precisión del módulo, pero NO son críticos para el funcionamiento básico.

**Archivo**: `backend/src/main/java/com/datakomerz/pymes/inventory/InventoryController.java`

```java
@GetMapping("/rotation-metrics")
public List<ProductRotationMetric> getRotationMetrics() {
  return inventoryService.calculateRotationMetrics();
}

@GetMapping("/valuation-history")
public List<ValuationHistoryPoint> getValuationHistory(
    @RequestParam(defaultValue = "4") int months) {
  return inventoryService.getValuationHistory(months);
}

@GetMapping("/replenishment-analysis")
public List<ReplenishmentSuggestion> getReplenishmentAnalysis() {
  return inventoryService.analyzeReplenishment();
}

@GetMapping("/efficiency-metrics")
public EfficiencyMetrics getEfficiencyMetrics() {
  return inventoryService.calculateEfficiencyMetrics();
}
```

---

### **Prioridad BAJA** 🟢

#### 3. Mejorar Mensajes de Error en Frontend

Actualmente, si el backend no responde, el frontend usa fallback silencioso. Sería bueno mostrar alertas visuales.

**Ejemplo**:
```typescript
const rotationQuery = useQuery({
  queryKey: ["inventory-rotation"],
  queryFn: () => api.get("/v1/inventory/rotation-metrics"),
  retry: false,
  onError: (error) => {
    console.error("Error al cargar métricas de rotación:", error);
    // Mostrar toast de advertencia al usuario
  }
});
```

---

## ✅ **Integraciones que SÍ Funcionan Correctamente**

1. ✅ **getInventorySummary()** → `/api/v1/inventory/summary`
2. ✅ **listInventoryAlerts()** → `/api/v1/inventory/alerts`
3. ✅ **getLocationStockSummary()** → `/api/locations/stock-summary`
4. ✅ **listProducts()** → `/api/v1/products`
5. ✅ **Ventas → Inventario**: Consume FIFO automático
6. ✅ **Compras → Inventario**: Crea lotes y movimientos

---

## 📊 **Resumen de Estado**

| Componente | Backend API | Frontend Query | Estado | Prioridad |
|-----------|-------------|----------------|--------|-----------|
| **InventoryPage KPIs** | ✅ `/v1/inventory/summary` | ✅ getInventorySummary | 🟢 Funcional | - |
| **Stock Crítico** | ✅ `/v1/inventory/alerts` | ✅ listInventoryAlerts | 🟢 Funcional | - |
| **ProductsCard** | ✅ `/v1/products` | ✅ listProducts | 🟡 Tipo incompleto | 🔴 Alta |
| **LocationsCard** | ✅ `/locations/stock-summary` | ✅ getLocationStockSummary | 🟢 Funcional | - |
| **ServicesCard** | ✅ `/services` | ✅ listServices | 🟢 Funcional | - |
| **RotationAnalysis** | ❌ Falta endpoint | ⚠️ Datos simulados | 🟡 Parcial | 🟡 Media |
| **ValuationChart** | ❌ Falta endpoint | ⚠️ Datos simulados | 🟡 Parcial | 🟡 Media |
| **ReplenishmentPanel** | ❌ Falta endpoint | ⚠️ Datos simulados | 🟡 Parcial | 🟡 Media |
| **EfficiencyMetrics** | ❌ Falta endpoint | ⚠️ Datos simulados | 🟡 Parcial | 🟡 Media |

---

## 🚀 **Próximos Pasos Recomendados**

1. **Inmediato** (Hoy):
   - [ ] Verificar tipo `Product` y agregar `cost`/`price` si faltan
   - [ ] Quitar `@ts-ignore` de InventoryRotationAnalysis

2. **Corto Plazo** (Esta semana):
   - [ ] Crear endpoint `/v1/inventory/rotation-metrics` en backend
   - [ ] Implementar cálculo de rotación basado en ventas reales (últimos 30/60 días)

3. **Mediano Plazo** (Próximas 2 semanas):
   - [ ] Implementar endpoints de valuación histórica
   - [ ] Implementar análisis de reabastecimiento con consumo real
   - [ ] Implementar métricas de eficiencia con datos reales

---

**Conclusión**: La integración básica funciona correctamente. Los componentes de análisis avanzado usan datos simulados como placeholder hasta que se implementen los endpoints correspondientes en el backend.

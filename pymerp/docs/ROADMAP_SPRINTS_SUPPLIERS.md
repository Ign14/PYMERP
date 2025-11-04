# 🚀 Roadmap de Sprints - Módulo Proveedores

**Fecha**: 3 de noviembre de 2025  
**Estado Sprint 1**: ✅ COMPLETADO (8/8 tareas)  
**Siguiente**: Sprint 2, 3 o 4 (a elegir según prioridad)

---

## ✅ **SPRINT 1 - COMPLETADO** (Mejoras de Alto Impacto)

### **Implementado**:
1. ✅ Análisis de estructura de datos (Supplier-Purchase)
2. ✅ Endpoint `GET /api/v1/suppliers/{id}/metrics`
3. ✅ Endpoint `GET /api/v1/suppliers/alerts`
4. ✅ SuppliersStatsCard modernizado (KPIs + alertas)
5. ✅ SupplierPerformancePanel (análisis de desempeño)
6. ✅ SupplierAlertsPanel (sistema de alertas proactivas)
7. ✅ Integración en SuppliersPage (layout 2x2)
8. ✅ Documentación completa

### **Resultados**:
- 3 nuevos archivos backend (DTOs + Service)
- 2 nuevos endpoints REST
- 3 componentes frontend (1 modernizado + 2 nuevos)
- Dark theme consistente
- Sin breaking changes

---

## 📋 **SPRINT 2 - Análisis Avanzado** (Impacto Medio-Alto)

**Duración estimada**: 1-2 horas  
**Prioridad**: ⭐⭐⭐⭐ (Alta)

### **Objetivos**:
Agregar capacidades de análisis comparativo y ranking de proveedores para mejorar la toma de decisiones.

### **Tareas (4)**:

#### **2.1 - Ranking de Proveedores**
**Componente**: `SuppliersRanking.tsx`

**Funcionalidad**:
- Ranking por criterios múltiples:
  * Por volumen de compras (últimos 12 meses)
  * Por confiabilidad (frecuencia + regularidad)
  * Por competitividad de precios
  * Por relación calidad-precio (valor)
  
- Visualización:
  ```tsx
  <div className="ranking-list">
    <div className="ranking-item rank-gold">
      🥇 #1 - Distribuidora Norte
      Score: 95.2 pts | Compras: $12M | Confiabilidad: 98%
    </div>
    <div className="ranking-item rank-silver">
      🥈 #2 - Logística Express
      Score: 87.5 pts | Compras: $8M | Confiabilidad: 92%
    </div>
  </div>
  ```

**Backend necesario**:
```java
@GetMapping("/ranking")
public List<SupplierRankingDTO> getRanking(
  @RequestParam(defaultValue = "volume") String criteria
) {
  // criteria: volume, reliability, price, value
}
```

**Estimación**: 30 minutos

---

#### **2.2 - Análisis de Concentración de Riesgo**
**Componente**: `SupplierRiskAnalysis.tsx`

**Funcionalidad**:
- Análisis ABC de proveedores:
  * A: 80% del volumen (críticos)
  * B: 15% del volumen (importantes)
  * C: 5% del volumen (ocasionales)
  
- Visualización con gráfico de dona:
  ```
  [Gráfico circular]
  - A: 3 proveedores (80%) - Rojo
  - B: 5 proveedores (15%) - Amarillo
  - C: 12 proveedores (5%) - Gris
  ```

- Alertas:
  * "⚠️ 3 productos con proveedor único"
  * "🔴 80% de compras en 2 proveedores - Alto riesgo"
  
**Backend necesario**:
```java
@GetMapping("/risk-analysis")
public SupplierRiskAnalysisDTO getRiskAnalysis() {
  return SupplierRiskAnalysisDTO.builder()
    .categoryA(List<Supplier>) // Top 80%
    .categoryB(List<Supplier>) // Next 15%
    .categoryC(List<Supplier>) // Last 5%
    .singleSourceProducts(List<Product>)
    .concentrationIndex(Double) // Herfindahl
    .build();
}
```

**Estimación**: 45 minutos

---

#### **2.3 - Historial de Precios**
**Componente**: `SupplierPriceHistory.tsx`

**Funcionalidad**:
- Selector: Proveedor + Producto
- Gráfico de líneas de evolución de precios
- Comparación entre proveedores para el mismo producto
- Indicadores:
  * Precio actual
  * Precio promedio histórico
  * Máximo/mínimo últimos 12 meses
  * Tendencia (↑ subiendo, ↓ bajando, → estable)
  
- Alertas:
  * "⚠️ Precio subió 15% en 3 meses"
  * "💡 Proveedor B ofrece 8% más barato para este producto"

**Backend necesario**:
```java
@GetMapping("/{supplierId}/price-history")
public List<PriceHistoryDTO> getPriceHistory(
  @PathVariable UUID supplierId,
  @RequestParam(required = false) UUID productId
) {
  // Retorna historial de precios por mes
}
```

**Estimación**: 45 minutos

---

#### **2.4 - Comparador de Proveedores**
**Componente**: `SupplierComparison.tsx`

**Funcionalidad**:
- Selección múltiple de proveedores (2-4)
- Tabla comparativa:

| Métrica | Proveedor A | Proveedor B | Proveedor C |
|---------|------------|-------------|-------------|
| Total Compras | 15 | 12 | 8 |
| Monto Total | $12M | $8M | $5M |
| AOV | $800k | $666k | $625k |
| Última Compra | Hace 5 días | Hace 30 días | Hace 90 días |
| Plazo Pago | 30 días | 45 días | 60 días |

- Ganador por categoría con icono 🏆
- Recomendación: "Proveedor A es mejor en 4/5 métricas"

**Backend**: Usar endpoints existentes (`/metrics`)

**Estimación**: 30 minutos

---

### **Integración en SuppliersPage**:
```tsx
<section className="responsive-grid mt-6">
  <div className="card"><SuppliersRanking /></div>
  <div className="card"><SupplierRiskAnalysis /></div>
</section>

<section className="responsive-grid mt-6">
  <div className="card"><SupplierPriceHistory /></div>
  <div className="card"><SupplierComparison /></div>
</section>
```

**Resultado**: Layout 4x2 (8 cards totales)

---

## 📋 **SPRINT 3 - Gestión de Contratos** (Impacto Medio)

**Duración estimada**: 2-3 horas  
**Prioridad**: ⭐⭐⭐ (Media)

### **Objetivos**:
Implementar gestión completa de contratos, reemplazando el mockdata existente.

### **Tareas (4)**:

#### **3.1 - Modelo de Contratos**
**Backend**: Nueva entidad `SupplierContract`

```java
@Entity
@Table(name = "supplier_contracts")
public class SupplierContract {
  @Id UUID id;
  @Column(name = "supplier_id") UUID supplierId;
  String contractNumber;
  LocalDate startDate;
  LocalDate endDate;
  BigDecimal committedAmount;
  String specialConditions; // JSON o texto
  String documentUrl; // S3 path del PDF
  String status; // ACTIVE, EXPIRING_SOON, EXPIRED, RENEWED
  LocalDate nextReviewDate;
  OffsetDateTime createdAt;
}
```

**Endpoints**:
```java
GET    /api/v1/suppliers/{id}/contracts
POST   /api/v1/suppliers/{id}/contracts
PUT    /api/v1/suppliers/{id}/contracts/{contractId}
DELETE /api/v1/suppliers/{id}/contracts/{contractId}
POST   /api/v1/suppliers/{id}/contracts/{contractId}/upload-document
```

**Estimación**: 60 minutos

---

#### **3.2 - Panel de Contratos**
**Componente**: `SupplierContractsPanel.tsx`

**Funcionalidad**:
- Tabla de contratos por proveedor:
  * N° contrato
  * Vigencia (desde/hasta)
  * Monto comprometido
  * Estado (badge con color)
  * Próxima revisión
  * Acciones (Ver PDF, Editar, Renovar)
  
- Estados con colores:
  * 🟢 Activo (>30 días para vencer)
  * 🟡 Por vencer (<30 días)
  * 🔴 Vencido
  * 🔵 Renovado

- Dialog para crear/editar contrato
- Upload de PDF (escaneo del contrato físico)

**Estimación**: 45 minutos

---

#### **3.3 - Alertas de Contratos**
**Modificar**: `SupplierService.java` (lógica de alertas)

**Nuevas alertas**:
- CONTRACT_EXPIRING_SOON (30 días antes)
- CONTRACT_EXPIRED (ya venció)
- CONTRACT_NO_DOCUMENT (sin PDF adjunto)
- CONTRACT_HIGH_VALUE (>$50M sin revisión en 6 meses)

**Integrar en**: `SupplierAlertsPanel.tsx`

**Estimación**: 30 minutos

---

#### **3.4 - Dashboard de Contratos**
**Componente**: `ContractsDashboard.tsx`

**Funcionalidad**:
- KPIs:
  * Total contratos activos
  * Monto total comprometido
  * Contratos por vencer (próximos 30 días)
  * Contratos vencidos sin renovar
  
- Timeline de vencimientos (próximos 12 meses)
- Gráfico de montos por proveedor

**Estimación**: 45 minutos

---

## 📋 **SPRINT 4 - Reportes y Exportación** (Impacto Bajo)

**Duración estimada**: 2-3 horas  
**Prioridad**: ⭐⭐ (Baja - Nice to have)

### **Objetivos**:
Exportar análisis en formatos profesionales (PDF/Excel) para compartir con gerencia.

### **Tareas (4)**:

#### **4.1 - Reporte de Desempeño (PDF)**
**Backend**: Generación de PDF con Apache POI o iText

**Contenido**:
- Por proveedor seleccionado:
  * KPIs principales
  * Gráficos de tendencia
  * Comparación vs mes anterior
  * Recomendaciones
  
**Endpoint**:
```java
GET /api/v1/suppliers/{id}/reports/performance?format=pdf
```

**Estimación**: 60 minutos

---

#### **4.2 - Reporte de Compras (Excel)**
**Backend**: Generación de Excel con Apache POI

**Contenido**:
- Detalle de todas las compras por proveedor
- Agrupado por mes/trimestre
- Subtotales y totales
- Filtros aplicados

**Endpoint**:
```java
GET /api/v1/suppliers/{id}/reports/purchases?format=xlsx&from={date}&to={date}
```

**Estimación**: 45 minutos

---

#### **4.3 - Análisis de Riesgo (PDF)**
**Backend**: PDF con análisis completo

**Contenido**:
- Concentración de compras
- Proveedores críticos (categoría A)
- Productos con proveedor único
- Recomendaciones de diversificación
- Plan de acción sugerido

**Endpoint**:
```java
GET /api/v1/suppliers/reports/risk-analysis?format=pdf
```

**Estimación**: 45 minutos

---

#### **4.4 - Panel de Reportes**
**Componente**: `SupplierReportsPanel.tsx`

**Funcionalidad**:
- Selector de tipo de reporte:
  * Desempeño por proveedor (PDF)
  * Compras detalladas (Excel)
  * Análisis de riesgo (PDF)
  * Estado de contratos (Excel)
  
- Filtros:
  * Rango de fechas
  * Proveedor(es)
  * Formato (PDF/Excel)
  
- Botón "Generar Reporte" → Descarga archivo
- Historial de reportes generados

**Estimación**: 30 minutos

---

## 🎯 **Recomendación de Priorización**

### **Opción A: Máximo Valor de Negocio** 🚀
```
Sprint 1 (HECHO) → Sprint 2 → Sprint 3
```
**Razón**: Sprint 2 agrega análisis crítico (ranking, riesgo), Sprint 3 gestiona contratos (muy solicitado)

### **Opción B: Rápida Iteración** ⚡
```
Sprint 1 (HECHO) → Sprint 3 → Sprint 2
```
**Razón**: Sprint 3 completa funcionalidad existente (contratos mockdata), Sprint 2 agrega analytics

### **Opción C: Datos Primero** 📊
```
Sprint 1 (HECHO) → Sprint 2 → Sprint 4
```
**Razón**: Sprint 2 genera insights, Sprint 4 los exporta para compartir

---

## 📊 **Matriz de Decisión**

| Sprint | Impacto | Esfuerzo | Complejidad | Dependencias | ROI |
|--------|---------|----------|-------------|--------------|-----|
| Sprint 1 | ⭐⭐⭐⭐⭐ | 2h | Media | Ninguna | Muy Alto |
| Sprint 2 | ⭐⭐⭐⭐ | 2h | Media | Sprint 1 | Alto |
| Sprint 3 | ⭐⭐⭐ | 3h | Alta | Sprint 1 | Medio |
| Sprint 4 | ⭐⭐ | 3h | Alta | Sprint 2 | Bajo |

---

## ✅ **Mi Recomendación Personal**

### **IMPLEMENTAR SPRINT 2 AHORA** 🎯

**Razones**:
1. **Complementa Sprint 1**: Agrega análisis que los usuarios ya están esperando
2. **Bajo esfuerzo**: 2 horas vs 3 horas de Sprint 3
3. **Alto valor**: Ranking y análisis de riesgo son críticos para decisiones
4. **Sin DB changes**: No requiere migraciones ni nuevas tablas
5. **Reutiliza datos**: Usa Purchase data que ya está integrado

**Orden sugerido de tareas**:
1. 2.1 Ranking (30 min) - Quick win
2. 2.4 Comparador (30 min) - Reutiliza `/metrics`
3. 2.2 Análisis de Riesgo (45 min) - Valor estratégico
4. 2.3 Historial de Precios (45 min) - Requiere más backend

**Después de Sprint 2**:
- Sprint 3 (si hay budget de tiempo y contratos son prioritarios)
- Sprint 4 (solo si hay necesidad de exportar reportes)

---

## 🚀 **¿Qué Prefieres?**

**Opción 1**: Implementar Sprint 2 completo ahora (2 horas)  
**Opción 2**: Implementar Sprint 3 completo ahora (3 horas)  
**Opción 3**: Solo las tareas de mayor valor de Sprint 2 (2.1 + 2.2) (1 hora)  
**Opción 4**: Mezclar: 2.1 Ranking + 3.2 Panel de Contratos (1.5 horas)  
**Opción 5**: Pausar y hacer testing exhaustivo de Sprint 1 primero

---

**Estado Actual**:
- ✅ Sprint 1: 100% completado
- ⏳ Backend iniciando: Esperando confirmación
- 📝 Documentación: 3 archivos listos
- 🎯 Listo para siguiente sprint

**¿Cuál sprint quieres implementar?** 🚀

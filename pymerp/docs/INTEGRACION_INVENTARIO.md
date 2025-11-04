# 📦 Integración Inventario ↔ Ventas/Compras

## ✅ Estado Actual (Implementaciones Existentes)

### 1. **Compras → Inventario** ✅ IMPLEMENTADO
**Ubicación**: `PurchaseService.java` líneas 100-122

Cuando se crea una compra (`create()` o `createWithFile()`):
- ✅ Si el item es un **producto**, automáticamente:
  - Crea un nuevo `InventoryLot` con la cantidad comprada
  - Registra un `InventoryMovement` tipo `PURCHASE_IN`
  - Establece costo unitario, fecha fabricación/vencimiento, ubicación
  
- ✅ Si el item es un **servicio**:
  - Actualiza `lastPurchaseDate` en la tabla `services`

```java
// EJEMPLO: Fragmento de PurchaseService.create()
if (itemReq.isProduct()) {
  var lot = new InventoryLot();
  lot.setCompanyId(companyId);
  lot.setProductId(itemReq.productId());
  lot.setQtyAvailable(itemReq.qty()); // ← Stock aumenta automáticamente
  lot.setCostUnit(itemReq.unitCost());
  lot.setExpDate(itemReq.expDate());
  lot.setLocationId(itemReq.locationId());
  lots.save(lot);

  var movement = new InventoryMovement();
  movement.setType("PURCHASE_IN"); // ← Rastreable en auditoría
  movement.setQty(itemReq.qty());
  movements.save(movement);
}
```

---

### 2. **Ventas → Inventario** ✅ IMPLEMENTADO
**Ubicación**: `SalesService.java` línea 102

Cuando se crea una venta (`create()`):
- ✅ Por cada item vendido, llama automáticamente a:
  ```java
  inventory.consumeFIFO(sale.getId(), item.productId(), item.qty(), 
                        item.locationId(), item.lotId());
  ```

**Lógica FIFO en `InventoryService.consumeFIFO()`** (líneas 59-88):
1. Si se especifica `lotId` → consume del lote específico
2. Si se especifica `locationId` → consume del lote más antiguo en esa ubicación
3. Si no se especifica nada → **FIFO automático** por fecha de vencimiento (`expDate`)

```java
// EJEMPLO: Lógica FIFO en InventoryService
candidates = lots.findByCompanyIdAndProductIdAndQtyAvailableGreaterThanOrderByExpDateAscCreatedAtAsc(
    companyId, productId, BigDecimal.ZERO);

for (var lot : candidates) {
  var take = lot.getQtyAvailable().min(remaining); // Tomar lo que se pueda
  lot.setQtyAvailable(lot.getQtyAvailable().subtract(take)); // ← Stock disminuye
  lots.save(lot);

  // Registrar movimiento de salida
  var movement = new InventoryMovement();
  movement.setType("SALE_OUT");
  movements.save(movement);
}
```

---

## 📊 Diagrama de Flujo

```
┌─────────────────────────────────────────────────────────────────┐
│                     FLUJO DE INVENTARIO                         │
└─────────────────────────────────────────────────────────────────┘

COMPRA RECIBIDA                           VENTA EMITIDA
      │                                         │
      ├─> PurchaseService.create()             ├─> SalesService.create()
      │                                         │
      ├─> Por cada ítem de producto:           ├─> Por cada ítem de venta:
      │   • Crear InventoryLot                 │   • inventory.consumeFIFO()
      │   • qtyAvailable = qty comprada        │   • Buscar lotes FIFO (expDate ASC)
      │   • costUnit = precio compra           │   • Reducir qtyAvailable
      │   • locationId (si aplica)             │   • Crear SaleLotAllocation
      │   • expDate/mfgDate                    │   • Movimiento "SALE_OUT"
      │                                         │
      ├─> Crear InventoryMovement              └─> Si no hay stock suficiente:
      │   • type = "PURCHASE_IN"                   → Lanzar excepción
      │   • refType = "PURCHASE"                   → Transacción rollback
      │   • qty = cantidad ingresada
      │
      └─> Stock AUMENTA ✅                     Stock DISMINUYE ✅
```

---

## 🎯 Puntos de Validación Actuales

### En Compras:
- ✅ **Transaccional**: Si falla guardar lote/movimiento, rollback completo
- ✅ **Diferencia productos/servicios**: Solo productos crean lotes
- ✅ **Trazabilidad**: Cada lote tiene `purchaseItemId` para auditoría
- ✅ **Ubicaciones**: Soporta asignar `locationId` al lote

### En Ventas:
- ✅ **FIFO inteligente**: Prioriza lotes próximos a vencer
- ✅ **Multi-lote**: Puede consumir de varios lotes si es necesario
- ✅ **Asignación granular**: Registra `SaleLotAllocation` por lote usado
- ✅ **Control de stock**: Si no hay suficiente qty, transacción falla

---

## 🚀 Mejoras Sugeridas (Opcionales)

### A. **Dashboard de Sincronización** (UI)
Crear panel en InventoryPage que muestre:
- Últimas 10 compras → impacto en stock
- Últimas 10 ventas → consumo FIFO
- Alertas de discrepancias (si las hubiera)

### B. **Webhook de Eventos** (Backend)
Emitir eventos para integraciones externas:
```java
@EventListener
public void onPurchaseReceived(PurchaseReceivedEvent event) {
  // Notificar a sistema externo de ERP
}

@EventListener
public void onSaleCompleted(SaleCompletedEvent event) {
  // Actualizar inventario en marketplace
}
```

### C. **Reportes de Rotación Real** (Backend)
Calcular rotación basada en ventas reales:
```sql
SELECT p.id, p.name, 
       SUM(si.qty) / 30 as avg_daily_sales,
       il.qty_available / (SUM(si.qty) / 30) as days_coverage
FROM sale_items si
JOIN products p ON si.product_id = p.id
JOIN inventory_lots il ON il.product_id = p.id
WHERE si.created_at > NOW() - INTERVAL '30 days'
GROUP BY p.id, il.qty_available
```

### D. **Validación Preventiva** (Frontend)
En formulario de ventas, mostrar:
- Stock disponible antes de confirmar
- Advertencia si qty solicitada > stock
- Sugerencia de productos alternativos

---

## 📝 Conclusión

**Las integraciones Ventas ↔ Inventario y Compras ↔ Inventario YA ESTÁN IMPLEMENTADAS** ✅

El sistema actual:
- ✅ Aumenta stock automáticamente al recibir compras
- ✅ Disminuye stock automáticamente al emitir ventas (FIFO)
- ✅ Registra movimientos auditables
- ✅ Maneja transacciones atómicas
- ✅ Soporta ubicaciones y lotes específicos

**No se requieren cambios en backend para las tareas 3.3 y 3.4** - Solo documentación/visualización en UI.

---

**Fecha**: 3 de noviembre de 2025  
**Autor**: Sistema PyMERP - Análisis de integración

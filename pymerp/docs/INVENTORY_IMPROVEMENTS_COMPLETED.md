# Mejoras de Inventario - Completadas

## Resumen de Cambios

Se han completado todas las mejoras solicitadas para la sección de inventario:

### 1. ✅ LocationsCard - Tarjeta de Ubicaciones

**Backend:**
- `LocationStockDTO.java`: Nuevo DTO con información de stock por ubicación
  - Incluye nombre de ubicación, tipo, y lista de productos con cantidades
- `LocationController.java`: Endpoint `GET /api/locations/stock-summary`
- `LocationService.java`: Método `getLocationStockSummary()`
- `InventoryLotRepository.java`: Método `findByCompanyIdAndLocationId()`

**Frontend:**
- `LocationsCard.tsx`: Componente completo con:
  - Grid de 2 columnas: lista de ubicaciones + panel de detalles
  - Filtro por tipo de ubicación (Almacén, Estante, Depósito)
  - Tabla de productos con cantidades por ubicación
  - Diálogo para crear/editar ubicaciones
- `client.ts`: Tipos y funciones para obtener resumen de stock

### 2. ✅ Service Entity - Entidad de Servicios

**Backend:**
- `Service.java`: Nueva entidad JPA para servicios
  - Campos: id, companyId, code, name, description, lastPurchaseDate, active, timestamps
  - Constraint único en company_id + code
- `V22__create_services.sql`: Migración Flyway
  - Tabla services con índices en company_id, active, last_purchase_date
- `ServiceRepository.java`: Repositorio JPA con métodos CRUD
- `ServiceService.java`: Lógica de negocio con CompanyContext
- `ServiceController.java`: Endpoints REST completos
- `ServiceReq.java`: DTO para requests

**Frontend:**
- `ServicesCard.tsx`: Componente completo con:
  - Tabla de servicios con código, nombre, última compra, estado
  - Filtros por activo/inactivo
  - Diálogo para crear/editar servicios
  - Formateo de fecha de última compra
- `client.ts`: Tipos ServiceDTO, ServicePayload y funciones CRUD

### 3. ✅ Purchase Items - Soporte para Productos y Servicios

**Backend:**
- `PurchaseItemReq.java`: Modificado para soportar productId O serviceId
  - Ambos campos son opcionales
  - Validación en constructor compacto: exactamente uno debe estar presente
  - Métodos helper: `isProduct()`, `isService()`
- `PurchaseService.java`: Lógica bifurcada en método `create()`
  - Productos: crean InventoryLot + InventoryMovement
  - Servicios: actualizan lastPurchaseDate en Service entity
  - Inyección de ServiceRepository

**Frontend:**
- `client.ts`: 
  - `PurchaseItemPayload` actualizado con productId y serviceId opcionales
- `PurchaseCreateDialog.tsx`: Actualizado completamente
  - Estado `itemType` para seleccionar entre "product" o "service"
  - Radio buttons para elegir tipo de item
  - Selector condicional de productos o servicios
  - Campos adicionales (vencimiento, ubicación) solo para productos
  - Tabla de items con columna de tipo y badges de colores
  - Función `addItem()` actualizada para manejar ambos tipos
  - Función `resetForm()` actualizada con nuevos estados

### 4. ✅ Integración en InventoryPage

**Frontend:**
- `InventoryPage.tsx`: Integración de LocationsCard y ServicesCard
- `App.css`: Estilos para locations-grid, badges, checkbox-label

## Compilación

- ✅ Backend: BUILD SUCCESSFUL (spotlessApply aplicado)
- ✅ Frontend: BUILD SUCCESSFUL (798 modules, 6.38s)

## Funcionalidades Disponibles

### Ubicaciones
1. Listar ubicaciones filtradas por tipo
2. Ver productos y cantidades por ubicación
3. Crear/editar ubicaciones
4. Eliminar ubicaciones
5. Ver resumen de stock por ubicación

### Servicios
1. Listar servicios activos/inactivos
2. Ver fecha de última compra
3. Crear/editar servicios
4. Eliminar servicios
5. Activar/desactivar servicios

### Compras
1. Registrar compras con productos
2. Registrar compras con servicios
3. Combinar productos y servicios en una misma compra
4. Para productos:
   - Asignar ubicación
   - Registrar fecha de vencimiento
   - Crear lote de inventario
   - Registrar movimiento de inventario
5. Para servicios:
   - Actualizar fecha de última compra
   - No afecta inventario físico

## Estructura de Datos

### LocationStockDTO
```java
record LocationStockDTO(
  String locationId,
  String locationName,
  String locationType,
  List<ProductStock> products
) {
  record ProductStock(
    String productId,
    String productCode,
    String productName,
    int totalQty
  )
}
```

### Service Entity
```java
- id: String (UUID)
- companyId: String
- code: String
- name: String
- description: String
- lastPurchaseDate: LocalDate
- active: Boolean
- createdAt: Instant
- updatedAt: Instant
```

### PurchaseItemPayload
```typescript
{
  productId?: string;    // Opcional, para productos
  serviceId?: string;    // Opcional, para servicios
  qty: number;
  unitCost: number;
  vatRate: number;
  expDate?: string;      // Solo para productos
  locationId?: string;   // Solo para productos
}
```

## Validaciones

1. **PurchaseItemReq**: Exactamente uno de productId o serviceId debe estar presente
2. **Service**: Código único por compañía
3. **LocationStock**: Solo ubicaciones de la compañía actual
4. **CompanyContext**: Todas las operaciones filtradas por compañía

## Testing Recomendado

1. Crear servicios desde ServicesCard
2. Crear ubicaciones desde LocationsCard
3. Crear compra con solo productos
4. Crear compra con solo servicios
5. Crear compra mixta (productos + servicios)
6. Verificar que lastPurchaseDate se actualiza en servicios
7. Verificar que productos crean lotes de inventario
8. Verificar stock por ubicación

## Archivos Modificados/Creados

**Backend (19 archivos):**
- Service.java (nuevo)
- V22__create_services.sql (nuevo)
- ServiceRepository.java (nuevo)
- ServiceService.java (nuevo)
- ServiceController.java (nuevo)
- ServiceReq.java (nuevo)
- LocationStockDTO.java (nuevo)
- LocationService.java (modificado)
- LocationController.java (modificado)
- InventoryLotRepository.java (modificado)
- PurchaseItemReq.java (modificado)
- PurchaseService.java (modificado)

**Frontend (7 archivos):**
- LocationsCard.tsx (nuevo)
- ServicesCard.tsx (nuevo)
- PurchaseCreateDialog.tsx (modificado)
- InventoryPage.tsx (modificado)
- client.ts (modificado)
- App.css (modificado)

## Estado Final

✅ Todas las tareas completadas
✅ Backend compilado sin errores
✅ Frontend compilado sin errores
✅ Migraciones aplicadas (V1-V22)
✅ Código formateado (spotlessApply)
✅ Tipos TypeScript actualizados
✅ Componentes React integrados
✅ Validaciones implementadas
✅ Flujo completo funcional

# 🎯 Prompt para Continuar: Validación y Testing de Ubicaciones/Servicios

## ✅ Estado Actual (Completado por Copilot)

**26 archivos modificados**: +1635/-1667 líneas

### Backend ✅
- **Locations**: Entidad refactorizada con `LocationStatus` (ACTIVE/BLOCKED), `LocationType` (BODEGA/CONTAINER/LOCAL/CAMION/CAMIONETA)
- **Services**: Entidad refactorizada con `category`, `unitPrice`, `ServiceStatus` (ACTIVE/INACTIVE)
- **Eliminados**: `parentLocationId`, `maxCapacity`, `capacityUnit` de Locations
- **Agregados**: `businessName`, `rut`, `status`, auditoría (`created_by`, `updated_by`)
- **API**: CRUD completo en `/api/v1/inventory/locations` y `/api/v1/services`
- **Migración V37**: Creada (reshape de tablas locations + services)

### Frontend ✅
- **InventoryPage**: Botón "+ Nuevo Producto" agregado
- **LocationList.tsx**: Nuevo componente con tabla CRUD
- **LocationFormDialog.tsx**: Modal con validación RUT chileno y code único
- **ServiceList.tsx**: Nuevo componente con filtros y acciones
- **ServiceFormDialog.tsx**: Modal con validación de precio > 0
- **PurchasesPage**: Integración de servicios (auto-fill unitPrice)

### Pendiente ⚠️
- V37 no aplicada en BD (última versión: V36)
- Backend no compilado aún (Gradle running)
- Tests no ejecutados (JAVA_HOME issue)

---

## 🔧 TAREAS INMEDIATAS PARA COPILOT

### 1️⃣ Aplicar Migración V37 y Arrancar Backend

**Acción:**
```bash
# Detener backend actual si está corriendo
# Luego ejecutar tarea de VSCode:
Task: "bootRun:dev (sin debug)"
```

**Objetivo:**
- Flyway debe detectar y aplicar V37 automáticamente
- Backend debe arrancar en puerto 8081 sin errores
- Verificar logs: buscar "Migration V37 -> SUCCESS"

**Criterios de éxito:**
```bash
# Verificar migración aplicada
docker exec pymes_postgres psql -U pymes -d pymes -t -c "SELECT version FROM flyway_schema_history WHERE version = '37';"
# Debe mostrar: 37

# Verificar columnas nuevas en locations
docker exec pymes_postgres psql -U pymes -d pymes -c "\d locations"
# Debe incluir: business_name, rut, status (sin parent_location_id, capacity)

# Verificar columnas nuevas en services
docker exec pymes_postgres psql -U pymes -d pymes -c "\d services"
# Debe incluir: category, unit_price, status (sin active, last_purchase_date)
```

---

### 2️⃣ Testing Manual en UI - Ubicaciones

**URL:** http://localhost:5173/app/inventory

**Casos de prueba:**

#### A) Crear Nueva Ubicación
1. Click en "+ Nueva Ubicación"
2. Completar formulario:
   - **Code**: `LOC-TEST-001` (verificar unicidad)
   - **Name**: `Bodega Central Santiago`
   - **Type**: Seleccionar `BODEGA`
   - **Business Name**: `Empresa Test SpA`
   - **RUT**: `76123456-7` (validar formato chileno)
   - **Description**: `Bodega principal para distribución`
   - **Status**: `ACTIVE`
3. Click "Crear"
4. **Verificar**: 
   - Modal se cierra
   - Nueva ubicación aparece en tabla
   - Mensaje de éxito

#### B) Editar Ubicación Existente
1. Click en acción "Editar" de una ubicación (ej: BOD-001)
2. **Verificar**: ID visible pero deshabilitado
3. Modificar: Name = `Bodega Principal Actualizada`
4. Click "Guardar"
5. **Verificar**: Cambios reflejados en tabla

#### C) Eliminar Ubicación
1. Click en acción "Eliminar"
2. **Verificar**: Confirmación aparece
3. Confirmar eliminación
4. **Verificar**: Ubicación removida de tabla

#### D) Validaciones
- Code duplicado debe mostrar error
- RUT inválido (ej: `12345`) debe mostrar error
- Campos requeridos vacíos deben bloquear submit

---

### 3️⃣ Testing Manual en UI - Servicios

**URL:** http://localhost:5173/app/purchases

**Casos de prueba:**

#### A) Crear Nuevo Servicio
1. Click en "+ Nuevo Servicio"
2. Completar formulario:
   - **Code**: `SRV-TEST-001`
   - **Name**: `Consultoría Técnica`
   - **Description**: `Servicios de asesoría IT`
   - **Category**: `Consultoría`
   - **Unit Price**: `150000`
   - **Status**: `ACTIVE`
3. Click "Crear"
4. **Verificar**: Servicio aparece en lista

#### B) Editar Servicio
1. Click "Editar" en servicio creado
2. **Verificar**: ID read-only
3. Modificar: Unit Price = `175000`
4. Guardar
5. **Verificar**: Precio actualizado

#### C) Integración en Compras
1. Ir a crear nueva compra
2. En sección "Items", seleccionar servicio `SRV-TEST-001`
3. **Verificar**: Unit Price se auto-completa con `175000`
4. Completar cantidad
5. **Verificar**: Total calculado correctamente

#### D) Filtros y Búsqueda
- Filtrar por status: ACTIVE/INACTIVE
- Buscar por código o nombre
- **Verificar**: Resultados correctos

---

### 4️⃣ Verificación de API (Postman/curl)

#### Locations API

```bash
# Listar ubicaciones
curl http://localhost:8081/api/v1/inventory/locations

# Crear ubicación (requiere autenticación)
curl -X POST http://localhost:8081/api/v1/inventory/locations \
  -H "Content-Type: application/json" \
  -d '{
    "code": "API-TEST-001",
    "name": "Ubicación API Test",
    "type": "LOCAL",
    "businessName": "Test SA",
    "rut": "77555666-4",
    "description": "Creado vía API",
    "status": "ACTIVE"
  }'

# Actualizar ubicación
curl -X PUT http://localhost:8081/api/v1/inventory/locations/{id} \
  -H "Content-Type: application/json" \
  -d '{"name": "Nombre Actualizado"}'

# Eliminar ubicación
curl -X DELETE http://localhost:8081/api/v1/inventory/locations/{id}
```

#### Services API

```bash
# Listar servicios
curl http://localhost:8081/api/v1/services

# Crear servicio
curl -X POST http://localhost:8081/api/v1/services \
  -H "Content-Type: application/json" \
  -d '{
    "code": "SRV-API-001",
    "name": "Servicio API Test",
    "category": "Testing",
    "unitPrice": 50000,
    "description": "Servicio de prueba",
    "status": "ACTIVE"
  }'
```

**Verificar respuestas:**
- HTTP 200 OK en GET
- HTTP 201 Created en POST
- HTTP 200 OK en PUT
- HTTP 204 No Content en DELETE
- HTTP 400 Bad Request en validaciones fallidas
- HTTP 401 Unauthorized sin autenticación

---

### 5️⃣ Verificación de Datos en BD

```bash
# Contar ubicaciones
docker exec pymes_postgres psql -U pymes -d pymes -t -c "SELECT COUNT(*) FROM locations;"

# Ver ubicaciones con nuevo esquema
docker exec pymes_postgres psql -U pymes -d pymes -c "SELECT code, name, type, business_name, rut, status FROM locations ORDER BY code;"

# Contar servicios
docker exec pymes_postgres psql -U pymes -d pymes -t -c "SELECT COUNT(*) FROM services;"

# Ver servicios con nuevo esquema
docker exec pymes_postgres psql -U pymes -d pymes -c "SELECT code, name, category, unit_price, status FROM services ORDER BY code;"

# Verificar que columnas legacy fueron eliminadas
docker exec pymes_postgres psql -U pymes -d pymes -c "\d locations" | grep -i "parent_location\|capacity"
# No debe mostrar resultados

docker exec pymes_postgres psql -U pymes -d pymes -c "\d services" | grep -i "active\|last_purchase"
# No debe mostrar resultados
```

---

### 6️⃣ Verificación de Seed Data V34 (Ubicaciones)

V37 actualiza los tipos de ubicaciones en seed data de V34:

```bash
# Verificar que tipos fueron migrados correctamente
docker exec pymes_postgres psql -U pymes -d pymes -c "SELECT code, type FROM locations WHERE code IN ('BOD-001', 'EST-A', 'EST-B', 'CUARENTENA');"

# Resultado esperado:
# BOD-001    | BODEGA
# EST-A      | LOCAL
# EST-B      | LOCAL
# CUARENTENA | BODEGA
```

---

## 🐛 Problemas Conocidos y Soluciones

### Problema 1: Error 500 en Ubicaciones
**Causa:** V37 no aplicada o backend no reiniciado  
**Solución:** Reiniciar backend para aplicar migración

### Problema 2: Validación RUT falla
**Causa:** Formato incorrecto (debe ser `12345678-9`)  
**Solución:** Verificar regex en `LocationFormDialog.tsx`

### Problema 3: Code duplicado no se valida
**Causa:** Backend no verifica unicidad  
**Solución:** Revisar `LocationService.java` método `create()`

### Problema 4: Auto-fill de unitPrice en compras no funciona
**Causa:** Servicio seleccionado no tiene precio configurado  
**Solución:** Asegurar que todos los servicios tienen `unitPrice > 0`

---

## 📋 Checklist de Validación Final

Backend:
- [ ] Compilación exitosa (0 errores)
- [ ] V37 aplicada en BD
- [ ] Tabla `locations`: columnas business_name, rut, status presentes
- [ ] Tabla `locations`: columnas parent_location_id, capacity eliminadas
- [ ] Tabla `services`: columnas category, unit_price, status presentes
- [ ] Tabla `services`: columnas active, last_purchase_date eliminadas
- [ ] Endpoints `/api/v1/inventory/locations` responden 200
- [ ] Endpoints `/api/v1/services` responden 200

Frontend:
- [ ] Botón "+ Nuevo Producto" visible en inventario
- [ ] Modal de productos se abre correctamente
- [ ] Sección "Ubicaciones" carga sin error 500
- [ ] CRUD completo de ubicaciones funciona
- [ ] Validación RUT funciona
- [ ] Code único validado
- [ ] ID no editable en modales
- [ ] Sección "Servicios" visible en compras
- [ ] CRUD completo de servicios funciona
- [ ] Auto-fill de unitPrice funciona en compras
- [ ] Filtros de status funcionan

Integración:
- [ ] Servicio seleccionado en compra llena unit_cost automáticamente
- [ ] Crear/editar ubicación persiste en BD
- [ ] Crear/editar servicio persiste en BD
- [ ] Eliminar ubicación/servicio actualiza UI
- [ ] Sin errores en consola del navegador
- [ ] Sin errores en logs de Spring Boot

---

## 🚀 Prompt Optimizado para Copilot (Copia y Pega)

```
# VALIDACIÓN Y TESTING - Ubicaciones y Servicios

## Contexto
26 archivos modificados por implementación completa de:
- Refactor de entidades Location/Service
- Migración V37 creada (pending apply)
- Nuevos componentes UI: LocationList, LocationFormDialog, ServiceList, ServiceFormDialog
- Integración en InventoryPage y PurchasesPage

## Tareas Requeridas

### 1. Aplicar Migración V37 y Arrancar Backend
- Ejecutar task VSCode "bootRun:dev (sin debug)"
- Verificar en logs: "Migration V37 -> SUCCESS"
- Confirmar backend UP en localhost:8081

### 2. Validar Esquema de Base de Datos
Ejecutar queries de verificación:
```bash
# V37 aplicada
docker exec pymes_postgres psql -U pymes -d pymes -t -c "SELECT version FROM flyway_schema_history WHERE version = '37';"

# Locations: nuevas columnas
docker exec pymes_postgres psql -U pymes -d pymes -c "\d locations"
# Esperado: business_name, rut, status
# NO debe tener: parent_location_id, capacity, capacity_unit

# Services: nuevas columnas
docker exec pymes_postgres psql -U pymes -d pymes -c "\d services"
# Esperado: category, unit_price, status
# NO debe tener: active, last_purchase_date
```

### 3. Testing Manual en UI

#### Ubicaciones (http://localhost:5173/app/inventory):
1. Verificar que sección "Ubicaciones" NO muestra error 500
2. Click "+ Nueva Ubicación"
3. Crear ubicación con:
   - Code: LOC-TEST-001
   - Name: Bodega Test
   - Type: BODEGA
   - Business Name: Empresa Test
   - RUT: 76123456-7 (validar formato)
   - Description: Testing
   - Status: ACTIVE
4. Guardar y confirmar aparece en tabla
5. Editar ubicación (verificar ID read-only)
6. Eliminar con confirmación
7. Probar validación: code duplicado, RUT inválido

#### Servicios (http://localhost:5173/app/purchases):
1. Verificar sección "Servicios" visible
2. Click "+ Nuevo Servicio"
3. Crear servicio con:
   - Code: SRV-TEST-001
   - Name: Consultoría Test
   - Category: Consultoría
   - Unit Price: 150000
   - Status: ACTIVE
4. Guardar y confirmar aparece en lista
5. Ir a crear compra
6. Seleccionar servicio SRV-TEST-001
7. **VERIFICAR**: Unit Price se auto-completa con 150000
8. Probar editar y eliminar servicio

### 4. API Testing (opcional)
Ejecutar curl para validar endpoints REST (ver sección 4 de NEXT_STEPS_LOCATIONS_SERVICES.md)

### 5. Reportar Resultados
Al finalizar, reportar:
- ✅/❌ Migración V37 aplicada
- ✅/❌ Error 500 en ubicaciones resuelto
- ✅/❌ CRUD ubicaciones funciona
- ✅/❌ CRUD servicios funciona
- ✅/❌ Auto-fill unitPrice en compras
- ✅/❌ Validaciones (RUT, code único, precio > 0)
- ✅/❌ ID read-only en modales
- Captura de pantalla de ubicaciones y servicios funcionando
- Logs de errores si existen

Si encuentras errores, proporciona:
- Stack trace completo
- Pasos para reproducir
- Consultas SQL relevantes
```

---

## 📝 Notas Adicionales

- **RUT chileno formato:** `12345678-9` (8 dígitos + guión + dígito verificador)
- **Tipos de ubicación:** BODEGA, CONTAINER, LOCAL, CAMION, CAMIONETA
- **Estados:** ACTIVE, BLOCKED (locations) / ACTIVE, INACTIVE (services)
- **Precio unitario:** Debe ser > 0, formato decimal (14,2)
- **Code único:** Backend debe validar unicidad en create/update

---

**Creado:** 12 de Noviembre 2025  
**Autor:** GitHub Copilot  
**Estado:** Listo para Testing Manual

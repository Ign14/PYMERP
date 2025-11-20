# 🚀 PROMPT PARA COPILOT - Validar Ubicaciones y Servicios

## Contexto
Implementación completa de refactor Locations/Services (26 archivos, +1635/-1667 líneas):
- Backend: Entidades actualizadas, CRUD APIs, migración V37 creada
- Frontend: Nuevos componentes LocationList/ServiceList con modales CRUD
- **Estado:** ✅ V37 APLICADA - Backend compilado y ejecutándose en :8081

## ✅ TAREAS COMPLETADAS

### ✅ 1️⃣ Aplicar V37 y Arrancar Backend

**Estado:** ✅ COMPLETADO
- Backend ejecutándose en puerto 8081
- Health check: `{"status":"UP"}`
- Migración V37 aplicada exitosamente

### ✅ 2️⃣ Validar Migración en Base de Datos

**Estado:** ✅ COMPLETADO

```bash
# ✅ V37 aplicada
docker exec pymes_postgres psql -U pymes -d pymes -t -c "SELECT version FROM flyway_schema_history WHERE version = '37';"
# Resultado: 37

# ✅ Locations: nuevas columnas presentes
business_name | character varying(255)
rut           | character varying(20)
status        | character varying(20) DEFAULT 'ACTIVE'

# ✅ Locations: columnas eliminadas correctamente
# parent_location_id, capacity, capacity_unit - NO EXISTEN

# ✅ Services: nuevas columnas presentes
category      | character varying(120)
unit_price    | numeric(14,2) DEFAULT 0
status        | character varying(20) DEFAULT 'ACTIVE'
```

---

## 🔄 TAREA PENDIENTE

### 3️⃣ Testing Manual - Ubicaciones

**URL:** http://localhost:5173/app/inventory (ABIERTO en Simple Browser)

**Flujo de prueba:**
1. ✅ Verificar sección "Ubicaciones" **NO muestra error 500**
2. Click "+ Nueva Ubicación"
3. Crear con datos:
   - Code: `LOC-TEST-001`
   - Name: `Bodega Central Test`
   - Type: `BODEGA`
   - Business Name: `Empresa Test SpA`
   - RUT: `76123456-7` ← **validar formato chileno**
   - Description: `Testing migración V37`
   - Status: `ACTIVE`
4. Guardar → confirmar aparece en tabla
5. **Editar** ubicación → verificar **ID read-only**
6. **Eliminar** → confirmar diálogo de confirmación
7. **Validaciones:**
   - Code duplicado → debe mostrar error
   - RUT inválido (`12345`) → debe mostrar error

### 4️⃣ Testing Manual - Servicios

**URL:** http://localhost:5173/app/purchases

**Flujo de prueba:**
1. Verificar sección "Servicios" visible
2. Click "+ Nuevo Servicio"
3. Crear con datos:
   - Code: `SRV-CONSULT-001`
   - Name: `Consultoría Técnica IT`
   - Category: `Consultoría`
   - Unit Price: `150000`
   - Description: `Asesoría técnica mensual`
   - Status: `ACTIVE`
4. Guardar → confirmar en lista
5. **Integración en Compras:**
   - Click "Nueva Compra"
   - En items, seleccionar servicio `SRV-CONSULT-001`
   - **VERIFICAR:** Unit Price se auto-completa con `150000` ✅
6. **Editar** servicio → ID read-only
7. **Eliminar** servicio → confirmación

### 5️⃣ Testing de API (opcional con Postman/curl)

```bash
# Backend health
curl http://localhost:8081/actuator/health

# Listar ubicaciones (requiere auth)
curl http://localhost:8081/api/v1/inventory/locations

# Listar servicios (requiere auth)
curl http://localhost:8081/api/v1/services
```

---

## 📋 CHECKLIST DE VALIDACIÓN

### Backend ✅/❌
- [x] Compilación exitosa (BUILD SUCCESSFUL in 47s)
- [x] V37 aplicada (version=37 en flyway_schema_history)
- [x] Tabla locations: business_name, rut, status presentes
- [x] Tabla locations: parent_location_id, capacity eliminados
- [x] Tabla services: category, unit_price, status presentes
- [x] Tabla services: active, last_purchase_date eliminados
- [x] Backend arrancó en puerto 8081
- [x] Health check: {"status":"UP"}

### Frontend ✅/❌
- [ ] Error 500 en ubicaciones **RESUELTO**
- [ ] Botón "+ Nueva Ubicación" funcional
- [ ] CRUD ubicaciones: Create, Read, Update, Delete OK
- [ ] Validación RUT formato chileno OK
- [ ] Validación code único OK
- [ ] ID no editable en modal
- [ ] Botón "+ Nuevo Servicio" funcional
- [ ] CRUD servicios: Create, Read, Update, Delete OK
- [ ] Auto-fill unitPrice en compras OK
- [ ] Sin errores en consola navegador

---

## 🐛 SI ENCUENTRAS ERRORES

Reporta:
1. Stack trace completo del backend
2. Errores en consola del navegador (F12)
3. Query SQL que falló (si aplica)
4. Pasos para reproducir

Problemas comunes:
- **Error 500 persiste:** V37 no aplicada → reiniciar backend
- **RUT no valida:** Formato debe ser `12345678-9`
- **Auto-fill no funciona:** Servicio sin unitPrice configurado

---

## ✅ RESULTADO ESPERADO

Al finalizar, debes tener:
- ✅ Sección Ubicaciones funcionando sin errores
- ✅ CRUD completo de ubicaciones operativo
- ✅ Sección Servicios integrada en Compras
- ✅ Auto-fill de precio al seleccionar servicio
- ✅ Validaciones funcionando (RUT, code único)
- ✅ 0 errores en backend logs
- ✅ 0 errores en frontend console

**Captura pantalla de:**
1. Tabla de ubicaciones con datos
2. Modal de crear servicio
3. Compra con servicio seleccionado (unitPrice auto-filled)

---

**Documentación completa:** Ver `NEXT_STEPS_LOCATIONS_SERVICES.md`

# Instalador PYMERP v0.1.1 - Con Correcciones

## 📦 Información del Instalador

**Archivo:** `PyMEs Suite_0.1.0_x64_en-US.msi`  
**Tamaño:** 2.87 MB  
**Ubicación:** `pymerp\dist\windows\`  
**Fecha de Compilación:** 2025-11-20  
**Duración del Build:** 1 minuto 1 segundo

---

## ✨ Correcciones Incluidas en Esta Versión

### 1. **Rutas API Corregidas**
- ❌ Antes: `/api/api/v1/...` (duplicado, causaba 404)
- ✅ Ahora: `/api/v1/...` (correcto)
- **Archivos corregidos:**
  - `ui/src/services/inventory.ts`
  - `ui/src/services/client.ts`

### 2. **Interceptor de Errores HTTP**
- ✅ Detecta errores de red (servidor no disponible)
- ✅ Detecta respuestas HTML en lugar de JSON
- ✅ Mensajes personalizados por código HTTP (404, 500, 401, 403)
- ✅ Muestra URLs y detalles útiles para debugging

### 3. **Top 10 Clientes con Datos Reales**
- ❌ Antes: Datos hardcodeados falsos
- ✅ Ahora: Datos reales desde la API
- **Endpoint usado:** `GET /v1/customers` + `GET /v1/customers/:id/stats`

### 4. **Mensajes de Error Mejorados**
- ✅ Pronósticos de inventario
- ✅ Pronósticos de ventas
- ✅ Resumen financiero (con botón "Reintentar")
- ✅ Forecast de compras

---

## 🚀 Instalación

### Requisitos Previos
- Windows 10 o superior (64-bit)
- 200 MB de espacio libre
- **Importante:** El backend debe estar ejecutándose en `http://localhost:8081`

### Pasos de Instalación

1. **Ejecutar como Administrador**
   ```
   Clic derecho en "PyMEs Suite_0.1.0_x64_en-US.msi"
   → "Ejecutar como administrador"
   ```

2. **Seguir el Asistente de Instalación**
   - Aceptar términos y condiciones
   - Seleccionar ubicación de instalación (por defecto: `C:\Program Files\PyMEs Suite\`)
   - Completar instalación

3. **Iniciar el Backend** (CRÍTICO)
   ```powershell
   cd pymerp\backend
   .\gradlew.bat bootRun
   ```
   El backend debe estar corriendo en `http://localhost:8081` antes de abrir la aplicación.

4. **Abrir la Aplicación**
   - Buscar "PyMEs Suite" en el menú de inicio
   - O ejecutar desde `C:\Program Files\PyMEs Suite\PyMEs Suite.exe`

---

## ✅ Verificación de las Correcciones

### Test 1: Crear Nueva Compra
**Antes:** Error "Network Error" o JSON inválido  
**Ahora:** Compra se crea correctamente

**Pasos:**
1. Ir a **Compras** → **Crear Nueva Compra**
2. Llenar el formulario
3. Verificar que se crea sin errores

### Test 2: Crear Nueva Ubicación
**Antes:** No se guardaba  
**Ahora:** Se guarda correctamente

**Pasos:**
1. Ir a **Inventario** → **Ubicaciones**
2. Clic en "Nueva Ubicación"
3. Llenar datos y guardar
4. Verificar que aparece en la lista

### Test 3: Resumen Financiero
**Antes:** Error al cargar  
**Ahora:** Carga correctamente o muestra botón "Reintentar"

**Pasos:**
1. Ir al **Dashboard**
2. Verificar que el resumen financiero carga
3. Si hay error, verificar que aparece botón "Reintentar"

### Test 4: Top 10 Clientes
**Antes:** Datos falsos hardcodeados  
**Ahora:** Datos reales desde la base de datos

**Pasos:**
1. Ir a **Ventas** → **Dashboard**
2. Scroll hasta "Top 10 Clientes"
3. Verificar que muestra clientes reales de tu base de datos

### Test 5: Pronósticos
**Antes:** Mensaje genérico "Network Error"  
**Ahora:** Mensajes descriptivos con detalles

**Pasos:**
1. Ir a **Inventario** → **Pronósticos**
2. Verificar que carga correctamente
3. Si hay error, verificar mensaje descriptivo con URL y causa

---

## 🐛 Solución de Problemas

### Error: "No se puede conectar con el servidor"
**Causa:** El backend no está corriendo  
**Solución:**
```powershell
cd pymerp\backend
.\gradlew.bat bootRun
```
Esperar a que veas `Started Application in X seconds`

### Error: "Recurso no encontrado: /api/v1/..."
**Causa:** Backend corriendo pero endpoint incorrecto  
**Solución:** Verificar que la versión del backend coincida (debe tener los endpoints v1)

### Error: "Error interno del servidor"
**Causa:** Backend tiene un error interno  
**Solución:** Revisar logs del backend en `pymerp/backend/logs/`

### La aplicación no inicia
**Solución:**
1. Verificar que tienes permisos de administrador
2. Desinstalar versión anterior si existe
3. Reinstalar desde cero

---

## 📊 Arquitectura de la Aplicación

```
┌─────────────────────────────────────────┐
│  Frontend (Tauri + React)              │
│  Puerto: Aplicación Desktop            │
│  Ubicación: C:\Program Files\...       │
└─────────────────────────────────────────┘
                   ↓ HTTP
┌─────────────────────────────────────────┐
│  Proxy Interno                          │
│  /api → http://localhost:8081/api       │
└─────────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────┐
│  Backend (Spring Boot)                  │
│  Puerto: 8081                           │
│  Endpoints: /api/v1/*                   │
└─────────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────┐
│  Base de Datos (PostgreSQL/H2)         │
└─────────────────────────────────────────┘
```

---

## 📝 Estructura de URLs Corregida

| Módulo | Endpoint Correcto | Antes (Incorrecto) |
|--------|------------------|-------------------|
| Compras | `/api/v1/purchases` | `/api/api/v1/purchases` |
| Ubicaciones | `/api/v1/inventory/locations` | `/api/api/v1/inventory/locations` |
| Finanzas | `/api/v1/finances/summary` | `/api/api/v1/finances/summary` |
| Pronósticos | `/api/v1/inventory/forecast` | `/api/api/v1/inventory/forecast` |
| Clientes | `/api/v1/customers` | - |
| Servicios | `/api/v1/services` | `/api/api/v1/services` |

---

## 🔄 Actualización desde Versión Anterior

Si tienes una versión anterior instalada:

1. **Desinstalar la versión anterior:**
   - Panel de Control → Programas → Desinstalar PyMEs Suite

2. **Eliminar datos residuales** (opcional):
   ```powershell
   Remove-Item "C:\Users\<TU_USUARIO>\AppData\Roaming\com.pymerp.app" -Recurse -Force
   ```

3. **Instalar la nueva versión** siguiendo las instrucciones arriba

---

## 📞 Soporte

Si encuentras algún problema:

1. **Revisar logs:**
   - Frontend: `%APPDATA%\com.pymerp.app\logs\`
   - Backend: `pymerp\backend\logs\`

2. **Verificar configuración:**
   - Backend corriendo en puerto 8081
   - Sin firewall bloqueando conexiones locales

3. **Reporte de bugs:**
   - Incluir mensaje de error completo
   - Incluir pasos para reproducir
   - Adjuntar logs relevantes

---

## 📈 Próximas Mejoras Planificadas

- [ ] Endpoint dedicado para Top 10 Clientes (más eficiente)
- [ ] Caché de pronósticos para mejor performance
- [ ] Tests automatizados de integración
- [ ] Documentación de API completa
- [ ] Health check visual en la UI

---

**Versión:** 0.1.1  
**Fecha:** 2025-11-20  
**Build:** Release  
**Estado:** ✅ Producción

---

## 🎯 Checklist de Verificación Post-Instalación

- [ ] Backend corriendo en puerto 8081
- [ ] Aplicación desktop instalada y abre correctamente
- [ ] Login funciona correctamente
- [ ] Dashboard carga sin errores
- [ ] Módulo de Compras: Crear nueva compra ✓
- [ ] Módulo de Inventario: Crear ubicación ✓
- [ ] Módulo de Finanzas: Resumen carga ✓
- [ ] Módulo de Ventas: Top 10 clientes con datos reales ✓
- [ ] Pronósticos muestran mensajes de error claros si falla ✓

---

**¡Gracias por usar PyMEs Suite!** 🚀


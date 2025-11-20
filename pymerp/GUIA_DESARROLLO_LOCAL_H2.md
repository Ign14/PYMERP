# 🚀 Guía de Desarrollo Local con H2

Esta guía te ayudará a ejecutar PYMERP localmente usando H2 en memoria, sin necesidad de Docker, PostgreSQL o Keycloak.

---

## ✅ Requisitos

- ✅ Java 21 (JDK)
- ✅ Node.js 18+ con npm
- ✅ Windows con PowerShell

---

## 📦 Paso 1: Verificar que no haya procesos previos

```powershell
# Verificar puerto 8081 (backend)
netstat -ano | findstr :8081

# Si hay algo corriendo, detenerlo:
taskkill /PID [PID_AQUI] /F

# Detener daemons de Gradle
cd backend
.\gradlew.bat --stop
```

---

## 🗄️ Paso 2: Iniciar el Backend (H2)

### Opción A: Usar el script de inicio

```powershell
cd backend
.\start-h2.bat
```

### Opción B: Comando directo

```powershell
cd backend
.\gradlew.bat bootRun --args="--spring.profiles.active=h2"
```

### ✅ Verificar que arrancó correctamente

**En el log deberías ver:**

```
INFO  c.datakomerz.pymes.PymesApplication - The following 1 profile is active: "h2"
INFO  o.s.b.w.e.tomcat.TomcatWebServer - Tomcat initialized with port 8081 (http)
INFO  o.s.b.a.h.H2ConsoleAutoConfiguration - H2 console available at '/h2-console'
...
INFO  c.datakomerz.pymes.PymesApplication - Started PymesApplication in X.XX seconds
```

**En otra terminal, ejecuta el script de verificación:**

```powershell
cd backend
.\verify-health.ps1
```

**Deberías ver:**

```
✅ Puerto 8081 en uso (backend corriendo)
✅ Health endpoint respondiendo: UP
🎉 Backend funcionando correctamente!
```

---

## 🎨 Paso 3: Iniciar el Frontend

```powershell
cd ui
npm install  # Solo la primera vez
npm run dev
```

**Deberías ver:**

```
VITE v5.x.x  ready in XXX ms

➜  Local:   http://localhost:5173/
➜  Network: use --host to expose
```

---

## 🧪 Paso 4: Verificar la Integración

1. **Abre tu navegador:** http://localhost:5173/

2. **Verifica que no haya errores de red en la consola del navegador (F12)**

3. **Prueba crear/listar recursos:**
   - Productos
   - Clientes
   - Ubicaciones de inventario
   - Servicios

4. **Revisa que los gráficos y KPIs carguen datos reales** (no dummy data)

---

## 🔍 Endpoints Disponibles

| Endpoint | URL | Descripción |
|----------|-----|-------------|
| Frontend | http://localhost:5173/ | Aplicación React/Vite |
| Backend API | http://localhost:8081/api/v1/ | REST API |
| Health Check | http://localhost:8081/actuator/health | Estado del backend |
| H2 Console | http://localhost:8081/h2-console | Base de datos en memoria |

### 🗄️ Acceso a H2 Console

- **JDBC URL:** `jdbc:h2:mem:pymes`
- **Username:** `sa`
- **Password:** _(dejar en blanco)_

---

## ⚠️ Notas Importantes

### 🔄 Datos en Memoria

- **Los datos se recrean en cada arranque** (`ddl-auto: create-drop`)
- Si quieres conservar datos entre reinicios, cambia en `application-h2.yml`:
  ```yaml
  jpa:
    hibernate:
      ddl-auto: update  # En lugar de create-drop
  ```

### 🚫 Limitaciones del Perfil H2

- ❌ Sin OAuth2/Keycloak (autenticación JWT local)
- ❌ Sin Redis
- ❌ Sin envío de emails
- ❌ Sin S3 real (storage simulado)
- ✅ Perfecto para desarrollo frontend/backend básico
- ✅ Ideal para pruebas rápidas sin Docker

### 🐳 Cuándo usar Docker

Usa Docker (`docker-compose up`) cuando necesites:
- Persistencia de datos real (PostgreSQL)
- Autenticación con Keycloak
- Redis para sesiones/caché
- Entorno de producción local completo

---

## 🐛 Troubleshooting

### ❌ "Port 8081 was already in use"

```powershell
# Encontrar el proceso
netstat -ano | findstr :8081

# Matar el proceso (reemplaza PID con el número que aparece)
taskkill /PID [PID] /F

# Detener Gradle daemons
cd backend
.\gradlew.bat --stop
```

### ❌ "Network Error" en el frontend

1. Verifica que el backend esté corriendo:
   ```powershell
   curl http://localhost:8081/actuator/health
   ```

2. Verifica que las rutas API no tengan el prefijo `/api` duplicado:
   - ✅ Correcto: `api.get('/v1/products')`
   - ❌ Incorrecto: `api.get('/api/v1/products')`

### ❌ "Unexpected token '<', "<!doctype"... is not valid JSON"

- Esto indica que el backend está devolviendo HTML (página de error) en lugar de JSON
- Verifica que la ruta API sea correcta
- Verifica que el endpoint exista en el backend

### ❌ El backend se queda "colgado" al arrancar

- Puede ser que esté intentando conectarse a Keycloak
- Verifica que `application-h2.yml` tenga:
  ```yaml
  spring:
    autoconfigure:
      exclude:
        - org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration
  ```

---

## 📚 Próximos Pasos

1. **Desarrollo local rápido:** Usa H2 para iterar rápidamente en features frontend/backend
2. **Testing con datos persistentes:** Cambia a `ddl-auto: update` o usa PostgreSQL local
3. **Integración completa:** Levanta Docker Compose para ambiente completo
4. **Producción:** Deploy con PostgreSQL, Keycloak, Redis en ambiente productivo

---

## 🔗 Referencias

- [application-h2.yml](./backend/src/main/resources/application-h2.yml) - Configuración del perfil H2
- [CORRECCIONES_ERRORES_RED.md](./CORRECCIONES_ERRORES_RED.md) - Correcciones de errores de red
- [PROXIMOS_PASOS.md](./PROXIMOS_PASOS.md) - Roadmap de desarrollo

---

**¿Problemas? Revisa los logs del backend y frontend, y asegúrate de estar usando el perfil H2.**


# 🚀 Guía Paso a Paso para Ejecutar PYMERP Completo

## Prerequisitos

Antes de comenzar, asegúrate de tener instalado:

```bash
# Verificar versiones
node --version        # v18 o superior
npm --version         # v9 o superior
java --version        # JDK 21
docker --version      # v20 o superior
docker-compose --version
git --version
```

---

## Opción 1: Despliegue Local de Producción (Recomendado para testing)

### Paso 1: Clonar el repositorio

```bash
cd C:\Users\ignac\Documents\Centro de modelacion xd\PYMERP
git pull origin feature/sprint-10-production-deploy
```

### Paso 2: Iniciar servicios de infraestructura

```bash
# Ir al directorio del proyecto
cd C:\Users\ignac\Documents\Centro de modelacion xd\PYMERP\pymerp

# Iniciar Docker Desktop (si no está corriendo)
# Luego ejecutar:
docker-compose -f docker-compose.local-prod.yml up -d postgres redis minio mailhog
```

**Espera 30 segundos** para que los servicios inicien.

### Paso 3: Compilar el backend

```bash
# Ir al directorio backend
cd backend

# Compilar sin tests (Windows)
.\gradlew.bat build -x test -x checkstyleMain -x checkstyleTest
```

**Tiempo estimado**: 2-3 minutos

### Paso 4: Iniciar el backend

```bash
# Desde el directorio backend
.\gradlew.bat bootRun
```

O usando Docker:

```bash
# Desde el directorio raíz
docker-compose -f docker-compose.local-prod.yml up -d backend
```

**Espera 60 segundos** para que Spring Boot inicie completamente.

**Verificar que está corriendo**:
```bash
curl http://localhost:8081/actuator/health
# Debe retornar: {"status":"UP"}
```

### Paso 5: Compilar el frontend

```bash
# Abrir nueva terminal
cd C:\Users\ignac\Documents\Centro de modelacion xd\PYMERP\pymerp\ui

# Instalar dependencias (solo primera vez)
npm install

# Compilar para producción
npm run build
```

**Tiempo estimado**: 1-2 minutos

### Paso 6: Iniciar NGINX (Proxy reverso)

```bash
# Desde el directorio raíz
cd C:\Users\ignac\Documents\Centro de modelacion xd\PYMERP\pymerp

docker-compose -f docker-compose.local-prod.yml up -d nginx
```

### Paso 7: Verificar que todo está corriendo

```bash
docker-compose -f docker-compose.local-prod.yml ps
```

Deberías ver todos los servicios **UP**:
```
NAME                    STATUS
pymerp-backend         Up (healthy)
pymerp-postgres        Up (healthy)
pymerp-redis           Up (healthy)
pymerp-minio           Up (healthy)
pymerp-mailhog         Up
pymerp-nginx           Up
```

### Paso 8: Acceder a la aplicación

**Navegador**: Abrir en Chrome/Firefox

```
https://localhost:8443
```

**⚠️ Importante**: Acepta el certificado SSL autofirmado:
- Chrome: Click "Avanzado" → "Continuar a localhost (sitio no seguro)"
- Firefox: "Avanzado" → "Aceptar el riesgo y continuar"

**Credenciales de login**:
```
Usuario: admin@dev.local
Contraseña: Admin1234
```

---

## Opción 2: Desarrollo Local (Para programar)

### Paso 1: Iniciar solo infraestructura

```bash
cd C:\Users\ignac\Documents\Centro de modelacion xd\PYMERP\pymerp

docker-compose -f docker-compose.local-prod.yml up -d postgres redis minio mailhog
```

### Paso 2: Iniciar backend en modo desarrollo

```bash
cd backend

# Modo desarrollo con hot-reload
.\gradlew.bat bootRun --args='--spring.profiles.active=dev'
```

Backend estará en: `http://localhost:8081`

### Paso 3: Iniciar frontend en modo desarrollo

```bash
# Nueva terminal
cd ui

npm install
npm run dev
```

Frontend estará en: `http://localhost:5173`

**Credenciales**:
```
Usuario: admin@dev.local
Contraseña: Admin1234
```

---

## 🛠️ Comandos Útiles

### Ver logs en tiempo real

```bash
# Todos los servicios
docker-compose -f docker-compose.local-prod.yml logs -f

# Solo backend
docker-compose -f docker-compose.local-prod.yml logs -f backend

# Solo NGINX
docker-compose -f docker-compose.local-prod.yml logs -f nginx
```

### Reiniciar un servicio

```bash
# Reiniciar backend
docker-compose -f docker-compose.local-prod.yml restart backend

# Reiniciar NGINX
docker-compose -f docker-compose.local-prod.yml restart nginx
```

### Detener todo

```bash
docker-compose -f docker-compose.local-prod.yml down
```

### Limpiar y empezar de cero

```bash
# ⚠️ CUIDADO: Esto borra la base de datos
docker-compose -f docker-compose.local-prod.yml down -v
```

---

## 🔍 Verificación de Componentes

### 1. Base de datos PostgreSQL
```bash
# Conectarse a la base de datos
docker exec -it pymerp-postgres psql -U pymes -d pymes

# Dentro de psql:
\dt                    # Listar tablas
SELECT COUNT(*) FROM flyway_schema_history;  # Ver migraciones
\q                     # Salir
```

### 2. Redis (Cache)
```bash
docker exec -it pymerp-redis redis-cli

# Dentro de redis-cli:
PING                   # Debe retornar PONG
KEYS *                 # Ver keys guardadas
exit
```

### 3. MinIO (S3 Storage)
Abrir navegador: `http://localhost:9001`
```
Usuario: minioadmin
Contraseña: MinioSecure2024!
```

### 4. MailHog (Email testing)
Abrir navegador: `http://localhost:8025`

Ver emails enviados por la aplicación.

---

## 📊 Endpoints Importantes

### Backend API
```
Health Check:     http://localhost:8081/actuator/health
API Docs:         http://localhost:8081/swagger-ui.html
Metrics:          http://localhost:8081/actuator/metrics
```

### Frontend
```
Producción:       https://localhost:8443
Desarrollo:       http://localhost:5173
```

### Páginas principales
```
Dashboard:        https://localhost:8443/app/dashboard
Ventas:           https://localhost:8443/app/sales
Compras:          https://localhost:8443/app/purchases
Inventario:       https://localhost:8443/app/inventory
Movimientos:      https://localhost:8443/app/inventory/movements
Finanzas:         https://localhost:8443/app/financials
Configuración:    https://localhost:8443/app/settings
```

---

## 🐛 Solución de Problemas

### Backend no inicia

```bash
# Ver logs
docker-compose -f docker-compose.local-prod.yml logs backend

# Problemas comunes:
# 1. Puerto 8081 ocupado
netstat -ano | findstr :8081
# Matar proceso si es necesario

# 2. PostgreSQL no está listo
# Esperar 30 segundos y reintentar
```

### Frontend muestra pantalla en blanco

```bash
# Verificar que el build se completó
ls ui/dist/

# Debe contener: index.html, assets/, etc.

# Reconstruir
cd ui
npm run build
```

### NGINX muestra 502 Bad Gateway

```bash
# Verificar que backend está corriendo
curl http://localhost:8081/actuator/health

# Si no responde, reiniciar backend
docker-compose -f docker-compose.local-prod.yml restart backend
```

### Error de certificado SSL

Esto es normal con certificados autofirmados. Opciones:

1. **Aceptar en navegador** (Recomendado para desarrollo)
2. **Usar HTTP**: `http://localhost:8080` (sin SSL)
3. **Importar certificado** en Windows:
   ```bash
   # Importar certificado
   certutil -addstore -f "ROOT" nginx/ssl/pymerp.crt
   ```

### Database connection error

```bash
# Verificar que PostgreSQL está corriendo
docker ps | findstr postgres

# Ver logs de PostgreSQL
docker-compose -f docker-compose.local-prod.yml logs postgres

# Recrear contenedor si es necesario
docker-compose -f docker-compose.local-prod.yml up -d --force-recreate postgres
```

---

## 📝 Resumen de Puertos

| Servicio | Puerto | URL |
|----------|--------|-----|
| Frontend (HTTPS) | 8443 | https://localhost:8443 |
| Frontend (HTTP) | 8080 | http://localhost:8080 |
| Backend API | 8081 | http://localhost:8081 |
| PostgreSQL | 5432 | localhost:5432 |
| Redis | 6379 | localhost:6379 |
| MinIO API | 9000 | http://localhost:9000 |
| MinIO Console | 9001 | http://localhost:9001 |
| MailHog Web | 8025 | http://localhost:8025 |
| MailHog SMTP | 1025 | localhost:1025 |

---

## 🎯 Flujo Completo de Inicio Rápido

```bash
# 1. Clonar/actualizar repo
cd C:\Users\ignac\Documents\Centro de modelacion xd\PYMERP\pymerp
git pull

# 2. Iniciar infraestructura
docker-compose -f docker-compose.local-prod.yml up -d postgres redis minio mailhog

# 3. Compilar backend
cd backend
.\gradlew.bat build -x test -x checkstyleMain -x checkstyleTest

# 4. Compilar frontend
cd ..\ui
npm install
npm run build

# 5. Iniciar todo con Docker
cd ..
docker-compose -f docker-compose.local-prod.yml up -d

# 6. Esperar 60 segundos y verificar
docker-compose -f docker-compose.local-prod.yml ps

# 7. Abrir navegador
start https://localhost:8443
```

**Login**: admin@dev.local / Admin1234

---

## ✅ Checklist de Verificación

- [ ] Docker Desktop está corriendo
- [ ] JDK 21 instalado y en PATH
- [ ] Node.js 18+ instalado
- [ ] Puertos 8080, 8081, 8443, 5432, 6379, 9000, 9001 libres
- [ ] Git actualizado al último commit
- [ ] Backend compiló sin errores
- [ ] Frontend compiló sin errores
- [ ] Todos los contenedores Docker están UP
- [ ] Backend health check retorna UP
- [ ] Navegador puede acceder a https://localhost:8443
- [ ] Login funciona correctamente

---

## 📚 Documentación Adicional

- **README.md**: Información general del proyecto
- **README_dev.md**: Guía de desarrollo
- **README_tests.md**: Guía de testing
- **INSTRUCCIONES_ENTORNO_INSTALACIONES_BUILD_ARRANQUE.md**: Instrucciones detalladas de instalación
- **arquitectura_pymes.md**: Arquitectura del sistema
- **docs/**: Documentación técnica adicional

---

## 🔄 Actualización de Cambios

Cuando hagas `git pull` y haya cambios en:

### Migraciones de base de datos
```bash
# Las migraciones se ejecutan automáticamente al iniciar el backend
# Ver estado de migraciones:
docker exec -it pymerp-postgres psql -U pymes -d pymes -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"
```

### Dependencias del backend
```bash
cd backend
.\gradlew.bat clean build -x test
```

### Dependencias del frontend
```bash
cd ui
npm install
npm run build
```

### Configuración de Docker
```bash
# Recrear servicios
docker-compose -f docker-compose.local-prod.yml up -d --force-recreate
```

---

## 🎨 Últimas Mejoras Implementadas

### Mejoras en Ubicaciones del Inventario (V33)
- ✅ Campos de estado: `active`, `isBlocked`
- ✅ Gestión de capacidad: `capacity`, `capacityUnit`
- ✅ Validación de jerarquía circular (máx 10 niveles)
- ✅ Endpoints de navegación: `/path`, `/children`, `/descendants`
- ✅ Validación de capacidad disponible: `/can-receive`
- ✅ UI mejorada con checkboxes de estado y selector de unidades
- ✅ Indicadores visuales: badges de estado inactivo/bloqueado

---

**Última actualización**: 11 de noviembre de 2025

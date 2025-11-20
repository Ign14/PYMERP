# Estado del Perfil H2 - PyMERP Backend

## ✅ Cambios Implementados

### 1. Configuración H2 (`application-h2.yml`)
- ✅ Puerto correcto: 8081
- ✅ Base de datos H2 en memoria con modo PostgreSQL
- ✅ `ddl-auto: create-drop` para recrear esquema en cada arranque
- ✅ Flyway desactivado (solo usa Hibernate DDL)
- ✅ OAuth2 Client auto-configuration excluida (no requiere Keycloak)
- ✅ Consola H2 habilitada en `/h2-console`
- ✅ Redis desactivado
- ✅ JWT, captcha y validaciones de secretos desactivados

### 2. Migraciones de Base de Datos
- ✅ Tipos normalizados a `TIMESTAMP WITH TIME ZONE`
- ✅ Orden de columnas UUID optimizado para H2/PostgreSQL

### 3. Modelo de Datos (`ContingencyQueueItem.java`)
- ✅ Payload JSON sin forzar `jsonb`
- ✅ Estado usa `varchar(24)`
- ✅ Blob como `bytea` para compatibilidad H2

## 📊 Estado Actual

### ✅ Backend Levantado
- **Puerto**: 8081
- **PID**: 86156
- **Estado**: Escuchando en `0.0.0.0:8081`

### ⚠️ Health Check
- **Endpoint**: `http://localhost:8081/actuator/health`
- **Estado**: 503 (Service Unavailable)
- **Causa probable**: Uno o más health indicators están fallando

### Posibles Causas del 503
1. **Redis**: El health check de Redis puede estar intentando conectarse aunque esté configurado como `enabled: false`
2. **Base de Datos**: H2 puede estar teniendo problemas con alguna query de health check
3. **Otros componentes**: Algún otro servicio externo configurado en los health indicators

## 🔍 Cómo Diagnosticar

### Opción 1: Revisar logs en consola
Abre una nueva terminal PowerShell y ejecuta:
```powershell
cd "C:\Users\ignac\Documents\Centro de modelacion xd\PYMERP\pymerp\backend"
.\gradlew.bat bootRun --args="--spring.profiles.active=h2"
```

Esto te mostrará todos los logs en tiempo real para identificar el problema.

### Opción 2: Revisar logs del proceso actual
Puedes intentar buscar logs en:
- `pymerp/backend/logs/` (si están configurados)
- Salida de la consola del proceso PID 86156

### Opción 3: Desactivar health checks problemáticos
Añade a `application-h2.yml`:
```yaml
management:
  health:
    redis:
      enabled: false
    db:
      enabled: false
  endpoint:
    health:
      show-details: always
```

### Opción 4: Verificar consola H2
1. Abre tu navegador en: `http://localhost:8081/h2-console`
2. Usa los siguientes datos:
   - **JDBC URL**: `jdbc:h2:mem:pymes`
   - **User**: `sa`
   - **Password**: (dejar vacío)
3. Verifica que las tablas se hayan creado correctamente

## 🎯 Próximos Pasos

### Paso 1: Verificar que la aplicación funciona (sin health check)
Intenta acceder a algún endpoint específico, por ejemplo:
```powershell
curl http://localhost:8081/api/v1/companies
```

Si este endpoint responde (aunque sea con un error 401 o 403 de autenticación), significa que la aplicación está funcionando correctamente.

### Paso 2: Detener el proceso actual
```powershell
taskkill /F /PID 86156
```

### Paso 3: Reiniciar con logs visibles
```powershell
cd "C:\Users\ignac\Documents\Centro de modelacion xd\PYMERP\pymerp\backend"
.\gradlew.bat bootRun --args="--spring.profiles.active=h2"
```

### Paso 4: Una vez confirmado que funciona
1. Commitear los cambios del perfil H2
2. Actualizar la documentación
3. Continuar con el desarrollo usando H2 para iteración rápida

## 📝 Notas

- **Ventaja de H2**: Arranque ultra-rápido (< 30 segundos vs varios minutos con Docker)
- **Limitación**: Datos se pierden en cada reinicio con `ddl-auto: create-drop`
- **Solución**: Cambiar a `ddl-auto: update` si necesitas persistir datos entre reinicios
- **Migración a Docker**: Los cambios son 100% compatibles, solo cambia el perfil a `dev` o `prod`

## ✨ Beneficios del Perfil H2

- ⚡ **Rapidez**: Sin dependencias externas (Postgres, Keycloak, Redis)
- 🔧 **Desarrollo**: Ideal para pruebas rápidas y desarrollo local
- 🐛 **Debugging**: Fácil acceso a la consola H2 para inspeccionar datos
- 🚀 **CI/CD**: Perfecto para tests automatizados sin Docker
- 🔄 **Portabilidad**: Funciona en cualquier máquina con JDK 21

---

**Fecha**: 2025-11-20
**Autor**: Equipo PyMERP
**Estado**: En progreso - Backend levantado, investigando health check


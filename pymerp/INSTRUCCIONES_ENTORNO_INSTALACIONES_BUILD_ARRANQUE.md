# Instrucciones de entorno, instalaciones, build y arranque de la app actual

Este documento reúne los requisitos, instalación de dependencias, comandos de build y formas de arranque para los componentes del proyecto PYMERP (backend Spring Boot, UI React/Vite, app Flutter, y servicios locales opcionales con Docker).

## 1) Requisitos del entorno (Windows)

- Windows 10/11
- PowerShell 7+ y cmd.exe (shell por defecto)
- Git
- Docker Desktop (opcional para levantar Postgres/Redis/MinIO/Mailhog)
- Java Development Kit (JDK) 17 o 21
  - Verificar: `java -version`
  - No necesitas instalar Gradle: el proyecto usa Gradle Wrapper (`gradlew.bat`).
- Node.js 18 LTS (o 20 LTS) + npm (para la UI)
  - Verificar: `node -v` y `npm -v`
- Flutter 3.x y Dart SDK (para la app Flutter)
  - Verificar: `flutter --version`

## 2) Servicios locales con Docker (opcional pero recomendado)

El archivo `docker-compose.yml` en la raíz define los servicios típicos de desarrollo: Postgres, Redis, MinIO, Mailhog, etc.

- Arrancar servicios:

```cmd
cd /d "c:\Users\ignac\Documents\Centro de modelacion xd\PYMERP\pymerp"
docker compose up -d
```

- Detener servicios:

```cmd
docker compose down
```

## 3) Variables de entorno de backend (desarrollo)

Para ejecuciones locales (dev), el backend usa estas variables (valores por defecto alineados a `docker-compose.yml`):

- POSTGRES_HOST=localhost
- POSTGRES_PORT=55432
- POSTGRES_DB=pymes
- POSTGRES_USER=pymes
- POSTGRES_PASSWORD=pymes
- REDIS_HOST=localhost
- REDIS_PORT=6379
- MAIL_HOST=localhost
- MAIL_PORT=1025
- AWS_REGION=us-east-1
- MINIO_ROOT_USER=minioadmin
- MINIO_ROOT_PASSWORD=minioadmin
- S3_ENDPOINT=http://localhost:9000
- S3_BUCKET=pymes-dev

Puedes exportarlas en tu terminal o usar las Tareas de VS Code que ya las incluyen (ver sección 7).

## 4) Backend (Spring Boot)

Ubicación: `backend/`

- Compilar + pruebas:

PowerShell / cmd:

```cmd
cd /d backend
.\gradlew.bat clean build --no-daemon
```

Git Bash (MSYS2):

```bash
cd backend
./gradlew clean build --no-daemon
```

Si ves el error `JAVA_HOME is set to an invalid directory`, corrige tu JAVA_HOME:

```bash
# Descubre JDKs instalados (ajusta según tu instalación)
ls "/c/Program Files/Java"
ls "/c/Program Files/Eclipse Adoptium"

# Ejemplo: apuntar a un JDK válido (modifica la ruta real encontrada)
export JAVA_HOME="/c/Program Files/Java/jdk-17.0.11"
export PATH="$JAVA_HOME/bin:$PATH"

# Vuelve a compilar
cd backend && ./gradlew clean build --no-daemon
```

Autodetección de JDK (Windows):

```powershell
# Desde la raíz del repo
powershell -ExecutionPolicy Bypass -File scripts\backend-build.ps1
```
Este script localizará automáticamente un JDK válido (17/21), configurará JAVA_HOME y ejecutará `gradlew.bat clean build`.

- Ejecutar migraciones Flyway (requiere DB disponible):

```cmd
cd /d "c:\Users\ignac\Documents\Centro de modelacion xd\PYMERP\pymerp"
# Opción 1 (recomendada en Windows/macOS con Docker Desktop): usa host.docker.internal y puerto 55432
powershell -ExecutionPolicy Bypass -File scripts\flyway-migrate.ps1

# Opción 2 (personalizar manualmente la URL/credenciales)
powershell -ExecutionPolicy Bypass -File scripts\flyway-migrate.ps1 `
  -DatabaseUrl "jdbc:postgresql://host.docker.internal:55432/pymes" `
  -DatabaseUser "pymes" `
  -DatabasePassword "pymes"

# Si ejecutas Flyway dentro de la misma red de docker-compose, puedes usar el hostname del servicio:
# powershell -ExecutionPolicy Bypass -File scripts\flyway-migrate.ps1 -DatabaseUrl "jdbc:postgresql://postgres:5432/pymes"
```

- Arrancar en modo simple (puerto 8080):

```cmd
cd /d backend
.\gradlew.bat bootRun
```

- Arrancar perfil dev (puerto 8081) desde cmd:

```cmd
cd /d backend
gradlew.bat bootRun --args="--spring.profiles.active=dev --server.port=8081"
```

- Arrancar con depuración (JDWP 5005): usa la tarea de VS Code "bootRun:dev (debug 5005)" (ver sección 7), o desde cmd:

PowerShell (recomendado):

```powershell
cd /d backend
./gradlew.bat "-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" bootRun --args="--spring.profiles.active=dev --server.port=8081"
```

cmd.exe:

```cmd
cd /d backend
gradlew.bat bootRun --args="--spring.profiles.active=dev --server.port=8081" -Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
```

Notas:
- En PowerShell, si prefieres, también puedes usar el operador `--%` para evitar parsing de la shell:

  ```powershell
  ./gradlew.bat --% -Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 bootRun --args="--spring.profiles.active=dev --server.port=8081"
  ```
- Si ves el prompt `¿Desea terminar el trabajo por lotes (S/N)?`, usa las tareas de VS Code o antepone `call` al encadenar `.bat`.

- Ejecutar solo tests:

```cmd
cd /d backend
gradlew.bat clean test --no-daemon
```

Endpoints principales:
- POST `http://localhost:8080/api/v1/billing/invoices` (o 8081 si perfil dev)
- POST `http://localhost:8080/api/v1/billing/non-fiscal`
- GET  `http://localhost:8080/api/v1/billing/documents/{id}`
- GET  `http://localhost:8080/api/v1/billing/documents/{id}/files/{version}` (version: LOCAL|OFFICIAL)
- POST `http://localhost:8080/webhooks/sii` (headers: `X-SII-Signature`, `X-SII-Timestamp`)

Notas:
- El proyecto incluye un proveedor SII stub para emitir sin dependencia externa.
- Descarga de archivos soporta `contentType=xml` para obtener el XML oficial.

## 5) UI (React + Vite)

Ubicación: `ui/`

- Instalar dependencias y levantar en modo dev (Vite):

```cmd
cd /d ui
npm ci
npm run dev
```

- Build de producción:

```cmd
cd /d ui
npm ci
npm run build
```

- Configurar la base de la API:
  - La UI usa `VITE_API_BASE` (por defecto `/api`).
  - En desarrollo, puedes apuntar al backend directamente, por ejemplo:

```cmd
set VITE_API_BASE=http://localhost:8080/api/v1
npm run dev
```

Sugerencia: crea un archivo `ui/.env.local` con `VITE_API_BASE=http://localhost:8080/api/v1` para no setearla cada vez.

Nota Flyway: si ves un error tipo "Connection to localhost:5432 refused" al usar el script por defecto, recuerda que dentro del contenedor `flyway` "localhost" no apunta al host. Usa `host.docker.internal` y el puerto mapeado (por defecto `55432`) o conecta por el hostname del servicio dentro de la red de compose (`postgres:5432`).

## 6) App Flutter

Ubicación: `app_flutter/`

- Preparar dependencias y código generado:

```cmd
cd /d app_flutter
flutter pub get
flutter pub run build_runner build --delete-conflicting-outputs
```

- Análisis y pruebas:

```cmd
flutter analyze
flutter test
```

- Ejecutar app de escritorio Windows (ejemplo):

```cmd
flutter run -d windows -t lib\main.dart --dart-define=BASE_URL=http://localhost:8080/api/v1 --dart-define=COMPANY_ID=dev-company
```

Notas:
- La app por defecto usa `Env.baseUrl = http://localhost:8080/api/v1`. Puedes sobreescribir con `--dart-define=BASE_URL=...`.
- El repositorio incluye lógica de polling HTTP para observar documentos.

## 7) Tareas de VS Code ya incluidas

En `.vscode/tasks.json` (o configuradas por el entorno) existen estas tareas útiles:

- "Test Protected Endpoint": ejecuta `scripts/test-protected.ps1`.
- "bootRun:dev (debug 5005)": levanta backend con perfil dev (8081) y depuración.
- "bootRun:dev (sin debug)": levanta backend con perfil dev (8081).
- "Iniciar backend (windows)": `gradlew.bat bootRun` (puerto 8080).
- "Iniciar backend (ruta backend)": `backend\gradlew.bat bootRun`.

Sugerencia Windows: si al ejecutar `.bat` ves el prompt `¿Desea terminar el trabajo por lotes (S/N)?`, usa estas tareas o antepone `call` al invocar scripts `.bat` desde otros `.bat`.

## 8) Flujo recomendado (todo en local)

1) Levanta servicios con Docker:
```cmd
docker compose up -d
```
2) Aplica migraciones Flyway (DB arriba):
```cmd
powershell -ExecutionPolicy Bypass -File scripts\flyway-migrate.ps1
```
3) Backend:
```cmd
cd /d backend
gradlew.bat bootRun
```
4) UI en dev (en otra terminal):
```cmd
cd /d ui
set VITE_API_BASE=http://localhost:8080/api/v1
npm run dev
```
5) (Opcional) Flutter:
```cmd
cd /d app_flutter
flutter pub get
flutter pub run build_runner build --delete-conflicting-outputs
flutter run -d windows -t lib\main.dart --dart-define=BASE_URL=http://localhost:8080/api/v1
```

## 9) Resolución de problemas (FAQ)

- "No existe Gradle build en la carpeta": ejecuta comandos dentro de `backend/`.
- Prompt "¿Desea terminar el trabajo por lotes (S/N)?": usa tareas de VS Code o `call gradlew.bat ...`.
- UI no llega al backend: verifica `VITE_API_BASE` y CORS/proxy; backend disponible en `http://localhost:8080` (o 8081 si dev).
- Migraciones Flyway fallan: confirma que Postgres esté arriba (`docker compose ps`) y credenciales en variables.
- MinIO/Mailhog no responden: confirma `S3_ENDPOINT` y `MAIL_HOST/MAIL_PORT`.

---

Con esto deberías poder instalar dependencias, compilar y arrancar cada componente del proyecto en Windows. Si necesitas una variante específica (por ejemplo, empaquetado desktop/Tauri), dímelo y agrego una sección dedicada.

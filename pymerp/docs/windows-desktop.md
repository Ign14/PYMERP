# Windows Desktop Packaging - PyMEs Suite

## Requerimientos
- Node.js 18+
- Rust + cargo (https://rustup.rs)
- Visual Studio Build Tools (MSVC) / Windows 10 SDK
- `yarn` o `npm`

## Pasos
1. Crear workspace Tauri
   ```bash
   cd desktop
   cargo tauri init --ci --app-name "PyMEs Suite" --dev-path "../ui" --dist-dir "../ui/dist"
   ```
2. Ajustar `tauri.conf.json`
   - `build.beforeBuildCommand`: `npm install && npm run build`
   - `build.devPath`: `../ui`
   - `build.distDir`: `../ui/dist`
   - `package.productName`: `PyMEs Suite`
   - `package.version`: sincronizar con `package.json`
3. Crear comandos nativos (opcional)
   - Offline sync, lector de documentos, integraciones.
4. Construir instalador
   ```bash
   npm run build:desktop  # script que ejecute cargo tauri build
   ```
5. Generar ejecutable `.exe`
   ```bash
   # Añadir flag de bundles para producir .msi y .exe en una sola corrida
   cargo tauri build --bundles msi,exe
   ```
   - El ejecutable sin instalador queda en `src-tauri/target/release/bundle/exe/`.
6. Distribución
   - Resultado MSI: `src-tauri/target/release/bundle/msi/pymes-suite_x64.msi`
   - Resultado EXE: `src-tauri/target/release/bundle/exe/pymes-suite_x64.exe`
   - Firmar con certificado corporativo (signtool.exe).

## Flujo sugerido
- CI: GitHub Actions con matrices (win-latest) ejecutando build Tauri.
- Auto-update: habilitar `tauri.conf.json > updater` con server S3/MinIO.

## Integraciones futuras
- Offline-first: incrustar SQLite y sincronización (compartido con Flutter app).
- Lectura de códigos de barra: wrapper nativo (Rust + librería HID).
- Impresión: exponer comando Tauri para spooler Windows.

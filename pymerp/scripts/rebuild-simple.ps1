# Script simplificado de rebuild con correcciones
param([string]$Version = "0.1.1")

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " PYMERP - Rebuild con Correcciones" -ForegroundColor White
Write-Host " Version: $Version" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$startTime = Get-Date
$projectRoot = Split-Path -Parent $PSScriptRoot

# Paso 1: Limpiar
Write-Host "[1/7] Limpiando builds anteriores..." -ForegroundColor Yellow
if (Test-Path "$projectRoot\ui\dist") { Remove-Item "$projectRoot\ui\dist" -Recurse -Force -ErrorAction SilentlyContinue }
if (Test-Path "$projectRoot\desktop\dist") { Remove-Item "$projectRoot\desktop\dist" -Recurse -Force -ErrorAction SilentlyContinue }
if (Test-Path "$projectRoot\dist\windows") { Remove-Item "$projectRoot\dist\windows" -Recurse -Force -ErrorAction SilentlyContinue }
Write-Host "  OK - Limpieza completada" -ForegroundColor Green

# Paso 2: Instalar dependencias Frontend
Write-Host "[2/7] Instalando dependencias del Frontend..." -ForegroundColor Yellow
Push-Location "$projectRoot\ui"
npm install --silent 2>$null
if ($LASTEXITCODE -ne 0) { throw "Error en npm install" }
Pop-Location
Write-Host "  OK - Dependencias instaladas" -ForegroundColor Green

# Paso 3: Compilar Frontend
Write-Host "[3/7] Compilando Frontend..." -ForegroundColor Yellow
Push-Location "$projectRoot\ui"
npm run build
if ($LASTEXITCODE -ne 0) { throw "Error compilando frontend" }
Pop-Location
Write-Host "  OK - Frontend compilado" -ForegroundColor Green

# Paso 4: Verificar Backend JAR
Write-Host "[4/7] Verificando Backend JAR..." -ForegroundColor Yellow
$backendJar = Get-ChildItem -Path "$projectRoot\backend\build\libs" -Filter "*.jar" -ErrorAction SilentlyContinue | 
              Where-Object { $_.Name -notlike "*-plain.jar" } | Select-Object -First 1

if (-not $backendJar) {
    Write-Host "  Backend no encontrado, compilando..." -ForegroundColor Cyan
    Push-Location "$projectRoot\backend"
    .\gradlew.bat clean build -x test
    if ($LASTEXITCODE -ne 0) { throw "Error compilando backend" }
    Pop-Location
    $backendJar = Get-ChildItem -Path "$projectRoot\backend\build\libs" -Filter "*.jar" | 
                  Where-Object { $_.Name -notlike "*-plain.jar" } | Select-Object -First 1
}
Write-Host "  OK - Backend listo: $($backendJar.Name)" -ForegroundColor Green

# Paso 5: Copiar Frontend a Desktop
Write-Host "[5/7] Copiando Frontend a Desktop..." -ForegroundColor Yellow
if (Test-Path "$projectRoot\desktop\dist") { Remove-Item "$projectRoot\desktop\dist" -Recurse -Force }
Copy-Item -Path "$projectRoot\ui\dist" -Destination "$projectRoot\desktop\dist" -Recurse -Force
Write-Host "  OK - Frontend copiado" -ForegroundColor Green

# Paso 6: Instalar dependencias Desktop
Write-Host "[6/7] Preparando Tauri..." -ForegroundColor Yellow
Push-Location "$projectRoot\desktop"
npm install --silent 2>$null
if ($LASTEXITCODE -ne 0) { throw "Error en npm install (desktop)" }
Pop-Location
Write-Host "  OK - Tauri preparado" -ForegroundColor Green

# Paso 7: Build Tauri
Write-Host "[7/7] Generando instalador Windows (esto puede tardar)..." -ForegroundColor Yellow
Push-Location "$projectRoot\desktop"
npm run build
if ($LASTEXITCODE -ne 0) { throw "Error en tauri build" }
Pop-Location
Write-Host "  OK - Instalador generado" -ForegroundColor Green

# Copiar a dist
Write-Host ""
Write-Host "Copiando archivos finales..." -ForegroundColor Yellow
$tauriBundle = "$projectRoot\desktop\src-tauri\target\release\bundle\msi"
$finalDist = "$projectRoot\dist\windows"

if (Test-Path $tauriBundle) {
    New-Item -ItemType Directory -Path $finalDist -Force | Out-Null
    Get-ChildItem -Path $tauriBundle -Filter "*.msi" | ForEach-Object {
        Copy-Item -Path $_.FullName -Destination $finalDist -Force
        Write-Host "  Copiado: $($_.Name)" -ForegroundColor Cyan
    }
}

$endTime = Get-Date
$duration = $endTime - $startTime

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host " BUILD COMPLETADO" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host "Duracion: $($duration.ToString('hh\:mm\:ss'))" -ForegroundColor White
Write-Host "Version: $Version" -ForegroundColor White
Write-Host ""
Write-Host "Archivos generados en: $finalDist" -ForegroundColor Cyan
Write-Host ""

if (Test-Path $finalDist) {
    Get-ChildItem -Path $finalDist -Filter "*.msi" | ForEach-Object {
        Write-Host "  -> $($_.Name) ($([math]::Round($_.Length/1MB, 2)) MB)" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "Correcciones incluidas:" -ForegroundColor Cyan
Write-Host "  * Rutas API corregidas" -ForegroundColor White
Write-Host "  * Manejo de errores mejorado" -ForegroundColor White
Write-Host "  * Top 10 Clientes con datos reales" -ForegroundColor White
Write-Host "  * Mensajes de error de pronosticos mejorados" -ForegroundColor White
Write-Host ""


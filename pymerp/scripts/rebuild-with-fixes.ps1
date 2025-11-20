# ========================================
# Script de Recompilación con Mejoras
# PYMERP - Aplicar correcciones y generar instalador
# ========================================

param(
    [string]$Version = "0.1.1",
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"

function Write-Success { Write-Host "✓ $args" -ForegroundColor Green }
function Write-Info { Write-Host "ℹ $args" -ForegroundColor Cyan }
function Write-Error { Write-Host "✗ $args" -ForegroundColor Red }
function Write-Header { 
    param([string]$Text)
    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "  $Text" -ForegroundColor White
    Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
}

Clear-Host
Write-Host @"

╔════════════════════════════════════════════════════╗
║                                                    ║
║     PYMERP - Recompilación con Correcciones      ║
║              Versión: $Version                    ║
║                                                    ║
╚════════════════════════════════════════════════════╝

"@ -ForegroundColor Magenta

$startTime = Get-Date
$projectRoot = Split-Path -Parent $PSScriptRoot

Write-Header "📋 Resumen de Correcciones Aplicadas"
Write-Info "✓ Rutas API duplicadas corregidas (/api/v1 → /v1)"
Write-Info "✓ Manejo de errores HTTP mejorado"
Write-Info "✓ Top 10 Clientes con datos reales"
Write-Info "✓ Mensajes de error de pronósticos mejorados"
Write-Info "✓ Interceptor de respuesta HTTP agregado"
Write-Host ""

# ========================================
# Paso 1: Limpiar builds anteriores
# ========================================
Write-Header "🧹 Limpiando builds anteriores"

$dirsToClean = @(
    "$projectRoot\ui\dist",
    "$projectRoot\desktop\src-tauri\target\release",
    "$projectRoot\dist\windows"
)

foreach ($dir in $dirsToClean) {
    if (Test-Path $dir) {
        Write-Info "Limpiando: $dir"
        Remove-Item -Path $dir -Recurse -Force -ErrorAction SilentlyContinue
    }
}
Write-Success "Limpieza completada"

# ========================================
# Paso 2: Instalar dependencias del Frontend
# ========================================
Write-Header "📦 Instalando dependencias del Frontend"

Push-Location "$projectRoot\ui"
try {
    Write-Info "Ejecutando npm install..."
    npm install --silent
    if ($LASTEXITCODE -ne 0) {
        throw "Error en npm install"
    }
    Write-Success "Dependencias instaladas"
} finally {
    Pop-Location
}

# ========================================
# Paso 3: Compilar Frontend
# ========================================
Write-Header "🔨 Compilando Frontend (React + Vite)"

Push-Location "$projectRoot\ui"
try {
    Write-Info "Ejecutando npm run build..."
    npm run build
    if ($LASTEXITCODE -ne 0) {
        throw "Error al compilar frontend"
    }
    
    $distSize = (Get-ChildItem "$projectRoot\ui\dist" -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB
    Write-Success "Frontend compilado ($('{0:N2}' -f $distSize) MB)"
} finally {
    Pop-Location
}

# ========================================
# Paso 4: Verificar Backend JAR
# ========================================
Write-Header "☕ Verificando Backend"

$backendJar = Get-ChildItem -Path "$projectRoot\backend\build\libs" -Filter "*.jar" -ErrorAction SilentlyContinue | 
              Where-Object { $_.Name -notlike "*-plain.jar" } | 
              Select-Object -First 1

if (-not $backendJar) {
    Write-Info "Backend JAR no encontrado, compilando..."
    Push-Location "$projectRoot\backend"
    try {
        .\gradlew.bat clean build -x test
        if ($LASTEXITCODE -ne 0) {
            throw "Error al compilar backend"
        }
        $backendJar = Get-ChildItem -Path "$projectRoot\backend\build\libs" -Filter "*.jar" | 
                      Where-Object { $_.Name -notlike "*-plain.jar" } | 
                      Select-Object -First 1
    } finally {
        Pop-Location
    }
}

if ($backendJar) {
    $jarSize = $backendJar.Length / 1MB
    Write-Success "Backend JAR listo: $($backendJar.Name) ($('{0:N2}' -f $jarSize) MB)"
} else {
    Write-Error "No se pudo compilar el backend"
    exit 1
}

# ========================================
# Paso 5: Copiar Frontend al Desktop
# ========================================
Write-Header "📂 Copiando Frontend a Desktop/Tauri"

$destDir = "$projectRoot\desktop\dist"
if (Test-Path $destDir) {
    Remove-Item -Path $destDir -Recurse -Force
}

Copy-Item -Path "$projectRoot\ui\dist" -Destination $destDir -Recurse -Force
Write-Success "Frontend copiado a desktop/dist"

# ========================================
# Paso 6: Instalar dependencias de Tauri
# ========================================
Write-Header "🦀 Preparando Tauri"

Push-Location "$projectRoot\desktop"
try {
    Write-Info "Instalando dependencias de Node..."
    npm install --silent
    if ($LASTEXITCODE -ne 0) {
        throw "Error en npm install (desktop)"
    }
    Write-Success "Dependencias de Tauri instaladas"
} finally {
    Pop-Location
}

# ========================================
# Paso 7: Build de Tauri (Instalador Windows)
# ========================================
Write-Header "🪟 Generando Instalador para Windows"

Push-Location "$projectRoot\desktop"
try {
    Write-Info "Ejecutando npm run tauri build..."
    Write-Info "Esto puede tomar varios minutos..."
    
    npm run tauri build
    
    if ($LASTEXITCODE -ne 0) {
        throw "Error al compilar Tauri"
    }
    
    Write-Success "Instalador generado exitosamente"
} finally {
    Pop-Location
}

# ========================================
# Paso 8: Copiar instalador a dist/
# ========================================
Write-Header "📦 Organizando archivos finales"

$tauriBundle = "$projectRoot\desktop\src-tauri\target\release\bundle\msi"
$finalDist = "$projectRoot\dist\windows"

if (Test-Path $tauriBundle) {
    New-Item -ItemType Directory -Path $finalDist -Force | Out-Null
    
    Get-ChildItem -Path $tauriBundle -Filter "*.msi" | ForEach-Object {
        Copy-Item -Path $_.FullName -Destination $finalDist -Force
        Write-Success "Copiado: $($_.Name)"
    }
    
    Get-ChildItem -Path $tauriBundle -Filter "*.msi.zip" | ForEach-Object {
        Copy-Item -Path $_.FullName -Destination $finalDist -Force
        Write-Success "Copiado: $($_.Name)"
    }
} else {
    Write-Warning "No se encontraron archivos MSI en $tauriBundle"
}

# ========================================
# Paso 9: Generar reporte
# ========================================
Write-Header "📊 Generando Reporte"

$endTime = Get-Date
$duration = $endTime - $startTime

$report = @"
╔════════════════════════════════════════════════════╗
║                                                    ║
║          BUILD COMPLETADO EXITOSAMENTE            ║
║                                                    ║
╚════════════════════════════════════════════════════╝

📅 Fecha: $($endTime.ToString("yyyy-MM-dd HH:mm:ss"))
⏱️  Duración: $($duration.ToString("hh\:mm\:ss"))
📦 Versión: $Version

📁 ARCHIVOS GENERADOS:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

"@

Write-Host $report -ForegroundColor Green

if (Test-Path $finalDist) {
    Get-ChildItem -Path $finalDist -File | ForEach-Object {
        $size = $_.Length / 1MB
        $sizeFormatted = "{0:N2}" -f $size
        Write-Host "  📦 $($_.Name)" -ForegroundColor Cyan
        Write-Host "     Tamano: $sizeFormatted MB" -ForegroundColor Gray
        Write-Host "     Ubicacion: $($_.FullName)" -ForegroundColor Gray
        Write-Host ""
    }
}

Write-Host @"
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✅ CORRECCIONES INCLUIDAS:
  • Rutas API corregidas (inventory.ts, client.ts)
  • Interceptor de errores HTTP agregado
  • Top 10 Clientes con datos reales
  • Mensajes de error mejorados en pronósticos
  • Resumen financiero con botón reintentar

📋 PRÓXIMOS PASOS:
  1. Probar el instalador en: $finalDist
  2. Instalar en un sistema limpio para testing
  3. Verificar todas las funcionalidades corregidas:
     - Crear compras
     - Crear ubicaciones
     - Ver resumen financiero
     - Ver pronósticos
     - Ver top 10 clientes

🚀 Para instalar: Ejecuta el archivo .msi como administrador

"@ -ForegroundColor White

Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""


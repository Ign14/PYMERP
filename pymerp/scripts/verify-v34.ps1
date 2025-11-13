# Script de Verificación Post-V34
# Verifica que la migración V34 se ejecutó correctamente

Write-Host "`n=== VERIFICACION DE MIGRACION V34 ===" -ForegroundColor Cyan
Write-Host "Fecha: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Gray

# Esperar a que el backend esté listo
Write-Host "`n[1/5] Verificando salud del backend..." -ForegroundColor Yellow
$maxAttempts = 30
$attempt = 0
$healthy = $false

while ($attempt -lt $maxAttempts -and -not $healthy) {
    try {
        $health = Invoke-RestMethod -Uri "http://localhost:8081/actuator/health" -ErrorAction Stop
        if ($health.status -eq "UP") {
            $healthy = $true
            Write-Host "  ✓ Backend saludable (status: UP)" -ForegroundColor Green
        }
    } catch {
        $attempt++
        Write-Host "  Intento $attempt/$maxAttempts..." -ForegroundColor Gray
        Start-Sleep -Seconds 2
    }
}

if (-not $healthy) {
    Write-Host "  ✗ Backend no respondió después de $maxAttempts intentos" -ForegroundColor Red
    exit 1
}

# Verificar productos creados
Write-Host "`n[2/5] Verificando productos creados..." -ForegroundColor Yellow
try {
    $products = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/products?size=50" -ErrorAction Stop
    $totalProducts = $products.totalElements
    
    if ($totalProducts -ge 20) {
        Write-Host "  ✓ $totalProducts productos encontrados" -ForegroundColor Green
        Write-Host "    - Productos activos: $($products.content.Count)" -ForegroundColor Gray
    } else {
        Write-Host "  ⚠ Solo $totalProducts productos (esperados: 20+)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ✗ Error al consultar productos: $($_.Exception.Message)" -ForegroundColor Red
}

# Verificar ubicaciones
Write-Host "`n[3/5] Verificando ubicaciones creadas..." -ForegroundColor Yellow
try {
    $locations = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/locations?size=50" -ErrorAction Stop
    $totalLocations = $locations.totalElements
    
    if ($totalLocations -ge 4) {
        Write-Host "  ✓ $totalLocations ubicaciones encontradas" -ForegroundColor Green
    } else {
        Write-Host "  ⚠ Solo $totalLocations ubicaciones (esperadas: 4+)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ✗ Error al consultar ubicaciones: $($_.Exception.Message)" -ForegroundColor Red
}

# Verificar KPIs de inventario
Write-Host "`n[4/5] Verificando KPIs de inventario..." -ForegroundColor Yellow
try {
    $kpis = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/inventory/kpis" -ErrorAction Stop
    
    Write-Host "  ✓ KPIs calculados correctamente:" -ForegroundColor Green
    Write-Host "    - Cobertura de Stock: $($kpis.stockCoverageDays) días" -ForegroundColor Gray
    Write-Host "    - Ratio de Rotación: $($kpis.turnoverRatio)" -ForegroundColor Gray
    Write-Host "    - Stock Muerto: `$$($kpis.deadStockValue)" -ForegroundColor Gray
    Write-Host "    - Productos Activos: $($kpis.activeProducts)" -ForegroundColor Gray
    Write-Host "    - Stock Crítico: $($kpis.criticalStockProducts) productos" -ForegroundColor Gray
} catch {
    Write-Host "  ✗ Error al consultar KPIs: $($_.Exception.Message)" -ForegroundColor Red
}

# Verificar análisis ABC
Write-Host "`n[5/5] Verificando análisis ABC..." -ForegroundColor Yellow
try {
    $abc = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/inventory/abc-analysis" -ErrorAction Stop
    
    $classA = ($abc | Where-Object { $_.classification -eq 'A' }).Count
    $classB = ($abc | Where-Object { $_.classification -eq 'B' }).Count
    $classC = ($abc | Where-Object { $_.classification -eq 'C' }).Count
    
    Write-Host "  ✓ Análisis ABC generado:" -ForegroundColor Green
    Write-Host "    - Clase A: $classA productos" -ForegroundColor Gray
    Write-Host "    - Clase B: $classB productos" -ForegroundColor Gray
    Write-Host "    - Clase C: $classC productos" -ForegroundColor Gray
} catch {
    Write-Host "  ✗ Error al consultar análisis ABC: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n=== VERIFICACION COMPLETADA ===" -ForegroundColor Cyan
Write-Host "Siguiente paso: Abrir http://localhost:5173/app/inventory para verificar la UI" -ForegroundColor Green
Write-Host ""

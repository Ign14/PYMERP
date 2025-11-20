# Script de verificación del backend
Write-Host "🔍 Verificando estado del backend..." -ForegroundColor Cyan
Write-Host ""

# Check port
$port = netstat -ano | Select-String ":8081.*LISTENING"
if ($port) {
    Write-Host "✅ Puerto 8081 en uso (backend corriendo)" -ForegroundColor Green
} else {
    Write-Host "❌ Puerto 8081 no está en uso (backend no está corriendo)" -ForegroundColor Red
    exit 1
}

# Check health endpoint
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/actuator/health" -Method Get -TimeoutSec 5
    Write-Host "✅ Health endpoint respondiendo: $($response.status)" -ForegroundColor Green
    Write-Host ""
    Write-Host "📊 Detalles:" -ForegroundColor Yellow
    $response | ConvertTo-Json -Depth 3 | Write-Host
} catch {
    Write-Host "❌ Health endpoint no responde: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "🎉 Backend funcionando correctamente!" -ForegroundColor Green
Write-Host ""
Write-Host "📍 Endpoints disponibles:" -ForegroundColor Cyan
Write-Host "   • API: http://localhost:8081/api/v1/" -ForegroundColor White
Write-Host "   • H2 Console: http://localhost:8081/h2-console" -ForegroundColor White
Write-Host "   • Health: http://localhost:8081/actuator/health" -ForegroundColor White
Write-Host ""


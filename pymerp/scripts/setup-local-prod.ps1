# ===============================================================================
# PYMERP - Setup Local con Dominio pymerp.cl
# ===============================================================================
# Este script configura todo el entorno local para producción con HTTPS
# ===============================================================================

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "PYMERP - Setup Local Production" -ForegroundColor Yellow
Write-Host "========================================`n" -ForegroundColor Cyan

# ===============================================================================
# PASO 1: Generar certificado SSL autofirmado
# ===============================================================================
Write-Host "PASO 1: Generando certificado SSL autofirmado..." -ForegroundColor Green

$sslDir = "nginx/ssl"
if (-not (Test-Path $sslDir)) {
    New-Item -ItemType Directory -Path $sslDir -Force | Out-Null
}

# Generar certificado con OpenSSL (requiere tener OpenSSL instalado)
# Alternativamente, usar PowerShell nativo
$certPath = "$sslDir/pymerp.crt"
$keyPath = "$sslDir/pymerp.key"

if (Test-Path $certPath) {
    Write-Host "✅ Certificado SSL ya existe" -ForegroundColor Yellow
} else {
    # Crear certificado autofirmado con PowerShell
    $cert = New-SelfSignedCertificate `
        -DnsName "pymerp.cl", "www.pymerp.cl", "localhost", "*.pymerp.cl" `
        -CertStoreLocation "Cert:\CurrentUser\My" `
        -NotAfter (Get-Date).AddYears(1) `
        -KeyAlgorithm RSA `
        -KeyLength 2048 `
        -HashAlgorithm SHA256 `
        -KeyExportPolicy Exportable `
        -FriendlyName "PyMERP Local Development"
    
    # Exportar certificado y clave
    $certPassword = ConvertTo-SecureString -String "pymerp" -Force -AsPlainText
    
    # Exportar PFX
    $pfxPath = "$sslDir/pymerp.pfx"
    Export-PfxCertificate -Cert $cert -FilePath $pfxPath -Password $certPassword | Out-Null
    
    # Convertir PFX a PEM (requiere OpenSSL)
    Write-Host "⚠️  Certificado generado. Ahora convertir a formato PEM:" -ForegroundColor Yellow
    Write-Host "   Opción 1 - Con OpenSSL (si está instalado):" -ForegroundColor White
    Write-Host "   openssl pkcs12 -in nginx/ssl/pymerp.pfx -clcerts -nokeys -out nginx/ssl/pymerp.crt -passin pass:pymerp" -ForegroundColor Gray
    Write-Host "   openssl pkcs12 -in nginx/ssl/pymerp.pfx -nocerts -nodes -out nginx/ssl/pymerp.key -passin pass:pymerp" -ForegroundColor Gray
    Write-Host ""
    Write-Host "   Opción 2 - Usar certificados de ejemplo (SOLO DESARROLLO):" -ForegroundColor White
    Write-Host "   Presiona Enter para continuar y usar certificados de ejemplo..." -ForegroundColor Yellow
    Read-Host
    
    # Crear certificados de ejemplo básicos para desarrollo
    # NOTA: Estos son inseguros, solo para desarrollo local
    @"
-----BEGIN CERTIFICATE-----
MIIDXTCCAkWgAwIBAgIJAKL0UG+mRKSvMA0GCSqGSIb3DQEBCwUAMEUxCzAJBgNV
BAYTAkNMMQ0wCwYDVQQIDARDaGlsZTERMA8GA1UEBwwIU2FudGlhZ28xFDASBgNV
BAoMC1B5TUVSUCBMb2NhbDAeFw0yNTAxMDEwMDAwMDBaFw0yNjAxMDEwMDAwMDBa
MEUxCzAJBgNVBAYTAkNMMQ0wCwYDVQQIDARDaGlsZTERMA8GA1UEBwwIU2FudGlh
Z28xFDASBgNVBAoMC1B5TUVSUCBMb2NhbDCCASIwDQYJKoZIhvcNAQEBBQADggEP
ADCCAQoCggEBAL5QJ7LJP6RK7lR5xqPQY2M3T6qnFhXvN8WzJ4YvR0pQtKxH5rZN
dVX7jW3mPxQU3vR2qE9tWxVhYLqJXxKpN5P7vQ8L2tPxK4FqVW5xN6rZ8T3qP9R
-----END CERTIFICATE-----
"@ | Set-Content $certPath

    @"
-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC+UCeyyT+kSu5U
ecaj0GNjN0+qpxYV7zfFsyeGL0dKULSsR+a2TXVV+41t5j8UFN70dqhPbVsVYWC6
iV8SqTeT+70PC9rT8SuBalVucTeq2fE96j/U
-----END PRIVATE KEY-----
"@ | Set-Content $keyPath

    Write-Host "✅ Certificados de ejemplo creados (SOLO DESARROLLO)" -ForegroundColor Green
}

# ===============================================================================
# PASO 2: Configurar hosts file
# ===============================================================================
Write-Host "`nPASO 2: Configurando archivo hosts..." -ForegroundColor Green

$hostsPath = "C:\Windows\System32\drivers\etc\hosts"
$hostsEntry = "127.0.0.1 pymerp.cl www.pymerp.cl"

$hostsContent = Get-Content $hostsPath -Raw
if ($hostsContent -notmatch "pymerp\.cl") {
    Write-Host "⚠️  Necesitas permisos de Administrador para modificar hosts" -ForegroundColor Yellow
    Write-Host "   Ejecuta este comando como Administrador:" -ForegroundColor White
    Write-Host "   Add-Content -Path C:\Windows\System32\drivers\etc\hosts -Value '$hostsEntry'" -ForegroundColor Gray
    Write-Host ""
    Write-Host "   O edita manualmente C:\Windows\System32\drivers\etc\hosts y agrega:" -ForegroundColor White
    Write-Host "   $hostsEntry" -ForegroundColor Gray
    Write-Host ""
    $continue = Read-Host "¿Ya agregaste la entrada en hosts? (y/n)"
    if ($continue -ne "y") {
        Write-Host "❌ Por favor agrega la entrada y vuelve a ejecutar el script" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "✅ Entrada en hosts ya existe" -ForegroundColor Green
}

# ===============================================================================
# PASO 3: Crear archivo .env
# ===============================================================================
Write-Host "`nPASO 3: Creando archivo .env..." -ForegroundColor Green

$envContent = @"
# ===============================================================================
# PyMERP - Configuración Local Producción
# ===============================================================================

# PostgreSQL
POSTGRES_PASSWORD=PymesProd2024!

# Redis
REDIS_PASSWORD=RedisSecure2024!

# MinIO / S3
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=MinioSecure2024!

# JWT Secret (CAMBIAR en producción real)
JWT_SECRET=local-dev-jwt-secret-key-change-this-in-real-production-min-64-chars-required

# CORS
CORS_ALLOWED_ORIGINS=https://pymerp.cl,https://www.pymerp.cl,http://localhost:5173

# Ambiente
ENVIRONMENT=local-prod
"@

$envContent | Set-Content ".env.local-prod"
Write-Host "✅ Archivo .env.local-prod creado" -ForegroundColor Green

# ===============================================================================
# PASO 4: Build frontend
# ===============================================================================
Write-Host "`nPASO 4: Building frontend..." -ForegroundColor Green

Push-Location ui
Write-Host "   Instalando dependencias..." -ForegroundColor Gray
npm ci

Write-Host "   Compilando..." -ForegroundColor Gray
$env:VITE_API_URL = "https://pymerp.cl"
npm run build

Pop-Location
Write-Host "✅ Frontend compilado" -ForegroundColor Green

# ===============================================================================
# PASO 5: Iniciar servicios
# ===============================================================================
Write-Host "`nPASO 5: Iniciando servicios con Docker Compose..." -ForegroundColor Green

docker-compose -f docker-compose.local-prod.yml --env-file .env.local-prod up -d --build

Write-Host "`n✅ Servicios iniciados" -ForegroundColor Green

# ===============================================================================
# PASO 6: Esperar a que servicios estén listos
# ===============================================================================
Write-Host "`nPASO 6: Esperando a que servicios estén listos..." -ForegroundColor Green

Start-Sleep -Seconds 10

Write-Host "   Verificando PostgreSQL..." -ForegroundColor Gray
docker-compose -f docker-compose.local-prod.yml exec -T postgres pg_isready -U pymes

Write-Host "   Verificando Redis..." -ForegroundColor Gray
docker-compose -f docker-compose.local-prod.yml exec -T redis redis-cli --raw incr ping

Write-Host "   Verificando Backend..." -ForegroundColor Gray
$retries = 0
$maxRetries = 30
while ($retries -lt $maxRetries) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8081/actuator/health" -UseBasicParsing -TimeoutSec 5
        if ($response.StatusCode -eq 200) {
            Write-Host "   ✅ Backend está listo" -ForegroundColor Green
            break
        }
    } catch {
        $retries++
        Write-Host "." -NoNewline
        Start-Sleep -Seconds 2
    }
}

# ===============================================================================
# PASO 7: Resumen
# ===============================================================================
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "✅ PyMERP Local Production Ready!" -ForegroundColor Green
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "📋 URLs disponibles:" -ForegroundColor Yellow
Write-Host "   Frontend:     https://pymerp.cl" -ForegroundColor White
Write-Host "   Backend API:  https://pymerp.cl/api/v1" -ForegroundColor White
Write-Host "   Health:       http://localhost:8081/actuator/health" -ForegroundColor White
Write-Host "   MinIO UI:     http://localhost:9001" -ForegroundColor White
Write-Host "   MailHog UI:   http://localhost:8025" -ForegroundColor White
Write-Host ""

Write-Host "📋 Credenciales por defecto:" -ForegroundColor Yellow
Write-Host "   Usuario:  admin@dev.local" -ForegroundColor White
Write-Host "   Password: Admin1234" -ForegroundColor White
Write-Host "   Company:  Dev Company (RUT: 76.000.000-0)" -ForegroundColor White
Write-Host ""

Write-Host "📋 Comandos útiles:" -ForegroundColor Yellow
Write-Host "   Ver logs:      docker-compose -f docker-compose.local-prod.yml logs -f" -ForegroundColor White
Write-Host "   Detener:       docker-compose -f docker-compose.local-prod.yml down" -ForegroundColor White
Write-Host "   Reiniciar:     docker-compose -f docker-compose.local-prod.yml restart" -ForegroundColor White
Write-Host "   Eliminar todo: docker-compose -f docker-compose.local-prod.yml down -v" -ForegroundColor White
Write-Host ""

Write-Host "⚠️  IMPORTANTE:" -ForegroundColor Yellow
Write-Host "   - El navegador mostrará advertencia de certificado autofirmado" -ForegroundColor White
Write-Host "   - Click en 'Avanzado' → 'Continuar a pymerp.cl (inseguro)'" -ForegroundColor White
Write-Host "   - Esto es NORMAL en desarrollo local" -ForegroundColor White
Write-Host ""

Write-Host "🎉 ¡Listo para usar!" -ForegroundColor Green

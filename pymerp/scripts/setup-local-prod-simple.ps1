# PyMERP - Setup Local Production
Write-Host "PyMERP - Setup Local con dominio pymerp.cl" -ForegroundColor Cyan

# Paso 1: Crear estructura de directorios
Write-Host "`n[1/6] Creando directorios..." -ForegroundColor Yellow
New-Item -ItemType Directory -Path "nginx/ssl" -Force | Out-Null

# Paso 2: Generar certificado SSL
Write-Host "[2/6] Generando certificado SSL..." -ForegroundColor Yellow
Write-Host "Necesitas OpenSSL instalado. Ejecuta manualmente:" -ForegroundColor White
Write-Host "openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout nginx/ssl/pymerp.key -out nginx/ssl/pymerp.crt -subj '/CN=pymerp.cl'" -ForegroundColor Gray

# Paso 3: Configurar hosts
Write-Host "`n[3/6] Configurar archivo hosts..." -ForegroundColor Yellow
Write-Host "Ejecuta como ADMINISTRADOR:" -ForegroundColor White
Write-Host 'Add-Content -Path C:\Windows\System32\drivers\etc\hosts -Value "127.0.0.1 pymerp.cl www.pymerp.cl"' -ForegroundColor Gray

# Paso 4: Crear .env
Write-Host "`n[4/6] Creando archivo .env..." -ForegroundColor Yellow
@"
POSTGRES_PASSWORD=PymesProd2024!
REDIS_PASSWORD=RedisSecure2024!
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=MinioSecure2024!
JWT_SECRET=local-dev-jwt-secret-key-change-this-in-real-production-min-64-chars
"@ | Set-Content ".env.local-prod"
Write-Host "OK" -ForegroundColor Green

# Paso 5: Build frontend
Write-Host "`n[5/6] Building frontend..." -ForegroundColor Yellow
Push-Location ui
npm ci
$env:VITE_API_URL = "https://pymerp.cl"
npm run build
Pop-Location
Write-Host "OK" -ForegroundColor Green

# Paso 6: Iniciar Docker
Write-Host "`n[6/6] Iniciando Docker Compose..." -ForegroundColor Yellow
docker-compose -f docker-compose.local-prod.yml --env-file .env.local-prod up -d --build

Write-Host "`nAccede a: https://pymerp.cl" -ForegroundColor Cyan

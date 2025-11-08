# ===============================================================================
# PYMERP - Script de Aprovisionamiento DigitalOcean
# ===============================================================================
# ADVERTENCIA: Este script crea recursos facturables en DigitalOcean (~$67/mes)
# Asegúrate de tener doctl instalado y autenticado antes de ejecutar
#
# Prerrequisitos:
#   1. Instalar doctl: https://github.com/digitalocean/doctl/releases
#   2. Autenticar: doctl auth init
#   3. Verificar: doctl account get
# ===============================================================================

# Función para logging
function Write-Step {
    param([string]$Message)
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host $Message -ForegroundColor Yellow
    Write-Host "========================================`n" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$Message)
    Write-Host "✅ $Message" -ForegroundColor Green
}

function Write-Error {
    param([string]$Message)
    Write-Host "❌ $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "ℹ️  $Message" -ForegroundColor Blue
}

# Verificar que doctl está instalado
Write-Step "PASO 0: Verificando prerequisitos"
try {
    $doctlVersion = doctl version 2>$null
    Write-Success "doctl instalado: $doctlVersion"
} catch {
    Write-Error "doctl no está instalado"
    Write-Info "Descargar desde: https://github.com/digitalocean/doctl/releases"
    Write-Info "Después ejecutar: doctl auth init"
    exit 1
}

# Verificar autenticación
try {
    $account = doctl account get --format Email --no-header 2>$null
    Write-Success "Autenticado como: $account"
} catch {
    Write-Error "doctl no está autenticado"
    Write-Info "Ejecutar: doctl auth init"
    exit 1
}

# Confirmación antes de continuar
Write-Host "`n⚠️  ADVERTENCIA: Este script creará los siguientes recursos facturables:" -ForegroundColor Yellow
Write-Host "   - PostgreSQL Cluster (db-s-1vcpu-1gb): ~`$15/mes" -ForegroundColor White
Write-Host "   - Redis Cluster (db-s-1vcpu-512mb): ~`$15/mes" -ForegroundColor White
Write-Host "   - Container Registry: ~`$5/mes" -ForegroundColor White
Write-Host "   - App Platform (basic-xs): ~`$5/mes base + `$5/mes por instancia" -ForegroundColor White
Write-Host "   - Spaces (Storage): ~`$5/mes + `$0.02/GB transfer" -ForegroundColor White
Write-Host "`n   TOTAL ESTIMADO: ~`$67/mes`n" -ForegroundColor Yellow

$confirm = Read-Host "¿Continuar con el aprovisionamiento? (yes/no)"
if ($confirm -ne "yes") {
    Write-Info "Aprovisionamiento cancelado por el usuario"
    exit 0
}

# ===============================================================================
# PASO 1: Crear PostgreSQL Managed Database
# ===============================================================================
Write-Step "PASO 1: Creando PostgreSQL Managed Database"
Write-Info "Configuración: PostgreSQL 16, 1GB RAM, 1 vCPU, región NYC3"
Write-Info "Tiempo estimado: 5-10 minutos"

try {
    doctl databases create pymerp-db-cluster `
        --engine pg `
        --version 16 `
        --size db-s-1vcpu-1gb `
        --region nyc3 `
        --num-nodes 1
    
    Write-Success "PostgreSQL cluster creado, esperando a que esté online..."
    
    # Esperar hasta que esté online (máximo 15 minutos)
    $maxAttempts = 90
    $attempt = 0
    $dbOnline = $false
    
    while ($attempt -lt $maxAttempts -and -not $dbOnline) {
        Start-Sleep -Seconds 10
        $attempt++
        
        $status = doctl databases list --format Name,Status --no-header | Select-String "pymerp-db-cluster"
        if ($status -match "online") {
            $dbOnline = $true
            Write-Success "PostgreSQL cluster está ONLINE"
        } else {
            Write-Host "." -NoNewline
        }
    }
    
    if (-not $dbOnline) {
        Write-Error "Timeout esperando PostgreSQL cluster (15 minutos)"
        Write-Info "Verificar manualmente: doctl databases list"
    }
    
    # Obtener connection details
    $dbId = doctl databases list --format ID --no-header | Select-Object -First 1
    Write-Info "Database Cluster ID: $dbId"
    
    Write-Host "`n📋 Connection String:" -ForegroundColor Cyan
    doctl databases connection $dbId --format URI
    
    Write-Host "`nGuardar estos valores para PASO 4:" -ForegroundColor Yellow
    $connDetails = doctl databases connection $dbId
    Write-Host $connDetails
    
} catch {
    Write-Error "Error creando PostgreSQL cluster: $_"
    exit 1
}

# ===============================================================================
# PASO 2: Crear Redis Managed Database
# ===============================================================================
Write-Step "PASO 2: Creando Redis Managed Database"
Write-Info "Configuración: Redis 7, 512MB RAM, eviction policy allkeys-lru"
Write-Info "Tiempo estimado: 3-5 minutos"

try {
    doctl databases create pymerp-redis-cluster `
        --engine redis `
        --version 7 `
        --size db-s-1vcpu-512mb `
        --region nyc3 `
        --num-nodes 1 `
        --eviction-policy allkeys-lru
    
    Write-Success "Redis cluster creado, esperando a que esté online..."
    
    # Esperar hasta que esté online
    $maxAttempts = 60
    $attempt = 0
    $redisOnline = $false
    
    while ($attempt -lt $maxAttempts -and -not $redisOnline) {
        Start-Sleep -Seconds 5
        $attempt++
        
        $status = doctl databases list --format Name,Status --no-header | Select-String "pymerp-redis-cluster"
        if ($status -match "online") {
            $redisOnline = $true
            Write-Success "Redis cluster está ONLINE"
        } else {
            Write-Host "." -NoNewline
        }
    }
    
    if (-not $redisOnline) {
        Write-Error "Timeout esperando Redis cluster (5 minutos)"
        Write-Info "Verificar manualmente: doctl databases list"
    }
    
    # Obtener connection details
    $redisId = doctl databases list --format ID --no-header | Select-Object -Last 1
    Write-Info "Redis Cluster ID: $redisId"
    
    Write-Host "`n📋 Connection Details:" -ForegroundColor Cyan
    doctl databases connection $redisId
    
} catch {
    Write-Error "Error creando Redis cluster: $_"
    exit 1
}

# ===============================================================================
# PASO 3: Crear Container Registry
# ===============================================================================
Write-Step "PASO 3: Creando Container Registry"

try {
    # Verificar si ya existe
    $registryExists = doctl registry get 2>$null
    if ($registryExists) {
        Write-Info "Container Registry ya existe:"
        doctl registry get
    } else {
        doctl registry create pymerp
        Write-Success "Container Registry creado"
        
        Start-Sleep -Seconds 3
        doctl registry get
    }
    
    Write-Info "`nPara hacer login al registry:"
    Write-Host "doctl registry login" -ForegroundColor White
    
} catch {
    Write-Error "Error con Container Registry: $_"
    # No es crítico, continuar
}

# ===============================================================================
# PASO 4: Crear Spaces Bucket
# ===============================================================================
Write-Step "PASO 4: Creando Spaces Bucket"
Write-Info "NOTA: Spaces debe crearse via DigitalOcean Console"
Write-Host "`n📋 Pasos manuales:" -ForegroundColor Yellow
Write-Host "1. Ir a: https://cloud.digitalocean.com/spaces" -ForegroundColor White
Write-Host "2. Click 'Create Space'" -ForegroundColor White
Write-Host "3. Configurar:" -ForegroundColor White
Write-Host "   - Name: pymes-prod" -ForegroundColor White
Write-Host "   - Region: NYC3" -ForegroundColor White
Write-Host "   - ACL: Private" -ForegroundColor White
Write-Host "   - Enable versioning: Yes" -ForegroundColor White
Write-Host "4. Settings → Manage Keys → Generate New Key" -ForegroundColor White
Write-Host "5. Guardar Access Key y Secret Key`n" -ForegroundColor White

$spacesConfirm = Read-Host "¿Ya creaste el Space pymes-prod? (yes/no)"
if ($spacesConfirm -ne "yes") {
    Write-Info "Crear Space manualmente antes de continuar con App Platform"
}

# ===============================================================================
# PASO 5: Listar recursos creados
# ===============================================================================
Write-Step "PASO 5: Resumen de recursos creados"

Write-Host "`n📊 Databases:" -ForegroundColor Cyan
doctl databases list --format ID,Name,Engine,Version,Status,Region

Write-Host "`n📦 Container Registry:" -ForegroundColor Cyan
doctl registry get 2>$null

# ===============================================================================
# PASO 6: Guardar IDs para siguiente paso
# ===============================================================================
Write-Step "PASO 6: Guardar información para configuración"

$dbId = doctl databases list --format ID,Name --no-header | Select-String "pymerp-db-cluster" | ForEach-Object { $_.ToString().Split()[0] }
$redisId = doctl databases list --format ID,Name --no-header | Select-String "pymerp-redis-cluster" | ForEach-Object { $_.ToString().Split()[0] }

Write-Host "`n📝 Exportar estas variables para siguiente paso:" -ForegroundColor Yellow
Write-Host "`$env:DB_CLUSTER_ID=`"$dbId`"" -ForegroundColor White
Write-Host "`$env:REDIS_CLUSTER_ID=`"$redisId`"" -ForegroundColor White

# Obtener connection strings
Write-Host "`n📋 PostgreSQL Connection:" -ForegroundColor Cyan
$pgConn = doctl databases connection $dbId
$pgHost = ($pgConn | Select-String "host\s+=\s+(.+)").Matches.Groups[1].Value.Trim()
$pgPort = ($pgConn | Select-String "port\s+=\s+(\d+)").Matches.Groups[1].Value.Trim()
$pgUser = ($pgConn | Select-String "user\s+=\s+(.+)").Matches.Groups[1].Value.Trim()
$pgPassword = ($pgConn | Select-String "password\s+=\s+(.+)").Matches.Groups[1].Value.Trim()
$pgDatabase = ($pgConn | Select-String "database\s+=\s+(.+)").Matches.Groups[1].Value.Trim()

Write-Host "`$env:POSTGRES_HOST=`"$pgHost`"" -ForegroundColor White
Write-Host "`$env:POSTGRES_PORT=`"$pgPort`"" -ForegroundColor White
Write-Host "`$env:POSTGRES_USER=`"$pgUser`"" -ForegroundColor White
Write-Host "`$env:POSTGRES_PASSWORD=`"$pgPassword`"" -ForegroundColor White
Write-Host "`$env:POSTGRES_DB=`"$pgDatabase`"" -ForegroundColor White

Write-Host "`n📋 Redis Connection:" -ForegroundColor Cyan
$redisConn = doctl databases connection $redisId
$redisHost = ($redisConn | Select-String "host\s+=\s+(.+)").Matches.Groups[1].Value.Trim()
$redisPort = ($redisConn | Select-String "port\s+=\s+(\d+)").Matches.Groups[1].Value.Trim()
$redisPassword = ($redisConn | Select-String "password\s+=\s+(.+)").Matches.Groups[1].Value.Trim()

Write-Host "`$env:REDIS_HOST=`"$redisHost`"" -ForegroundColor White
Write-Host "`$env:REDIS_PORT=`"$redisPort`"" -ForegroundColor White
Write-Host "`$env:REDIS_PASSWORD=`"$redisPassword`"" -ForegroundColor White
Write-Host "`$env:REDIS_SSL_ENABLED=`"true`"" -ForegroundColor White

Write-Host "`n📋 Spaces (configurar manualmente):" -ForegroundColor Cyan
Write-Host "`$env:S3_ENDPOINT=`"https://nyc3.digitaloceanspaces.com`"" -ForegroundColor White
Write-Host "`$env:STORAGE_S3_BUCKET=`"pymes-prod`"" -ForegroundColor White
Write-Host "`$env:STORAGE_S3_ACCESS_KEY=`"<from-spaces-settings>`"" -ForegroundColor White
Write-Host "`$env:STORAGE_S3_SECRET_KEY=`"<from-spaces-settings>`"" -ForegroundColor White
Write-Host "`$env:STORAGE_S3_REGION=`"nyc3`"" -ForegroundColor White

# ===============================================================================
# PASO 7: Próximos pasos
# ===============================================================================
Write-Step "PASO 7: Próximos pasos"

Write-Host "✅ Infraestructura base creada exitosamente`n" -ForegroundColor Green

Write-Host "📋 Siguiente: Crear App Platform" -ForegroundColor Yellow
Write-Host "ANTES de crear App Platform, necesitas:" -ForegroundColor White
Write-Host "1. Copiar y ejecutar las variables de entorno de arriba" -ForegroundColor White
Write-Host "2. Configurar Spaces bucket manualmente" -ForegroundColor White
Write-Host "3. Actualizar do-app-spec.yml con los connection strings reales" -ForegroundColor White
Write-Host "4. Ejecutar: doctl apps create --spec do-app-spec.yml`n" -ForegroundColor White

Write-Host "📋 Después:" -ForegroundColor Yellow
Write-Host "1. Configurar DNS (docs/domain-ssl-setup.md)" -ForegroundColor White
Write-Host "2. Ejecutar scripts/setup-digitalocean-env.sh" -ForegroundColor White
Write-Host "3. Ejecutar scripts/pre-deploy-checklist.sh" -ForegroundColor White
Write-Host "4. Merge a main y deploy`n" -ForegroundColor White

Write-Host "💰 Costos estimados mensuales:" -ForegroundColor Cyan
Write-Host "   - PostgreSQL: `$15/mes" -ForegroundColor White
Write-Host "   - Redis: `$15/mes" -ForegroundColor White
Write-Host "   - Container Registry: `$5/mes" -ForegroundColor White
Write-Host "   - Spaces: `$5/mes + transfer" -ForegroundColor White
Write-Host "   - App Platform: `$5-15/mes (cuando se cree)" -ForegroundColor White
Write-Host "   - TOTAL: ~`$50-70/mes`n" -ForegroundColor White

Write-Success "Script completado"

# Script para probar el filtrado automático por tenant en Products
# Requiere que el backend esté corriendo en http://localhost:8081

$baseUrl = "http://localhost:8081/api/v1"

# Colores para output
function Write-Success { param($msg) Write-Host $msg -ForegroundColor Green }
function Write-Error { param($msg) Write-Host $msg -ForegroundColor Red }
function Write-Info { param($msg) Write-Host $msg -ForegroundColor Cyan }
function Write-Warning { param($msg) Write-Host $msg -ForegroundColor Yellow }

Write-Info "`n========================================="
Write-Info "  TEST: Filtrado Automático por Tenant"
Write-Info "=========================================`n"

# 1. Obtener token de autenticación
Write-Info "1. Autenticando como admin..."
$authBody = @{
    username = "admin"
    password = "admin123"
} | ConvertTo-Json

try {
    $authResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body $authBody -ContentType "application/json"
    $token = $authResponse.token
    Write-Success "   ✓ Token obtenido"
} catch {
    Write-Error "   ✗ Error de autenticación: $_"
    exit 1
}

# Headers con autenticación
$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# 2. Obtener las compañías del usuario
Write-Info "`n2. Obteniendo compañías del usuario admin..."
try {
    $companiesResponse = Invoke-RestMethod -Uri "$baseUrl/companies" -Method GET -Headers $headers
    $companies = $companiesResponse.content
    
    if ($companies.Count -lt 2) {
        Write-Warning "   ⚠ Solo hay $($companies.Count) compañía(s). Se necesitan al menos 2 para probar el filtrado."
        Write-Info "   Compañías disponibles:"
        $companies | ForEach-Object { Write-Info "     - $($_.name) (ID: $($_.id))" }
    } else {
        Write-Success "   ✓ Se encontraron $($companies.Count) compañías"
        $companies | ForEach-Object { Write-Info "     - $($_.name) (ID: $($_.id))" }
    }
    
    $company1 = $companies[0].id
    $company2 = if ($companies.Count -gt 1) { $companies[1].id } else { $null }
    
} catch {
    Write-Error "   ✗ Error obteniendo compañías: $_"
    exit 1
}

# 3. Crear productos para tenant 1
Write-Info "`n3. Creando productos para Tenant 1 (Company: $company1)..."
$headers1 = $headers.Clone()
$headers1["X-Company-Id"] = $company1

$product1Data = @{
    code = "TEST-TENANT1-001"
    name = "Producto Test Tenant 1"
    description = "Este producto pertenece al tenant 1"
    category = "TEST"
    unitPrice = 100.0
    taxRate = 0.19
    active = $true
} | ConvertTo-Json

try {
    $product1 = Invoke-RestMethod -Uri "$baseUrl/products" -Method POST -Body $product1Data -Headers $headers1
    Write-Success "   ✓ Producto creado: $($product1.name) (ID: $($product1.id))"
    $product1Id = $product1.id
} catch {
    Write-Error "   ✗ Error creando producto: $_"
    exit 1
}

# 4. Crear productos para tenant 2 (si existe)
if ($company2) {
    Write-Info "`n4. Creando productos para Tenant 2 (Company: $company2)..."
    $headers2 = $headers.Clone()
    $headers2["X-Company-Id"] = $company2

    $product2Data = @{
        code = "TEST-TENANT2-001"
        name = "Producto Test Tenant 2"
        description = "Este producto pertenece al tenant 2"
        category = "TEST"
        unitPrice = 200.0
        taxRate = 0.19
        active = $true
    } | ConvertTo-Json

    try {
        $product2 = Invoke-RestMethod -Uri "$baseUrl/products" -Method POST -Body $product2Data -Headers $headers2
        Write-Success "   ✓ Producto creado: $($product2.name) (ID: $($product2.id))"
        $product2Id = $product2.id
    } catch {
        Write-Error "   ✗ Error creando producto: $_"
        exit 1
    }
}

# 5. Listar productos con tenant 1 - debe ver solo el suyo
Write-Info "`n5. Listando productos con X-Company-Id: $company1..."
try {
    $listTenant1 = Invoke-RestMethod -Uri "$baseUrl/products" -Method GET -Headers $headers1
    Write-Info "   Productos visibles para Tenant 1:"
    $listTenant1.content | ForEach-Object { 
        Write-Info "     - $($_.name) (ID: $($_.id), Code: $($_.code))"
    }
    
    # Verificar que NO ve el producto del tenant 2
    if ($company2 -and $product2Id) {
        $seesProduct2 = $listTenant1.content | Where-Object { $_.id -eq $product2Id }
        if ($seesProduct2) {
            Write-Error "   ✗ FALLO: Tenant 1 puede ver producto de Tenant 2!"
        } else {
            Write-Success "   ✓ CORRECTO: Tenant 1 NO ve productos de Tenant 2"
        }
    }
    
    # Verificar que SÍ ve su propio producto
    $seesProduct1 = $listTenant1.content | Where-Object { $_.id -eq $product1Id }
    if ($seesProduct1) {
        Write-Success "   ✓ CORRECTO: Tenant 1 ve su propio producto"
    } else {
        Write-Error "   ✗ FALLO: Tenant 1 NO ve su propio producto!"
    }
} catch {
    Write-Error "   ✗ Error listando productos: $_"
}

# 6. Listar productos con tenant 2 - debe ver solo el suyo (si existe tenant 2)
if ($company2) {
    Write-Info "`n6. Listando productos con X-Company-Id: $company2..."
    try {
        $listTenant2 = Invoke-RestMethod -Uri "$baseUrl/products" -Method GET -Headers $headers2
        Write-Info "   Productos visibles para Tenant 2:"
        $listTenant2.content | ForEach-Object { 
            Write-Info "     - $($_.name) (ID: $($_.id), Code: $($_.code))"
        }
        
        # Verificar que NO ve el producto del tenant 1
        $seesProduct1 = $listTenant2.content | Where-Object { $_.id -eq $product1Id }
        if ($seesProduct1) {
            Write-Error "   ✗ FALLO: Tenant 2 puede ver producto de Tenant 1!"
        } else {
            Write-Success "   ✓ CORRECTO: Tenant 2 NO ve productos de Tenant 1"
        }
        
        # Verificar que SÍ ve su propio producto
        $seesProduct2 = $listTenant2.content | Where-Object { $_.id -eq $product2Id }
        if ($seesProduct2) {
            Write-Success "   ✓ CORRECTO: Tenant 2 ve su propio producto"
        } else {
            Write-Error "   ✗ FALLO: Tenant 2 NO ve su propio producto!"
        }
    } catch {
        Write-Error "   ✗ Error listando productos: $_"
    }
}

# 7. Intentar acceder al producto de tenant 2 con headers de tenant 1
if ($company2 -and $product2Id) {
    Write-Info "`n7. Intentando acceder a producto de Tenant 2 con X-Company-Id de Tenant 1..."
    try {
        $crossTenantAccess = Invoke-RestMethod -Uri "$baseUrl/products/$product2Id" -Method GET -Headers $headers1
        Write-Error "   ✗ FALLO: Tenant 1 pudo acceder a producto de Tenant 2!"
        Write-Info "     Producto obtenido: $($crossTenantAccess.name)"
    } catch {
        if ($_.Exception.Response.StatusCode -eq 404) {
            Write-Success "   ✓ CORRECTO: Acceso denegado (404 Not Found)"
        } else {
            Write-Warning "   ? Error inesperado: $($_.Exception.Response.StatusCode)"
        }
    }
}

# 8. Limpiar - eliminar productos de prueba
Write-Info "`n8. Limpiando productos de prueba..."
try {
    if ($product1Id) {
        Invoke-RestMethod -Uri "$baseUrl/products/$product1Id" -Method DELETE -Headers $headers1 | Out-Null
        Write-Success "   ✓ Producto Tenant 1 eliminado"
    }
    if ($company2 -and $product2Id) {
        Invoke-RestMethod -Uri "$baseUrl/products/$product2Id" -Method DELETE -Headers $headers2 | Out-Null
        Write-Success "   ✓ Producto Tenant 2 eliminado"
    }
} catch {
    Write-Warning "   ⚠ Error limpiando productos: $_"
}

Write-Info "`n========================================="
Write-Success "  TEST COMPLETADO"
Write-Info "=========================================`n"

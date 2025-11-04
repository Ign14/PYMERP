<#
Idempotent PowerShell script to test protected endpoint against Keycloak + Spring Boot backend.
Usage: .\scripts\test-protected.ps1
It reads values from .env in repo root if present; otherwise prompts for missing values.
#>
Set-StrictMode -Version Latest

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
$repoRoot = Resolve-Path "$scriptRoot\.."

function Read-EnvFile($path) {
    $hash = @{}
    if (-Not (Test-Path $path)) { return $hash }
    Get-Content $path | ForEach-Object {
        if ($_ -match '^[\s#]*$') { return }
        $line = $_.Trim()
        if ($line.StartsWith('#')) { return }
        $parts = $line -split '=',2
        if ($parts.Length -eq 2) { $hash[$parts[0].Trim()] = $parts[1].Trim() }
    }
    return $hash
}

$envPath = Join-Path $repoRoot '.env'
$envs = Read-EnvFile $envPath

function Resolve([string]$key, [string]$default) {
    if ($envs.ContainsKey($key) -and $envs[$key]) { return $envs[$key] }
    $val = (Get-Item -Path Env:$key -ErrorAction SilentlyContinue).Value
    if ($val) { return $val }
    return $default
}

# Valores por defecto seguros para entorno dev
$AUTH_MODE = Resolve 'AUTH_MODE' 'internal'  # 'internal' | 'keycloak'
$KEYCLOAK_URL = Resolve 'KEYCLOAK_URL' 'http://localhost:8082'
$KEYCLOAK_REALM = Resolve 'KEYCLOAK_REALM' 'pymerp'
$KEYCLOAK_CLIENT_ID = Resolve 'KEYCLOAK_CLIENT_ID' 'pymerp-frontend'
$OIDC_USERNAME = Resolve 'OIDC_USERNAME' 'admin'
$OIDC_PASSWORD = Resolve 'OIDC_PASSWORD' 'Admin1234'
$COMPANY_ID = Resolve 'COMPANY_ID' '00000000-0000-0000-0000-000000000001'
$BACKEND_BASE = Resolve 'BACKEND_BASE' 'http://localhost:8081'

Write-Host "Using Keycloak: $KEYCLOAK_URL (realm: $KEYCLOAK_REALM), client: $KEYCLOAK_CLIENT_ID"

function Get-Token() {
    if ($AUTH_MODE -ieq 'internal') {
        $loginUrl = "$BACKEND_BASE/api/v1/auth/login"
        $body = @{ email = $OIDC_USERNAME; password = $OIDC_PASSWORD } | ConvertTo-Json -Depth 2
        try {
            $resp = Invoke-RestMethod -Method Post -Uri $loginUrl -ContentType 'application/json' -Body $body -Headers @{ 'X-Company-Id' = $COMPANY_ID } -UseBasicParsing
            return @{ access_token = $resp.token; expires_in = $resp.expiresIn }
        } catch {
            Write-Host "Internal login failed: $($_.Exception.Message)" -ForegroundColor Red
            if ($_.Exception.Response) {
                $r = $_.Exception.Response.GetResponseStream(); $sr = New-Object System.IO.StreamReader($r); $text = $sr.ReadToEnd(); Write-Host $text
            }
            exit 2
        }
    }
    $tokenUrl = "$KEYCLOAK_URL/realms/$KEYCLOAK_REALM/protocol/openid-connect/token"
    $body = @{ grant_type = 'password'; client_id = $KEYCLOAK_CLIENT_ID; username = $OIDC_USERNAME; password = $OIDC_PASSWORD }
    try {
        $resp = Invoke-RestMethod -Method Post -Uri $tokenUrl -ContentType 'application/x-www-form-urlencoded' -Body $body -UseBasicParsing
        return $resp
    } catch {
        Write-Host "Token request failed: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.Exception.Response) {
            $r = $_.Exception.Response.GetResponseStream()
            $sr = New-Object System.IO.StreamReader($r)
            $text = $sr.ReadToEnd(); Write-Host $text
        }
        exit 2
    }
}

function Show-ClaimsFromToken($token) {
    # JWT structure: header.payload.signature
    $parts = $token -split '\.'
    if ($parts.Length -lt 2) { Write-Host 'Token no parece un JWT'; return }
    $payload = $parts[1]
    function PadBase64($s) { switch ($s.Length % 4) { 0 { } 2 { $s += '==' } 3 { $s += '=' } default { $s += '===' } }; return $s }
    $payload = $payload.Replace('-', '+').Replace('_','/')
    $payload = PadBase64 $payload
    try { $json = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($payload)) } catch { Write-Host 'Error decoding payload' -ForegroundColor Yellow; return }
    $claims = $json | ConvertFrom-Json
    Write-Host "--- JWT Claims (main) ---"
    $keys = @('sub','preferred_username','email','scope','realm_access','resource_access')
    foreach ($k in $keys) {
        if ($claims.PSObject.Properties.Name -contains $k) {
            Write-Host "$k:`n$(($claims.$k | ConvertTo-Json -Depth 5))`n"
        }
    }
    Write-Host "Full payload:`n$($json)`n"
}

function Call-Endpoint($method, $url, $token, $companyId) {
    $headers = @{
        'X-Company-Id' = $companyId
    }
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    try {
        $resp = Invoke-WebRequest -Method $method -Uri $url -Headers $headers -UseBasicParsing -ErrorAction Stop
        return @{ Status = $resp.StatusCode; Body = $resp.Content }
    } catch {
        $status = $_.Exception.Response.StatusCode.value__ 2>$null
        $content = $null
        if ($_.Exception.Response) {
            $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $content = $sr.ReadToEnd()
        }
        return @{ Status = $status; Body = $content }
    }
}

$tokenResp = Get-Token
$accessToken = $tokenResp.access_token
if (-not $accessToken) { Write-Host 'No se obtuvo access_token' -ForegroundColor Red; exit 3 }

Write-Host "Token obtenido. Exp (seconds): $($tokenResp.expires_in)"

Write-Host "Calling Health: $BACKEND_BASE/actuator/health"
$health = Call-Endpoint -method 'GET' -url "$BACKEND_BASE/actuator/health" -token $accessToken -companyId $COMPANY_ID
Write-Host "Health -> HTTP $($health.Status)"
if ($health.Body) { Write-Host $health.Body }

Write-Host "Calling Products: $BACKEND_BASE/api/v1/products"
$products = Call-Endpoint -method 'GET' -url "$BACKEND_BASE/api/v1/products" -token $accessToken -companyId $COMPANY_ID
Write-Host "Products -> HTTP $($products.Status)"
if ($products.Body) { Write-Host $products.Body }

if ($products.Status -in 401,403) {
    Write-Host "Authentication/Authorization failed (status $($products.Status)). Decoding token claims for inspection..." -ForegroundColor Yellow
    Show-ClaimsFromToken $accessToken
    Write-Host "Recommendation: Revise mapping en SecurityConfig/OidcRoleMapper. Asegúrese de normalizar roles a ROLE_* o mapear scope adecuado." -ForegroundColor Cyan
}

Write-Host "Done."

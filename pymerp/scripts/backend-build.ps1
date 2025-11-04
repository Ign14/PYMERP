<#
Construye el backend detectando automáticamente un JDK válido (configura JAVA_HOME y PATH)
y ejecuta Gradle Wrapper con clean build.
#>
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
$repoRoot = Resolve-Path (Join-Path $scriptRoot '..')

# Importar helper de Java
. (Resolve-Path (Join-Path $repoRoot 'scripts/detect-java.ps1'))

# Detectar / configurar JAVA_HOME
if (-not (Set-JavaHome -VerboseOutput)) {
  Write-Error 'No se pudo configurar JAVA_HOME. Instala JDK 17 o 21 y reintenta.'
  exit 1
}

# Ejecutar build
$backendPath = Resolve-Path (Join-Path $repoRoot 'backend')
Push-Location $backendPath
try {
  Write-Host "Ejecutando: gradlew.bat clean build --no-daemon" -ForegroundColor Cyan
  & .\gradlew.bat clean build --no-daemon
} finally {
  Pop-Location
}

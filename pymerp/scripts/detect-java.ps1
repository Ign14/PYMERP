<#
Detecta automáticamente una instalación de JDK en Windows y configura JAVA_HOME y PATH
para la sesión actual de PowerShell.

Uso:
  . .\scripts\detect-java.ps1   # dot-source para mantener variables en sesión
  Set-JavaHome

O desde otro script:
  Import-Module -Name (Resolve-Path "scripts/detect-java.ps1")
  Set-JavaHome -Verbose
#>
Set-StrictMode -Version Latest

function Get-JavaCandidates {
  [CmdletBinding()]
  param()
  $paths = @(
    'C:\\Program Files\\Eclipse Adoptium',
    'C:\\Program Files\\Java',
    'C:\\Program Files\\Microsoft',
    'C:\\Program Files (x86)\\Java'
  )
  $candidates = @()
  foreach ($base in $paths) {
    try {
      if (Test-Path $base) {
        $candidates += Get-ChildItem -Path $base -Recurse -Depth 3 -Filter java.exe -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName }
      }
    } catch { }
  }
  # De registros conocidos (JavaSoft)
  $regKeys = @(
    'HKLM:\SOFTWARE\JavaSoft\JDK',
    'HKLM:\SOFTWARE\WOW6432Node\JavaSoft\JDK',
    'HKLM:\SOFTWARE\Eclipse Adoptium',
    'HKLM:\SOFTWARE\AdoptOpenJDK'
  )
  foreach ($rk in $regKeys) {
    try {
      if (Test-Path $rk) {
        $subs = Get-ChildItem $rk -ErrorAction SilentlyContinue
        foreach ($sub in $subs) {
          try {
            $regJavaHome = (Get-ItemProperty -Path $sub.PSPath -ErrorAction SilentlyContinue).JavaHome
            if ($regJavaHome -and (Test-Path (Join-Path $regJavaHome 'bin/java.exe'))) {
              $candidates += (Join-Path $regJavaHome 'bin/java.exe')
            }
          } catch { }
        }
      }
    } catch { }
  }
  $candidates | Sort-Object -Unique
}

function Get-JavaVersion([string]$javaExe) {
  try {
    $out = & $javaExe -version 2>&1 | Out-String
    if ($out -match 'version "([0-9]+(?:\.[0-9]+)*)') { return $Matches[1] }
  } catch { }
  return $null
}

function Compare-Version([string]$a, [string]$b) {
  # Devuelve -1 si a<b, 0 si igual, 1 si a>b
  $pa = $a.Split('.') | ForEach-Object { [int]$_ }
  $pb = $b.Split('.') | ForEach-Object { [int]$_ }
  $len = [Math]::Max($pa.Length, $pb.Length)
  for ($i=0; $i -lt $len; $i++) {
    if ($i -lt $pa.Length) { $va = $pa[$i] } else { $va = 0 }
    if ($i -lt $pb.Length) { $vb = $pb[$i] } else { $vb = 0 }
    if ($va -lt $vb) { return -1 }
    if ($va -gt $vb) { return 1 }
  }
  return 0
}

function Set-JavaHome {
  [CmdletBinding()]
  param(
    [switch]$VerboseOutput
  )
  $cands = Get-JavaCandidates
  if (-not $cands -or $cands.Count -eq 0) {
    Write-Error 'No se encontraron JDK instalados. Instala JDK 17 o 21 (Temurin recomendado).'
    return $false
  }
  $scored = @()
  foreach ($c in $cands) {
    $ver = Get-JavaVersion $c
    if ($ver) { $scored += [PSCustomObject]@{ Java=$c; Version=$ver } }
  }
  if ($scored.Count -eq 0) {
    Write-Error 'No fue posible leer versión de Java de los candidatos encontrados.'
    return $false
  }
  # Elegir el mayor (prefiere 21 > 17, etc.)
  $selected = $scored | Sort-Object -Property @{ Expression = { $_.Version }; Descending = $true } | Select-Object -First 1
  $javaExe = $selected.Java
  $javaHome = Split-Path (Split-Path $javaExe) -Parent
  $env:JAVA_HOME = $javaHome
  if (-not ($env:Path -like "*$javaHome\\bin*")) {
    $env:Path = "$javaHome\\bin;" + $env:Path
  }
  if ($VerboseOutput) {
    Write-Host "JAVA_HOME=$($env:JAVA_HOME)" -ForegroundColor Green
    $verOut = & $javaExe -version 2>&1 | Out-String
    Write-Host $verOut
  }
  return $true
}

Export-ModuleMember -Function Set-JavaHome

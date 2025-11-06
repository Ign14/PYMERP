# Script para capturar error completo de bootRun

cd $PSScriptRoot

Write-Host "Iniciando bootRun..."
.\gradlew.bat bootRun --args="--spring.profiles.active=dev --server.port=8081" --stacktrace --info 2>&1 | Tee-Object -FilePath "bootrun-full.log"

Write-Host "`nLog completo guardado en bootrun-full.log"

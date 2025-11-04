Param(
  [string]$FlywayImage = "flyway/flyway:10.17",
  # Por defecto, usar el Postgres de docker-compose expuesto en el host (Windows/macOS)
  [string]$DatabaseUrl = "jdbc:postgresql://host.docker.internal:55432/pymes",
  [string]$DatabaseUser = "pymes",
  [string]$DatabasePassword = "pymes",
  [string]$Schemas = "public"
)

# Resolver ruta relativa al script (backend/src/main/resources/db/migration)
$migrationsPath = (Resolve-Path -Path (Join-Path $PSScriptRoot "../src/main/resources/db/migration")).Path

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
  Write-Error "Docker is required to run Flyway migrations. Install Docker and try again."
  exit 1
}

docker run --rm `
  -v "${migrationsPath}:/flyway/sql" `
  -e "FLYWAY_URL=$DatabaseUrl" `
  -e "FLYWAY_USER=$DatabaseUser" `
  -e "FLYWAY_PASSWORD=$DatabasePassword" `
  -e "FLYWAY_SCHEMAS=$Schemas" `
  $FlywayImage migrate

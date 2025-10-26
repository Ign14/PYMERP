Param(
  [string]$FlywayImage = "flyway/flyway:10.17",
  [string]$DatabaseUrl = "jdbc:postgresql://localhost:5432/pymes",
  [string]$DatabaseUser = "pymes",
  [string]$DatabasePassword = "CHANGE_ME_DB_PASSWORD",
  [string]$Schemas = "public"
)

 $migrationsPath = (Resolve-Path -Path "../backend/src/main/resources/db/migration").Path

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

#!/usr/bin/env bash
set -euo pipefail

FLYWAY_IMAGE=${FLYWAY_IMAGE:-flyway/flyway:10.17}
DATABASE_URL=${DATABASE_URL:-jdbc:postgresql://localhost:5432/pymes}
DATABASE_USER=${DATABASE_USER:-pymes}
DATABASE_PASSWORD=${DATABASE_PASSWORD:-CHANGE_ME_DB_PASSWORD}
SCHEMAS=${SCHEMAS:-public}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MIGRATIONS_PATH="${SCRIPT_DIR}/../backend/src/main/resources/db/migration"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required to run Flyway migrations. Install Docker and try again." >&2
  exit 1
fi

docker run --rm \
  -v "${MIGRATIONS_PATH}:/flyway/sql" \
  -e "FLYWAY_URL=${DATABASE_URL}" \
  -e "FLYWAY_USER=${DATABASE_USER}" \
  -e "FLYWAY_PASSWORD=${DATABASE_PASSWORD}" \
  -e "FLYWAY_SCHEMAS=${SCHEMAS}" \
  "${FLYWAY_IMAGE}" migrate "$@"

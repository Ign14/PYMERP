#!/bin/bash
set -e

echo "===== PYMERP Inventory Locations Verification ====="

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

FAILED=0

# A) Migraciones
echo "→ Aplicando migraciones Flyway..."
cd backend
./gradlew flywayMigrate || { echo -e "${RED}✗ Flyway failed${NC}"; FAILED=1; }

echo "→ Verificando tabla inventory_locations..."
docker exec pymes_postgres psql -U pymes -d pymes -c "\d inventory_locations" || FAILED=1

echo "→ Verificando backfill (no debe haber lotes sin ubicación)..."
NULL_LOTS=$(docker exec pymes_postgres psql -U pymes -d pymes -t -c "SELECT COUNT(*) FROM inventory_lots WHERE location_id IS NULL;")
if [ "$NULL_LOTS" -ne 0 ]; then
  echo -e "${RED}✗ Hay $NULL_LOTS lotes sin ubicación${NC}"
  FAILED=1
fi

# B) Compilación
echo "→ Compilando backend..."
./gradlew clean build -x test || { echo -e "${RED}✗ Build failed${NC}"; FAILED=1; }

# C) Tests
echo "→ Ejecutando tests de integración..."
./gradlew test --tests com.datakomerz.pymes.inventory.InventoryLocationIT || { echo -e "${RED}✗ Tests failed${NC}"; FAILED=1; }

# D) Docker
echo "→ Verificando servicios Docker..."
cd ..
docker-compose ps | grep "Up" || FAILED=1

echo "→ Probando endpoints..."
curl -sf http://localhost:8081/actuator/health || { echo -e "${RED}✗ Health failed${NC}"; FAILED=1; }

if [ $FAILED -eq 0 ]; then
  echo -e "${GREEN}===== ✓ Verificación completada exitosamente =====${NC}"
else
  echo -e "${RED}===== ✗ Verificación con errores =====${NC}"
  exit 1
fi

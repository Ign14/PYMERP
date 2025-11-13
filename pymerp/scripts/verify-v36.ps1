# Script de Verificación V36 - Pruebas Exhaustivas de Inventario
# Ejecutar DESPUÉS de que el backend complete inicialización

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "VERIFICACIÓN MIGRACIÓN V36" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Verificar migración Flyway
Write-Host "[1/10] Verificando migración Flyway..." -ForegroundColor Yellow
$flywayCheck = docker exec pymes_postgres psql -U pymes -d pymes -t -c "SELECT version, success FROM flyway_schema_history WHERE version = '36';"
Write-Host $flywayCheck -ForegroundColor Green

# 2. Contar proveedores
Write-Host "[2/10] Contando proveedores..." -ForegroundColor Yellow
$suppliers = docker exec pymes_postgres psql -U pymes -d pymes -t -c "SELECT COUNT(*) FROM suppliers;"
Write-Host "  Proveedores: $suppliers (Esperado: 3)" -ForegroundColor Green

# 3. Contar clientes
Write-Host "[3/10] Contando clientes..." -ForegroundColor Yellow
$customers = docker exec pymes_postgres psql -U pymes -d pymes -t -c "SELECT COUNT(*) FROM customers;"
Write-Host "  Clientes: $customers (Esperado: 5)" -ForegroundColor Green

# 4. Contar productos totales
Write-Host "[4/10] Contando productos..." -ForegroundColor Yellow
$products = docker exec pymes_postgres psql -U pymes -d pymes -t -c "SELECT COUNT(*) FROM products;"
Write-Host "  Productos totales: $products (Esperado: 28 = 23 de V34 + 5 de V36)" -ForegroundColor Green

# 5. Verificar productos con critical_stock
Write-Host "[5/10] Verificando productos con stock crítico..." -ForegroundColor Yellow
$criticalProducts = docker exec pymes_postgres psql -U pymes -d pymes -t -c "SELECT COUNT(*) FROM products WHERE critical_stock IS NOT NULL AND critical_stock > 0;"
Write-Host "  Productos con critical_stock: $criticalProducts" -ForegroundColor Green

# 6. Verificar compra F-00001234
Write-Host "[6/10] Verificando compra F-00001234..." -ForegroundColor Yellow
$purchase = docker exec pymes_postgres psql -U pymes -d pymes -t -c "SELECT doc_number, status, total FROM purchases WHERE doc_number = 'F-00001234';"
Write-Host "  Compra: $purchase" -ForegroundColor Green

# 7. Verificar items de compra
Write-Host "[7/10] Verificando items de compra..." -ForegroundColor Yellow
$purchaseItems = docker exec pymes_postgres psql -U pymes -d pymes -t -c "SELECT COUNT(*) FROM purchase_items pi JOIN purchases p ON pi.purchase_id = p.id WHERE p.doc_number = 'F-00001234';"
Write-Host "  Items de compra: $purchaseItems (Esperado: 2)" -ForegroundColor Green

# 8. Verificar venta B-00000123
Write-Host "[8/10] Verificando venta B-00000123..." -ForegroundColor Yellow
$sale = docker exec pymes_postgres psql -U pymes -d pymes -t -c "SELECT doc_number, status, total FROM sales WHERE doc_number = 'B-00000123';"
Write-Host "  Venta: $sale" -ForegroundColor Green

# 9. Verificar movimientos de inventario
Write-Host "[9/10] Contando movimientos de inventario..." -ForegroundColor Yellow
$movements = docker exec pymes_postgres psql -U pymes -d pymes -t -c "SELECT COUNT(*) FROM inventory_movements;"
Write-Host "  Movimientos totales: $movements" -ForegroundColor Green

# 10. Verificar histórico de precios
Write-Host "[10/10] Verificando histórico de precios..." -ForegroundColor Yellow
$priceHistory = docker exec pymes_postgres psql -U pymes -d pymes -t -c "SELECT COUNT(*) FROM price_history;"
Write-Host "  Registros de precios: $priceHistory" -ForegroundColor Green

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "VERIFICACIÓN COMPLETADA" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Test backend health
Write-Host ""
Write-Host "Verificando salud del backend..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8081/actuator/health" -Method GET
    Write-Host "Backend: $($health.status)" -ForegroundColor Green
} catch {
    Write-Host "Backend: NO DISPONIBLE" -ForegroundColor Red
}

# Sprint 3: RBAC Complete - Checklist de Implementación

**Estado**: EN PROGRESO  
**Fecha**: 2025-01-04  
**Horas Estimadas**: 14h

---

## ✅ Tareas Completadas

### Tarea 3.1: Documentación de Matriz RBAC (2h) ✅
- [x] Crear `docs/RBAC_MATRIX.md` con definición de roles
- [x] Documentar 20+ controladores con sus permisos
- [x] Definir reglas de negocio (principio de menor privilegio)
- [x] Ejemplos de implementación y tests

**Entregables**:
- `docs/RBAC_MATRIX.md` (800+ líneas)
- 5 roles definidos: ADMIN, SETTINGS, ERP_USER, READONLY, ACTUATOR_ADMIN
- Matriz completa de permisos por endpoint

---

### Tarea 3.2: Implementación en Controladores Críticos (4h) ✅
- [x] CustomerController - 10 endpoints protegidos
- [x] ProductController - 6 endpoints protegidos
- [x] Importar `@PreAuthorize` correctamente
- [x] Aplicar patrón: GET (todos), POST/PUT (operativos), DELETE (admin)

**Cambios Realizados**:

#### CustomerController
```java
@GetMapping                  → @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
@GetMapping("/{id}")         → @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
@GetMapping("/segments")     → @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
@GetMapping("/{id}/sales")   → @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
@GetMapping("/{id}/stats")   → @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
@GetMapping("/export")       → @PreAuthorize("hasAnyRole('ERP_USER', 'SETTINGS', 'ADMIN')")
@PostMapping                 → @PreAuthorize("hasAnyRole('ERP_USER', 'SETTINGS', 'ADMIN')")
@PutMapping("/{id}")         → @PreAuthorize("hasAnyRole('ERP_USER', 'SETTINGS', 'ADMIN')")
@DeleteMapping("/{id}")      → @PreAuthorize("hasRole('ADMIN')")
@PostMapping("/import")      → @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
```

#### ProductController
```java
@GetMapping                      → @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
@PostMapping                     → @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
@PutMapping("/{id}")             → @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
@PatchMapping("/{id}/status")    → @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
@PatchMapping("/{id}/inventory-alert") → @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
```

---

## ⏳ Tareas Pendientes

### Tarea 3.3: Implementación en Controladores Restantes (6h)

**Pendiente Aplicar RBAC**:

#### 1. SalesController (`/api/v1/sales`)
- [ ] GET endpoints → ERP_USER, READONLY, ADMIN
- [ ] POST /sales → ERP_USER, ADMIN
- [ ] PUT /sales/{id} → ERP_USER, ADMIN
- [ ] DELETE /sales/{id} → ADMIN
- [ ] GET /analytics/* → ERP_USER, READONLY, ADMIN

#### 2. PurchaseController (`/api/v1/purchases`)
- [ ] GET endpoints → ERP_USER, READONLY, ADMIN
- [ ] POST /purchases → ERP_USER, ADMIN
- [ ] PUT /purchases/{id} → ERP_USER, ADMIN
- [ ] DELETE /purchases/{id} → ADMIN
- [ ] GET /analytics/* → ERP_USER, READONLY, ADMIN

#### 3. SupplierController (`/api/v1/suppliers`)
- [ ] GET endpoints → ERP_USER, READONLY, SETTINGS, ADMIN
- [ ] POST /suppliers → SETTINGS, ADMIN
- [ ] PUT /suppliers/{id} → SETTINGS, ADMIN
- [ ] DELETE /suppliers/{id} → ADMIN

#### 4. InventoryController (`/api/v1/inventory`)
- [ ] GET endpoints → ERP_USER, READONLY, ADMIN
- [ ] POST /adjustment → ERP_USER, SETTINGS, ADMIN
- [ ] GET /audit → ERP_USER, READONLY, ADMIN

#### 5. FinanceController (`/api/v1/finances`)
- [ ] GET /summary → ERP_USER, READONLY, ADMIN
- [ ] GET /cashflow → ERP_USER, READONLY, ADMIN
- [ ] GET /accounts-receivable → ERP_USER, READONLY, ADMIN
- [ ] GET /accounts-payable → ERP_USER, READONLY, ADMIN

#### 6. BillingController (`/api/v1/billing`)
- [ ] GET endpoints → ERP_USER, READONLY, ADMIN
- [ ] POST /invoices → ERP_USER, ADMIN
- [ ] POST /invoices/{id}/resend → ADMIN

#### 7. CompanyController (`/api/v1/company`)
- [ ] GET /company → ERP_USER, READONLY, SETTINGS, ADMIN
- [ ] PUT /company → ADMIN, SETTINGS
- [ ] POST /company/logo → ADMIN, SETTINGS

#### 8. LocationController (`/api/v1/locations`)
- [ ] GET endpoints → ERP_USER, READONLY, SETTINGS, ADMIN
- [ ] POST /locations → SETTINGS, ADMIN
- [ ] PUT /locations/{id} → SETTINGS, ADMIN
- [ ] DELETE /locations/{id} → ADMIN

#### 9. ServiceController (`/api/v1/services`)
- [ ] GET endpoints → ERP_USER, READONLY, SETTINGS, ADMIN
- [ ] POST /services → SETTINGS, ADMIN
- [ ] PUT /services/{id} → SETTINGS, ADMIN
- [ ] DELETE /services/{id} → ADMIN

#### 10. PricingController (`/api/v1/pricing`)
- [ ] GET /pricing → ERP_USER, READONLY, SETTINGS, ADMIN
- [ ] POST /pricing → SETTINGS, ADMIN
- [ ] PUT /pricing/{id} → SETTINGS, ADMIN
- [ ] DELETE /pricing/{id} → ADMIN

#### 11. SalesReportController (`/api/v1/sales-reports`)
- [ ] GET /daily → ERP_USER, READONLY, ADMIN
- [ ] GET /weekly → ERP_USER, READONLY, ADMIN
- [ ] GET /monthly → ERP_USER, READONLY, ADMIN
- [ ] GET /abc-analysis → ERP_USER, READONLY, ADMIN

#### 12. AccountRequestController (`/api/v1/requests`)
- [x] POST /requests → PUBLIC (ya público)
- [ ] GET /requests → ADMIN
- [ ] POST /requests/{id}/approve → ADMIN
- [ ] POST /requests/{id}/reject → ADMIN

#### 13. BillingDownloadController
- [ ] GET /billing/{id}/pdf → ERP_USER, READONLY, ADMIN

#### 14. HealthController
- [x] GET /health → PUBLIC (ya público)

#### 15. SiiWebhookController
- [x] POST /webhooks/billing → PUBLIC (firmado con HMAC)

**UserAccountController** ya tiene RBAC aplicado ✅

---

### Tarea 3.4: Tests de Autorización (3h)

**Crear tests para validar RBAC**:

#### CustomerControllerAuthTest
```java
@Test
@WithMockUser(roles = "READONLY")
void readonlyCanListCustomers() {
  // GET debe funcionar
}

@Test
@WithMockUser(roles = "READONLY")
void readonlyCannotCreateCustomer() {
  // POST debe dar 403 Forbidden
}

@Test
@WithMockUser(roles = "ERP_USER")
void erpUserCanCreateCustomer() {
  // POST debe funcionar
}

@Test
@WithMockUser(roles = "ERP_USER")
void erpUserCannotDeleteCustomer() {
  // DELETE debe dar 403 Forbidden
}

@Test
@WithMockUser(roles = "ADMIN")
void adminCanDeleteCustomer() {
  // DELETE debe funcionar
}
```

**Cobertura de Tests**:
- [ ] CustomerController (5 tests)
- [ ] ProductController (5 tests)
- [ ] SalesController (5 tests)
- [ ] PurchaseController (5 tests)
- [ ] SupplierController (4 tests)
- [ ] UserAccountController (4 tests - ya implementados)

---

### Tarea 3.5: Documentación de Implementación (1h)

- [ ] Actualizar README_dev.md con sección RBAC
- [ ] Crear guía de troubleshooting para errores 403 Forbidden
- [ ] Documentar cómo asignar roles a usuarios (Keycloak + internal JWT)

---

## 📊 Progreso del Sprint

| Tarea | Horas Estimadas | Horas Reales | Estado |
|-------|----------------|--------------|--------|
| 3.1 Documentar Matriz RBAC | 2h | 1.5h | ✅ COMPLETADO |
| 3.2 Implementar Controladores Críticos | 4h | 2h | ✅ COMPLETADO |
| 3.3 Implementar Controladores Restantes | 6h | 0h | ⏳ PENDIENTE |
| 3.4 Tests de Autorización | 3h | 0h | ⏳ PENDIENTE |
| 3.5 Documentación | 1h | 0h | ⏳ PENDIENTE |
| **Total** | **16h** | **3.5h** | **22% completo** |

---

## 🎯 Próximos Pasos Inmediatos

1. **Aplicar RBAC a SalesController** (30 min)
   - Leer archivo completo
   - Aplicar anotaciones según matriz
   - Commit: `feat(sales): add RBAC to SalesController`

2. **Aplicar RBAC a PurchaseController** (30 min)
   - Leer archivo completo
   - Aplicar anotaciones según matriz
   - Commit: `feat(purchases): add RBAC to PurchaseController`

3. **Aplicar RBAC a SupplierController** (20 min)
4. **Aplicar RBAC a InventoryController** (20 min)
5. **Aplicar RBAC a FinanceController** (15 min)
6. **Aplicar RBAC a BillingController** (20 min)
7. **Aplicar RBAC a CompanyController** (15 min)
8. **Aplicar RBAC a catálogos** (Locations, Services, Pricing) (30 min total)
9. **Aplicar RBAC a reportes** (SalesReportController) (15 min)
10. **Crear tests de autorización** (2h)
11. **Documentación final** (1h)

---

## 🔗 Referencias

- RBAC Matrix: `docs/RBAC_MATRIX.md`
- Security Config: `backend/src/main/java/com/datakomerz/pymes/config/SecurityConfig.java`
- UserAccountController (ejemplo): `backend/src/main/java/com/datakomerz/pymes/auth/api/UserAccountController.java`

---

**Nota**: El patrón de implementación está claro. Los próximos pasos serán aplicar sistemáticamente las anotaciones `@PreAuthorize` a cada controlador siguiendo la matriz RBAC documentada.

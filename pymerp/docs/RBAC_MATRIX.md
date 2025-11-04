# RBAC Matrix - Role-Based Access Control

**Proyecto**: PYMERP  
**Versión**: 1.0  
**Fecha**: 2025-01-04

---

## 📋 Definición de Roles

### ROLE_ADMIN
**Descripción**: Administrador del sistema con acceso completo  
**Permisos**: CRUD completo en todas las entidades, configuración de sistema, gestión de usuarios  
**Casos de Uso**: Configuración inicial, troubleshooting, gestión de usuarios

### ROLE_SETTINGS
**Descripción**: Gestor de configuraciones y catálogos  
**Permisos**: CRUD en configuraciones, productos, servicios, locaciones, proveedores  
**Casos de Uso**: Mantener catálogos actualizados, configurar parámetros de negocio

### ROLE_ERP_USER
**Descripción**: Usuario operativo del ERP  
**Permisos**: Lectura/escritura en operaciones diarias (ventas, compras, inventario, finanzas)  
**Casos de Uso**: Registrar ventas, compras, consultar inventario, ver reportes

### ROLE_READONLY
**Descripción**: Usuario de solo lectura  
**Permisos**: Solo consultas (GET), no puede modificar datos  
**Casos de Uso**: Auditoría, reportes, supervisión sin riesgo de modificación

### ROLE_ACTUATOR_ADMIN
**Descripción**: Administrador de monitoreo técnico  
**Permisos**: Acceso a endpoints Actuator (métricas, health, prometheus)  
**Casos de Uso**: DevOps, SRE, monitoreo de infraestructura

---

## 🔐 Matriz de Permisos por Endpoint

### AuthController (`/api/v1/auth`)
| Endpoint | Método | Roles | Notas |
|----------|--------|-------|-------|
| `/login` | POST | `PUBLIC` | Login público |
| `/logout` | POST | `AUTHENTICATED` | Cualquier usuario autenticado |
| `/refresh` | POST | `AUTHENTICATED` | Cualquier usuario autenticado |

---

### UserAccountController (`/api/v1/users`)
| Endpoint | Método | Roles | Notas |
|----------|--------|-------|-------|
| `/api/v1/users` | GET | `ADMIN`, `SETTINGS` | Listar usuarios |
| `/api/v1/users` | POST | `ADMIN`, `SETTINGS` | Crear usuario |
| `/api/v1/users/{id}` | PUT | `ADMIN`, `SETTINGS` | Actualizar usuario |
| `/api/v1/users/{id}` | DELETE | `ADMIN` | Solo ADMIN puede eliminar |

**Implementación Actual**:
```java
@PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_SETTINGS')")
```

---

### CustomerController (`/api/v1/customers`)
| Endpoint | Método | Roles | Notas |
|----------|--------|-------|-------|
| `/api/v1/customers` | GET | `ERP_USER`, `READONLY` | Listar clientes |
| `/api/v1/customers/{id}` | GET | `ERP_USER`, `READONLY` | Ver detalle cliente |
| `/api/v1/customers` | POST | `ERP_USER`, `SETTINGS` | Crear cliente |
| `/api/v1/customers/{id}` | PUT | `ERP_USER`, `SETTINGS` | Actualizar cliente |
| `/api/v1/customers/{id}` | DELETE | `ADMIN` | Solo ADMIN elimina |
| `/api/v1/customers/import` | POST | `SETTINGS` | Importación masiva |

**Recomendación**:
```java
// GET methods
@PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")

// POST/PUT methods
@PreAuthorize("hasAnyRole('ERP_USER', 'SETTINGS', 'ADMIN')")

// DELETE methods
@PreAuthorize("hasRole('ADMIN')")
```

---

### ProductController (`/api/v1/products`)
| Endpoint | Método | Roles | Notas |
|----------|--------|-------|-------|
| `/api/v1/products` | GET | `ERP_USER`, `READONLY` | Listar productos |
| `/api/v1/products/{id}` | GET | `ERP_USER`, `READONLY` | Ver detalle producto |
| `/api/v1/products` | POST | `SETTINGS` | Crear producto (catálogo) |
| `/api/v1/products/{id}` | PUT | `SETTINGS` | Actualizar producto |
| `/api/v1/products/{id}` | DELETE | `ADMIN` | Solo ADMIN elimina |
| `/api/v1/products/abc-analysis` | GET | `ERP_USER`, `READONLY` | Análisis ABC |
| `/api/v1/products/forecast` | GET | `ERP_USER`, `READONLY` | Forecasting |

**Implementación Actual**:
```java
@PreAuthorize("hasAuthority('ROLE_ERP_USER') or hasAuthority('SCOPE_products:read')")
```

**Recomendación**:
```java
// GET methods
@PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")

// POST/PUT methods (catálogo)
@PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")

// DELETE methods
@PreAuthorize("hasRole('ADMIN')")
```

---

### SupplierController (`/api/v1/suppliers`)
| Endpoint | Método | Roles | Notas |
|----------|--------|-------|-------|
| `/api/v1/suppliers` | GET | `ERP_USER`, `READONLY` | Listar proveedores |
| `/api/v1/suppliers/{id}` | GET | `ERP_USER`, `READONLY` | Ver detalle proveedor |
| `/api/v1/suppliers` | POST | `SETTINGS` | Crear proveedor (catálogo) |
| `/api/v1/suppliers/{id}` | PUT | `SETTINGS` | Actualizar proveedor |
| `/api/v1/suppliers/{id}` | DELETE | `ADMIN` | Solo ADMIN elimina |
| `/api/v1/suppliers/{id}/performance` | GET | `ERP_USER`, `READONLY` | Análisis de desempeño |

---

### SalesController (`/api/v1/sales`)
| Endpoint | Método | Roles | Notas |
|----------|--------|-------|-------|
| `/api/v1/sales` | GET | `ERP_USER`, `READONLY` | Listar ventas |
| `/api/v1/sales/{id}` | GET | `ERP_USER`, `READONLY` | Ver detalle venta |
| `/api/v1/sales` | POST | `ERP_USER` | Registrar venta |
| `/api/v1/sales/{id}` | PUT | `ERP_USER`, `ADMIN` | Modificar venta |
| `/api/v1/sales/{id}` | DELETE | `ADMIN` | Solo ADMIN anula |
| `/api/v1/sales/analytics/*` | GET | `ERP_USER`, `READONLY` | Reportes de ventas |

**Recomendación**:
```java
// GET methods
@PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'ADMIN')")

// POST methods (crear venta)
@PreAuthorize("hasAnyRole('ERP_USER', 'ADMIN')")

// PUT methods (modificar venta)
@PreAuthorize("hasAnyRole('ERP_USER', 'ADMIN')")

// DELETE methods (anular)
@PreAuthorize("hasRole('ADMIN')")
```

---

### PurchaseController (`/api/v1/purchases`)
| Endpoint | Método | Roles | Notas |
|----------|--------|-------|-------|
| `/api/v1/purchases` | GET | `ERP_USER`, `READONLY` | Listar compras |
| `/api/v1/purchases/{id}` | GET | `ERP_USER`, `READONLY` | Ver detalle compra |
| `/api/v1/purchases` | POST | `ERP_USER` | Registrar compra |
| `/api/v1/purchases/{id}` | PUT | `ERP_USER`, `ADMIN` | Modificar compra |
| `/api/v1/purchases/{id}` | DELETE | `ADMIN` | Solo ADMIN anula |
| `/api/v1/purchases/analytics/*` | GET | `ERP_USER`, `READONLY` | Reportes de compras |

---

### InventoryController (`/api/v1/inventory`)
| Endpoint | Método | Roles | Notas |
|----------|--------|-------|-------|
| `/api/v1/inventory` | GET | `ERP_USER`, `READONLY` | Consultar inventario |
| `/api/v1/inventory/{id}` | GET | `ERP_USER`, `READONLY` | Detalle de producto |
| `/api/v1/inventory/movements` | GET | `ERP_USER`, `READONLY` | Movimientos de inventario |
| `/api/v1/inventory/adjustment` | POST | `ERP_USER`, `SETTINGS` | Ajuste de inventario |
| `/api/v1/inventory/audit` | GET | `ERP_USER`, `READONLY` | Auditoría de inventario |

---

### FinanceController (`/api/v1/finances`)
| Endpoint | Método | Roles | Notas |
|----------|--------|-------|-------|
| `/api/v1/finances/summary` | GET | `ERP_USER`, `READONLY`, `ADMIN` | Resumen financiero |
| `/api/v1/finances/cashflow` | GET | `ERP_USER`, `READONLY`, `ADMIN` | Flujo de caja |
| `/api/v1/finances/accounts-receivable` | GET | `ERP_USER`, `READONLY` | Cuentas por cobrar |
| `/api/v1/finances/accounts-payable` | GET | `ERP_USER`, `READONLY` | Cuentas por pagar |

---

### BillingController (`/api/v1/billing`)
| Endpoint | Método | Roles | Notas |
|----------|--------|-------|-------|
| `/api/v1/billing/invoices` | GET | `ERP_USER`, `READONLY` | Listar facturas |
| `/api/v1/billing/invoices/{id}` | GET | `ERP_USER`, `READONLY` | Ver factura |
| `/api/v1/billing/invoices` | POST | `ERP_USER` | Crear factura (DTE) |
| `/api/v1/billing/invoices/{id}/pdf` | GET | `ERP_USER`, `READONLY` | Descargar PDF |
| `/api/v1/billing/invoices/{id}/resend` | POST | `ADMIN` | Reenviar al SII |

---

### CompanyController (`/api/v1/company`)
| Endpoint | Método | Roles | Notas |
|----------|--------|-------|-------|
| `/api/v1/company` | GET | `ERP_USER`, `READONLY` | Ver info de empresa |
| `/api/v1/company` | PUT | `ADMIN`, `SETTINGS` | Actualizar empresa |
| `/api/v1/company/logo` | POST | `ADMIN`, `SETTINGS` | Subir logo |

---

### LocationController (`/api/v1/locations`)
| Endpoint | Método | Roles | Notas |
|----------|--------|-------|-------|
| `/api/v1/locations` | GET | `ERP_USER`, `READONLY` | Listar locaciones |
| `/api/v1/locations` | POST | `SETTINGS` | Crear locación (catálogo) |
| `/api/v1/locations/{id}` | PUT | `SETTINGS` | Actualizar locación |
| `/api/v1/locations/{id}` | DELETE | `ADMIN` | Solo ADMIN elimina |

---

### ServiceController (`/api/v1/services`)
| Endpoint | Método | Roles | Notas |
|----------|--------|-------|-------|
| `/api/v1/services` | GET | `ERP_USER`, `READONLY` | Listar servicios |
| `/api/v1/services` | POST | `SETTINGS` | Crear servicio (catálogo) |
| `/api/v1/services/{id}` | PUT | `SETTINGS` | Actualizar servicio |
| `/api/v1/services/{id}` | DELETE | `ADMIN` | Solo ADMIN elimina |

---

### PricingController (`/api/v1/pricing`)
| Endpoint | Método | Roles | Notas |
|----------|--------|-------|-------|
| `/api/v1/pricing` | GET | `ERP_USER`, `READONLY` | Consultar precios |
| `/api/v1/pricing` | POST | `SETTINGS` | Crear política de precios |
| `/api/v1/pricing/{id}` | PUT | `SETTINGS` | Actualizar precios |
| `/api/v1/pricing/{id}` | DELETE | `ADMIN` | Eliminar política |

---

### AccountRequestController (`/api/v1/requests`)
| Endpoint | Método | Roles | Notas |
|----------|--------|-------|-------|
| `/api/v1/requests` | POST | `PUBLIC` | Solicitud de cuenta (público) |
| `/api/v1/requests` | GET | `ADMIN` | Ver solicitudes (solo admin) |
| `/api/v1/requests/{id}/approve` | POST | `ADMIN` | Aprobar solicitud |
| `/api/v1/requests/{id}/reject` | POST | `ADMIN` | Rechazar solicitud |

---

### SalesReportController (`/api/v1/sales-reports`)
| Endpoint | Método | Roles | Notas |
|----------|--------|-------|-------|
| `/api/v1/sales-reports/daily` | GET | `ERP_USER`, `READONLY` | Reporte diario |
| `/api/v1/sales-reports/weekly` | GET | `ERP_USER`, `READONLY` | Reporte semanal |
| `/api/v1/sales-reports/monthly` | GET | `ERP_USER`, `READONLY` | Reporte mensual |
| `/api/v1/sales-reports/abc-analysis` | GET | `ERP_USER`, `READONLY` | Análisis ABC |

---

### SiiWebhookController (`/webhooks/billing`)
| Endpoint | Método | Roles | Notas |
|----------|--------|-------|-------|
| `/webhooks/billing` | POST | `PUBLIC` | Webhook del SII (público, firmado) |

---

### HealthController (`/health`)
| Endpoint | Método | Roles | Notas |
|----------|--------|-------|-------|
| `/health` | GET | `PUBLIC` | Health check público |

---

## 📊 Resumen de Permisos

### Por Rol

| Rol | Controllers con Acceso | Endpoints Totales |
|-----|------------------------|-------------------|
| **ADMIN** | Todos (20) | 100% |
| **SETTINGS** | Catálogos (8): Products, Suppliers, Services, Locations, Company, Pricing, Users | ~40% |
| **ERP_USER** | Operaciones (10): Sales, Purchases, Inventory, Customers, Finances, Billing, Reports | ~60% |
| **READONLY** | Solo GET en todos los controladores | ~50% (solo lectura) |
| **ACTUATOR_ADMIN** | Solo Actuator endpoints | ~5% |

### Por Operación

| Operación | Roles Permitidos |
|-----------|-----------------|
| **Listar/Ver** (GET) | `ERP_USER`, `READONLY`, `SETTINGS`, `ADMIN` |
| **Crear** (POST) | `ERP_USER`, `SETTINGS`, `ADMIN` (según contexto) |
| **Actualizar** (PUT) | `ERP_USER`, `SETTINGS`, `ADMIN` (según contexto) |
| **Eliminar** (DELETE) | `ADMIN` (exclusivo en mayoría de casos) |

---

## 🔒 Reglas de Negocio

### 1. Principio de Menor Privilegio
- Usuarios operativos (`ERP_USER`) **NO** deben eliminar datos históricos
- Solo `ADMIN` puede eliminar clientes, productos, proveedores
- `SETTINGS` puede crear/modificar catálogos pero no eliminar

### 2. Separación de Responsabilidades
- **SETTINGS**: Gestiona catálogos y configuraciones (productos, servicios, proveedores)
- **ERP_USER**: Ejecuta operaciones diarias (ventas, compras, inventario)
- **ADMIN**: Administración del sistema, eliminaciones críticas, usuarios

### 3. Auditoría
- Todas las operaciones POST/PUT/DELETE deben registrar quién las ejecutó
- Los logs deben incluir rol del usuario y timestamp

### 4. Excepciones
- **AuthController**: Login y logout son públicos (sin autenticación previa)
- **AccountRequestController**: Solicitudes de cuenta son públicas (registro)
- **SiiWebhookController**: Webhooks externos son públicos pero firmados (validación HMAC)
- **HealthController**: Health checks son públicos (load balancers)

---

## 🚀 Implementación

### 1. Anotaciones en Controladores

**Ejemplo - CustomerController**:
```java
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

  // Listar/Ver - Permitir lectura
  @GetMapping
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public ResponseEntity<List<Customer>> list() { ... }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ERP_USER', 'READONLY', 'SETTINGS', 'ADMIN')")
  public ResponseEntity<Customer> get(@PathVariable Long id) { ... }

  // Crear - Solo usuarios operativos y configuradores
  @PostMapping
  @PreAuthorize("hasAnyRole('ERP_USER', 'SETTINGS', 'ADMIN')")
  public ResponseEntity<Customer> create(@RequestBody Customer customer) { ... }

  // Actualizar - Solo usuarios operativos y configuradores
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ERP_USER', 'SETTINGS', 'ADMIN')")
  public ResponseEntity<Customer> update(@PathVariable Long id, @RequestBody Customer customer) { ... }

  // Eliminar - Solo ADMIN (crítico)
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable Long id) { ... }

  // Importación masiva - Solo SETTINGS/ADMIN (catálogos)
  @PostMapping("/import")
  @PreAuthorize("hasAnyRole('SETTINGS', 'ADMIN')")
  public ResponseEntity<ImportResult> importCustomers(@RequestBody List<Customer> customers) { ... }
}
```

### 2. Tests de Autorización

**Ejemplo**:
```java
@Test
@WithMockUser(roles = "READONLY")
void testReadOnlyCannotCreateCustomer() {
  mockMvc.perform(post("/api/v1/customers")
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"name\":\"Test\"}"))
    .andExpect(status().isForbidden());
}

@Test
@WithMockUser(roles = "ERP_USER")
void testErpUserCanCreateCustomer() {
  mockMvc.perform(post("/api/v1/customers")
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"name\":\"Test\"}"))
    .andExpect(status().isCreated());
}

@Test
@WithMockUser(roles = "ERP_USER")
void testErpUserCannotDeleteCustomer() {
  mockMvc.perform(delete("/api/v1/customers/1"))
    .andExpect(status().isForbidden());
}

@Test
@WithMockUser(roles = "ADMIN")
void testAdminCanDeleteCustomer() {
  mockMvc.perform(delete("/api/v1/customers/1"))
    .andExpect(status().isNoContent());
}
```

---

## 📖 Referencias

- Spring Security Method Security: https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html
- @PreAuthorize: https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/access/prepost/PreAuthorize.html
- Role vs Authority: https://www.baeldung.com/spring-security-granted-authority-vs-role

---

**Versión**: 1.0  
**Autor**: Sprint 3 - RBAC Complete  
**Próxima Revisión**: Sprint 8 (JPA Auditing)

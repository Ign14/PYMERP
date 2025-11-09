# Sprint: Refactorización Indicadores, DTE Chile y UX Mejorada

## Fecha inicio: 2025-11-08
## Estado: EN PROGRESO

---

## ✅ COMPLETADO

### 1. Backend - Términos de Pago/Cobro
- [x] Migración V29: Columnas `payment_term_days` en `sales` y `purchases`
- [x] Constraint CHECK: valores permitidos (7, 15, 30, 60 días)
- [x] Enum `PaymentTerm` con métodos `fromDays()` y `isValid()`
- [x] Validador personalizado `@ValidPaymentTerm`
- [x] Actualizado `SaleReq` y `PurchaseReq` con validación
- [x] Métodos `getDueDate()` en entidades `Sale` y `Purchase`

**Archivos modificados:**
- `backend/src/main/resources/db/migration/V29__add_payment_terms.sql`
- `backend/src/main/java/com/datakomerz/pymes/finances/PaymentTerm.java`
- `backend/src/main/java/com/datakomerz/pymes/validation/ValidPaymentTerm.java`
- `backend/src/main/java/com/datakomerz/pymes/validation/ValidPaymentTermValidator.java`
- `backend/src/main/java/com/datakomerz/pymes/sales/dto/SaleReq.java`
- `backend/src/main/java/com/datakomerz/pymes/purchases/dto/PurchaseReq.java`

---

## 🔄 EN PROGRESO

### 2. Backend - Indicadores Financieros con Buckets
**Objetivo**: Cuentas por cobrar/pagar agrupadas por vencimiento

**Buckets definidos**:
- Vencido (< 0 días)
- 0-7 días
- 8-15 días
- 16-30 días
- 31-60 días
- 60+ días

**Entregables**:
- [ ] `FinancesService.getAccountsReceivableBuckets()`
- [ ] `FinancesService.getAccountsPayableBuckets()`
- [ ] Endpoint GET `/api/v1/finances/accounts-receivable/buckets`
- [ ] Endpoint GET `/api/v1/finances/accounts-payable/buckets`
- [ ] Tests unitarios

---

## ⏳ PENDIENTE

### 3. Backend - CAPTCHA en Ventas/Compras
**Componentes existentes**: 
- `SimpleCaptchaValidationService` (ya implementado)
- `SimpleCaptchaPayload` (ya en `SaleReq` y `PurchaseReq`)

**Tareas**:
- [ ] Integrar validación CAPTCHA en `SalesService.create()`
- [ ] Integrar validación CAPTCHA en `PurchasesService.create()`
- [ ] Tests: rechazo si CAPTCHA incorrecto cuando `app.security.captcha.enabled=true`

---

### 4. Backend - Plantillas DTE Chile

**Objetivo**: Sistema de plantillas XML para documentos tributarios electrónicos (SII)

#### 4.1 Diseño XSD Plantillas
**Estructura propuesta**:
```xml
<PrintTemplate xmlns="https://pymerp.cl/schema/print-template/v1">
  <Metadata>
    <Name>Factura Electrónica 33</Name>
    <DocumentType>33</DocumentType>
    <Version>1.0</Version>
  </Metadata>
  
  <Layout>
    <Page size="letter" margins="10mm 10mm 10mm 10mm"/>
    <Header>
      <Logo source="{company.logoUrl}" height="40mm"/>
      <CompanyInfo>
        <Field binding="company.legalName" font="bold" size="14"/>
        <Field binding="company.taxId" label="RUT:" />
        ...
      </CompanyInfo>
    </Header>
    
    <Body>
      <ItemsTable>
        <Column binding="item.code" label="Código" width="15%"/>
        <Column binding="item.description" label="Descripción" width="40%"/>
        ...
      </ItemsTable>
    </Body>
    
    <Footer>
      <Barcode type="PDF417" binding="ted.base64"/>
      <Text>Representación impresa de DTE</Text>
    </Footer>
  </Layout>
  
  <DataBindings>
    <!-- Mapeo de campos JSON a nodos DTE -->
  </DataBindings>
</PrintTemplate>
```

**Tareas**:
- [ ] Crear `schema/print-template-v1.xsd`
- [ ] Plantilla XML: Factura Electrónica (33)
- [ ] Plantilla XML: Orden de Compra (no DTE)
- [ ] Documentar estructura en `docs/DTE_TEMPLATES.md`

#### 4.2 Servicio de Renderizado PDF
**Componentes**:
- [ ] `PdfRenderService.generatePdf(templateXml, payloadJson)`
- [ ] `BarcodeService.generatePDF417(tedXml)` (usando ZXing)
- [ ] `DteMapper.toXml(FiscalDocument)` (mapear a estructura SII)
- [ ] Endpoint POST `/api/v1/documents/render-pdf`

**Dependencias** (agregar a `build.gradle`):
```gradle
implementation 'com.itextpdf:itext7-core:7.2.5'
implementation 'com.google.zxing:core:3.5.3' // Ya existe
implementation 'com.google.zxing:javase:3.5.3' // Ya existe
```

---

### 5. Backend - Gráficos con Granularidad Adaptativa

**Endpoint**: `GET /api/v1/analytics/trends?from={date}&to={date}&granularity={auto|day|month|quarter|year}`

**Lógica de granularidad**:
```java
if (days <= 31) → GROUP BY DATE_TRUNC('day', created_at)
if (days <= 365) → GROUP BY DATE_TRUNC('month', created_at)
if (days <= 900) → GROUP BY DATE_TRUNC('quarter', created_at)
else → GROUP BY DATE_TRUNC('year', created_at)
```

**Tareas**:
- [ ] `AnalyticsService.getTrendData(from, to, granularity)`
- [ ] DTO `TrendDataPoint(timestamp, total, count)`
- [ ] Tests: verificar totales consistentes vs suma de agregados

---

### 6. Frontend - Indicadores Condicionales

**Objetivo**: No renderizar tiles vacíos en Dashboard

**Componente**: `ui/src/components/Dashboard.tsx`

**Lógica**:
```tsx
{stats.totalSales > 0 && (
  <Tile title="Ventas" value={stats.totalSales} />
)}
```

**Tareas**:
- [ ] Modificar renderizado condicional
- [ ] Test: `expect(screen.queryByText(/Ventas/i)).not.toBeInTheDocument()` si `totalSales === 0`

---

### 7. Frontend - Formularios con Payment Terms

#### 7.1 Formulario Ventas
**Archivo**: `ui/src/components/SaleForm.tsx`

**Cambios**:
```tsx
<select name="paymentTermDays" required>
  <option value="7">7 días</option>
  <option value="15">15 días</option>
  <option value="30">30 días</option>
  <option value="60">60 días</option>
</select>

{captchaEnabled && (
  <CaptchaChallenge 
    question={`¿Cuánto es ${captcha.a} + ${captcha.b}?`}
    onAnswer={setCaptchaAnswer}
  />
)}
```

#### 7.2 Formulario Compras
**Similar a ventas**, agregar `paymentTermDays` select

#### 7.3 Orden de Compra (Nueva)
**Archivo**: `ui/src/components/PurchaseOrderForm.tsx`

**Campos**:
- Proveedor (autocomplete)
- Fecha emisión
- Término de pago (7/15/30/60)
- Moneda (CLP, USD)
- Centro de costo (opcional)
- Ítems: producto, qty, precio unitario, descuento
- Estados: Borrador, Emitida
- Botones: Guardar, Guardar borrador, Imprimir, Cancelar

---

### 8. Frontend - Gráficos Responsivos

**Componente**: `ui/src/components/TrendChart.tsx`

**Props**:
```tsx
interface TrendChartProps {
  data: TrendDataPoint[]
  granularity: 'day' | 'month' | 'quarter' | 'year'
  dateRange: { from: Date; to: Date }
}
```

**Lógica**:
- Ajustar formato eje X según granularidad
- Tooltips con formato apropiado
- Librería: Recharts o Chart.js

---

### 9. Frontend - Indicadores Financieros

**Componente**: `ui/src/components/AccountsPayableChart.tsx`

**Visualización**: Barras apiladas por bucket
- Rojo: Vencido
- Naranja: 0-7 días
- Amarillo: 8-15 días
- Verde: 16+ días

---

### 10. Flutter - Sincronización

**Modelos**:
- Agregar `paymentTermDays` a `Sale` y `Purchase`
- Actualizar formularios
- Offline: CAPTCHA pre-rellenado si no hay conexión

---

### 11. Tests

#### Backend
- [ ] `PaymentTermValidatorTest`: rechazar 10, 45 días
- [ ] `SalesServiceTest`: CAPTCHA requerido si enabled
- [ ] `FinancesServiceTest`: buckets con casos edge (sin documentos)
- [ ] `PdfRenderServiceTest`: generar PDF válido

#### Frontend
- [ ] `Dashboard.test.tsx`: tiles ocultos si count=0
- [ ] `SaleForm.test.tsx`: validación payment terms
- [ ] `TrendChart.test.tsx`: granularidad correcta

---

### 12. Documentación

**Archivos a crear**:
- [ ] `docs/DTE_CHILE.md`: Normativa SII, códigos documentos, estructura XML
- [ ] `docs/TEMPLATES.md`: Uso de XSD, ejemplos, bindings
- [ ] `docs/CAPTCHA.md`: Configuración, uso en forms
- [ ] `backend/README_FINANCES.md`: Buckets, cuentas por cobrar/pagar

---

## Normativa SII (Referencias)

### Documentos Tributarios Electrónicos
- **Factura Afecta**: Código 33
- **Factura Exenta**: Código 34
- **Boleta Afecta**: Código 39
- **Boleta Exenta**: Código 41
- **Guía de Despacho**: Código 52
- **Nota de Débito**: Código 56
- **Nota de Crédito**: Código 61

### Estructura XML DTE (simplificada)
```xml
<DTE xmlns="http://www.sii.cl/SiiDte">
  <Documento>
    <Encabezado>
      <IdDoc>
        <TipoDTE>33</TipoDTE>
        <Folio>12345</Folio>
        <FchEmis>2025-11-08</FchEmis>
      </IdDoc>
      <Emisor>
        <RUTEmisor>76123456-7</RUTEmisor>
        <RznSoc>Empresa Demo SpA</RznSoc>
        <GiroEmis>Venta al por menor</GiroEmis>
      </Emisor>
      <Receptor>
        <RUTRecep>12345678-9</RUTRecep>
        <RznSocRecep>Cliente S.A.</RznSocRecep>
      </Receptor>
      <Totales>
        <MntNeto>100000</MntNeto>
        <TasaIVA>19</TasaIVA>
        <IVA>19000</IVA>
        <MntTotal>119000</MntTotal>
      </Totales>
    </Encabezado>
    <Detalle>
      <NroLinDet>1</NroLinDet>
      <NmbItem>Producto A</NmbItem>
      <QtyItem>10</QtyItem>
      <PrcItem>10000</PrcItem>
      <MontoItem>100000</MontoItem>
    </Detalle>
  </Documento>
  <Signature><!-- Firma digital XMLDSIG --></Signature>
</DTE>
```

### TED (Timbre Electrónico DTE)
- Codificado en PDF417
- Debe incluirse en representación impresa
- Generado después de firma digital

**Glosa requerida**: "Representación impresa de Documento Tributario Electrónico"

---

## Próximos Pasos (Orden recomendado)

1. **Completar indicadores financieros con buckets** (Backend)
2. **Integrar CAPTCHA en ventas/compras** (Backend)
3. **Diseñar XSD plantillas + 2 ejemplos XML** (Backend)
4. **Implementar PdfRenderService básico** (Backend)
5. **Frontend: Indicadores condicionales** (UI)
6. **Frontend: Forms con payment terms** (UI)
7. **Frontend: Gráficos responsivos** (UI)
8. **Tests unitarios críticos** (Backend + Frontend)
9. **Documentación DTE Chile** (Docs)
10. **Integración Flutter** (App)

---

## Comandos útiles

```bash
# Backend: Aplicar migración V29
cd backend
./gradlew bootRun  # PostgreSQL levantará la migración

# Frontend: Tests
cd ui
npm run test -- --coverage

# Verificar Swagger
http://localhost:8081/swagger-ui/index.html

# Verificar salud backend
curl http://localhost:8081/actuator/health
```

---

## Notas técnicas

- **ZXing**: Librería para PDF417 ya presente en `build.gradle`
- **CAPTCHA**: `SimpleCaptchaValidationService` ya funcional
- **Multitenencia**: Mantener `@TenantFiltered` en nuevas entidades
- **Seguridad**: DTOs validados con Jakarta Validation
- **Offline**: Flutter debe cachear payment terms disponibles


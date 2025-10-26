# API para Proveedor de Facturación

## Propósito
Esta especificación resume los endpoints que el proveedor externo debe exponer para integrarse con el backend de PyMERP durante escenarios de contingencia y operación normal. Incluye requisitos de idempotencia, formato de mensajes y manejo de errores.

## Autenticación y seguridad
- **Idempotencia**: todos los `POST` deben incluir el encabezado `Idempotency-Key` (máximo 100 caracteres, sensible a mayúsculas/minúsculas). El backend reutiliza respuestas anteriores si recibe la misma clave.  
- **Firma HMAC**: los webhooks enviados hacia PyMERP deben firmarse siguiendo `SEGURIDAD_WEBHOOKS.md`.  
- **Canal seguro**: se requiere HTTPS mutuo o, al menos, TLS 1.2 con lista blanca de IPs.

## Encabezados estándar
| Encabezado | Obligatorio | Descripción |
| --- | --- | --- |
| `Authorization` | Sí | Implementación específica (por ejemplo `Bearer <token>`). |
| `Idempotency-Key` | Sí en `POST /invoices/offline` | Identificador único por documento. El cuerpo puede repetirlo en `idempotencyKey`; ambos valores deben coincidir. |
| `X-Signature` | Solo webhooks | Formato `t=<epoch>,v1=<sha256>`. |

## Endpoints esperados

### 1. Emitir documento offline
- **Ruta**: `POST /api/v1/provider/invoices/offline`
- **Request body** (JSON):
  ```json
  {
    "documentType": "FACTURA_ELECTRONICA",
    "taxMode": "AFECTA",
    "sale": {
      "id": "6c5d9c3e-5b35-46c7-a42b-8d2f8fbb12d1",
      "items": [
        {
          "productId": "131c3b0a-05c3-4815-ac96-2a4bb52a857e",
          "quantity": 2,
          "unitPrice": 7490
        }
      ],
      "net": 14980,
      "vat": 2846,
      "total": 17826,
      "customerName": "Cliente Contingencia",
      "customerTaxId": "76859432-1",
      "pointOfSale": "POS-01",
      "deviceId": "DEV-01"
    },
    "idempotencyKey": "idem-20250101-0001",
    "forceOffline": true,
    "connectivityHint": "no-link"
  }
  ```
- **Respuesta (201 Created)**:
  ```json
  {
    "id": "b9ce6c41-0ab3-4b8b-897e-9574dce65c9c",
    "documentType": "FACTURA_ELECTRONICA",
    "status": "OFFLINE_PENDING",
    "number": null,
    "provisionalNumber": "CTG-2025-00042",
    "trackId": null,
    "provider": "demo-provider",
    "offline": true,
    "createdAt": "2025-01-01T12:34:56Z",
    "updatedAt": "2025-01-01T12:34:56Z",
    "links": {
      "localPdf": "s3://pymes-contingency-placeholder/documents/CTG-2025-00042-local.pdf",
      "officialPdf": null
    },
    "files": []
  }
  ```

### 2. Consultar documento
- **Ruta**: `GET /api/v1/provider/invoices/{providerDocumentId}`
- **Respuesta (200 OK)**:
  ```json
  {
    "providerDocumentId": "doc-123",
    "status": "ACCEPTED",
    "number": "F001-000123",
    "trackId": "track-555",
    "officialDocument": {
      "downloadUrl": "https://provider.example.com/documents/doc-123.pdf",
      "filename": "doc-123.pdf",
      "contentType": "application/pdf"
    }
  }
  ```
- Usado por `ContingencySyncJob` cuando el proveedor entrega solo un track ID en la primera llamada.

### 3. Confirmar recepción de contingencia
- **Ruta**: `POST /api/v1/provider/invoices/{providerDocumentId}/ack`
- **Cuerpo**:
  ```json
  {
    "status": "ACCEPTED",
    "rejectionReason": null
  }
  ```
- **Respuesta**: `202 Accepted`. Permite notificar al backend antes del webhook si la sincronización fue exitosa.

## Códigos de error esperados
| Código | Contexto | Acción recomendada |
| --- | --- | --- |
| `400 Bad Request` | Falta `Idempotency-Key`, payload inválido o campos obligatorios ausentes. | Corregir datos y reintentar con la misma clave. |
| `401 Unauthorized` | Token inválido o expirado. | Refrescar credenciales. |
| `409 Conflict` | Reutilización de `Idempotency-Key` con cuerpo distinto. | Revisar duplicados; no reintentar hasta aclarar. |
| `422 Unprocessable Entity` | Validaciones de negocio fallidas (por ejemplo, producto sin configuración tributaria). | Corregir datos y reemitir. |
| `429 Too Many Requests` | Límite de rate alcanzado. | Implementar backoff exponencial acorde a `billing.offline.retry.backoffMs`. |
| `500 Internal Server Error` | Error no controlado en el proveedor. | Registrar incidente y escalar. |
| `503 Service Unavailable` | Proveedor detenido por mantenimiento. | El backend activará modo contingencia automáticamente. |

## Consideraciones de idempotencia
- Usar claves determinísticas (`<companyId>-<saleId>-<sequence>`).  
- Conservar la clave original al reintentar solicitudes fallidas.  
- `forceOffline=true` permite crear documentos sin consulta inmediata al proveedor cuando está caído.

## Webhooks desde el proveedor hacia PyMERP
- Endpoint: `POST https://<pymerp>/webhooks/billing`.  
- Encabezado obligatorio: `X-Signature`.  
- Payload mínimo:
  ```json
  {
    "documentId": "b9ce6c41-0ab3-4b8b-897e-9574dce65c9c",
    "status": "ACCEPTED",
    "providerDocumentId": "doc-123",
    "trackId": "track-555",
    "number": "F001-000123",
    "downloadUrl": "https://provider.example.com/documents/doc-123.pdf"
  }
  ```
- Verificar detalles de seguridad en `SEGURIDAD_WEBHOOKS.md`.

## Versionado
- Incluir encabezado `X-Provider-Version` con el número de versión del API.  
- Cambios incompatibles deben anunciarse con 30 días de anticipación. Se sugiere `SemVer`.

# Seguridad para Webhooks de Facturación

## Secretos utilizados
- **Entrante** (`billing.webhook.secret` – env `BILLING_WEBHOOK_SECRET`): valida el encabezado `X-Signature` recibido por `POST /webhooks/billing`.  
- **Saliente** (`webhooks.hmac.secret` – env `WEBHOOKS_HMAC_SECRET`): se usa para firmar notificaciones emitidas por PyMERP hacia terceros.

Mantenga ambos secretos en un gestor seguro (por ejemplo, AWS Secrets Manager o HashiCorp Vault) y evite compartirlos por canales no cifrados.

## Formato de firma (entrante)
El proveedor debe construir el encabezado `X-Signature` con el siguiente formato:

```
t=<epoch_seconds>,v1=<sha256_hex>
```

1. Tomar el cuerpo sin modificar (JSON UTF-8).  
2. Construir `payload = "<epoch_seconds>.<body>"`.  
3. Calcular `HMAC-SHA256` usando `billing.webhook.secret`.  
4. Convertir el resultado a hexadecimal en minúsculas.  
5. Enviar el encabezado `X-Signature` con los valores anteriores.

Ejemplo en Node.js:

```js
const crypto = require("crypto");
const secret = process.env.BILLING_WEBHOOK_SECRET;
const body = JSON.stringify(payload);
const timestamp = Math.floor(Date.now() / 1000);
const toSign = `${timestamp}.${body}`;
const signature = crypto.createHmac("sha256", secret).update(toSign).digest("hex");
const header = `t=${timestamp},v1=${signature}`;
```

## Validación en PyMERP
- Se rechaza la petición (`401 Unauthorized`) si falta el encabezado, si la firma no coincide o si el timestamp difiere más que la tolerancia (`billing.webhook.tolerance`, 5 minutos por defecto).  
- El verificador admite firmas prefijadas con `sha256=` para compatibilidad.

## Rotación de secretos
1. Generar un nuevo secreto en el gestor seguro.  
2. Actualizar primero el proveedor externo para que firme con el valor nuevo, manteniendo el anterior en paralelo si es posible.  
3. Actualizar las variables `BILLING_WEBHOOK_SECRET` y `WEBHOOKS_HMAC_SECRET` en PyMERP (deploy sin reiniciar clientes si usa `Spring Cloud Config`).  
4. Verificar éxito con una llamada de prueba (`curl` o `scripts/test-protected.ps1` si requiere token).  
5. Revocar el secreto anterior y auditar accesos.

## Desfase de reloj
- El verificador usa `java.time.Clock`; sincronice servidores con NTP (`chrony`, `systemd-timesyncd` o `Windows Time Service`).  
- Ajuste la tolerancia mediante `billing.webhook.tolerance=PT10M` si el proveedor no puede garantizar ±5 minutos, pero documente el riesgo en Operaciones.

## Firma de webhooks salientes
Cuando PyMERP publíca webhooks a terceros:
1. Calcular `timestamp` y `signature` con el secreto `webhooks.hmac.secret`.  
2. Enviar encabezados:
   - `X-Signature: t=<epoch>,v1=<hex>`  
   - `X-Pymerp-Webhook: billing` (opcional para identificar el origen).  
3. Registrar el hash y la URL en logs de auditoría.

## Checklist de seguridad
- [ ] Secretos rotados al menos cada 90 días.  
- [ ] Almacenamiento cifrado en reposo.  
- [ ] Logs sin volcar payloads sensibles.  
- [ ] Tests automatizados que validan firma utilizando `BillingWebhookControllerTest`.

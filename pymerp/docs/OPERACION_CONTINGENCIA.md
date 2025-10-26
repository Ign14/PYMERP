# Operación de Contingencia de Facturación

## Resumen
El modo de contingencia permite emitir documentos fiscales cuando el proveedor externo no está disponible y sincronizarlos después mediante `ContingencySyncJob`. Esta guía describe cómo activar o desactivar el modo offline, ejecutar scripts de soporte y monitorear la salud del proceso.

## Activación rápida
1. **Actualizar configuración**  
   - Variable de entorno: `BILLING_OFFLINE_ENABLED=true`.  
   - Mensaje legal desplegado en los documentos: `billing.offline.legend="Emitido en contingencia; sujeto a validación"`.  
   - Ajustar reintentos si es necesario: `billing.offline.retry.maxAttempts` y `billing.offline.retry.backoffMs`.
2. **Validar credenciales**  
   - S3: definir `STORAGE_S3_BUCKET`, `STORAGE_S3_ACCESS_KEY`, `STORAGE_S3_SECRET_KEY`, `STORAGE_S3_REGION`.  
   - Webhooks: `BILLING_WEBHOOK_SECRET` (entrante) y `WEBHOOKS_HMAC_SECRET` (saliente).
3. **Aplicar migraciones**  
   Ejecutar `scripts/flyway-migrate.sh` (Linux/macOS) o `scripts/flyway-migrate.ps1` (Windows) con las credenciales de base de datos correctas.
4. **Sembrar datos mínimos**  
   Correr `scripts/seed-contingency.sh` o `scripts/seed-contingency.ps1` para crear la empresa base, un usuario administrador y el bucket requerido. Revise los parámetros dummy antes de ejecutarlos.
5. **Desplegar backend y Flutter**  
   El pipeline `Contingency CI` compila y prueba ambos componentes automáticamente.

## Desactivación
1. Cambiar `BILLING_OFFLINE_ENABLED=false` y desplegar.  
2. Confirmar que la cola `contingency_queue_items` está vacía (`status` distinto de `OFFLINE_PENDING` o `SYNCING`).  
3. Eliminar o archivar documentos provisionales sobrantes.  
4. Revocar credenciales temporales creadas durante la contingencia.

## Monitoreo
- **Métricas principales (Micrometer/Prometheus)**  
  - `billing.contingency.pending`: ítems pendientes.  
  - `billing.contingency.syncing`: ítems en sincronización.  
  - `billing.contingency.failures`: fallos permanentes.  
  - `billing.contingency.latencyMs`: latencia desde emisión hasta sincronización.  
- **Puntos de control**  
  - Endpoint `GET /actuator/health` para validar conectividad con la base y Redis.  
  - `GET /actuator/metrics/billing.contingency.*` para inspección puntual.  
  - `GET /api/v1/billing/documents?offline=true` (requiere autenticación) para revisar documentos aún no sincronizados.

## Ejecución de sincronización
- **Automática**: `ContingencySyncJob` corre de acuerdo al `fixedDelay` configurado (`billing.offline.retry.backoffMs`).  
- **Manual**:
  ```bash
  # Ejecutar pruebas puntuales contra el job con perfil test-scheduler
  ./gradlew test --tests "com.datakomerz.pymes.billing.job.ContingencySyncJobTest" \
    -Dspring.profiles.active=test-scheduler
  ```
  - Para casos urgentes, se puede invocar el bean desde una consola `spring-shell` o tarea `ApplicationRunner`.

## Resolución de fallos frecuentes
| Síntoma | Causa probable | Acciones |
| --- | --- | --- |
| `billing.contingency.failures` incrementa rápidamente | Error permanente devuelto por el proveedor | Revisar mensaje en `fiscal_documents.error_detail`, corregir y reactivar el documento. |
| Ítems quedan en `SYNCING` | Corte de energía o excepción transitoria | Revisar logs de `ContingencySyncJob`, verificar conectividad S3 y reintentar con `billing.offline.retry.maxAttempts` mayor. |
| Webhook invalida la firma | Secret desalineado o reloj fuera de tolerancia | Rotar secreto (`BILLING_WEBHOOK_SECRET`) y verificar NTP (tolerancia por defecto 5 minutos). |

## Pipeline CI relacionado
- Workflow: `.github/workflows/contingency-ci.yml`.
  - Job `Backend Tests`: ejecuta `./gradlew test` y publica artefactos (`templates/billing/*.html`, `db/migration/*.sql`).
  - Job `Contingency Sync Job Check`: garantiza que `ContingencySyncJob` siga operando con el perfil `test-scheduler`.
  - Job `Flutter Tests`: corre `flutter test` dentro de `app_flutter`.

## Lista de verificación post contingencia
1. Confirmar que todos los documentos offline obtuvieron número oficial y estado `ACCEPTED`.  
2. Bajar `billing.offline.retry.maxAttempts` si se elevó temporalmente.  
3. Regenerar credenciales temporales (S3, webhooks) usadas durante la emergencia.  
4. Documentar la incidencia en el runbook y actualizar métricas históricas.

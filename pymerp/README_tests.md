# Guía de Pruebas E2E

## Corte de internet al confirmar
- Ejecuta `./gradlew bootRun` en `backend/` y `flutter run` en `app_flutter/` usando la configuración de pruebas (`application-test.yml` / entorno de staging).
- En la app Flutter inicia sesión y registra una venta hasta el paso de confirmación. Mantén abierta la vista `RegistrarVentaPage`.
- Desconecta la red (apaga Wi‑Fi o usa `nmcli/networksetup` según tu SO) justo antes de pulsar **Confirmar emisión**.
- Confirma la emisión: verifica en la UI que el documento queda con badge `Contingencia` y estado `OFFLINE_PENDING`.
- Revisa en el backend (tabla `contingency_queue_items`) que se creó un item con `status=OFFLINE_PENDING` y que existe el PDF local en `storage/billing/fiscal/<id>/local-*.pdf`.

## Reconexión
- Restablece la conexión a internet.
- Ejecuta manualmente `ContingencySyncJob` con `./gradlew bootRun --args='--billing.sync'` o invoca el endpoint de sincronización si está disponible.
- Observa en la app que el estado pasa a `SYNCING` y luego a `SENT`. El backend debe reflejar `contingency_queue_items.status=SYNCING`.
- Simula la recepción del webhook `ACCEPTED` usando `curl`:
  ```bash
  curl -X POST http://localhost:8080/webhooks/billing \
    -H 'Content-Type: application/json' \
    -d @backend/src/test/resources/fixtures/billing/webhook_accepted.json
  ```
- Verifica que el documento cambia a `ACCEPTED`, que se descargan los archivos oficiales y que el historial muestra la numeración definitiva.

# Módulo de persistencia de facturación (offline)

```text
Sale (sales)
   │ 1 ── n
   │
   ├─ FiscalDocument (fiscal_documents)
   │      │ 1 ── n
   │      └─ DocumentFile (document_files, kind=FISCAL)
   │
   └─ NonFiscalDocument (non_fiscal_documents)
          │ 1 ── n
          └─ DocumentFile (document_files, kind=NON_FISCAL)

FiscalDocument
   │ 1 ── 0..1
   └─ ContingencyQueueItem (contingency_queue_items)
```

- `FiscalDocument` guarda el seguimiento de los DTE emitidos (online u offline) con folio provisional y metadatos de sincronización.
- `NonFiscalDocument` cubre cotizaciones y comprobantes internos siempre en estado `READY`.
- `DocumentFile` mantiene el historial de archivos generados (PDF local/oficial) con referencias encadenadas mediante `previousFileId`.
- `ContingencyQueueItem` encola las emisiones offline junto con el payload JSON encriptable (`encryptedBlob`) para reintentos posteriores.

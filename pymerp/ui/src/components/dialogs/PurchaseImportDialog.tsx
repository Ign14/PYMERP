import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { importPurchasesFromCSV, type PurchaseImportResult } from '../../services/client'

type Props = {
  open: boolean
  onClose: () => void
  onImported: () => void
}

export default function PurchaseImportDialog({ open, onClose, onImported }: Props) {
  const [file, setFile] = useState<File | null>(null)
  const [result, setResult] = useState<PurchaseImportResult | null>(null)

  const importMutation = useMutation({
    mutationFn: (file: File) => importPurchasesFromCSV(file),
    onSuccess: data => {
      setResult(data)
      if (data.success) {
        onImported()
      }
    },
    onError: error => {
      window.alert(`Error al importar: ${error}`)
    },
  })

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0]
    if (selectedFile) {
      if (!selectedFile.name.endsWith('.csv')) {
        window.alert('Solo se permiten archivos CSV')
        return
      }
      setFile(selectedFile)
      setResult(null)
    }
  }

  const handleImport = () => {
    if (!file) {
      window.alert('Seleccione un archivo CSV')
      return
    }
    importMutation.mutate(file)
  }

  const handleClose = () => {
    setFile(null)
    setResult(null)
    onClose()
  }

  if (!open) return null

  return (
    <div className="modal-backdrop" onClick={handleClose}>
      <div className="modal-card" onClick={e => e.stopPropagation()}>
        <h2>Importar compras desde CSV</h2>

        <div className="form-group">
          <label>Archivo CSV</label>
          <input
            type="file"
            accept=".csv"
            onChange={handleFileChange}
            disabled={importMutation.isPending}
          />
          {file && (
            <p className="muted" style={{ marginTop: '0.5rem' }}>
              Archivo seleccionado: {file.name} ({(file.size / 1024).toFixed(2)} KB)
            </p>
          )}
        </div>

        <div className="form-group">
          <p className="muted">
            <strong>Formato esperado del CSV:</strong>
            <br />
            Tipo Documento, Número, Proveedor ID, Estado, Neto, IVA, Total, Fecha Emisión
            <br />
            <em>
              Ejemplo: Factura, 12345, {'{UUID}'}, Recibida, 10000, 1900, 11900, 2024-01-15 10:30:00
            </em>
          </p>
        </div>

        {result && (
          <div
            className={`alert ${result.success ? 'success' : 'error'}`}
            style={{ marginTop: '1rem' }}
          >
            <p>
              <strong>
                {result.success ? '✓ Importación exitosa' : '⚠ Importación con errores'}
              </strong>
            </p>
            <p>
              Importadas: {result.imported} de {result.total} filas
            </p>
            {result.errors && result.errors.length > 0 && (
              <div style={{ marginTop: '0.5rem' }}>
                <strong>Errores:</strong>
                <ul style={{ marginTop: '0.5rem', maxHeight: '200px', overflow: 'auto' }}>
                  {result.errors.map((error, idx) => (
                    <li key={idx}>{error}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}

        <div className="modal-actions">
          <button
            className="btn"
            type="button"
            onClick={handleImport}
            disabled={!file || importMutation.isPending}
          >
            {importMutation.isPending ? 'Importando...' : 'Importar'}
          </button>
          <button
            className="btn ghost"
            type="button"
            onClick={handleClose}
            disabled={importMutation.isPending}
          >
            {result ? 'Cerrar' : 'Cancelar'}
          </button>
        </div>
      </div>
    </div>
  )
}

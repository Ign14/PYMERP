import { useState } from 'react'

export default function PurchasesExportDialog({
  open,
  onClose,
  startDate,
  endDate,
  statusFilter,
}: {
  open: boolean
  onClose: () => void
  startDate: string
  endDate: string
  statusFilter?: string
}) {
  const [format, setFormat] = useState<'csv' | 'json'>('csv')
  const [selectedColumns, setSelectedColumns] = useState<string[]>([
    'fecha',
    'documento',
    'proveedor',
    'estado',
    'total',
  ])

  const allColumns = [
    { id: 'fecha', label: 'Fecha' },
    { id: 'documento', label: 'Documento' },
    { id: 'numero', label: 'Número' },
    { id: 'proveedor', label: 'Proveedor' },
    { id: 'estado', label: 'Estado' },
    { id: 'neto', label: 'Neto' },
    { id: 'iva', label: 'IVA' },
    { id: 'total', label: 'Total' },
  ]

  const toggleColumn = (columnId: string) => {
    setSelectedColumns(prev =>
      prev.includes(columnId) ? prev.filter(id => id !== columnId) : [...prev, columnId]
    )
  }

  const handleExport = () => {
    alert(
      `Exportando en formato ${format.toUpperCase()} con columnas: ${selectedColumns.join(', ')}`
    )
    onClose()
  }

  if (!open) return null

  return (
    <div
      className="fixed inset-0 bg-black/50 flex items-center justify-center z-50"
      onClick={onClose}
    >
      <div
        className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-6 max-w-2xl w-full mx-4"
        onClick={e => e.stopPropagation()}
      >
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-neutral-100 text-2xl font-semibold">Exportar Compras</h2>
          <button
            onClick={onClose}
            className="text-neutral-400 hover:text-neutral-100 text-2xl font-bold"
          >
            ×
          </button>
        </div>

        {/* Filtros aplicados */}
        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4 mb-6">
          <h3 className="text-neutral-300 font-medium mb-2">Filtros Aplicados</h3>
          <div className="text-sm text-neutral-400 space-y-1">
            <p>
              📅 Rango: {startDate} a {endDate}
            </p>
            <p>📊 Estado: {statusFilter || 'Todos'}</p>
          </div>
        </div>

        {/* Formato */}
        <div className="mb-6">
          <h3 className="text-neutral-300 font-medium mb-3">Formato de Exportación</h3>
          <div className="flex gap-3">
            <button
              onClick={() => setFormat('csv')}
              className={`px-4 py-2 rounded-lg border ${
                format === 'csv'
                  ? 'bg-blue-950 border-blue-800 text-blue-400'
                  : 'bg-neutral-800 border-neutral-700 text-neutral-400'
              }`}
            >
              CSV
            </button>
            <button
              onClick={() => setFormat('json')}
              className={`px-4 py-2 rounded-lg border ${
                format === 'json'
                  ? 'bg-blue-950 border-blue-800 text-blue-400'
                  : 'bg-neutral-800 border-neutral-700 text-neutral-400'
              }`}
            >
              JSON
            </button>
          </div>
        </div>

        {/* Selección de columnas */}
        <div className="mb-6">
          <h3 className="text-neutral-300 font-medium mb-3">Columnas a Exportar</h3>
          <div className="grid grid-cols-2 gap-2">
            {allColumns.map(col => (
              <label
                key={col.id}
                className="flex items-center gap-2 bg-neutral-800 border border-neutral-700 rounded-lg p-2 cursor-pointer hover:bg-neutral-750"
              >
                <input
                  type="checkbox"
                  checked={selectedColumns.includes(col.id)}
                  onChange={() => toggleColumn(col.id)}
                  className="form-checkbox"
                />
                <span className="text-neutral-300 text-sm">{col.label}</span>
              </label>
            ))}
          </div>
        </div>

        {/* Acciones */}
        <div className="flex justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-neutral-800 text-neutral-300 rounded-lg hover:bg-neutral-700"
          >
            Cancelar
          </button>
          <button
            onClick={handleExport}
            disabled={selectedColumns.length === 0}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
          >
            Exportar
          </button>
        </div>
      </div>
    </div>
  )
}

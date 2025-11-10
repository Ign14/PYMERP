import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { listInventoryMovements, listProducts, InventoryMovementDetail, Product } from '../services/client'
import PageHeader from '../components/layout/PageHeader'

const MOVEMENT_TYPES = [
  { value: '', label: 'Todos los tipos' },
  { value: 'PURCHASE_IN', label: 'Entrada por compra' },
  { value: 'SALE_OUT', label: 'Salida por venta' },
  { value: 'ADJUSTMENT', label: 'Ajuste manual' },
  { value: 'TRANSFER', label: 'Transferencia' },
]

export default function InventoryMovementsPage() {
  const [productId, setProductId] = useState('')
  const [type, setType] = useState('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')
  const [page, setPage] = useState(0)
  const [productSearch, setProductSearch] = useState('')

  const movementsQuery = useQuery({
    queryKey: ['inventoryMovements', { productId, type, dateFrom, dateTo, page }],
    queryFn: () =>
      listInventoryMovements({
        productId: productId || undefined,
        type: type || undefined,
        dateFrom: dateFrom || undefined,
        dateTo: dateTo || undefined,
        page,
        size: 20,
      }),
  })

  const productsQuery = useQuery({
    queryKey: ['products', { q: productSearch }],
    queryFn: () => listProducts({ q: productSearch || undefined, page: 0, size: 100 }),
    enabled: productSearch.length > 2,
  })

  const movements = movementsQuery.data?.content || []
  const totalPages = movementsQuery.data?.totalPages || 0

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleString('es-CL', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  const getTypeLabel = (typeValue: string) => {
    const found = MOVEMENT_TYPES.find(t => t.value === typeValue)
    return found ? found.label : typeValue
  }

  const getTypeColor = (typeValue: string) => {
    switch (typeValue) {
      case 'PURCHASE_IN':
        return '#10b981'
      case 'SALE_OUT':
        return '#ef4444'
      case 'ADJUSTMENT':
        return '#f59e0b'
      case 'TRANSFER':
        return '#3b82f6'
      default:
        return '#6b7280'
    }
  }

  return (
    <div className="page">
      <PageHeader title="Movimientos de Inventario" />

      <section className="card">
        <div className="card-header">
          <h3>Filtros</h3>
        </div>

        <div className="inline-actions" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
          <div>
            <label htmlFor="productFilter" className="label">
              Producto
            </label>
            <input
              id="productFilter"
              type="text"
              className="input"
              placeholder="Buscar por nombre o SKU..."
              value={productSearch}
              onChange={e => setProductSearch(e.target.value)}
            />
            {productsQuery.data && productsQuery.data.content.length > 0 && (
              <select
                className="input"
                style={{ marginTop: '0.5rem' }}
                value={productId}
                onChange={e => {
                  setProductId(e.target.value)
                  setPage(0)
                }}
              >
                <option value="">Seleccionar producto...</option>
                {productsQuery.data.content.map((p: Product) => (
                  <option key={p.id} value={p.id}>
                    {p.sku} - {p.name}
                  </option>
                ))}
              </select>
            )}
          </div>

          <div>
            <label htmlFor="typeFilter" className="label">
              Tipo de movimiento
            </label>
            <select
              id="typeFilter"
              className="input"
              value={type}
              onChange={e => {
                setType(e.target.value)
                setPage(0)
              }}
            >
              {MOVEMENT_TYPES.map(t => (
                <option key={t.value} value={t.value}>
                  {t.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="dateFromFilter" className="label">
              Desde
            </label>
            <input
              id="dateFromFilter"
              type="datetime-local"
              className="input"
              value={dateFrom}
              onChange={e => {
                setDateFrom(e.target.value)
                setPage(0)
              }}
            />
          </div>

          <div>
            <label htmlFor="dateToFilter" className="label">
              Hasta
            </label>
            <input
              id="dateToFilter"
              type="datetime-local"
              className="input"
              value={dateTo}
              onChange={e => {
                setDateTo(e.target.value)
                setPage(0)
              }}
            />
          </div>
        </div>

        <div style={{ marginTop: '1rem' }}>
          <button
            className="btn ghost"
            type="button"
            onClick={() => {
              setProductId('')
              setType('')
              setDateFrom('')
              setDateTo('')
              setProductSearch('')
              setPage(0)
            }}
          >
            Limpiar filtros
          </button>
        </div>
      </section>

      <section className="card">
        {movementsQuery.isLoading && <p className="muted">Cargando movimientos...</p>}
        {movementsQuery.isError && (
          <p className="panel-error">Error: {(movementsQuery.error as Error)?.message}</p>
        )}

        {!movementsQuery.isLoading && !movementsQuery.isError && movements.length === 0 && (
          <p className="muted">No se encontraron movimientos con los filtros aplicados.</p>
        )}

        {movements.length > 0 && (
          <>
            <div className="table-wrapper">
              <table className="table">
                <thead>
                  <tr>
                    <th>Fecha</th>
                    <th>Tipo</th>
                    <th>Producto</th>
                    <th>SKU</th>
                    <th>Lote/Batch</th>
                    <th>Cantidad</th>
                    <th>Ubicación</th>
                    <th>Referencia</th>
                    <th>Usuario</th>
                  </tr>
                </thead>
                <tbody>
                  {movements.map((movement: InventoryMovementDetail) => (
                    <tr key={movement.id}>
                      <td className="mono small">{formatDate(movement.createdAt)}</td>
                      <td>
                        <span
                          style={{
                            color: getTypeColor(movement.type),
                            fontWeight: 'bold',
                            fontSize: '0.875rem',
                          }}
                        >
                          {getTypeLabel(movement.type)}
                        </span>
                      </td>
                      <td>{movement.productName || '—'}</td>
                      <td className="mono small">{movement.productSku || '—'}</td>
                      <td className="mono small">
                        {movement.lotId ? (
                          <>
                            {movement.lotId.substring(0, 8)}
                            {movement.batchName && (
                              <span style={{ marginLeft: '0.5rem', color: '#6b7280' }}>
                                ({movement.batchName})
                              </span>
                            )}
                          </>
                        ) : (
                          '—'
                        )}
                      </td>
                      <td
                        className="mono"
                        style={{
                          color: movement.type === 'PURCHASE_IN' ? '#10b981' : '#ef4444',
                          fontWeight: 'bold',
                        }}
                      >
                        {movement.type === 'PURCHASE_IN' ? '+' : ''}
                        {movement.qty}
                      </td>
                      <td>
                        {movement.locationCode ? (
                          <span title={movement.locationName || ''}>
                            {movement.locationCode}
                          </span>
                        ) : (
                          '—'
                        )}
                      </td>
                      <td className="mono small">
                        {movement.refType && movement.refId
                          ? `${movement.refType}:${movement.refId.substring(0, 8)}`
                          : movement.note || '—'}
                      </td>
                      <td className="small">{movement.createdBy || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="pagination" style={{ marginTop: '1rem' }}>
              <button
                className="btn ghost"
                disabled={page === 0}
                onClick={() => setPage(p => Math.max(0, p - 1))}
              >
                ← Anterior
              </button>
              <span className="muted">
                Página {page + 1} de {totalPages || 1}
              </span>
              <button
                className="btn ghost"
                disabled={page >= totalPages - 1}
                onClick={() => setPage(p => p + 1)}
              >
                Siguiente →
              </button>
            </div>
          </>
        )}
      </section>
    </div>
  )
}

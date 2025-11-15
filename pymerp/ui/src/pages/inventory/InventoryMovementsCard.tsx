import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import ExportCsvButton, { CsvField } from '../../components/ExportCsvButton'
import { getMovements, InventoryMovementHistoryEntry } from '../../services/inventory'
import { listProducts } from '../../services/client'

const MOVEMENT_TYPES = [
  { value: '', label: 'Todos los tipos' },
  { value: 'PURCHASE_IN', label: 'Compras' },
  { value: 'SALE_OUT', label: 'Ventas' },
  { value: 'ADJUSTMENT', label: 'Ajustes' },
  { value: 'TRANSFER', label: 'Transferencias' },
]

const MOVEMENTS_PAGE_SIZE = 12

const TYPE_COLOR: Record<string, string> = {
  PURCHASE_IN: '#10b981',
  SALE_OUT: '#ef4444',
  ADJUSTMENT: '#f59e0b',
  TRANSFER: '#3b82f6',
}

type MovementFilters = {
  type: string
  productId: string
  lotId: string
  dateFrom: string
  dateTo: string
  page: number
}

export default function InventoryMovementsCard() {
  const [filters, setFilters] = useState<MovementFilters>({
    type: '',
    productId: '',
    lotId: '',
    dateFrom: '',
    dateTo: '',
    page: 0,
  })

  const movementsQuery = useQuery({
    queryKey: [
      'inventory-movements',
      filters.type,
      filters.productId,
      filters.lotId,
      filters.dateFrom,
      filters.dateTo,
      filters.page,
    ],
    queryFn: () =>
      getMovements({
        type: filters.type || undefined,
        productId: filters.productId || undefined,
        lotId: filters.lotId || undefined,
        dateFrom: filters.dateFrom ? new Date(filters.dateFrom).toISOString() : undefined,
        dateTo: filters.dateTo ? new Date(filters.dateTo).toISOString() : undefined,
        page: filters.page,
        size: MOVEMENTS_PAGE_SIZE,
        sort: 'createdAt,desc',
      }),
    keepPreviousData: true,
  })

  const productsQuery = useQuery({
    queryKey: ['inventory-movement-products'],
    queryFn: () => listProducts({ size: 200, status: 'all' }),
    staleTime: 60_000,
  })

  const productIndex = useMemo(() => {
    const items = productsQuery.data?.content ?? []
    return new Map(items.map(product => [product.id, product]))
  }, [productsQuery.data])

  const movements = movementsQuery.data?.content ?? []
  const totalPages = movementsQuery.data?.totalPages ?? 1

  const handleFilterChange = (key: keyof Omit<MovementFilters, 'page'>, value: string) => {
    setFilters(prev => ({ ...prev, [key]: value, page: 0 }))
  }

  const goPrevious = () => {
    setFilters(prev => ({ ...prev, page: Math.max(0, prev.page - 1) }))
  }

  const goNext = () => {
    setFilters(prev => ({ ...prev, page: Math.min(totalPages - 1, prev.page + 1) }))
  }

  const formatDate = (value: string) => {
    return new Date(value).toLocaleString('es-CL', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  const formatQtyValue = (value: number | null | undefined) =>
    value === null || value === undefined ? '—' : value.toLocaleString('es-CL')

  const getReferenceUrl = (refType?: string | null, refId?: string | null) => {
    if (!refType || !refId) {
      return undefined
    }
    const normalized = refType.toLowerCase()
    if (normalized.includes('purchase')) return `/app/purchases/${refId}`
    if (normalized.includes('sale')) return `/app/sales/${refId}`
    if (normalized.includes('adjust')) return `/app/inventory/adjustments/${refId}`
    if (normalized.includes('transfer')) return `/app/inventory/transfers/${refId}`
    return `/app/inventory/movements/${refId}`
  }

    const csvFields: CsvField<InventoryMovementHistoryEntry>[] = [
      {
        key: 'createdAt',
        label: 'Fecha',
        format: value => formatDate(String(value)),
      },
    { key: 'type', label: 'Tipo' },
    {
      key: 'productId',
      label: 'Producto',
      format: value => productIndex.get(String(value ?? ''))?.name ?? String(value ?? '—'),
    },
    { key: 'lotId', label: 'Lote' },
    {
      key: 'qtyChange',
      label: 'ΔCantidad',
      format: value => (value === null || value === undefined ? '' : String(value)),
    },
    {
      key: 'beforeQty',
      label: 'Antes',
      format: value => formatQtyValue(value),
    },
    {
      key: 'afterQty',
      label: 'Después',
      format: value => formatQtyValue(value),
    },
    {
      key: 'refType',
      label: 'Referencia',
      format: (value, row) =>
        `${value ?? '—'} ${row.refId ? String(row.refId).substring(0, 8) : ''}`.trim(),
    },
  ]

  return (
    <div className="card">
      <div className="card-header">
        <div>
          <h2>Historial de Movimientos (Auditoría)</h2>
          <p className="muted small">
            Registra cada entrada y salida de stock para mantener trazabilidad y auditoría.
          </p>
        </div>
        <div className="inline-actions">
          <ExportCsvButton
            filename="movimientos-inventario.csv"
        fields={csvFields}
            data={movements}
            disabled={movements.length === 0}
          />
        </div>
      </div>
      <div className="card-body" style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
        <div className="inline-actions" style={{ flexWrap: 'wrap', gap: '0.75rem' }}>
          <select
            className="input"
            value={filters.type}
            onChange={event => handleFilterChange('type', event.target.value)}
          >
            {MOVEMENT_TYPES.map(option => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          <select
            className="input"
            value={filters.productId}
            onChange={event => handleFilterChange('productId', event.target.value)}
          >
            <option value="">Todos los productos</option>
            {Array.from(productIndex.values()).map(product => (
              <option key={product.id} value={product.id}>
                {product.sku} · {product.name}
              </option>
            ))}
          </select>
          <input
            className="input"
            placeholder="Filtrar por lote..."
            value={filters.lotId}
            onChange={event => handleFilterChange('lotId', event.target.value)}
          />
          <label className="small">
            Desde
            <input
              type="date"
              className="input"
              value={filters.dateFrom}
              onChange={event => handleFilterChange('dateFrom', event.target.value)}
            />
          </label>
          <label className="small">
            Hasta
            <input
              type="date"
              className="input"
              value={filters.dateTo}
              onChange={event => handleFilterChange('dateTo', event.target.value)}
            />
          </label>
        </div>
      </div>
      {movementsQuery.isLoading ? (
        <p className="muted">Cargando movimientos…</p>
      ) : movementsQuery.isError ? (
        <p className="error">
          {(movementsQuery.error as Error)?.message ?? 'No se pudieron cargar los movimientos'}
        </p>
      ) : (
        <>
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Tipo</th>
                  <th>Producto</th>
                  <th>Lote</th>
                  <th>ΔCantidad</th>
                  <th>Antes / Después</th>
                  <th>Ubicación</th>
                  <th>Usuario</th>
                  <th>TraceId</th>
                  <th>Referencia</th>
                </tr>
              </thead>
              <tbody>
                {movements.length === 0 ? (
                  <tr>
                    <td colSpan={10} className="muted">
                      Ningún movimiento coincide con los filtros aplicados.
                    </td>
                  </tr>
                ) : (
                  movements.map(movement => {
                    const product = productIndex.get(movement.productId ?? '')
                    const refUrl = getReferenceUrl(movement.refType, movement.refId ?? undefined)
                    const locationFrom = movement.locationFrom?.name ?? movement.locationFrom?.id
                    const locationTo = movement.locationTo?.name ?? movement.locationTo?.id
                    const locationText =
                      locationFrom && locationTo
                        ? `${locationFrom} → ${locationTo}`
                        : locationFrom || locationTo || '—'

                    const deltaSign = movement.qtyChange > 0 ? '+' : ''
                    const beforeAfterText = `${formatQtyValue(movement.beforeQty)} → ${formatQtyValue(
                      movement.afterQty
                    )}`

                    return (
                      <tr key={movement.id}>
                        <td className="mono small">{formatDate(movement.createdAt)}</td>
                        <td>
                          <span
                            style={{
                              color: TYPE_COLOR[movement.type] ?? '#6b7280',
                              fontWeight: 600,
                              fontSize: '0.85rem',
                            }}
                          >
                            {movement.type}
                          </span>
                        </td>
                        <td>
                          {product?.name ?? movement.productId ?? '—'}
                          <div className="muted small">
                            {product?.sku ? `SKU: ${product.sku}` : movement.productId ?? ''}
                          </div>
                        </td>
                        <td className="mono small">{movement.lotId ?? '—'}</td>
                        <td className="mono">
                          <span
                            style={{
                              color: movement.qtyChange > 0 ? '#10b981' : '#ef4444',
                              fontWeight: 600,
                            }}
                          >
                            {deltaSign}
                            {movement.qtyChange.toLocaleString('es-CL')}
                          </span>
                        </td>
                        <td className="mono small">{beforeAfterText}</td>
                        <td className="small">{locationText}</td>
                        <td className="small">{movement.userId ?? movement.traceId ?? '—'}</td>
                        <td className="mono small">{movement.traceId ?? '—'}</td>
                        <td className="mono small">
                          {refUrl && movement.refId ? (
                            <Link to={refUrl}>{movement.refType} · {movement.refId.substring(0, 8)}</Link>
                          ) : (
                            movement.refType ? `${movement.refType} · ${movement.refId}` : '—'
                          )}
                        </td>
                      </tr>
                    )
                  })
                )}
              </tbody>
            </table>
          </div>
          <div className="pagination" style={{ marginTop: '1rem' }}>
            <button className="btn ghost" disabled={filters.page === 0} onClick={goPrevious}>
              ← Anterior
            </button>
            <span className="muted">
              Página {filters.page + 1} de {totalPages}
            </span>
            <button className="btn ghost" disabled={filters.page >= totalPages - 1} onClick={goNext}>
              Siguiente →
            </button>
          </div>
        </>
      )}
      <div className="muted small" style={{ marginTop: '0.5rem' }}>
        TraceId permite reconstruir auditorías completas; referencia se enlaza a la operación relacionada.
      </div>
    </div>
  )
}

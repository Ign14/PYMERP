import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import Modal from '../../components/dialogs/Modal'
import StatusBadge from '../../components/StatusBadge'
import {
  assignLotLocation,
  getLocations,
  getLots,
  InventoryLotListItem,
  ListInventoryLotsParams,
} from '../../services/inventory'
import { listProducts, listSuppliers } from '../../services/client'

const PAGE_SIZE = 10

const STATUS_OPTIONS = [
  { value: '', label: 'Todos los estados' },
  { value: 'OK', label: 'OK' },
  { value: 'BAJO_STOCK', label: 'Bajo stock' },
  { value: 'POR_VENCER', label: 'Por vencer' },
  { value: 'VENCIDO', label: 'Vencido' },
]

const SORT_OPTIONS = [
  { value: 'expDate,asc', label: 'Expiración ↑' },
  { value: 'expDate,desc', label: 'Expiración ↓' },
  { value: 'qtyAvailable,desc', label: 'Cantidad disponible ↓' },
  { value: 'qtyAvailable,asc', label: 'Cantidad disponible ↑' },
  { value: 'fechaIngreso,desc', label: 'Ingreso reciente' },
]

type AssignDialogState = {
  open: boolean
  lot: InventoryLotListItem | null
}

export default function LotsListPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const [filters, setFilters] = useState({
    q: '',
    status: '',
    productId: '',
    supplierId: '',
    locationId: '',
    dateFrom: '',
    dateTo: '',
    sort: SORT_OPTIONS[0].value,
    page: 0,
  })
  const [assign, setAssign] = useState<AssignDialogState>({ open: false, lot: null })
  const [selectedLocationId, setSelectedLocationId] = useState<string | null>(null)

  const lotsQueryKey = useMemo(
    () => [
      'inventory-lots',
      filters.q,
      filters.status,
      filters.productId,
      filters.supplierId,
      filters.locationId,
      filters.dateFrom,
      filters.dateTo,
      filters.sort,
      filters.page,
      PAGE_SIZE,
    ],
    [filters]
  )

  const lotsQuery = useQuery({
    queryKey: lotsQueryKey,
    queryFn: () =>
      getLots({
        q: filters.q || undefined,
        status: filters.status || undefined,
        productId: filters.productId || undefined,
        supplierId: filters.supplierId || undefined,
        locationId: filters.locationId || undefined,
        dateFrom: filters.dateFrom ? new Date(filters.dateFrom).toISOString() : undefined,
        dateTo: filters.dateTo ? new Date(filters.dateTo).toISOString() : undefined,
        sort: filters.sort,
        page: filters.page,
        size: PAGE_SIZE,
      }),
    keepPreviousData: true,
  })

  const productQuery = useQuery({
    queryKey: ['inventory-products'],
    queryFn: () => listProducts({ size: 200, status: 'all' }),
    staleTime: 60_000,
  })

  const supplierQuery = useQuery({
    queryKey: ['inventory-suppliers'],
    queryFn: () => listSuppliers(undefined, true),
    staleTime: 60_000,
  })

  const locationsQuery = useQuery({
    queryKey: ['assignable-locations'],
    queryFn: () =>
      getLocations({
        enabled: true,
        page: 0,
        size: 200,
        sort: 'name,asc',
      }),
    staleTime: 60_000,
  })

  const mutation = useMutation({
    mutationFn: (payload: { lotId: string; locationId: string }) =>
      assignLotLocation(payload.lotId, payload.locationId),
    onMutate: async variables => {
      await queryClient.cancelQueries({ queryKey: lotsQueryKey })
      const previous = queryClient.getQueryData<typeof lotsQuery['data']>(lotsQueryKey)
      if (previous) {
        const location = locationsQuery.data?.content.find(loc => loc.id === variables.locationId)
        const optimistic = {
          ...previous,
          content: previous.content.map(lot =>
            lot.lotId === variables.lotId
              ? {
                  ...lot,
                  location: location
                    ? { id: location.id, name: location.name }
                    : { id: variables.locationId, name: 'Ubicación seleccionada' },
                }
              : lot
          ),
        }
        queryClient.setQueryData(lotsQueryKey, optimistic)
      }
      return { previous }
    },
    onError: (_, __, context) => {
      if (context?.previous) {
        queryClient.setQueryData(lotsQueryKey, context.previous)
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: lotsQueryKey })
    },
  })

  const lotContent = lotsQuery.data?.content ?? []
  const pageCount = lotsQuery.data?.totalPages ?? 1

  const openAssignDialog = (lot: InventoryLotListItem) => {
    setAssign({ open: true, lot })
    setSelectedLocationId(lot.location?.id ?? null)
  }

  const closeAssignDialog = () => {
    setAssign({ open: false, lot: null })
    setSelectedLocationId(null)
  }

  const handleAssign = () => {
    if (!assign.lot || !selectedLocationId) {
      return
    }
    mutation.mutate({ lotId: assign.lot.lotId, locationId: selectedLocationId })
    closeAssignDialog()
  }

  const handleToggleFilter = (key: keyof typeof filters, value: string) => {
    setFilters(prev => ({
      ...prev,
      [key]: value,
      page: key === 'page' ? prev.page : 0,
    }))
  }

  const goPrevious = () => {
    setFilters(prev => ({ ...prev, page: Math.max(0, prev.page - 1) }))
  }

  const goNext = () => {
    setFilters(prev => ({
      ...prev,
      page: prev.page + 1 < pageCount ? prev.page + 1 : prev.page,
    }))
  }

  const productOptions = productQuery.data?.content ?? []
  const supplierOptions = supplierQuery.data ?? []
  const locationOptions = locationsQuery.data?.content ?? []
  const locationLookup = useMemo(
    () => new Map(locationOptions.map(location => [location.id, location] as const)),
    [locationOptions]
  )

  const formatLocationLabel = (lot: InventoryLotListItem) => {
    if (!lot.location?.id) {
      return 'DEFAULT (automático)'
    }
    const match = locationLookup.get(lot.location.id)
    if (match) {
      return match.code ? `${match.code} · ${match.name}` : match.name
    }
    return lot.location.name ?? 'Ubicación'
  }

  const handleQuickLocationFilter = (locationId: string) => {
    setFilters(prev => ({
      ...prev,
      locationId,
      page: 0,
    }))
  }

  return (
    <div className="card">
      <div className="card-header">
        <div>
          <h2>Lista de lotes</h2>
          <p className="muted small">
            Controla los lotes activos y responde rápidamente a alertas de stock o vencimientos.
          </p>
        </div>
      </div>
      <div className="card-body" style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        <div className="inline-actions" style={{ flexWrap: 'wrap', gap: '0.75rem' }}>
          <input
            className="input"
            placeholder="Buscar productos, proveedores o lotes..."
            value={filters.q}
            onChange={event => handleToggleFilter('q', event.target.value)}
          />
          <select
            className="input"
            value={filters.productId}
            onChange={event => handleToggleFilter('productId', event.target.value)}
          >
            <option value="">Todos los productos</option>
            {productOptions.map(product => (
              <option key={product.id} value={product.id}>
                {product.sku} · {product.name}
              </option>
            ))}
          </select>
          <select
            className="input"
            value={filters.supplierId}
            onChange={event => handleToggleFilter('supplierId', event.target.value)}
          >
            <option value="">Todos los proveedores</option>
            {supplierOptions.map(supplier => (
              <option key={supplier.id} value={supplier.id}>
                {supplier.name}
              </option>
            ))}
          </select>
          <select
            className="input"
            value={filters.locationId}
            onChange={event => handleToggleFilter('locationId', event.target.value)}
          >
            <option value="">Todas las ubicaciones</option>
            {locationOptions.map(location => (
              <option key={location.id} value={location.id}>
                {location.name}
              </option>
            ))}
          </select>
          <select
            className="input"
            value={filters.status}
            onChange={event => handleToggleFilter('status', event.target.value)}
          >
            {STATUS_OPTIONS.map(option => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>
        <div className="inline-actions" style={{ flexWrap: 'wrap', gap: '0.75rem' }}>
          <label className="small" htmlFor="lot-date-from">
            Desde
            <input
              id="lot-date-from"
              type="date"
              className="input"
              value={filters.dateFrom}
              onChange={event => handleToggleFilter('dateFrom', event.target.value)}
            />
          </label>
          <label className="small" htmlFor="lot-date-to">
            Hasta
            <input
              id="lot-date-to"
              type="date"
              className="input"
              value={filters.dateTo}
              onChange={event => handleToggleFilter('dateTo', event.target.value)}
            />
          </label>
          <select
            className="input"
            value={filters.sort}
            onChange={event => handleToggleFilter('sort', event.target.value)}
          >
            {SORT_OPTIONS.map(option => (
              <option key={option.value} value={option.value}>
                Ordenar por {option.label}
              </option>
            ))}
          </select>
        </div>
      </div>
      {lotsQuery.isLoading ? (
        <p className="muted">Cargando lotes…</p>
      ) : lotsQuery.isError ? (
        <p className="error">{(lotsQuery.error as Error)?.message ?? 'No se pudieron cargar lotes'}</p>
      ) : (
        <>
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th>Lote</th>
                  <th>Producto</th>
                  <th>Proveedor</th>
                  <th>Ubicación</th>
                  <th>Cant. disp.</th>
                  <th>Cant. reserv.</th>
                  <th>Estado</th>
                  <th>Fecha ingreso</th>
                  <th>Fecha expiración</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {lotContent.length === 0 ? (
                  <tr>
                    <td colSpan={10} className="muted">
                      No hay lotes registrados para los filtros actuales.
                    </td>
                  </tr>
                ) : (
                  lotContent.map(lot => (
                    <tr key={lot.lotId}>
                      <td className="mono">{lot.lotId}</td>
                      <td>
                        {lot.product.name}
                        <div className="muted small">{lot.product.sku}</div>
                      </td>
                      <td>{lot.supplier?.name ?? '—'}</td>
                      <td>
                        {lot.location?.id ? (
                          <button
                            type="button"
                            onClick={() => handleQuickLocationFilter(lot.location!.id)}
                            style={{
                              background: 'none',
                              border: 'none',
                              padding: 0,
                              color: '#0d6efd',
                              textDecoration: 'underline',
                              cursor: 'pointer',
                            }}
                          >
                            {formatLocationLabel(lot)}
                          </button>
                        ) : (
                          <span className="muted">DEFAULT (automático)</span>
                        )}
                      </td>
                      <td className="mono font-semibold">
                        {lot.qtyAvailable.toLocaleString('es-CL')}
                      </td>
                      <td className="mono">{lot.qtyReserved.toLocaleString('es-CL')}</td>
                      <td>
                        <StatusBadge status={lot.status} />
                      </td>
                      <td className="mono">
                        {new Date(lot.fechaIngreso).toLocaleDateString('es-CL')}
                      </td>
                      <td className="mono">
                        {lot.fechaExpiracion
                          ? new Date(lot.fechaExpiracion).toLocaleDateString('es-CL')
                          : '—'}
                      </td>
                      <td>
                        <div className="inline-actions" style={{ flexDirection: 'column', gap: '0.25rem' }}>
                          <button
                            className="btn ghost small"
                            type="button"
                            onClick={() => openAssignDialog(lot)}
                          >
                            Asignar ubicación
                          </button>
                          <button
                            className="btn ghost small"
                            type="button"
                            onClick={() =>
                              navigate(`/app/inventory/movements?lotId=${encodeURIComponent(lot.lotId)}`)
                            }
                          >
                            Ver movimientos
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
          <div
            className="inline-actions"
            style={{ justifyContent: 'space-between', marginTop: '0.75rem' }}
          >
            <div className="muted">
              Página {filters.page + 1} de {pageCount}
            </div>
            <div className="inline-actions">
              <button className="btn ghost small" onClick={goPrevious} disabled={filters.page === 0}>
                Anterior
              </button>
              <button
                className="btn ghost small"
                onClick={goNext}
                disabled={filters.page + 1 >= pageCount}
              >
                Siguiente
              </button>
            </div>
          </div>
        </>
      )}

      <Modal open={assign.open} onClose={closeAssignDialog} title="Asignar ubicación">
        {assign.lot ? (
          <div className="form-grid">
            <label>
              <span>Lote</span>
              <input className="input" value={assign.lot.lotId} disabled />
            </label>
            <label>
              <span>Ubicación</span>
              <select
                className="input"
                value={selectedLocationId ?? ''}
                onChange={event => setSelectedLocationId(event.target.value)}
                disabled={mutation.isPending}
              >
                <option value="">Selecciona una ubicación</option>
                {locationOptions.map(location => (
                  <option key={location.id} value={location.id}>
                    {location.name}
                  </option>
                ))}
              </select>
            </label>
            {mutation.isError ? (
              <p className="error">{(mutation.error as Error)?.message ?? 'No se pudo asignar'}</p>
            ) : null}
            <div className="modal-actions" style={{ gridColumn: '1 / -1' }}>
              <button
                className="btn ghost"
                type="button"
                onClick={closeAssignDialog}
                disabled={mutation.isPending}
              >
                Cancelar
              </button>
              <button
                className="btn"
                type="button"
                onClick={handleAssign}
                disabled={mutation.isPending || !selectedLocationId}
              >
                {mutation.isPending ? 'Guardando…' : 'Asignar'}
              </button>
            </div>
          </div>
        ) : (
          <p className="muted">Selecciona un lote primero.</p>
        )}
      </Modal>
    </div>
  )
}

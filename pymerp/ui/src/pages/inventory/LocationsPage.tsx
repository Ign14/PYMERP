import { useMemo, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import LocationForm from './components/LocationForm'
import {
  getLocations,
  InventoryLocation,
  toggleLocationEnabled,
} from '../../services/inventory'

type StatusFilter = 'enabled' | 'disabled' | 'all'

const SORT_OPTIONS = [
  { value: 'name,asc', label: 'Nombre (A → Z)' },
  { value: 'name,desc', label: 'Nombre (Z → A)' },
  { value: 'code,asc', label: 'Código (A → Z)' },
  { value: 'createdAt,desc', label: 'Fecha (recientes)' },
]

const PAGE_SIZE = 8

export default function LocationsPage() {
  const queryClient = useQueryClient()
  const [filters, setFilters] = useState({
    q: '',
    page: 0,
    sort: SORT_OPTIONS[0].value,
    statusFilter: 'enabled' as StatusFilter,
  })
  const [inputValue, setInputValue] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [editingLocation, setEditingLocation] = useState<InventoryLocation | null>(null)

  const enabledParam =
    filters.statusFilter === 'enabled'
      ? true
      : filters.statusFilter === 'disabled'
      ? false
      : undefined

  const locationsQuery = useQuery({
    queryKey: [
      'inventory-locations',
      filters.q,
      enabledParam,
      filters.sort,
      filters.page,
      PAGE_SIZE,
    ],
    queryFn: () =>
      getLocations({
        q: filters.q || undefined,
        enabled: enabledParam,
        sort: filters.sort,
        page: filters.page,
        size: PAGE_SIZE,
      }),
    keepPreviousData: true,
  })

  const handleSearch = () => {
    setFilters(prev => ({ ...prev, q: inputValue.trim(), page: 0 }))
  }

  const handleSortChange = (value: string) => {
    setFilters(prev => ({ ...prev, sort: value, page: 0 }))
  }

  const handleStatusChange = (value: StatusFilter) => {
    setFilters(prev => ({ ...prev, statusFilter: value, page: 0 }))
  }

  const goToPrevious = () => {
    setFilters(prev => ({ ...prev, page: Math.max(0, prev.page - 1) }))
  }

  const goToNext = () => {
    if (!locationsQuery.data) return
    setFilters(prev => ({
      ...prev,
      page: prev.page + 1 < (locationsQuery.data?.totalPages ?? 1) ? prev.page + 1 : prev.page,
    }))
  }

  const locationList = locationsQuery.data?.content ?? []
  const totalPages = locationsQuery.data?.totalPages ?? 0

  const existingCodes = useMemo(
    () => locationList.map(item => item.code ?? ''),
    [locationList]
  )
  const existingNames = useMemo(
    () => locationList.map(item => item.name ?? ''),
    [locationList]
  )

  const handleOpenCreate = () => {
    setEditingLocation(null)
    setFormOpen(true)
  }

  const handleEdit = (location: InventoryLocation) => {
    setEditingLocation(location)
    setFormOpen(true)
  }

  const handleCloseForm = () => {
    setFormOpen(false)
    setEditingLocation(null)
  }

  const handleSaved = () => {
    queryClient.invalidateQueries({ queryKey: ['inventory-locations'] })
    handleCloseForm()
  }

  const handleToggleStatus = async (location: InventoryLocation) => {
    await toggleLocationEnabled(location.id, !location.enabled)
    queryClient.invalidateQueries({ queryKey: ['inventory-locations'] })
  }

  return (
    <div className="card">
      <div className="card-header">
        <div>
          <h2>Ubicaciones</h2>
          <p className="muted small">
            Gestiona centros logísticos y controla su estado en tiempo real.
          </p>
        </div>
        <div className="inline-actions">
          <button className="btn" onClick={handleOpenCreate}>
            + Nueva ubicación
          </button>
        </div>
      </div>
      <div className="inline-actions" style={{ flexWrap: 'wrap', gap: '0.5rem' }}>
        <input
          className="input"
          placeholder="Buscar por nombre o código"
          value={inputValue}
          onChange={event => setInputValue(event.target.value)}
          onKeyDown={event => {
            if (event.key === 'Enter') {
              handleSearch()
            }
          }}
        />
        <button className="btn ghost" type="button" onClick={handleSearch}>
          Buscar
        </button>
        <select
          className="input"
          value={filters.sort}
          onChange={event => handleSortChange(event.target.value)}
        >
          {SORT_OPTIONS.map(option => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <div className="inline-actions">
          {(['enabled', 'disabled', 'all'] as StatusFilter[]).map(value => (
            <button
              key={value}
              className={`btn ghost${filters.statusFilter === value ? ' active' : ''}`}
              type="button"
              onClick={() => handleStatusChange(value)}
            >
              {value === 'enabled'
                ? 'Activas'
                : value === 'disabled'
                ? 'Deshabilitadas'
                : 'Todas'}
            </button>
          ))}
        </div>
      </div>
      {locationsQuery.isLoading ? (
        <p className="muted">Cargando ubicaciones…</p>
      ) : locationsQuery.isError ? (
        <p className="error">
          {(locationsQuery.error as Error)?.message ?? 'No se pudieron cargar las ubicaciones'}
        </p>
      ) : (
        <>
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th>Código</th>
                  <th>Nombre</th>
                  <th>Descripción</th>
                  <th>Estado</th>
                  <th>Creada</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {locationList.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="muted">
                      No hay ubicaciones que coincidan con los filtros
                    </td>
                  </tr>
                ) : (
                  locationList.map(location => (
                    <tr key={location.id}>
                      <td className="mono">{location.code || '—'}</td>
                      <td>{location.name}</td>
                      <td>{location.description || '—'}</td>
                      <td>
                        <span className={`badge ${location.enabled ? 'badge-success' : 'badge-warning'}`}>
                          {location.enabled ? 'Activa' : 'Deshabilitada'}
                        </span>
                      </td>
                      <td>{new Date(location.createdAt).toLocaleDateString('es-CL')}</td>
                      <td>
                        <div className="table-actions">
                          <button
                            className="btn ghost small"
                            type="button"
                            onClick={() => handleEdit(location)}
                          >
                            Editar
                          </button>
                          <button
                            className="btn ghost small"
                            type="button"
                            onClick={() => handleToggleStatus(location)}
                          >
                            {location.enabled ? 'Deshabilitar' : 'Habilitar'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
          <div className="inline-actions" style={{ justifyContent: 'space-between', marginTop: '0.75rem' }}>
            <div className="muted small">
              Página {filters.page + 1} de {totalPages || 1}
            </div>
            <div className="inline-actions">
              <button className="btn ghost small" onClick={goToPrevious} disabled={filters.page === 0}>
                Anterior
              </button>
              <button
                className="btn ghost small"
                onClick={goToNext}
                disabled={filters.page + 1 >= totalPages}
              >
                Siguiente
              </button>
            </div>
          </div>
        </>
      )}
      <LocationForm
        open={formOpen}
        location={editingLocation}
        onClose={handleCloseForm}
        onSaved={handleSaved}
        existingCodes={existingCodes}
        existingNames={existingNames}
      />
    </div>
  )
}

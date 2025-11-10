import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import {
  createLocation,
  deleteLocation,
  getLocationStockSummary,
  listLocations,
  getCompanyParentLocations,
  Location,
  LocationPayload,
  LocationStockSummary,
  LocationType,
  ParentLocationResponse,
} from '../services/client'
import { useAuth } from '../context/AuthContext'

export default function LocationsCard() {
  const { session } = useAuth()
  const [selectedLocation, setSelectedLocation] = useState<Location | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingLocation, setEditingLocation] = useState<Location | null>(null)
  const [formData, setFormData] = useState<LocationPayload>({
    code: '',
    name: '',
    description: '',
    type: 'WAREHOUSE' as LocationType,
    parentLocationId: undefined,
    active: true,
    isBlocked: false,
    capacity: undefined,
    capacityUnit: 'UNITS',
  })

  const queryClient = useQueryClient()

  const locationsQuery = useQuery({
    queryKey: ['locations'],
    queryFn: () => listLocations(),
  })

  const parentLocationsQuery = useQuery({
    queryKey: ['parent-locations', session?.companyId],
    queryFn: () => getCompanyParentLocations(session?.companyId ?? ''),
    enabled: !!session?.companyId,
  })

  const stockSummaryQuery = useQuery<LocationStockSummary[]>({
    queryKey: ['location-stock-summary'],
    queryFn: getLocationStockSummary,
  })

  const createMutation = useMutation({
    mutationFn: (payload: LocationPayload) => createLocation(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['locations'] })
      queryClient.invalidateQueries({ queryKey: ['location-stock-summary'] })
      setDialogOpen(false)
      resetForm()
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteLocation(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['locations'] })
      queryClient.invalidateQueries({ queryKey: ['location-stock-summary'] })
      setSelectedLocation(null)
    },
  })

  const resetForm = () => {
    setFormData({
      code: '',
      name: '',
      description: '',
      type: 'WAREHOUSE' as LocationType,
      parentLocationId: undefined,
      active: true,
      isBlocked: false,
      capacity: undefined,
      capacityUnit: 'UNITS',
    })
    setEditingLocation(null)
  }

  const handleOpenDialog = (location?: Location) => {
    if (location) {
      setEditingLocation(location)
      setFormData({
        code: location.code,
        name: location.name,
        description: location.description,
        type: location.type,
        parentLocationId: location.parentLocationId,
        active: location.active,
        isBlocked: location.isBlocked,
        capacity: location.capacity,
        capacityUnit: location.capacityUnit || 'UNITS',
      })
    } else {
      resetForm()
    }
    setDialogOpen(true)
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!formData.code || !formData.name) {
      alert('Código y nombre son requeridos')
      return
    }
    createMutation.mutate(formData)
  }

  const handleDelete = () => {
    if (!selectedLocation) return
    if (window.confirm(`¿Eliminar ubicación ${selectedLocation.name}?`)) {
      deleteMutation.mutate(selectedLocation.id)
    }
  }

  const locations = locationsQuery.data ?? []
  const stockSummary = stockSummaryQuery.data ?? []

  // Agrupar ubicaciones por tipo
  const locationsByType = locations.reduce(
    (acc, loc) => {
      if (!acc[loc.type]) acc[loc.type] = []
      acc[loc.type].push(loc)
      return acc
    },
    {} as Record<LocationType, Location[]>
  )

  // Obtener stock de la ubicación seleccionada
  const selectedLocationStock = selectedLocation
    ? stockSummary.find(s => s.locationId === selectedLocation.id)
    : null

  return (
    <div className="card">
      <div className="card-header">
        <h2>Ubicaciones</h2>
        <p className="muted small">Gestiona las ubicaciones físicas del inventario</p>
      </div>

      <div className="inline-actions">
        <button className="btn primary" type="button" onClick={() => handleOpenDialog()}>
          + Nueva Ubicación
        </button>
        <button
          className="btn ghost"
          type="button"
          onClick={() => queryClient.invalidateQueries({ queryKey: ['locations'] })}
          disabled={locationsQuery.isFetching}
        >
          {locationsQuery.isFetching ? 'Cargando...' : 'Refrescar'}
        </button>
      </div>

      {locationsQuery.isLoading && <p>Cargando ubicaciones...</p>}
      {locationsQuery.isError && <p className="error">{locationsQuery.error.message}</p>}

      {!locationsQuery.isLoading && !locationsQuery.isError && (
        <>
          <div className="locations-grid">
            {/* Columna izquierda: Lista de ubicaciones por tipo */}
            <div className="locations-list">
              {Object.entries(locationsByType).map(([type, locs]) => (
                <div key={type} className="location-type-section">
                  <h3 className="location-type-title">{type}</h3>
                  <div className="location-items">
                    {locs.map(location => {
                      const stock = stockSummary.find(s => s.locationId === location.id)
                      const totalProducts = stock?.products.length ?? 0
                      const isSelected = selectedLocation?.id === location.id

                      return (
                        <div
                          key={location.id}
                          className={`location-item${isSelected ? ' selected' : ''}`}
                          onClick={() => setSelectedLocation(location)}
                          style={{
                            opacity: location.active ? 1 : 0.6,
                            border: location.isBlocked ? '2px solid var(--warning)' : undefined
                          }}
                        >
                          <div className="location-item-header">
                            <span className="location-code mono">{location.code}</span>
                            <div style={{ display: 'flex', gap: '0.25rem', alignItems: 'center' }}>
                              {!location.active && (
                                <span className="location-badge" style={{ background: 'var(--error)', color: 'white', fontSize: '0.75rem' }}>
                                  Inactiva
                                </span>
                              )}
                              {location.isBlocked && (
                                <span className="location-badge" style={{ background: 'var(--warning)', color: 'white', fontSize: '0.75rem' }}>
                                  🔒 Bloqueada
                                </span>
                              )}
                              {totalProducts > 0 && (
                                <span className="location-badge">{totalProducts} productos</span>
                              )}
                            </div>
                          </div>
                          <div className="location-name">{location.name}</div>
                          {location.parentLocationId && (
                            <div className="location-parent muted small">
                              📍 Ubicación padre configurada
                            </div>
                          )}
                          {location.capacity && (
                            <div className="location-parent muted small">
                              📦 Capacidad: {location.capacity} {location.capacityUnit || 'unidades'}
                            </div>
                          )}
                          {location.description && (
                            <div className="location-description muted small">
                              {location.description}
                            </div>
                          )}
                        </div>
                      )
                    })}
                  </div>
                </div>
              ))}
            </div>

            {/* Columna derecha: Detalles de la ubicación seleccionada */}
            <div className="location-details">
              {selectedLocation ? (
                <>
                  <div className="panel">
                    <h3 className="panel-title">Detalles de Ubicación</h3>
                    <div className="detail-row">
                      <span className="muted">Código:</span>
                      <span className="mono">{selectedLocation.code}</span>
                    </div>
                    <div className="detail-row">
                      <span className="muted">Nombre:</span>
                      <span>{selectedLocation.name}</span>
                    </div>
                    <div className="detail-row">
                      <span className="muted">Tipo:</span>
                      <span>{selectedLocation.type}</span>
                    </div>
                    <div className="detail-row">
                      <span className="muted">Estado:</span>
                      <span>
                        {selectedLocation.active ? (
                          <span style={{ color: 'var(--success-color)' }}>✓ Activa</span>
                        ) : (
                          <span style={{ color: 'var(--error-color)' }}>✗ Inactiva</span>
                        )}
                        {selectedLocation.isBlocked && (
                          <span style={{ color: 'var(--warning)', marginLeft: '0.5rem' }}>🔒 Bloqueada</span>
                        )}
                      </span>
                    </div>
                    {selectedLocation.capacity && (
                      <div className="detail-row">
                        <span className="muted">Capacidad:</span>
                        <span className="mono">
                          {selectedLocation.capacity} {selectedLocation.capacityUnit || 'unidades'}
                        </span>
                      </div>
                    )}
                    {selectedLocation.parentLocationId && (
                      <div className="detail-row">
                        <span className="muted">Ubicación Padre:</span>
                        <span>
                          {(() => {
                            const parent =
                              parentLocationsQuery.data?.find(
                                p => p.id === selectedLocation.parentLocationId
                              ) ||
                              locations.find(l => l.id === selectedLocation.parentLocationId)
                            return parent ? (
                              <span className="mono" style={{ color: 'var(--accent)' }}>
                                📍 {parent.code} - {parent.name}
                              </span>
                            ) : (
                              <span className="muted">ID: {selectedLocation.parentLocationId}</span>
                            )
                          })()}
                        </span>
                      </div>
                    )}
                    {selectedLocation.description && (
                      <div className="detail-row">
                        <span className="muted">Descripción:</span>
                        <span>{selectedLocation.description}</span>
                      </div>
                    )}
                    <div className="inline-actions" style={{ marginTop: '1rem' }}>
                      <button
                        className="btn ghost"
                        type="button"
                        onClick={() => handleOpenDialog(selectedLocation)}
                      >
                        Editar
                      </button>
                      <button
                        className="btn ghost"
                        type="button"
                        onClick={handleDelete}
                        disabled={deleteMutation.isPending}
                      >
                        {deleteMutation.isPending ? 'Eliminando...' : 'Eliminar'}
                      </button>
                    </div>
                  </div>

                  <div className="panel">
                    <h3 className="panel-title">Productos en esta Ubicación</h3>
                    {stockSummaryQuery.isLoading && <p className="muted">Cargando stock...</p>}
                    {selectedLocationStock && selectedLocationStock.products.length > 0 ? (
                      <div className="table-wrapper compact">
                        <table className="table">
                          <thead>
                            <tr>
                              <th>SKU</th>
                              <th>Producto</th>
                              <th>Cantidad</th>
                              <th>Lotes</th>
                            </tr>
                          </thead>
                          <tbody>
                            {selectedLocationStock.products.map(product => (
                              <tr key={product.productId}>
                                <td className="mono small">{product.productSku}</td>
                                <td>{product.productName}</td>
                                <td className="mono">{product.totalQuantity.toFixed(2)}</td>
                                <td className="mono">{product.lotCount}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    ) : (
                      <p className="muted small">No hay productos en esta ubicación</p>
                    )}
                  </div>
                </>
              ) : (
                <div className="empty-state">
                  <p className="muted">Selecciona una ubicación para ver detalles</p>
                </div>
              )}
            </div>
          </div>
        </>
      )}

      {/* Dialog de crear/editar ubicación */}
      {dialogOpen && (
        <div className="modal-overlay" onClick={() => setDialogOpen(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editingLocation ? 'Editar Ubicación' : 'Nueva Ubicación'}</h2>
              <button className="btn-close" onClick={() => setDialogOpen(false)}>
                ×
              </button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Código *</label>
                <input
                  className="input"
                  type="text"
                  required
                  value={formData.code}
                  onChange={e => setFormData({ ...formData, code: e.target.value })}
                  placeholder="Ej: ALM-01"
                />
              </div>
              <div className="form-group">
                <label>Nombre *</label>
                <input
                  className="input"
                  type="text"
                  required
                  value={formData.name}
                  onChange={e => setFormData({ ...formData, name: e.target.value })}
                  placeholder="Ej: Almacén Principal"
                />
              </div>
              <div className="form-group">
                <label>Tipo</label>
                <select
                  className="input"
                  value={formData.type}
                  onChange={e => setFormData({ ...formData, type: e.target.value as LocationType })}
                >
                  <option value="WAREHOUSE">WAREHOUSE</option>
                  <option value="SHELF">SHELF</option>
                  <option value="BIN">BIN</option>
                </select>
              </div>
              <div className="form-group">
                <label>Ubicación Padre (opcional)</label>
                <select
                  className="input"
                  value={formData.parentLocationId ?? ''}
                  onChange={e =>
                    setFormData({
                      ...formData,
                      parentLocationId: e.target.value || undefined,
                    })
                  }
                >
                  <option value="">-- Sin ubicación padre --</option>
                  {parentLocationsQuery.data && parentLocationsQuery.data.length > 0 && (
                    <optgroup label="Ubicaciones Padre de la Empresa">
                      {parentLocationsQuery.data.map(loc => (
                        <option key={loc.id} value={loc.id}>
                          {loc.code} - {loc.name}
                        </option>
                      ))}
                    </optgroup>
                  )}
                  {locations.length > 0 && (
                    <optgroup label="Otras Ubicaciones">
                      {locations.map(loc => (
                        <option key={loc.id} value={loc.id}>
                          {loc.code} - {loc.name} ({loc.type})
                        </option>
                      ))}
                    </optgroup>
                  )}
                </select>
              </div>
              <div className="form-group">
                <label>Descripción (opcional)</label>
                <textarea
                  className="input"
                  rows={3}
                  value={formData.description ?? ''}
                  onChange={e => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Descripción de la ubicación"
                />
              </div>
              <div className="form-group" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <label style={{ gridColumn: '1 / -1', marginBottom: '-0.5rem' }}>Estado</label>
                <div>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={formData.active ?? true}
                      onChange={e => setFormData({ ...formData, active: e.target.checked })}
                    />
                    <span>Activa</span>
                  </label>
                </div>
                <div>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={formData.isBlocked ?? false}
                      onChange={e => setFormData({ ...formData, isBlocked: e.target.checked })}
                    />
                    <span>Bloqueada</span>
                  </label>
                </div>
              </div>
              <div className="form-group" style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '1rem' }}>
                <div>
                  <label>Capacidad máxima (opcional)</label>
                  <input
                    className="input"
                    type="number"
                    min="0"
                    step="0.001"
                    value={formData.capacity || ''}
                    onChange={e => setFormData({ ...formData, capacity: e.target.value ? Number(e.target.value) : undefined })}
                    placeholder="Ej: 1000"
                  />
                </div>
                <div>
                  <label>Unidad</label>
                  <select
                    className="input"
                    value={formData.capacityUnit || 'UNITS'}
                    onChange={e => setFormData({ ...formData, capacityUnit: e.target.value })}
                  >
                    <option value="UNITS">Unidades</option>
                    <option value="PALLETS">Pallets</option>
                    <option value="CUBIC_METERS">m³</option>
                    <option value="SQUARE_METERS">m²</option>
                    <option value="KILOGRAMS">Kg</option>
                  </select>
                </div>
              </div>
              <div className="modal-actions">
                <button
                  className="btn ghost"
                  type="button"
                  onClick={() => setDialogOpen(false)}
                  disabled={createMutation.isPending}
                >
                  Cancelar
                </button>
                <button className="btn primary" type="submit" disabled={createMutation.isPending}>
                  {createMutation.isPending
                    ? 'Guardando...'
                    : editingLocation
                      ? 'Actualizar'
                      : 'Crear'}
                </button>
              </div>
              {createMutation.isError && (
                <p className="error">
                  {(createMutation.error as Error)?.message ?? 'Error al guardar'}
                </p>
              )}
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

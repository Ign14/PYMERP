import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Location,
  LocationStatus,
  deleteLocation,
  listLocations,
} from '../../services/client'
import LocationFormDialog from './LocationFormDialog'

const TYPE_LABELS: Record<Location['type'], string> = {
  BODEGA: 'Bodega',
  CONTAINER: 'Container',
  LOCAL: 'Local',
  CAMION: 'Camión',
  CAMIONETA: 'Camioneta',
}

const STATUS_STYLES: Record<LocationStatus, { label: string; className: string }> = {
  ACTIVE: { label: 'Activa', className: 'badge-success' },
  BLOCKED: { label: 'Bloqueada', className: 'badge-warning' },
}

export default function LocationList() {
  const queryClient = useQueryClient()
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingLocation, setEditingLocation] = useState<Location | null>(null)

  const locationsQuery = useQuery({
    queryKey: ['locations'],
    queryFn: () => listLocations(),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteLocation(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['locations'] })
    },
  })

  const locations = locationsQuery.data ?? []
  const existingCodes = useMemo(
    () =>
      locations
        .filter(item => (editingLocation ? item.id !== editingLocation.id : true))
        .map(item => item.code.trim().toLowerCase()),
    [locations, editingLocation]
  )

  const handleOpenCreate = () => {
    setEditingLocation(null)
    setDialogOpen(true)
  }

  const handleEdit = (location: Location) => {
    setEditingLocation(location)
    setDialogOpen(true)
  }

  const handleClose = () => {
    setDialogOpen(false)
    setEditingLocation(null)
  }

  const handleSaved = () => {
    queryClient.invalidateQueries({ queryKey: ['locations'] })
    handleClose()
  }

  const handleDelete = (location: Location) => {
    if (
      window.confirm(`¿Seguro que deseas eliminar la ubicación ${location.code} - ${location.name}?`)
    ) {
      deleteMutation.mutate(location.id)
    }
  }

  return (
    <div className="card">
      <div className="card-header">
        <div>
          <h2>Ubicaciones</h2>
          <p className="muted small">Administra bodegas, locales y puntos logísticos.</p>
        </div>
        <div className="inline-actions">
          <button className="btn" type="button" onClick={handleOpenCreate}>
            + Nueva Ubicación
          </button>
          <button
            className="btn ghost"
            type="button"
            onClick={() => queryClient.invalidateQueries({ queryKey: ['locations'] })}
            disabled={locationsQuery.isFetching}
          >
            {locationsQuery.isFetching ? 'Actualizando...' : 'Refrescar'}
          </button>
        </div>
      </div>

      {locationsQuery.isLoading && <p className="muted">Cargando ubicaciones...</p>}
      {locationsQuery.isError && (
        <p className="error">
          {(locationsQuery.error as Error)?.message ?? 'No se pudieron cargar las ubicaciones'}
        </p>
      )}

      {!locationsQuery.isLoading && !locationsQuery.isError && (
        <>
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th>Código</th>
                  <th>Nombre</th>
                  <th>Tipo</th>
                  <th>Razón social</th>
                  <th>Estado</th>
                  <th style={{ width: '150px' }}>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {locations.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="muted">
                      No hay ubicaciones registradas
                    </td>
                  </tr>
                ) : (
                  locations.map(location => {
                    const statusStyle = STATUS_STYLES[location.status] ?? STATUS_STYLES.ACTIVE
                    return (
                      <tr key={location.id}>
                        <td className="mono">{location.code}</td>
                        <td>{location.name}</td>
                        <td>{TYPE_LABELS[location.type] ?? location.type}</td>
                        <td>{location.businessName || '—'}</td>
                        <td>
                          <span className={`badge ${statusStyle.className}`}>{statusStyle.label}</span>
                        </td>
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
                              onClick={() => handleDelete(location)}
                              disabled={deleteMutation.isPending}
                            >
                              {deleteMutation.isPending ? 'Eliminando...' : 'Eliminar'}
                            </button>
                          </div>
                        </td>
                      </tr>
                    )
                  })
                )}
              </tbody>
            </table>
          </div>
        </>
      )}

      <LocationFormDialog
        open={dialogOpen}
        location={editingLocation}
        onClose={handleClose}
        onSaved={handleSaved}
        existingCodes={existingCodes}
      />
    </div>
  )
}

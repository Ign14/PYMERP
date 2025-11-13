import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  ServiceDTO,
  ServiceStatus,
  deleteService,
  listServices,
} from '../../services/client'
import ServiceFormDialog from './ServiceFormDialog'

const STATUS_FILTERS: { value: 'ALL' | ServiceStatus; label: string }[] = [
  { value: 'ALL', label: 'Todos' },
  { value: 'ACTIVE', label: 'Activos' },
  { value: 'INACTIVE', label: 'Inactivos' },
]

const STATUS_BADGES: Record<ServiceStatus, { label: string; className: string }> = {
  ACTIVE: { label: 'Activo', className: 'badge-success' },
  INACTIVE: { label: 'Inactivo', className: 'badge-muted' },
}

const currencyFormatter = new Intl.NumberFormat('es-CL', {
  style: 'currency',
  currency: 'CLP',
  minimumFractionDigits: 0,
})

export default function ServiceList() {
  const queryClient = useQueryClient()
  const [filter, setFilter] = useState<'ALL' | ServiceStatus>('ACTIVE')
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingService, setEditingService] = useState<ServiceDTO | null>(null)

  const servicesQuery = useQuery({
    queryKey: ['services', { status: filter }],
    queryFn: () => listServices(filter === 'ALL' ? undefined : { status: filter }),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteService(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['services'] })
    },
  })

  const services = servicesQuery.data ?? []
  const existingCodes = useMemo(
    () =>
      services
        .filter(item => (editingService ? item.id !== editingService.id : true))
        .map(item => item.code.trim().toLowerCase()),
    [services, editingService]
  )

  const handleOpenCreate = () => {
    setEditingService(null)
    setDialogOpen(true)
  }

  const handleEdit = (service: ServiceDTO) => {
    setEditingService(service)
    setDialogOpen(true)
  }

  const handleClose = () => {
    setDialogOpen(false)
    setEditingService(null)
  }

  const handleSaved = () => {
    queryClient.invalidateQueries({ queryKey: ['services'] })
    handleClose()
  }

  const handleDelete = (service: ServiceDTO) => {
    if (window.confirm(`¿Eliminar servicio ${service.code} - ${service.name}?`)) {
      deleteMutation.mutate(service.id)
    }
  }

  return (
    <div className="card">
      <div className="card-header">
        <div>
          <h2>Servicios</h2>
          <p className="muted small">Registra servicios para órdenes de compra sin inventario.</p>
        </div>
        <div className="inline-actions">
          <select
            className="input"
            value={filter}
            onChange={event => setFilter(event.target.value as 'ALL' | ServiceStatus)}
          >
            {STATUS_FILTERS.map(option => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          <button className="btn" type="button" onClick={handleOpenCreate}>
            + Nuevo Servicio
          </button>
          <button
            className="btn ghost"
            type="button"
            onClick={() => queryClient.invalidateQueries({ queryKey: ['services'] })}
            disabled={servicesQuery.isFetching}
          >
            {servicesQuery.isFetching ? 'Actualizando...' : 'Refrescar'}
          </button>
        </div>
      </div>

      {servicesQuery.isLoading && <p className="muted">Cargando servicios...</p>}
      {servicesQuery.isError && (
        <p className="error">
          {(servicesQuery.error as Error)?.message ?? 'No se pudieron obtener los servicios'}
        </p>
      )}

      {!servicesQuery.isLoading && !servicesQuery.isError && (
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>Código</th>
                <th>Nombre</th>
                <th>Categoría</th>
                <th>Precio</th>
                <th>Estado</th>
                <th style={{ width: '150px' }}>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {services.length === 0 ? (
                <tr>
                  <td colSpan={6} className="muted">
                    No hay servicios registrados
                  </td>
                </tr>
              ) : (
                services.map(service => {
                  const badge = STATUS_BADGES[service.status] ?? STATUS_BADGES.ACTIVE
                  return (
                    <tr key={service.id}>
                      <td className="mono">{service.code}</td>
                      <td>{service.name}</td>
                      <td>{service.category || '—'}</td>
                      <td>{currencyFormatter.format(service.unitPrice ?? 0)}</td>
                      <td>
                        <span className={`badge ${badge.className}`}>{badge.label}</span>
                      </td>
                      <td>
                        <div className="table-actions">
                          <button
                            className="btn ghost small"
                            type="button"
                            onClick={() => handleEdit(service)}
                          >
                            Editar
                          </button>
                          <button
                            className="btn ghost small"
                            type="button"
                            onClick={() => handleDelete(service)}
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
      )}

      <ServiceFormDialog
        open={dialogOpen}
        service={editingService}
        onClose={handleClose}
        onSaved={handleSaved}
        existingCodes={existingCodes}
      />
    </div>
  )
}

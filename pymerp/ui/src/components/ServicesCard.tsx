import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import {
  createService,
  deleteService,
  listServices,
  ServiceDTO,
  ServicePayload,
  updateService,
} from '../services/client'

export default function ServicesCard() {
  const [selectedService, setSelectedService] = useState<ServiceDTO | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingService, setEditingService] = useState<ServiceDTO | null>(null)
  const [statusFilter, setStatusFilter] = useState<'active' | 'inactive' | 'all'>('active')
  const [formData, setFormData] = useState<ServicePayload>({
    code: '',
    name: '',
    description: '',
    active: true,
  })

  const queryClient = useQueryClient()

  const servicesQuery = useQuery({
    queryKey: ['services', { status: statusFilter }],
    queryFn: () => {
      if (statusFilter === 'all') {
        return listServices()
      }
      return listServices(statusFilter === 'active')
    },
  })

  const createMutation = useMutation({
    mutationFn: (payload: ServicePayload) => createService(payload),
    onSuccess: newService => {
      queryClient.invalidateQueries({ queryKey: ['services'] })
      setDialogOpen(false)
      setSelectedService(newService)
      resetForm()
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: ServicePayload }) =>
      updateService(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['services'] })
      setDialogOpen(false)
      resetForm()
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteService(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['services'] })
      setSelectedService(null)
    },
  })

  const resetForm = () => {
    setFormData({
      code: '',
      name: '',
      description: '',
      active: true,
    })
    setEditingService(null)
  }

  const handleOpenDialog = (service?: ServiceDTO) => {
    if (service) {
      setEditingService(service)
      setFormData({
        code: service.code,
        name: service.name,
        description: service.description,
        active: service.active,
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

    if (editingService) {
      updateMutation.mutate({ id: editingService.id, payload: formData })
    } else {
      createMutation.mutate(formData)
    }
  }

  const handleDelete = () => {
    if (!selectedService) return
    if (window.confirm(`¿Eliminar servicio ${selectedService.name}?`)) {
      deleteMutation.mutate(selectedService.id)
    }
  }

  const services = servicesQuery.data ?? []

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return 'Nunca'
    return new Date(dateStr).toLocaleDateString('es-CL', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    })
  }

  return (
    <div className="card">
      <div className="card-header">
        <div>
          <h2>Servicios</h2>
          <p className="muted small">Gestiona servicios que no requieren inventario</p>
        </div>
      </div>

      <div className="inline-actions">
        <select
          className="input"
          value={statusFilter}
          onChange={e => setStatusFilter(e.target.value as 'active' | 'inactive' | 'all')}
        >
          <option value="active">Activos</option>
          <option value="inactive">Inactivos</option>
          <option value="all">Todos</option>
        </select>
        <button className="btn primary" type="button" onClick={() => handleOpenDialog()}>
          + Nuevo Servicio
        </button>
        <button
          className="btn ghost"
          type="button"
          onClick={() => queryClient.invalidateQueries({ queryKey: ['services'] })}
          disabled={servicesQuery.isFetching}
        >
          {servicesQuery.isFetching ? 'Cargando...' : 'Refrescar'}
        </button>
      </div>

      {servicesQuery.isLoading && <p>Cargando servicios...</p>}
      {servicesQuery.isError && <p className="error">{servicesQuery.error.message}</p>}

      {!servicesQuery.isLoading && !servicesQuery.isError && (
        <>
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th>Código</th>
                  <th>Nombre</th>
                  <th>Última Compra</th>
                  <th>Estado</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {services.map(service => {
                  const isSelected = selectedService?.id === service.id
                  return (
                    <tr
                      key={service.id}
                      className={isSelected ? 'selected' : ''}
                      onClick={() => setSelectedService(service)}
                    >
                      <td className="mono">{service.code}</td>
                      <td>{service.name}</td>
                      <td className="mono small">{formatDate(service.lastPurchaseDate)}</td>
                      <td>
                        <span
                          className={`badge ${service.active ? 'badge-success' : 'badge-muted'}`}
                        >
                          {service.active ? 'Activo' : 'Inactivo'}
                        </span>
                      </td>
                      <td>
                        <button
                          className="btn ghost small"
                          type="button"
                          onClick={e => {
                            e.stopPropagation()
                            handleOpenDialog(service)
                          }}
                        >
                          Editar
                        </button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          {services.length === 0 && (
            <div className="empty-state">
              <p className="muted">No hay servicios registrados</p>
            </div>
          )}

          {selectedService && (
            <div className="panel">
              <h3 className="panel-title">Detalles del Servicio</h3>
              <div className="detail-row">
                <span className="muted">Código:</span>
                <span className="mono">{selectedService.code}</span>
              </div>
              <div className="detail-row">
                <span className="muted">Nombre:</span>
                <span>{selectedService.name}</span>
              </div>
              {selectedService.description && (
                <div className="detail-row">
                  <span className="muted">Descripción:</span>
                  <span>{selectedService.description}</span>
                </div>
              )}
              <div className="detail-row">
                <span className="muted">Última compra:</span>
                <span>{formatDate(selectedService.lastPurchaseDate)}</span>
              </div>
              <div className="detail-row">
                <span className="muted">Estado:</span>
                <span>{selectedService.active ? 'Activo' : 'Inactivo'}</span>
              </div>
              <div className="inline-actions" style={{ marginTop: '1rem' }}>
                <button
                  className="btn ghost"
                  type="button"
                  onClick={() => handleOpenDialog(selectedService)}
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
          )}
        </>
      )}

      {/* Dialog de crear/editar servicio */}
      {dialogOpen && (
        <div className="modal-overlay" onClick={() => setDialogOpen(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editingService ? 'Editar Servicio' : 'Nuevo Servicio'}</h2>
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
                  placeholder="Ej: SRV-001"
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
                  placeholder="Ej: Consultoría de TI"
                />
              </div>
              <div className="form-group">
                <label>Descripción (opcional)</label>
                <textarea
                  className="input"
                  rows={3}
                  value={formData.description ?? ''}
                  onChange={e => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Descripción del servicio"
                />
              </div>
              <div className="form-group">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={formData.active ?? true}
                    onChange={e => setFormData({ ...formData, active: e.target.checked })}
                  />
                  Activo
                </label>
              </div>
              <div className="modal-actions">
                <button
                  className="btn ghost"
                  type="button"
                  onClick={() => setDialogOpen(false)}
                  disabled={createMutation.isPending || updateMutation.isPending}
                >
                  Cancelar
                </button>
                <button
                  className="btn primary"
                  type="submit"
                  disabled={createMutation.isPending || updateMutation.isPending}
                >
                  {createMutation.isPending || updateMutation.isPending
                    ? 'Guardando...'
                    : editingService
                      ? 'Actualizar'
                      : 'Crear'}
                </button>
              </div>
              {(createMutation.isError || updateMutation.isError) && (
                <p className="error">
                  {((createMutation.error || updateMutation.error) as Error)?.message ??
                    'Error al guardar'}
                </p>
              )}
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  approveAccountRequest,
  listAccountRequests,
  rejectAccountRequest,
  type AccountRequestAdmin,
} from '../../services/client'

type FilterMode = 'PENDING' | 'ALL'

export function AccountRequestManagement() {
  const [filter, setFilter] = useState<FilterMode>('PENDING')
  const [selectedRequest, setSelectedRequest] = useState<AccountRequestAdmin | null>(null)
  const [rejectReason, setRejectReason] = useState('')
  const queryClient = useQueryClient()

  const requestsQuery = useQuery({
    queryKey: ['account-requests', filter],
    queryFn: async () => {
      if (filter === 'PENDING') {
        return listAccountRequests({ status: 'PENDING' })
      }
      return listAccountRequests({ days: 30 })
    },
    staleTime: 60_000,
  })

  const approveMutation = useMutation({
    mutationFn: (id: string) => approveAccountRequest(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['account-requests'] })
      setSelectedRequest(null)
    },
  })

  const rejectMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => rejectAccountRequest(id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['account-requests'] })
      setSelectedRequest(null)
      setRejectReason('')
    },
  })

  const requests = requestsQuery.data ?? []

  const openRejectModal = (request: AccountRequestAdmin) => {
    setSelectedRequest(request)
    setRejectReason('')
  }

  return (
    <div className="card mt-6">
      <div className="card-header">
        <div>
          <h2>Monitoreo de solicitudes de cuenta</h2>
          <p className="muted">
            Aprobación o rechazo de registros enviados desde el landing público.
          </p>
        </div>
        <div className="card-actions">
          <button
            className={filter === 'PENDING' ? 'btn-primary' : 'btn-secondary'}
            onClick={() => setFilter('PENDING')}
          >
            Pendientes
          </button>
          <button
            className={filter === 'ALL' ? 'btn-primary' : 'btn-secondary'}
            onClick={() => setFilter('ALL')}
          >
            Últimos 30 días
          </button>
          <button
            className="btn-link"
            onClick={() => requestsQuery.refetch()}
            disabled={requestsQuery.isFetching}
          >
            {requestsQuery.isFetching ? 'Actualizando...' : 'Actualizar'}
          </button>
        </div>
      </div>
      <div className="card-body">
        {requestsQuery.isLoading && <p>Cargando solicitudes...</p>}
        {requestsQuery.isError && (
          <p className="error">No fue posible cargar las solicitudes. Intenta nuevamente.</p>
        )}
        {!requestsQuery.isLoading && !requestsQuery.isError && requests.length === 0 && (
          <p className="muted">No hay solicitudes para el filtro seleccionado.</p>
        )}
        {requests.length > 0 && (
          <div className="table-responsive">
            <table className="table">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Usuario</th>
                  <th>Email</th>
                  <th>Empresa</th>
                  <th>Estado</th>
                  <th>IP / User Agent</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {requests.map(request => (
                  <tr key={request.id} className={request.status === 'PENDING' ? 'bg-yellow-50' : ''}>
                    <td>{new Date(request.createdAt).toLocaleString()}</td>
                    <td>
                      <div className="text-sm font-medium">{request.fullName}</div>
                      <div className="text-xs muted">{request.rut}</div>
                    </td>
                    <td>{request.email}</td>
                    <td>{request.companyName}</td>
                    <td>
                      <span
                        className={`badge ${
                          request.status === 'APPROVED'
                            ? 'badge-success'
                            : request.status === 'REJECTED'
                              ? 'badge-error'
                              : 'badge-warning'
                        }`}
                      >
                        {request.status}
                      </span>
                      {request.processedByUsername && (
                        <div className="text-xs muted">Por {request.processedByUsername}</div>
                      )}
                      {request.rejectionReason && request.status === 'REJECTED' && (
                        <div className="text-xs text-red-600">Razón: {request.rejectionReason}</div>
                      )}
                    </td>
                    <td>
                      <div className="text-xs">{request.ipAddress ?? '-'}</div>
                      <div className="text-xs muted">
                        {request.userAgent?.slice(0, 60) ?? '-'}
                        {request.userAgent && request.userAgent.length > 60 ? '…' : ''}
                      </div>
                    </td>
                    <td>
                      {request.status === 'PENDING' ? (
                        <div className="flex gap-2">
                          <button
                            className="btn-success"
                            onClick={() => approveMutation.mutate(request.id)}
                            disabled={approveMutation.isPending}
                          >
                            Aprobar
                          </button>
                          <button className="btn-danger" onClick={() => openRejectModal(request)}>
                            Rechazar
                          </button>
                        </div>
                      ) : (
                        <span className="text-xs muted">
                          Procesado el{' '}
                          {request.processedAt
                            ? new Date(request.processedAt).toLocaleString()
                            : 'N/D'}
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {selectedRequest && (
        <div className="modal" role="dialog" aria-modal="true">
          <div className="modal-content">
            <h3>Rechazar solicitud</h3>
            <p className="muted">
              Indica el motivo del rechazo para {selectedRequest.fullName} ({selectedRequest.email}
              ).
            </p>
            <textarea
              value={rejectReason}
              onChange={event => setRejectReason(event.target.value)}
              rows={4}
              className="w-full border p-2 mt-2"
              placeholder="Motivo del rechazo..."
            />
            <div className="flex gap-2 mt-4 justify-end">
              <button
                className="btn-secondary"
                type="button"
                onClick={() => {
                  setSelectedRequest(null)
                  setRejectReason('')
                }}
              >
                Cancelar
              </button>
              <button
                className="btn-danger"
                type="button"
                disabled={!rejectReason.trim() || rejectMutation.isPending}
                onClick={() =>
                  rejectMutation.mutate({ id: selectedRequest.id, reason: rejectReason.trim() })
                }
              >
                Confirmar rechazo
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

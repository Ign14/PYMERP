import { useEffect, useState } from 'react'
import {
  getAccountsReceivable,
  type AccountReceivable,
  type PaginatedResponse,
} from '../../services/client'

type StatusFilter = 'ALL' | 'PENDING' | 'OVERDUE' | 'DUE_SOON'

export default function AccountsReceivablePanel() {
  const [data, setData] = useState<PaginatedResponse<AccountReceivable> | null>(null)
  const [loading, setLoading] = useState(true)
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL')
  const [page, setPage] = useState(0)
  const pageSize = 10

  useEffect(() => {
    loadData()
  }, [statusFilter, page])

  const loadData = async () => {
    try {
      setLoading(true)
      const result = await getAccountsReceivable({
        status: statusFilter === 'ALL' ? undefined : statusFilter,
        page,
        size: pageSize,
      })
      setData(result)
    } catch (err) {
      console.error('Error loading receivables:', err)
    } finally {
      setLoading(false)
    }
  }

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP',
      minimumFractionDigits: 0,
    }).format(value)
  }

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('es-CL')
  }

  const getStatusBadge = (status: string) => {
    const styles = {
      OVERDUE: 'bg-red-950 text-red-400 border border-red-800',
      DUE_SOON: 'bg-yellow-950 text-yellow-400 border border-yellow-800',
      PENDING: 'bg-blue-950 text-blue-400 border border-blue-800',
    }
    const labels = {
      OVERDUE: 'Vencida',
      DUE_SOON: 'Por vencer',
      PENDING: 'Pendiente',
    }
    return (
      <span
        className={`px-2 py-1 rounded-md text-xs font-medium ${styles[status as keyof typeof styles]}`}
      >
        {labels[status as keyof typeof labels]}
      </span>
    )
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg">
      <div className="p-5 border-b border-neutral-800">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-neutral-100">Cuentas por Cobrar</h2>
          <div className="flex gap-2">
            {(['ALL', 'PENDING', 'DUE_SOON', 'OVERDUE'] as StatusFilter[]).map(status => (
              <button
                key={status}
                onClick={() => {
                  setStatusFilter(status)
                  setPage(0)
                }}
                className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${
                  statusFilter === status
                    ? 'bg-blue-600 text-white'
                    : 'bg-neutral-800 text-neutral-300 hover:bg-neutral-700'
                }`}
              >
                {status === 'ALL'
                  ? 'Todas'
                  : status === 'PENDING'
                    ? 'Pendientes'
                    : status === 'DUE_SOON'
                      ? 'Por vencer'
                      : 'Vencidas'}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full">
          <thead className="bg-neutral-950">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium text-neutral-400 uppercase">
                Cliente
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-neutral-400 uppercase">
                Documento
              </th>
              <th className="px-4 py-3 text-right text-xs font-medium text-neutral-400 uppercase">
                Monto
              </th>
              <th className="px-4 py-3 text-right text-xs font-medium text-neutral-400 uppercase">
                Saldo
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-neutral-400 uppercase">
                Emisión
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-neutral-400 uppercase">
                Vencimiento
              </th>
              <th className="px-4 py-3 text-center text-xs font-medium text-neutral-400 uppercase">
                Días
              </th>
              <th className="px-4 py-3 text-center text-xs font-medium text-neutral-400 uppercase">
                Estado
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-neutral-800">
            {loading ? (
              <tr>
                <td colSpan={8} className="px-4 py-8 text-center text-neutral-400">
                  <div className="flex items-center justify-center">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                    <span className="ml-3">Cargando...</span>
                  </div>
                </td>
              </tr>
            ) : !data || data.content.length === 0 ? (
              <tr>
                <td colSpan={8} className="px-4 py-8 text-center text-neutral-400">
                  No hay cuentas por cobrar
                </td>
              </tr>
            ) : (
              data.content.map(item => (
                <tr key={item.id} className="hover:bg-neutral-800">
                  <td className="px-4 py-3 text-sm text-neutral-100">{item.customerName}</td>
                  <td className="px-4 py-3 text-sm text-neutral-400">
                    {item.docType} {item.docNumber.substring(0, 8)}
                  </td>
                  <td className="px-4 py-3 text-sm text-neutral-100 text-right font-medium">
                    {formatCurrency(item.total)}
                  </td>
                  <td className="px-4 py-3 text-sm text-neutral-100 text-right font-medium">
                    {formatCurrency(item.balance)}
                  </td>
                  <td className="px-4 py-3 text-sm text-neutral-400">
                    {formatDate(item.issuedAt)}
                  </td>
                  <td className="px-4 py-3 text-sm text-neutral-400">{formatDate(item.dueDate)}</td>
                  <td className="px-4 py-3 text-sm text-center">
                    {item.daysOverdue > 0 ? (
                      <span className="text-red-400 font-semibold">+{item.daysOverdue}</span>
                    ) : (
                      <span className="text-neutral-500">0</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-center">{getStatusBadge(item.paymentStatus)}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalPages > 1 && (
        <div className="px-5 py-4 border-t border-neutral-800 flex items-center justify-between">
          <div className="text-sm text-neutral-400">
            Mostrando {page * pageSize + 1} - {Math.min((page + 1) * pageSize, data.totalElements)}{' '}
            de {data.totalElements}
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => setPage(p => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-3 py-1.5 rounded-md bg-neutral-800 border border-neutral-700 text-sm font-medium text-neutral-300 hover:bg-neutral-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Anterior
            </button>
            <span className="px-3 py-1.5 text-sm text-neutral-300">
              Página {page + 1} de {data.totalPages}
            </span>
            <button
              onClick={() => setPage(p => Math.min(data.totalPages - 1, p + 1))}
              disabled={page >= data.totalPages - 1}
              className="px-3 py-1.5 rounded-md bg-neutral-800 border border-neutral-700 text-sm font-medium text-neutral-300 hover:bg-neutral-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Siguiente
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

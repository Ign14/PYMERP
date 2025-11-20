import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { listCustomers, getCustomerStats } from '../../services/client'

type CustomerStat = {
  customerId: string
  customerName: string
  totalRevenue: number
  purchaseCount: number
  avgTicket: number
  lastPurchaseDate: string
}

type SalesTopCustomersPanelProps = {
  startDate?: string
  endDate?: string
}

function formatCurrency(value: number): string {
  return `$${Math.round(value).toLocaleString('es-CL')}`
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr)
  return date.toLocaleDateString('es-CL', { day: 'numeric', month: 'short' })
}

export default function SalesTopCustomersPanel({
  startDate,
  endDate,
}: SalesTopCustomersPanelProps) {
  // Obtener todos los clientes activos
  const customersQuery = useQuery({
    queryKey: ['customers', 'all', 'active'],
    queryFn: () => listCustomers({ active: true, page: 0, size: 1000 }),
  })

  // Obtener estadísticas para cada cliente
  const customerStatsQueries = useQuery({
    queryKey: ['customers', 'top-stats', customersQuery.data?.content?.map(c => c.id)],
    queryFn: async () => {
      if (!customersQuery.data?.content) return []
      
      const statsPromises = customersQuery.data.content.map(async (customer) => {
        try {
          const stats = await getCustomerStats(customer.id)
          return {
            customerId: customer.id,
            customerName: customer.name || customer.businessName || 'Sin nombre',
            totalRevenue: Number(stats.totalRevenue) || 0,
            purchaseCount: stats.totalSales || 0,
            avgTicket: stats.totalSales > 0 ? (Number(stats.totalRevenue) || 0) / stats.totalSales : 0,
            lastPurchaseDate: stats.lastSaleDate || '',
          }
        } catch (err) {
          console.error(`Error loading stats for customer ${customer.id}:`, err)
          return null
        }
      })
      
      const results = await Promise.all(statsPromises)
      return results.filter((r): r is CustomerStat => r !== null && r.totalRevenue > 0)
    },
    enabled: !!customersQuery.data?.content?.length,
  })

  const topCustomers = useMemo(() => {
    if (!customerStatsQueries.data) return []
    return customerStatsQueries.data
      .sort((a, b) => b.totalRevenue - a.totalRevenue)
      .slice(0, 10)
  }, [customerStatsQueries.data])

  const maxRevenue = topCustomers.length > 0 ? topCustomers[0].totalRevenue : 1

  if (customersQuery.isLoading || customerStatsQueries.isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5 mb-6">
        <header className="mb-4">
          <h2 className="text-xl font-semibold text-neutral-100 mb-1">👥 Top 10 Clientes</h2>
          <p className="text-sm text-neutral-400">Cargando datos...</p>
        </header>
        <div className="space-y-3">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="h-16 bg-neutral-800 rounded animate-pulse" />
          ))}
        </div>
      </div>
    )
  }

  if (customersQuery.isError || customerStatsQueries.isError) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5 mb-6">
        <header className="mb-4">
          <h2 className="text-xl font-semibold text-neutral-100 mb-1">👥 Top 10 Clientes</h2>
          <p className="text-sm text-red-400">Error al cargar datos de clientes</p>
        </header>
      </div>
    )
  }

  if (topCustomers.length === 0) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5 mb-6">
        <header className="mb-4">
          <h2 className="text-xl font-semibold text-neutral-100 mb-1">👥 Top 10 Clientes</h2>
          <p className="text-sm text-neutral-400">No hay datos de ventas disponibles</p>
        </header>
      </div>
    )
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5 mb-6">
      <header className="mb-4">
        <h2 className="text-xl font-semibold text-neutral-100 mb-1">👥 Top 10 Clientes</h2>
        <p className="text-sm text-neutral-400">Clientes con mayor volumen de compras</p>
      </header>

      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="border-b border-neutral-700">
              <th className="text-left py-3 px-2 text-xs font-semibold text-neutral-400 uppercase">
                #
              </th>
              <th className="text-left py-3 px-2 text-xs font-semibold text-neutral-400 uppercase">
                Cliente
              </th>
              <th className="text-right py-3 px-2 text-xs font-semibold text-neutral-400 uppercase">
                Ingresos
              </th>
              <th className="text-center py-3 px-2 text-xs font-semibold text-neutral-400 uppercase">
                Compras
              </th>
              <th className="text-right py-3 px-2 text-xs font-semibold text-neutral-400 uppercase">
                Ticket Prom.
              </th>
              <th className="text-center py-3 px-2 text-xs font-semibold text-neutral-400 uppercase">
                Última Compra
              </th>
            </tr>
          </thead>
          <tbody>
            {topCustomers.map((customer, index) => {
              const percentage = (customer.totalRevenue / maxRevenue) * 100
              const isRecent =
                new Date(customer.lastPurchaseDate) > new Date(Date.now() - 3 * 24 * 60 * 60 * 1000)

              return (
                <tr
                  key={customer.customerId}
                  className="border-b border-neutral-800 hover:bg-neutral-800 transition-colors"
                >
                  <td className="py-4 px-2">
                    <div className="flex items-center gap-2">
                      <span className="inline-flex items-center justify-center w-7 h-7 rounded-full bg-neutral-700 text-neutral-300 text-xs font-bold">
                        {index + 1}
                      </span>
                      {index < 3 && (
                        <span className="text-lg">
                          {index === 0 ? '🥇' : index === 1 ? '🥈' : '🥉'}
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="py-4 px-2">
                    <div>
                      <p className="text-neutral-100 font-medium">{customer.customerName}</p>
                      <div className="relative w-full h-1.5 bg-neutral-700 rounded-full overflow-hidden mt-1">
                        <div
                          className="absolute top-0 left-0 h-full bg-gradient-to-r from-purple-500 to-pink-500 rounded-full"
                          style={{ width: `${percentage}%` }}
                        ></div>
                      </div>
                    </div>
                  </td>
                  <td className="py-4 px-2 text-right">
                    <p className="text-neutral-100 font-bold">
                      {formatCurrency(customer.totalRevenue)}
                    </p>
                  </td>
                  <td className="py-4 px-2 text-center">
                    <span className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs font-medium bg-neutral-800 text-neutral-300 border border-neutral-700">
                      {customer.purchaseCount}
                    </span>
                  </td>
                  <td className="py-4 px-2 text-right">
                    <p className="text-neutral-300">{formatCurrency(customer.avgTicket)}</p>
                  </td>
                  <td className="py-4 px-2 text-center">
                    <span
                      className={`inline-flex items-center gap-1 px-2 py-1 rounded text-xs font-medium ${
                        isRecent
                          ? 'bg-green-950 text-green-400 border border-green-800'
                          : 'bg-neutral-800 text-neutral-400 border border-neutral-700'
                      }`}
                    >
                      {isRecent && '🟢 '}
                      {formatDate(customer.lastPurchaseDate)}
                    </span>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}

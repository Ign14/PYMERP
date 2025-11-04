import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { CustomerSaleHistoryItem, getCustomerSaleHistory } from '../../services/client'

type Props = {
  customerId: string
  customerName: string
}

type TimelineEvent = {
  id: string
  type: 'sale' | 'registration'
  date: Date
  title: string
  description: string
  amount?: number
  icon: string
  color: string
}

export default function CustomersActivityTimeline({ customerId, customerName }: Props) {
  const [filterPeriod, setFilterPeriod] = useState<number>(365) // días

  const salesQuery = useQuery({
    queryKey: ['customers', customerId, 'sales-history'],
    queryFn: () => getCustomerSaleHistory(customerId, 0, 100),
    staleTime: 30_000,
  })

  // Convertir ventas en eventos de timeline
  const timelineEvents = useMemo<TimelineEvent[]>(() => {
    const events: TimelineEvent[] = []

    // Agregar ventas
    if (salesQuery.data?.content) {
      salesQuery.data.content.forEach(sale => {
        const saleDate = new Date(sale.saleDate)
        const now = new Date()
        const daysDiff = Math.floor((now.getTime() - saleDate.getTime()) / (1000 * 60 * 60 * 24))

        if (daysDiff <= filterPeriod) {
          events.push({
            id: `sale-${sale.saleId}`,
            type: 'sale',
            date: saleDate,
            title: `Venta ${sale.docType || ''} ${sale.docNumber || ''}`,
            description: `${sale.itemCount || 0} productos • Total: $${(sale.total || 0).toLocaleString('es-CL')}`,
            amount: sale.total,
            icon: '💰',
            color: 'bg-green-950 border-green-800 text-green-400',
          })
        }
      })
    }

    // Ordenar por fecha descendente (más reciente primero)
    return events.sort((a, b) => b.date.getTime() - a.date.getTime())
  }, [salesQuery.data, filterPeriod])

  // Agrupar eventos por mes
  const groupedEvents = useMemo(() => {
    const groups: Record<string, TimelineEvent[]> = {}

    timelineEvents.forEach(event => {
      const monthKey = event.date.toLocaleDateString('es-CL', { year: 'numeric', month: 'long' })
      if (!groups[monthKey]) {
        groups[monthKey] = []
      }
      groups[monthKey].push(event)
    })

    return groups
  }, [timelineEvents])

  const formatRelativeDate = (date: Date) => {
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))

    if (diffDays === 0) return 'Hoy'
    if (diffDays === 1) return 'Ayer'
    if (diffDays < 7) return `Hace ${diffDays} días`
    if (diffDays < 30) return `Hace ${Math.floor(diffDays / 7)} semanas`
    if (diffDays < 365) return `Hace ${Math.floor(diffDays / 30)} meses`
    return `Hace ${Math.floor(diffDays / 365)} años`
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-5">
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-xl font-semibold text-neutral-100">
          📅 Timeline de Actividad - {customerName}
        </h3>
        <select
          className="input bg-neutral-800 border-neutral-700 text-neutral-100 text-sm"
          value={filterPeriod}
          onChange={e => setFilterPeriod(parseInt(e.target.value))}
        >
          <option value={30}>Últimos 30 días</option>
          <option value={90}>Últimos 3 meses</option>
          <option value={180}>Últimos 6 meses</option>
          <option value={365}>Último año</option>
          <option value={730}>Últimos 2 años</option>
          <option value={9999}>Todo el historial</option>
        </select>
      </div>

      {salesQuery.isLoading && (
        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-8 text-center">
          <p className="text-neutral-400">Cargando actividad...</p>
        </div>
      )}

      {salesQuery.isError && (
        <div className="bg-red-950 border border-red-800 rounded-lg p-4">
          <p className="text-red-400">Error al cargar el historial de actividad</p>
        </div>
      )}

      {salesQuery.data && timelineEvents.length === 0 && (
        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-8 text-center">
          <p className="text-neutral-400">No hay actividad en el período seleccionado</p>
        </div>
      )}

      {salesQuery.data && timelineEvents.length > 0 && (
        <div className="space-y-6 max-h-[600px] overflow-y-auto pr-2">
          {Object.entries(groupedEvents).map(([month, events]) => (
            <div key={month}>
              <div className="sticky top-0 bg-neutral-900 border-b border-neutral-700 pb-2 mb-4 z-10">
                <h4 className="text-sm font-semibold text-neutral-300 uppercase tracking-wide">
                  {month}
                </h4>
              </div>

              <div className="space-y-3 relative before:absolute before:left-[19px] before:top-2 before:bottom-2 before:w-[2px] before:bg-neutral-700">
                {events.map(event => (
                  <div key={event.id} className="relative pl-12">
                    {/* Icono del evento */}
                    <div
                      className={`absolute left-0 top-1 w-10 h-10 rounded-full border-2 flex items-center justify-center text-lg ${event.color}`}
                    >
                      {event.icon}
                    </div>

                    {/* Contenido del evento */}
                    <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4 hover:bg-neutral-750 transition-colors">
                      <div className="flex justify-between items-start mb-2">
                        <div>
                          <h5 className="font-semibold text-neutral-100">{event.title}</h5>
                          <p className="text-sm text-neutral-400">{event.description}</p>
                        </div>
                        {event.amount && (
                          <span className="text-lg font-bold text-green-400">
                            ${event.amount.toLocaleString('es-CL')}
                          </span>
                        )}
                      </div>
                      <div className="flex gap-3 text-xs text-neutral-500">
                        <span>📆 {event.date.toLocaleDateString('es-CL')}</span>
                        <span>
                          ⏰{' '}
                          {event.date.toLocaleTimeString('es-CL', {
                            hour: '2-digit',
                            minute: '2-digit',
                          })}
                        </span>
                        <span className="text-neutral-400">{formatRelativeDate(event.date)}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Resumen de actividad */}
      {salesQuery.data && timelineEvents.length > 0 && (
        <div className="mt-4 pt-4 border-t border-neutral-700">
          <div className="grid grid-cols-3 gap-4 text-center">
            <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-3">
              <p className="text-2xl font-bold text-neutral-100">{timelineEvents.length}</p>
              <p className="text-xs text-neutral-400">Eventos totales</p>
            </div>
            <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-3">
              <p className="text-2xl font-bold text-green-400">
                {timelineEvents.filter(e => e.type === 'sale').length}
              </p>
              <p className="text-xs text-neutral-400">Ventas registradas</p>
            </div>
            <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-3">
              <p className="text-2xl font-bold text-blue-400">
                $
                {timelineEvents
                  .filter(e => e.amount)
                  .reduce((sum, e) => sum + (e.amount || 0), 0)
                  .toLocaleString('es-CL')}
              </p>
              <p className="text-xs text-neutral-400">Total del período</p>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

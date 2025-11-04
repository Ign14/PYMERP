import { useQuery } from '@tanstack/react-query'
import { listPurchases, listSuppliers, Supplier } from '../../services/client'

interface PurchasesAlertsPanelProps {
  startDate?: string
  endDate?: string
}

interface Alert {
  id: string
  type: 'price-anomaly' | 'inactive-supplier' | 'low-stock'
  severity: 'high' | 'medium' | 'low'
  title: string
  description: string
  icon: string
}

export function PurchasesAlertsPanel({ startDate, endDate }: PurchasesAlertsPanelProps) {
  // Obtener compras del período actual
  const { data: currentPurchases } = useQuery({
    queryKey: ['purchases', 'current', startDate, endDate],
    queryFn: () =>
      listPurchases({
        from: startDate ? new Date(startDate).toISOString() : undefined,
        to: endDate ? new Date(endDate).toISOString() : undefined,
        size: 10000,
      }),
  })

  // Obtener compras del período anterior (mismo rango de días)
  const { data: previousPurchases } = useQuery({
    queryKey: ['purchases', 'previous', startDate, endDate],
    queryFn: async () => {
      if (!startDate || !endDate) return null

      const start = new Date(startDate)
      const end = new Date(endDate)
      const diffDays = Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24))

      const prevEnd = new Date(start)
      prevEnd.setDate(prevEnd.getDate() - 1)
      const prevStart = new Date(prevEnd)
      prevStart.setDate(prevStart.getDate() - diffDays)

      return listPurchases({
        from: prevStart.toISOString(),
        to: prevEnd.toISOString(),
        size: 10000,
      })
    },
    enabled: !!startDate && !!endDate,
  })

  // Obtener todos los proveedores
  const { data: suppliersData } = useQuery({
    queryKey: ['suppliers'],
    queryFn: () => listSuppliers(),
  })

  const alerts: Alert[] = []

  if (currentPurchases?.content && previousPurchases?.content) {
    // 1. Detectar anomalías de precios (>20% variación)
    const currentBySupplier = new Map<string, { total: number; count: number; name: string }>()
    const previousBySupplier = new Map<string, { total: number; count: number }>()

    currentPurchases.content.forEach(p => {
      if (!p.supplierId) return
      const current = currentBySupplier.get(p.supplierId) || {
        total: 0,
        count: 0,
        name: p.supplierName || '',
      }
      current.total += p.total
      current.count += 1
      currentBySupplier.set(p.supplierId, current)
    })

    previousPurchases.content.forEach(p => {
      if (!p.supplierId) return
      const previous = previousBySupplier.get(p.supplierId) || { total: 0, count: 0 }
      previous.total += p.total
      previous.count += 1
      previousBySupplier.set(p.supplierId, previous)
    })

    currentBySupplier.forEach((current, supplierId) => {
      const previous = previousBySupplier.get(supplierId)
      if (previous && previous.count > 0) {
        const currentAvg = current.total / current.count
        const previousAvg = previous.total / previous.count
        const change = ((currentAvg - previousAvg) / previousAvg) * 100

        if (Math.abs(change) > 20) {
          alerts.push({
            id: `price-anomaly-${supplierId}`,
            type: 'price-anomaly',
            severity: Math.abs(change) > 50 ? 'high' : 'medium',
            title: 'Anomalía en precios detectada',
            description: `${current.name}: ${change > 0 ? 'Aumento' : 'Disminución'} del ${Math.abs(change).toFixed(1)}% en promedio de compra`,
            icon: '📊',
          })
        }
      }
    })
  }

  // 2. Detectar proveedores inactivos (sin compras en el período)
  if (suppliersData && currentPurchases?.content) {
    const activeSupplierIds = new Set(
      currentPurchases.content.map(p => p.supplierId).filter(Boolean)
    )
    const thirtyDaysAgo = new Date()
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30)

    suppliersData.forEach((supplier: Supplier) => {
      if (!activeSupplierIds.has(supplier.id.toString())) {
        alerts.push({
          id: `inactive-${supplier.id}`,
          type: 'inactive-supplier',
          severity: 'low',
          title: 'Proveedor sin actividad reciente',
          description: `${supplier.name} no tiene compras en el período seleccionado`,
          icon: '🕒',
        })
      }
    })
  }

  // 3. Alertas de stock bajo (basado en patrones de compra)
  if (currentPurchases?.content) {
    const purchaseFrequency = new Map<string, { count: number; name: string }>()

    currentPurchases.content.forEach(purchase => {
      if (!purchase.supplierId) return
      const current = purchaseFrequency.get(purchase.supplierId) || {
        count: 0,
        name: purchase.supplierName || '',
      }
      current.count += 1
      purchaseFrequency.set(purchase.supplierId, current)
    })

    // Si un proveedor tiene alta frecuencia (>5 compras) podría indicar stock bajo
    purchaseFrequency.forEach((data, supplierId) => {
      if (data.count > 5) {
        alerts.push({
          id: `low-stock-${supplierId}`,
          type: 'low-stock',
          severity: 'medium',
          title: 'Posible stock bajo detectado',
          description: `Alta frecuencia de compras a ${data.name} (${data.count} órdenes). Podría indicar reabastecimiento frecuente`,
          icon: '⚠️',
        })
      }
    })
  }

  // Ordenar por severidad
  const severityOrder = { high: 0, medium: 1, low: 2 }
  alerts.sort((a, b) => severityOrder[a.severity] - severityOrder[b.severity])

  // Limitar a las 5 alertas más importantes
  const topAlerts = alerts.slice(0, 5)

  if (topAlerts.length === 0) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-6">
        <h3 className="text-lg font-semibold mb-4 text-neutral-100">Alertas Inteligentes</h3>
        <div className="text-center py-8 text-neutral-400">
          <div className="text-6xl mb-2">✓</div>
          <p>No se detectaron alertas. Todo está funcionando correctamente.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-6">
      <h3 className="text-lg font-semibold mb-4 flex items-center gap-2 text-neutral-100">
        <span className="text-2xl">🔔</span>
        Alertas Inteligentes ({topAlerts.length})
      </h3>
      <div className="space-y-3">
        {topAlerts.map(alert => {
          const severityColors = {
            high: 'bg-red-950 border-red-800 text-red-400',
            medium: 'bg-yellow-950 border-yellow-800 text-yellow-400',
            low: 'bg-blue-950 border-blue-800 text-blue-400',
          }

          return (
            <div
              key={alert.id}
              className={`border rounded-lg p-3 ${severityColors[alert.severity]}`}
            >
              <div className="flex items-start gap-3">
                <span className="text-2xl flex-shrink-0">{alert.icon}</span>
                <div className="flex-1 min-w-0">
                  <h4 className="font-semibold text-sm">{alert.title}</h4>
                  <p className="text-sm mt-1 opacity-90">{alert.description}</p>
                </div>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

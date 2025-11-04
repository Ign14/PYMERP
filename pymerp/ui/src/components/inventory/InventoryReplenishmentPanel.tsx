import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { listProducts } from '../../services/client'

export default function InventoryReplenishmentPanel() {
  const productsQuery = useQuery({
    queryKey: ['products-replenishment'],
    queryFn: () => listProducts({ size: 100, status: 'all' }),
  })

  const products = productsQuery.data?.content ?? []

  const replenishmentNeeds = useMemo(() => {
    return products
      .map(p => {
        const stock = Number(p.stock ?? 0)
        // Simular consumo diario y punto de reorden
        const dailyConsumption = Math.random() * 5 + 1
        const leadTimeDays = 7 // Tiempo de entrega simulado
        const safetyStock = dailyConsumption * 3
        const reorderPoint = dailyConsumption * leadTimeDays + safetyStock
        const optimalQty = dailyConsumption * 30 // Stock para 30 días

        const needsReplenishment = stock < reorderPoint
        const daysOfCoverage = dailyConsumption > 0 ? stock / dailyConsumption : 999
        const suggestedOrder = Math.max(0, optimalQty - stock)

        return {
          id: p.id,
          name: p.name ?? 'Sin nombre',
          stock,
          reorderPoint,
          daysOfCoverage,
          suggestedOrder,
          needsReplenishment,
          urgency: daysOfCoverage < 7 ? 'critical' : daysOfCoverage < 14 ? 'high' : 'normal',
        }
      })
      .filter(p => p.needsReplenishment)
      .sort((a, b) => a.daysOfCoverage - b.daysOfCoverage)
      .slice(0, 8)
  }, [products])

  if (productsQuery.isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
        <h3 className="text-neutral-100 mb-4">Panel de Reabastecimiento</h3>
        <div className="animate-pulse bg-neutral-800 rounded-lg h-80"></div>
      </div>
    )
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-neutral-100">Panel de Reabastecimiento</h3>
        <span className="px-3 py-1 bg-orange-950 text-orange-400 border border-orange-800 rounded-full text-sm">
          {replenishmentNeeds.length} productos requieren pedido
        </span>
      </div>

      {replenishmentNeeds.length === 0 ? (
        <div className="bg-green-950 border border-green-800 rounded-lg p-4 text-center">
          <p className="text-green-400">✅ Todos los productos tienen stock suficiente</p>
        </div>
      ) : (
        <div className="space-y-3">
          {replenishmentNeeds.map(item => {
            const urgencyConfig = {
              critical: {
                bg: 'bg-red-950',
                border: 'border-red-800',
                text: 'text-red-400',
                icon: '🔴',
              },
              high: {
                bg: 'bg-yellow-950',
                border: 'border-yellow-800',
                text: 'text-yellow-400',
                icon: '⚠️',
              },
              normal: {
                bg: 'bg-blue-950',
                border: 'border-blue-800',
                text: 'text-blue-400',
                icon: 'ℹ️',
              },
            }[item.urgency] || {
              bg: 'bg-neutral-950',
              border: 'border-neutral-800',
              text: 'text-neutral-400',
              icon: 'ℹ️',
            }

            return (
              <div
                key={item.id}
                className={`${urgencyConfig.bg} border ${urgencyConfig.border} rounded-lg p-4`}
              >
                <div className="flex items-start justify-between mb-2">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-lg">{urgencyConfig.icon}</span>
                      <h4 className="text-neutral-100 font-medium">{item.name}</h4>
                    </div>
                    <p className="text-neutral-400 text-sm">
                      Stock actual:{' '}
                      <strong className={urgencyConfig.text}>{item.stock} unidades</strong>
                    </p>
                    <p className="text-neutral-400 text-sm">
                      Cobertura: <strong>{item.daysOfCoverage.toFixed(0)} días</strong>
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-neutral-400 text-xs mb-1">Cantidad sugerida</p>
                    <p className={`${urgencyConfig.text} text-2xl font-bold`}>
                      {Math.ceil(item.suggestedOrder)}
                    </p>
                  </div>
                </div>
                <button
                  className={`w-full mt-2 px-4 py-2 ${urgencyConfig.bg} ${urgencyConfig.text} border ${urgencyConfig.border} rounded-lg hover:bg-opacity-80 transition`}
                >
                  Generar Orden de Compra
                </button>
              </div>
            )
          })}
        </div>
      )}

      <div className="mt-4 bg-blue-950 border border-blue-800 rounded-lg p-3 text-blue-400 text-sm">
        💡 <strong>Sugerencia:</strong> Punto de reorden calculado según consumo + plazo de entrega
        + stock de seguridad
      </div>
    </div>
  )
}

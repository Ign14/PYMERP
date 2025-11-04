import { useQuery } from '@tanstack/react-query'
import { getSalesABCAnalysis } from '../services/client'
import { useMemo } from 'react'

export default function SalesABCTable() {
  const { data: abcData = [], isLoading } = useQuery({
    queryKey: ['salesABCAnalysis'],
    queryFn: () => getSalesABCAnalysis(),
    refetchInterval: 5 * 60 * 1000,
  })

  const sortedData = useMemo(() => {
    return [...abcData].sort((a, b) => b.totalRevenue - a.totalRevenue)
  }, [abcData])

  if (isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
        <div className="animate-pulse">
          <div className="h-6 bg-neutral-800 rounded w-1/3 mb-4"></div>
          <div className="space-y-3">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="h-12 bg-neutral-800 rounded"></div>
            ))}
          </div>
        </div>
      </div>
    )
  }

  const classColors = {
    A: { bg: 'bg-red-950', border: 'border-red-900', text: 'text-red-200' },
    B: { bg: 'bg-amber-950', border: 'border-amber-900', text: 'text-amber-200' },
    C: { bg: 'bg-emerald-950', border: 'border-emerald-900', text: 'text-emerald-200' },
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
      <div className="flex items-center justify-between mb-6">
        <h3 className="text-lg font-semibold text-white">Clasificación Detallada de Productos</h3>
        <span className="text-sm text-neutral-400">{sortedData.length} productos</span>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-neutral-800">
              <th className="text-left py-3 px-2 text-neutral-400 font-medium">Clase</th>
              <th className="text-left py-3 px-2 text-neutral-400 font-medium">Producto</th>
              <th className="text-right py-3 px-2 text-neutral-400 font-medium">Ingresos</th>
              <th className="text-right py-3 px-2 text-neutral-400 font-medium">Ventas</th>
              <th className="text-right py-3 px-2 text-neutral-400 font-medium">% Total</th>
              <th className="text-right py-3 px-2 text-neutral-400 font-medium">% Acum.</th>
              <th className="text-right py-3 px-2 text-neutral-400 font-medium">Precio Prom.</th>
              <th className="text-left py-3 px-2 text-neutral-400 font-medium">
                Acción Recomendada
              </th>
            </tr>
          </thead>
          <tbody>
            {sortedData.map((item, index) => {
              const colors =
                classColors[item.classification as keyof typeof classColors] || classColors.C
              const isTopTen = index < 10

              return (
                <tr
                  key={item.productId}
                  className={`border-b border-neutral-800 hover:bg-neutral-800/50 transition-colors ${
                    isTopTen ? 'bg-neutral-800/30' : ''
                  }`}
                >
                  <td className="py-3 px-2">
                    <span
                      className={`inline-block px-2 py-1 rounded text-xs font-semibold ${colors.bg} ${colors.border} ${colors.text}`}
                    >
                      {item.classification}
                    </span>
                  </td>
                  <td className="py-3 px-2">
                    <div className="flex flex-col">
                      <span className="text-white font-medium">{item.productName}</span>
                      <span className="text-xs text-neutral-400">ID: {item.productId}</span>
                    </div>
                  </td>
                  <td className="py-3 px-2 text-right text-white font-medium">
                    ${item.totalRevenue.toLocaleString('es-CL')}
                  </td>
                  <td className="py-3 px-2 text-right text-neutral-300">{item.salesCount}</td>
                  <td className="py-3 px-2 text-right text-neutral-300">
                    {item.percentageOfTotal.toFixed(2)}%
                  </td>
                  <td className="py-3 px-2 text-right text-neutral-300">
                    {item.cumulativePercentage.toFixed(2)}%
                  </td>
                  <td className="py-3 px-2 text-right text-neutral-300">
                    ${item.averagePrice.toLocaleString('es-CL')}
                  </td>
                  <td className="py-3 px-2 text-neutral-400 text-xs max-w-xs truncate">
                    {item.recommendedAction}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {sortedData.length === 0 && (
        <div className="text-center py-12 text-neutral-500">
          No hay datos de clasificación ABC disponibles
        </div>
      )}

      {sortedData.length > 0 && (
        <div className="mt-4 text-xs text-neutral-500 flex items-center gap-2">
          <div className="w-3 h-3 bg-neutral-800/30 rounded"></div>
          <span>Top 10 productos destacados</span>
        </div>
      )}
    </div>
  )
}

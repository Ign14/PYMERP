import { useQuery } from '@tanstack/react-query'
import { getSupplierRanking, SupplierRanking } from '../services/client'
import { useState } from 'react'

export default function SuppliersRanking() {
  const [criteria, setCriteria] = useState<string>('volume')

  const rankingQuery = useQuery<SupplierRanking[], Error>({
    queryKey: ['supplier-ranking', criteria],
    queryFn: () => getSupplierRanking(criteria),
    refetchOnWindowFocus: false,
  })

  if (rankingQuery.isLoading) {
    return (
      <div className="card-content">
        <h2 className="text-lg font-semibold text-neutral-100">🏆 Ranking de Proveedores</h2>
        <p className="text-neutral-400 mt-4">Cargando ranking...</p>
      </div>
    )
  }

  if (rankingQuery.isError) {
    return (
      <div className="card-content">
        <h2 className="text-lg font-semibold text-neutral-100">🏆 Ranking de Proveedores</h2>
        <p className="text-red-400 mt-4">
          {rankingQuery.error?.message ?? 'Error al cargar ranking'}
        </p>
      </div>
    )
  }

  const rankings = rankingQuery.data ?? []

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value)
  }

  const getMedalIcon = (rank: number) => {
    if (rank === 1) return '🥇'
    if (rank === 2) return '🥈'
    if (rank === 3) return '🥉'
    return null
  }

  const getRankColor = (rank: number) => {
    if (rank === 1) return 'text-yellow-400'
    if (rank === 2) return 'text-neutral-300'
    if (rank === 3) return 'text-orange-400'
    return 'text-neutral-500'
  }

  const getCategoryColor = (category: string) => {
    if (category === 'A') return 'bg-red-950/50 text-red-400 border-red-800'
    if (category === 'B') return 'bg-yellow-950/50 text-yellow-400 border-yellow-800'
    return 'bg-neutral-800/50 text-neutral-400 border-neutral-700'
  }

  const getCategoryLabel = (category: string) => {
    if (category === 'A') return 'Crítico'
    if (category === 'B') return 'Importante'
    return 'Ocasional'
  }

  return (
    <div className="card-content">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-semibold text-neutral-100">🏆 Ranking de Proveedores</h2>
        <select
          value={criteria}
          onChange={e => setCriteria(e.target.value)}
          className="px-3 py-1 text-sm rounded bg-neutral-900 text-neutral-200 border border-neutral-700 focus:outline-none focus:border-blue-600"
        >
          <option value="volume">Por Volumen</option>
          <option value="reliability">Por Confiabilidad</option>
          <option value="value">Por Valor</option>
        </select>
      </div>

      {rankings.length === 0 ? (
        <div className="text-center py-8">
          <p className="text-neutral-400">No hay datos de compras en el último año</p>
          <p className="text-sm text-neutral-500 mt-2">
            Realiza compras para ver el ranking de proveedores
          </p>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-neutral-800">
                <th className="text-left py-2 px-2 text-neutral-400 font-medium">#</th>
                <th className="text-left py-2 px-2 text-neutral-400 font-medium">Proveedor</th>
                <th className="text-right py-2 px-2 text-neutral-400 font-medium">Score</th>
                <th className="text-right py-2 px-2 text-neutral-400 font-medium">Compras</th>
                <th className="text-right py-2 px-2 text-neutral-400 font-medium">Monto</th>
                <th className="text-right py-2 px-2 text-neutral-400 font-medium">Confiabilidad</th>
                <th className="text-center py-2 px-2 text-neutral-400 font-medium">Categoría</th>
              </tr>
            </thead>
            <tbody>
              {rankings.map(ranking => {
                const medal = getMedalIcon(ranking.rank)
                const rankColor = getRankColor(ranking.rank)

                return (
                  <tr
                    key={ranking.supplierId}
                    className="border-b border-neutral-800/50 hover:bg-neutral-900/30 transition-colors"
                  >
                    <td className={`py-3 px-2 font-semibold ${rankColor}`}>
                      {medal ? (
                        <span className="text-xl">{medal}</span>
                      ) : (
                        <span>{ranking.rank}</span>
                      )}
                    </td>
                    <td className="py-3 px-2">
                      <div className="flex flex-col">
                        <span className="text-neutral-200 font-medium">{ranking.supplierName}</span>
                        {ranking.rank <= 3 && (
                          <span className="text-xs text-neutral-500">
                            {ranking.rank === 1 ? 'Top proveedor' : `Top ${ranking.rank}`}
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="py-3 px-2 text-right">
                      <div className="flex flex-col items-end">
                        <span className="font-semibold text-blue-400">
                          {ranking.score.toFixed(1)}
                        </span>
                        <div className="w-16 bg-neutral-800 rounded-full h-1 mt-1">
                          <div
                            className="bg-blue-500 h-1 rounded-full transition-all"
                            style={{ width: `${Math.min(ranking.score, 100)}%` }}
                          />
                        </div>
                      </div>
                    </td>
                    <td className="py-3 px-2 text-right text-neutral-300">
                      {ranking.totalPurchases.toLocaleString('es-CL')}
                    </td>
                    <td className="py-3 px-2 text-right text-neutral-300 font-medium">
                      {formatCurrency(ranking.totalAmount)}
                    </td>
                    <td className="py-3 px-2 text-right">
                      <div className="flex flex-col items-end">
                        <span
                          className={`text-sm ${
                            ranking.reliability >= 80
                              ? 'text-green-400'
                              : ranking.reliability >= 50
                                ? 'text-yellow-400'
                                : 'text-orange-400'
                          }`}
                        >
                          {ranking.reliability.toFixed(0)}%
                        </span>
                      </div>
                    </td>
                    <td className="py-3 px-2 text-center">
                      <span
                        className={`px-2 py-1 text-xs font-medium rounded border ${getCategoryColor(ranking.category)}`}
                        title={getCategoryLabel(ranking.category)}
                      >
                        {ranking.category}
                      </span>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      <div className="mt-4 pt-3 border-t border-neutral-800">
        <div className="flex items-center gap-6 text-xs text-neutral-500">
          <div className="flex items-center gap-2">
            <span className="text-lg">🥇</span>
            <span>Oro: #1</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-lg">🥈</span>
            <span>Plata: #2</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-lg">🥉</span>
            <span>Bronce: #3</span>
          </div>
          <div className="flex items-center gap-1 ml-auto">
            <span className="w-2 h-2 rounded-full bg-red-500"></span>
            <span>A: Crítico (80%)</span>
          </div>
          <div className="flex items-center gap-1">
            <span className="w-2 h-2 rounded-full bg-yellow-500"></span>
            <span>B: Importante (15%)</span>
          </div>
          <div className="flex items-center gap-1">
            <span className="w-2 h-2 rounded-full bg-neutral-500"></span>
            <span>C: Ocasional (5%)</span>
          </div>
        </div>
      </div>
    </div>
  )
}

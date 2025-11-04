import { useQuery } from '@tanstack/react-query'
import { getPurchaseABCAnalysis } from '../../services/client'
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  Cell,
} from 'recharts'

export default function PurchaseABCChart() {
  const { data: classifications, isLoading } = useQuery({
    queryKey: ['purchaseABCAnalysis'],
    queryFn: () => getPurchaseABCAnalysis(),
    refetchInterval: 5 * 60 * 1000,
  })

  if (isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6 h-96 animate-pulse">
        <div className="h-8 bg-neutral-800 rounded w-1/3 mb-4"></div>
        <div className="h-full bg-neutral-800 rounded"></div>
      </div>
    )
  }

  if (!classifications || classifications.length === 0) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-white mb-4">
          📊 Distribución ABC de Proveedores
        </h3>
        <p className="text-neutral-400 text-center py-8">
          No hay datos suficientes para análisis ABC
        </p>
      </div>
    )
  }

  // Agrupar por clasificación
  const summary = classifications.reduce(
    (acc, item) => {
      const key = item.classification
      acc[key].count++
      acc[key].totalSpent += item.totalSpent
      acc[key].percentage += item.percentageOfTotal
      return acc
    },
    {
      A: { count: 0, totalSpent: 0, percentage: 0 },
      B: { count: 0, totalSpent: 0, percentage: 0 },
      C: { count: 0, totalSpent: 0, percentage: 0 },
    }
  )

  const chartData = [
    {
      name: 'Clase A (Críticos)',
      proveedores: summary.A.count,
      gasto: summary.A.totalSpent,
      porcentaje: summary.A.percentage,
    },
    {
      name: 'Clase B (Importantes)',
      proveedores: summary.B.count,
      gasto: summary.B.totalSpent,
      porcentaje: summary.B.percentage,
    },
    {
      name: 'Clase C (Ocasionales)',
      proveedores: summary.C.count,
      gasto: summary.C.totalSpent,
      porcentaje: summary.C.percentage,
    },
  ]

  const COLORS = {
    A: '#ef4444', // red
    B: '#f59e0b', // amber
    C: '#10b981', // green
  }

  const formatCurrency = (value: number) => {
    return `$${value.toLocaleString('es-CL', { maximumFractionDigits: 0 })}`
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
      <h3 className="text-lg font-semibold text-white mb-6">
        📊 Distribución ABC de Proveedores (Pareto 80-15-5)
      </h3>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-6">
        <div className="bg-gradient-to-br from-red-900/30 to-red-800/20 border border-red-800 rounded-lg p-4">
          <div className="text-sm text-red-400 mb-1">Clase A - Críticos</div>
          <div className="text-2xl font-bold text-white">{summary.A.count} proveedores</div>
          <div className="text-xs text-red-300 mt-2">
            {summary.A.percentage.toFixed(1)}% del gasto total
          </div>
          <div className="text-sm text-red-200 mt-1">{formatCurrency(summary.A.totalSpent)}</div>
        </div>

        <div className="bg-gradient-to-br from-amber-900/30 to-amber-800/20 border border-amber-800 rounded-lg p-4">
          <div className="text-sm text-amber-400 mb-1">Clase B - Importantes</div>
          <div className="text-2xl font-bold text-white">{summary.B.count} proveedores</div>
          <div className="text-xs text-amber-300 mt-2">
            {summary.B.percentage.toFixed(1)}% del gasto total
          </div>
          <div className="text-sm text-amber-200 mt-1">{formatCurrency(summary.B.totalSpent)}</div>
        </div>

        <div className="bg-gradient-to-br from-green-900/30 to-green-800/20 border border-green-800 rounded-lg p-4">
          <div className="text-sm text-green-400 mb-1">Clase C - Ocasionales</div>
          <div className="text-2xl font-bold text-white">{summary.C.count} proveedores</div>
          <div className="text-xs text-green-300 mt-2">
            {summary.C.percentage.toFixed(1)}% del gasto total
          </div>
          <div className="text-sm text-green-200 mt-1">{formatCurrency(summary.C.totalSpent)}</div>
        </div>
      </div>

      <ResponsiveContainer width="100%" height={300}>
        <BarChart data={chartData}>
          <CartesianGrid strokeDasharray="3 3" stroke="#404040" />
          <XAxis dataKey="name" stroke="#a3a3a3" tick={{ fill: '#a3a3a3' }} />
          <YAxis
            stroke="#a3a3a3"
            tick={{ fill: '#a3a3a3' }}
            tickFormatter={value => formatCurrency(value)}
          />
          <Tooltip
            contentStyle={{
              backgroundColor: '#171717',
              border: '1px solid #404040',
              borderRadius: '8px',
            }}
            labelStyle={{ color: '#fff' }}
            formatter={(value: number, name: string) => {
              if (name === 'gasto') return formatCurrency(value)
              if (name === 'porcentaje') return `${value.toFixed(1)}%`
              return value
            }}
          />
          <Legend wrapperStyle={{ color: '#a3a3a3' }} />
          <Bar dataKey="gasto" fill="#8884d8" name="Gasto Total">
            {chartData.map((entry, index) => {
              const className = entry.name.includes('A')
                ? 'A'
                : entry.name.includes('B')
                  ? 'B'
                  : 'C'
              return <Cell key={`cell-${index}`} fill={COLORS[className]} />
            })}
          </Bar>
        </BarChart>
      </ResponsiveContainer>

      <div className="mt-4 bg-blue-900/20 border border-blue-800 rounded-lg p-4">
        <p className="text-sm text-blue-300">
          💡 <strong>Análisis Pareto (80-15-5):</strong> Clase A representa ~80% del gasto (pocos
          proveedores clave), Clase B ~15% (proveedores moderados), y Clase C ~5% (muchos
          proveedores ocasionales).
        </p>
      </div>
    </div>
  )
}

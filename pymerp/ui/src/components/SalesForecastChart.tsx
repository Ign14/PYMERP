import { useQuery } from '@tanstack/react-query'
import { getSalesProductForecast } from '../services/client'
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts'

export default function SalesForecastChart() {
  const { data: forecastData = [], isLoading } = useQuery({
    queryKey: ['salesProductForecast'],
    queryFn: () => getSalesProductForecast(),
    refetchInterval: 5 * 60 * 1000,
  })

  if (isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
        <div className="animate-pulse">
          <div className="h-6 bg-neutral-800 rounded w-1/3 mb-4"></div>
          <div className="h-64 bg-neutral-800 rounded"></div>
        </div>
      </div>
    )
  }

  // Top 5 productos por demanda pronosticada
  const top5 = forecastData.slice(0, 5)

  // Datos para el gráfico
  const chartData = top5.map(item => ({
    name: item.productName,
    historical: item.historicalAverage,
    forecasted: item.forecastedDemand,
  }))

  const getTrendIcon = (trend: string) => {
    if (trend === 'increasing') return '📈'
    if (trend === 'decreasing') return '📉'
    return '➡️'
  }

  const getTrendColor = (trend: string) => {
    if (trend === 'increasing') return 'text-red-400'
    if (trend === 'decreasing') return 'text-green-400'
    return 'text-neutral-400'
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
      <h3 className="text-lg font-semibold text-white mb-6">
        Pronóstico de Demanda - Top 5 Productos
      </h3>

      {/* Tarjetas resumen */}
      <div className="grid grid-cols-1 md:grid-cols-5 gap-4 mb-6">
        {top5.map((item, index) => {
          const variation =
            item.historicalAverage > 0
              ? ((item.forecastedDemand - item.historicalAverage) / item.historicalAverage) * 100
              : 0

          return (
            <div
              key={item.productId}
              className="bg-neutral-800 border border-neutral-700 rounded-lg p-3"
            >
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs text-neutral-400">#{index + 1}</span>
                <span className={`text-lg ${getTrendColor(item.trend)}`}>
                  {getTrendIcon(item.trend)}
                </span>
              </div>
              <div className="text-sm font-medium text-white mb-1 truncate">{item.productName}</div>
              <div className="text-xs text-neutral-400 mb-1">
                Pronóstico:{' '}
                <span className="text-white font-semibold">
                  {item.forecastedDemand.toFixed(0)} u/mes
                </span>
              </div>
              <div className="text-xs text-neutral-400">
                Histórico: {item.historicalAverage.toFixed(0)} u/mes
              </div>
              <div
                className={`text-xs font-semibold mt-1 ${
                  variation > 0
                    ? 'text-red-400'
                    : variation < 0
                      ? 'text-green-400'
                      : 'text-neutral-400'
                }`}
              >
                {variation > 0 ? '+' : ''}
                {variation.toFixed(1)}%
              </div>
              <div className="mt-2 text-xs">
                <span className="text-neutral-500">Confianza: </span>
                <span
                  className={`font-semibold ${
                    item.confidence >= 70
                      ? 'text-green-400'
                      : item.confidence >= 50
                        ? 'text-amber-400'
                        : 'text-red-400'
                  }`}
                >
                  {item.confidence.toFixed(0)}%
                </span>
              </div>
            </div>
          )
        })}
      </div>

      {/* Gráfico de líneas */}
      <ResponsiveContainer width="100%" height={300}>
        <LineChart data={chartData}>
          <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
          <XAxis dataKey="name" stroke="#9ca3af" angle={-15} textAnchor="end" height={80} />
          <YAxis stroke="#9ca3af" />
          <Tooltip
            contentStyle={{
              backgroundColor: '#1f2937',
              border: '1px solid #374151',
              borderRadius: '0.375rem',
              color: '#fff',
            }}
            formatter={(value: number) => [value.toFixed(2) + ' u/mes', '']}
          />
          <Legend />
          <Line
            type="monotone"
            dataKey="historical"
            name="Demanda Histórica"
            stroke="#3b82f6"
            strokeWidth={2}
            dot={{ fill: '#3b82f6', r: 4 }}
          />
          <Line
            type="monotone"
            dataKey="forecasted"
            name="Demanda Pronosticada"
            stroke="#f59e0b"
            strokeWidth={2}
            strokeDasharray="5 5"
            dot={{ fill: '#f59e0b', r: 4 }}
          />
        </LineChart>
      </ResponsiveContainer>

      {forecastData.length === 0 && (
        <div className="text-center py-12 text-neutral-500">
          No hay datos de pronóstico disponibles
        </div>
      )}
    </div>
  )
}

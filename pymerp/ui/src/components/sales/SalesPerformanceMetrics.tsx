import { useMemo } from 'react'

type PerformanceMetrics = {
  cancellationRate: number
  avgTicket: number
  peakHour: string
  weeklyTrend: 'up' | 'down' | 'stable'
  totalSales: number
  totalCancelled: number
}

type SalesPerformanceMetricsProps = {
  startDate?: string
  endDate?: string
}

function formatCurrency(value: number): string {
  return `$${Math.round(value).toLocaleString('es-CL')}`
}

// Datos de demostración
function generatePerformanceMetrics(): PerformanceMetrics {
  return {
    cancellationRate: 4.2,
    avgTicket: 125500,
    peakHour: '14:00 - 15:00',
    weeklyTrend: 'up',
    totalSales: 634,
    totalCancelled: 27,
  }
}

export default function SalesPerformanceMetrics({
  startDate,
  endDate,
}: SalesPerformanceMetricsProps) {
  const metrics = useMemo(() => generatePerformanceMetrics(), [])

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5 mb-6">
      <header className="mb-4">
        <h2 className="text-xl font-semibold text-neutral-100 mb-1">📊 Métricas de Rendimiento</h2>
        <p className="text-sm text-neutral-400">Indicadores clave de desempeño comercial</p>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Tasa de cancelación */}
        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
          <div className="flex items-center gap-2 mb-2">
            <span className="text-2xl">🚫</span>
            <h3 className="text-sm font-medium text-neutral-400">Tasa de Cancelación</h3>
          </div>
          <p className="text-3xl font-bold text-neutral-100 mb-1">
            {metrics.cancellationRate.toFixed(1)}%
          </p>
          <div className="flex items-center gap-2">
            <span className="text-xs text-neutral-400">
              {metrics.totalCancelled} de {metrics.totalSales} ventas
            </span>
            {metrics.cancellationRate < 5 ? (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium bg-green-950 text-green-400 border border-green-800">
                🟢 Bajo
              </span>
            ) : metrics.cancellationRate < 10 ? (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium bg-yellow-950 text-yellow-400 border border-yellow-800">
                🟡 Medio
              </span>
            ) : (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium bg-red-950 text-red-400 border border-red-800">
                🔴 Alto
              </span>
            )}
          </div>
        </div>

        {/* Ticket promedio */}
        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
          <div className="flex items-center gap-2 mb-2">
            <span className="text-2xl">🎫</span>
            <h3 className="text-sm font-medium text-neutral-400">Ticket Promedio</h3>
          </div>
          <p className="text-3xl font-bold text-neutral-100 mb-1">
            {formatCurrency(metrics.avgTicket)}
          </p>
          <p className="text-xs text-neutral-400">Por transacción emitida</p>
        </div>

        {/* Horario pico */}
        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
          <div className="flex items-center gap-2 mb-2">
            <span className="text-2xl">⏰</span>
            <h3 className="text-sm font-medium text-neutral-400">Horario Pico</h3>
          </div>
          <p className="text-3xl font-bold text-neutral-100 mb-1">
            {metrics.peakHour.split(' - ')[0]}
          </p>
          <p className="text-xs text-neutral-400">Mayor volumen de ventas</p>
        </div>

        {/* Tendencia semanal */}
        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
          <div className="flex items-center gap-2 mb-2">
            <span className="text-2xl">📈</span>
            <h3 className="text-sm font-medium text-neutral-400">Tendencia Semanal</h3>
          </div>
          <div className="flex items-center gap-2 mb-1">
            {metrics.weeklyTrend === 'up' ? (
              <>
                <span className="text-3xl font-bold text-green-400">↗</span>
                <span className="text-lg font-semibold text-neutral-100">Crecimiento</span>
              </>
            ) : metrics.weeklyTrend === 'down' ? (
              <>
                <span className="text-3xl font-bold text-red-400">↘</span>
                <span className="text-lg font-semibold text-neutral-100">Descenso</span>
              </>
            ) : (
              <>
                <span className="text-3xl font-bold text-neutral-400">→</span>
                <span className="text-lg font-semibold text-neutral-100">Estable</span>
              </>
            )}
          </div>
          <p className="text-xs text-neutral-400">Comparado con semana anterior</p>
        </div>
      </div>

      {/* Barra de progreso del período */}
      <div className="mt-6 bg-neutral-800 border border-neutral-700 rounded-lg p-4">
        <div className="flex justify-between items-center mb-2">
          <h3 className="text-sm font-medium text-neutral-100">Progreso del período</h3>
          <span className="text-sm text-neutral-400">{metrics.totalSales} ventas totales</span>
        </div>
        <div className="relative w-full h-3 bg-neutral-700 rounded-full overflow-hidden">
          <div
            className="absolute top-0 left-0 h-full bg-gradient-to-r from-green-500 to-cyan-500 rounded-full"
            style={{
              width: `${((metrics.totalSales - metrics.totalCancelled) / metrics.totalSales) * 100}%`,
            }}
          ></div>
        </div>
        <div className="flex justify-between items-center mt-2">
          <span className="text-xs text-green-400">
            {metrics.totalSales - metrics.totalCancelled} emitidas (
            {((1 - metrics.cancellationRate / 100) * 100).toFixed(1)}%)
          </span>
          <span className="text-xs text-red-400">
            {metrics.totalCancelled} canceladas ({metrics.cancellationRate.toFixed(1)}%)
          </span>
        </div>
      </div>
    </div>
  )
}

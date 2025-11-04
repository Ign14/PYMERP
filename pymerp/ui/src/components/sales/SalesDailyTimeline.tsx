import { useMemo } from 'react'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'

type HourlySale = {
  hour: string
  sales: number
  count: number
}

type SalesDailyTimelineProps = {
  date?: string
}

function formatCurrency(value: number): string {
  return `$${Math.round(value).toLocaleString('es-CL')}`
}

// Datos de demostración - ventas por hora
function generateHourlySales(): HourlySale[] {
  return [
    { hour: '09:00', sales: 850000, count: 8 },
    { hour: '10:00', sales: 1200000, count: 12 },
    { hour: '11:00', sales: 1850000, count: 18 },
    { hour: '12:00', sales: 2400000, count: 22 },
    { hour: '13:00', sales: 1500000, count: 14 },
    { hour: '14:00', sales: 3200000, count: 28 },
    { hour: '15:00', sales: 2900000, count: 26 },
    { hour: '16:00', sales: 2100000, count: 19 },
    { hour: '17:00', sales: 1700000, count: 16 },
    { hour: '18:00', sales: 1200000, count: 11 },
    { hour: '19:00', sales: 900000, count: 8 },
  ]
}

export default function SalesDailyTimeline({ date }: SalesDailyTimelineProps) {
  const hourlySales = useMemo(() => generateHourlySales(), [])
  const totalSales = hourlySales.reduce((sum, h) => sum + h.sales, 0)
  const totalCount = hourlySales.reduce((sum, h) => sum + h.count, 0)
  const peakHour = hourlySales.reduce((max, h) => (h.sales > max.sales ? h : max), hourlySales[0])

  const chartData = hourlySales.map(h => ({
    hora: h.hour,
    ventas: h.sales / 1000000, // Convertir a millones
    cantidad: h.count,
  }))

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5 mb-6">
      <header className="mb-4">
        <h2 className="text-xl font-semibold text-neutral-100 mb-1">
          ⏰ Timeline Diario de Ventas
        </h2>
        <p className="text-sm text-neutral-400">
          Patrón de ventas por hora | Total: {formatCurrency(totalSales)} ({totalCount} ventas)
        </p>
      </header>

      {/* KPIs del día */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
          <h3 className="text-xs font-medium text-neutral-400 mb-1">Hora Pico</h3>
          <p className="text-2xl font-bold text-neutral-100">{peakHour.hour}</p>
          <p className="text-xs text-neutral-400 mt-1">
            {formatCurrency(peakHour.sales)} en {peakHour.count} ventas
          </p>
        </div>
        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
          <h3 className="text-xs font-medium text-neutral-400 mb-1">Promedio por Hora</h3>
          <p className="text-2xl font-bold text-neutral-100">
            {formatCurrency(totalSales / hourlySales.length)}
          </p>
          <p className="text-xs text-neutral-400 mt-1">
            {(totalCount / hourlySales.length).toFixed(1)} ventas/hora
          </p>
        </div>
        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
          <h3 className="text-xs font-medium text-neutral-400 mb-1">Ticket Promedio</h3>
          <p className="text-2xl font-bold text-neutral-100">
            {formatCurrency(totalSales / totalCount)}
          </p>
          <p className="text-xs text-neutral-400 mt-1">Por transacción</p>
        </div>
      </div>

      {/* Gráfico de barras por hora */}
      <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
        <div style={{ height: 320 }} role="img" aria-label="Ventas por hora del día">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData} margin={{ top: 10, right: 10, left: 10, bottom: 10 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#404040" />
              <XAxis dataKey="hora" stroke="#a3a3a3" tick={{ fill: '#a3a3a3', fontSize: 12 }} />
              <YAxis stroke="#a3a3a3" tick={{ fill: '#a3a3a3', fontSize: 12 }} />
              <Tooltip
                contentStyle={{
                  backgroundColor: '#171717',
                  border: '1px solid #404040',
                  borderRadius: '0.5rem',
                  color: '#f5f5f5',
                }}
                formatter={(value: number, name: string) => {
                  if (name === 'ventas') return [`${value.toFixed(2)}M`, 'Ventas (millones)']
                  if (name === 'cantidad') return [value, 'Cantidad']
                  return [value, name]
                }}
              />
              <Bar dataKey="ventas" fill="#22d3ee" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Indicadores de horarios */}
      <div className="mt-4 flex flex-wrap gap-2">
        <span className="inline-flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-medium bg-green-950 text-green-400 border border-green-800">
          🟢 Horario pico: 14:00-15:00
        </span>
        <span className="inline-flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-medium bg-yellow-950 text-yellow-400 border border-yellow-800">
          🟡 Horario medio: 11:00-12:00, 16:00-17:00
        </span>
        <span className="inline-flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-medium bg-neutral-800 text-neutral-400 border border-neutral-700">
          ⚪ Horario bajo: 09:00-10:00, 19:00
        </span>
      </div>
    </div>
  )
}

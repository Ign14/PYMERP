import type { CSSProperties } from 'react'
import {
  Area,
  AreaChart,
  CartesianGrid,
  Line,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { formatMoneyCLP } from '../../utils/currency'

type SalesTrendChartProps = {
  points: Array<{ date: string; total: number }>
  formatter?: (value: number) => string
  averageLine?: number
}

const hiddenListStyles: CSSProperties = {
  position: 'absolute',
  width: 1,
  height: 1,
  padding: 0,
  margin: -1,
  overflow: 'hidden',
  clip: 'rect(0, 0, 0, 0)',
  whiteSpace: 'nowrap',
  border: 0,
}

export default function SalesTrendChart({
  points,
  formatter = formatMoneyCLP,
  averageLine,
}: SalesTrendChartProps) {
  // Convertir los puntos de ventas para el gráfico
  const data = points.map(point => ({
    date: point.date,
    total: point.total ?? 0,
  }))

  const hasActivity = data.some(point => point.total > 0)

  return (
    <div className="chart-container" data-testid="sales-trend-chart" data-point-count={data.length}>
      <div style={{ height: 260 }} role="img" aria-label="Tendencia de ventas diarias">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data} margin={{ top: 10, right: 24, left: 0, bottom: 0 }}>
            <defs>
              <linearGradient id="salesTrendGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#22d3ee" stopOpacity={0.4} />
                <stop offset="95%" stopColor="#22d3ee" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#404040" />
            <XAxis dataKey="date" stroke="#a3a3a3" tick={{ fontSize: 12, fill: '#a3a3a3' }} />
            <YAxis
              stroke="#a3a3a3"
              tick={{ fill: '#a3a3a3' }}
              tickFormatter={value => formatter(Number(value))}
              width={96}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: '#171717',
                border: '1px solid #404040',
                borderRadius: '0.5rem',
                color: '#f5f5f5',
              }}
              formatter={(value: number, name) => {
                const label = name === 'total' ? 'Venta del día' : 'Promedio período'
                return [formatter(value), label]
              }}
              labelFormatter={label => `Fecha: ${label}`}
            />
            {/* Relleno bajo ventas del día */}
            <Area
              type="monotone"
              dataKey="total"
              stroke="#22d3ee"
              strokeWidth={2}
              fill="url(#salesTrendGradient)"
              dot={{ r: 4, strokeWidth: 2, fill: '#0891b2', stroke: '#22d3ee' }}
              activeDot={{ r: 6, fill: '#06b6d4', stroke: '#22d3ee', strokeWidth: 2 }}
            />
            {/* Línea de promedio de ventas diarias */}
            {averageLine !== undefined && averageLine > 0 && (
              <ReferenceLine
                y={averageLine}
                stroke="#fb923c"
                strokeWidth={2}
                strokeDasharray="5 5"
                label={{
                  value: `Promedio: ${formatter(averageLine)}`,
                  position: 'right',
                  style: { fill: '#fb923c', fontSize: 12, fontWeight: 600 },
                }}
              />
            )}
          </AreaChart>
        </ResponsiveContainer>
      </div>
      {!hasActivity && (
        <p className="text-neutral-400" role="status">
          Sin ventas registradas en el período seleccionado.
        </p>
      )}
      <ul style={hiddenListStyles} data-testid="sales-trend-points">
        {data.map(point => (
          <li
            key={point.date}
            data-testid="sales-trend-point"
          >{`${point.date}: ${point.total}`}</li>
        ))}
      </ul>
    </div>
  )
}

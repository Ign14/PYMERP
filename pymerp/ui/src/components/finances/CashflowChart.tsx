import { useEffect, useState } from 'react'
import { getCashflowProjection, type CashflowProjection } from '../../services/client'
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

export default function CashflowChart() {
  const [data, setData] = useState<CashflowProjection[]>([])
  const [loading, setLoading] = useState(true)
  const [days, setDays] = useState(30)

  useEffect(() => {
    loadData()
  }, [days])

  const loadData = async () => {
    try {
      setLoading(true)
      const result = await getCashflowProjection(days)
      setData(result)
    } catch (err) {
      console.error('Error loading cashflow:', err)
    } finally {
      setLoading(false)
    }
  }

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value)
  }

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr)
    return `${date.getDate()}/${date.getMonth() + 1}`
  }

  const chartData = data.map(item => ({
    date: formatDate(item.date),
    ingresos: item.expectedIncome,
    egresos: item.expectedExpense,
    neto: item.netCashflow,
    acumulado: item.cumulativeBalance,
  }))

  if (loading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-8">
        <div className="flex items-center justify-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          <span className="ml-3 text-neutral-400">Cargando proyección...</span>
        </div>
      </div>
    )
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg">
      <div className="p-5 border-b border-neutral-800 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-neutral-100">Proyección de Flujo de Caja</h2>
        <div className="flex gap-2">
          {[7, 15, 30, 60].map(d => (
            <button
              key={d}
              onClick={() => setDays(d)}
              className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${
                days === d
                  ? 'bg-blue-600 text-white'
                  : 'bg-neutral-800 text-neutral-300 hover:bg-neutral-700'
              }`}
            >
              {d} días
            </button>
          ))}
        </div>
      </div>

      <div className="p-5">
        <ResponsiveContainer width="100%" height={400}>
          <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
            <XAxis dataKey="date" stroke="#6b7280" style={{ fontSize: '12px' }} />
            <YAxis
              stroke="#6b7280"
              style={{ fontSize: '12px' }}
              tickFormatter={value => {
                if (value >= 1000000) return `${(value / 1000000).toFixed(1)}M`
                if (value >= 1000) return `${(value / 1000).toFixed(0)}K`
                return value.toString()
              }}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: 'white',
                border: '1px solid #e5e7eb',
                borderRadius: '8px',
                padding: '8px',
              }}
              formatter={(value: number) => formatCurrency(value)}
            />
            <Legend wrapperStyle={{ paddingTop: '20px' }} iconType="line" />
            <Line
              type="monotone"
              dataKey="ingresos"
              stroke="#10b981"
              strokeWidth={2}
              name="Ingresos Esperados"
              dot={{ r: 3 }}
              activeDot={{ r: 5 }}
            />
            <Line
              type="monotone"
              dataKey="egresos"
              stroke="#ef4444"
              strokeWidth={2}
              name="Egresos Esperados"
              dot={{ r: 3 }}
              activeDot={{ r: 5 }}
            />
            <Line
              type="monotone"
              dataKey="neto"
              stroke="#3b82f6"
              strokeWidth={2}
              name="Flujo Neto"
              dot={{ r: 3 }}
              activeDot={{ r: 5 }}
            />
            <Line
              type="monotone"
              dataKey="acumulado"
              stroke="#8b5cf6"
              strokeWidth={2}
              strokeDasharray="5 5"
              name="Balance Acumulado"
              dot={{ r: 3 }}
              activeDot={{ r: 5 }}
            />
          </LineChart>
        </ResponsiveContainer>

        <div className="mt-6 grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="bg-green-950 border border-green-800 rounded-lg p-4">
            <div className="text-xs text-green-400 font-medium mb-1">Total Ingresos</div>
            <div className="text-lg font-bold text-green-300">
              {formatCurrency(data.reduce((sum, d) => sum + d.expectedIncome, 0))}
            </div>
          </div>
          <div className="bg-red-950 border border-red-800 rounded-lg p-4">
            <div className="text-xs text-red-400 font-medium mb-1">Total Egresos</div>
            <div className="text-lg font-bold text-red-300">
              {formatCurrency(data.reduce((sum, d) => sum + d.expectedExpense, 0))}
            </div>
          </div>
          <div className="bg-blue-950 border border-blue-800 rounded-lg p-4">
            <div className="text-xs text-blue-400 font-medium mb-1">Flujo Neto</div>
            <div className="text-lg font-bold text-blue-300">
              {formatCurrency(data.reduce((sum, d) => sum + d.netCashflow, 0))}
            </div>
          </div>
          <div className="bg-purple-950 border border-purple-800 rounded-lg p-4">
            <div className="text-xs text-purple-400 font-medium mb-1">Balance Final</div>
            <div className="text-lg font-bold text-purple-300">
              {formatCurrency(data[data.length - 1]?.cumulativeBalance || 0)}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

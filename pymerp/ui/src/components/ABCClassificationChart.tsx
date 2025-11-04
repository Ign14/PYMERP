import { useQuery } from '@tanstack/react-query'
import { getABCAnalysis } from '../services/client'
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

export default function ABCClassificationChart() {
  const {
    data: analysis,
    isLoading,
    error,
  } = useQuery({
    queryKey: ['abcAnalysis'],
    queryFn: () => getABCAnalysis(),
    refetchInterval: 300000, // Refetch every 5 minutes
  })

  if (isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6 animate-pulse">
        <div className="h-6 bg-neutral-800 rounded w-1/3 mb-6"></div>
        <div className="h-80 bg-neutral-800 rounded"></div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="bg-red-900/20 border border-red-800 rounded-lg p-6 text-center">
        <p className="text-red-400">Error al cargar análisis ABC</p>
      </div>
    )
  }

  if (!analysis || analysis.length === 0) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6 text-center">
        <p className="text-neutral-400">No hay datos suficientes para análisis ABC</p>
        <p className="text-xs text-neutral-500 mt-2">
          El análisis requiere productos con inventario valorizado
        </p>
      </div>
    )
  }

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value)
  }

  // Agrupar por clasificación
  const classA = analysis.filter(item => item.classification === 'A')
  const classB = analysis.filter(item => item.classification === 'B')
  const classC = analysis.filter(item => item.classification === 'C')

  const totalValueA = classA.reduce((sum, item) => sum + item.totalValue, 0)
  const totalValueB = classB.reduce((sum, item) => sum + item.totalValue, 0)
  const totalValueC = classC.reduce((sum, item) => sum + item.totalValue, 0)
  const totalValue = totalValueA + totalValueB + totalValueC

  const chartData = [
    {
      name: 'Clase A',
      productos: classA.length,
      valor: totalValueA,
      porcentaje: ((totalValueA / totalValue) * 100).toFixed(1),
    },
    {
      name: 'Clase B',
      productos: classB.length,
      valor: totalValueB,
      porcentaje: ((totalValueB / totalValue) * 100).toFixed(1),
    },
    {
      name: 'Clase C',
      productos: classC.length,
      valor: totalValueC,
      porcentaje: ((totalValueC / totalValue) * 100).toFixed(1),
    },
  ]

  const colors = {
    A: '#22c55e', // green
    B: '#eab308', // yellow
    C: '#f97316', // orange
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
      <div className="mb-6">
        <h2 className="text-xl font-semibold text-neutral-100 mb-2">Análisis ABC de Inventario</h2>
        <p className="text-sm text-neutral-400">
          Clasificación por valor de inventario (Principio de Pareto 80-15-5)
        </p>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div className="bg-green-900/20 border border-green-800 rounded-lg p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm font-medium text-neutral-300">Clase A</span>
            <span className="px-2 py-1 bg-green-900/40 border border-green-800 rounded text-xs font-semibold text-green-400">
              {classA.length} productos
            </span>
          </div>
          <div className="text-2xl font-bold text-green-400 mb-1">
            {formatCurrency(totalValueA)}
          </div>
          <div className="text-xs text-neutral-400">
            {((totalValueA / totalValue) * 100).toFixed(1)}% del valor total
          </div>
          <div className="text-xs text-neutral-500 mt-2">Alta rotación - Control diario</div>
        </div>

        <div className="bg-yellow-900/20 border border-yellow-800 rounded-lg p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm font-medium text-neutral-300">Clase B</span>
            <span className="px-2 py-1 bg-yellow-900/40 border border-yellow-800 rounded text-xs font-semibold text-yellow-400">
              {classB.length} productos
            </span>
          </div>
          <div className="text-2xl font-bold text-yellow-400 mb-1">
            {formatCurrency(totalValueB)}
          </div>
          <div className="text-xs text-neutral-400">
            {((totalValueB / totalValue) * 100).toFixed(1)}% del valor total
          </div>
          <div className="text-xs text-neutral-500 mt-2">Rotación media - Control semanal</div>
        </div>

        <div className="bg-orange-900/20 border border-orange-800 rounded-lg p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm font-medium text-neutral-300">Clase C</span>
            <span className="px-2 py-1 bg-orange-900/40 border border-orange-800 rounded text-xs font-semibold text-orange-400">
              {classC.length} productos
            </span>
          </div>
          <div className="text-2xl font-bold text-orange-400 mb-1">
            {formatCurrency(totalValueC)}
          </div>
          <div className="text-xs text-neutral-400">
            {((totalValueC / totalValue) * 100).toFixed(1)}% del valor total
          </div>
          <div className="text-xs text-neutral-500 mt-2">Baja rotación - Control mensual</div>
        </div>
      </div>

      {/* Bar Chart */}
      <div className="mb-4">
        <h3 className="text-sm font-semibold text-neutral-300 mb-3">
          Distribución de Valor por Clasificación
        </h3>
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={chartData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#404040" />
            <XAxis dataKey="name" stroke="#a3a3a3" />
            <YAxis stroke="#a3a3a3" tickFormatter={value => formatCurrency(value)} />
            <Tooltip
              contentStyle={{
                backgroundColor: '#262626',
                border: '1px solid #404040',
                borderRadius: '8px',
              }}
              labelStyle={{ color: '#f5f5f5' }}
              formatter={(value: number) => formatCurrency(value)}
            />
            <Legend />
            <Bar dataKey="valor" name="Valor Total" radius={[8, 8, 0, 0]}>
              {chartData.map((entry, index) => (
                <Cell
                  key={`cell-${index}`}
                  fill={index === 0 ? colors.A : index === 1 ? colors.B : colors.C}
                />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* Quick Stats */}
      <div className="bg-neutral-800/50 border border-neutral-700 rounded-lg p-4">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-center">
          <div>
            <div className="text-xs text-neutral-400 mb-1">Total Productos</div>
            <div className="text-lg font-semibold text-neutral-100">{analysis.length}</div>
          </div>
          <div>
            <div className="text-xs text-neutral-400 mb-1">Valor Total Inventario</div>
            <div className="text-lg font-semibold text-neutral-100">
              {formatCurrency(totalValue)}
            </div>
          </div>
          <div>
            <div className="text-xs text-neutral-400 mb-1">Concentración Clase A</div>
            <div className="text-lg font-semibold text-green-400">
              {((classA.length / analysis.length) * 100).toFixed(1)}% productos
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

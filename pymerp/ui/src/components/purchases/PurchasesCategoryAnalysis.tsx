import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { listPurchases } from '../../services/client'
import { createCurrencyFormatter } from '../../utils/currency'
import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from 'recharts'

type PurchasesCategoryAnalysisProps = {
  startDate: string
  endDate: string
  statusFilter?: string
}

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#14b8a6']

export default function PurchasesCategoryAnalysis({
  startDate,
  endDate,
  statusFilter,
}: PurchasesCategoryAnalysisProps) {
  const currencyFormatter = useMemo(() => createCurrencyFormatter(), [])
  const formatCurrency = (value: number) => currencyFormatter.format(value ?? 0)

  const purchasesQuery = useQuery({
    queryKey: ['purchases-category', startDate, endDate, statusFilter],
    queryFn: async () => {
      const result = await listPurchases({
        page: 0,
        size: 10000,
        status: statusFilter || undefined,
        from: new Date(startDate + 'T00:00:00').toISOString(),
        to: new Date(endDate + 'T23:59:59').toISOString(),
      })
      return result.content ?? []
    },
  })

  const purchases = purchasesQuery.data ?? []

  const categoryData = useMemo(() => {
    const categoryMap = new Map<string, { total: number; count: number }>()

    purchases.forEach(p => {
      // Simular categorías basadas en el tipo de documento o proveedor
      // En un caso real, esto vendría de un campo `category` en el purchase
      const category = p.docType ?? 'General'
      const existing = categoryMap.get(category) ?? { total: 0, count: 0 }

      categoryMap.set(category, {
        total: existing.total + (p.total ?? 0),
        count: existing.count + 1,
      })
    })

    const categories = Array.from(categoryMap.entries()).map(([name, data]) => ({
      name,
      value: data.total,
      count: data.count,
    }))

    return categories.sort((a, b) => b.value - a.value)
  }, [purchases])

  const top5Categories = categoryData.slice(0, 5)
  const totalAmount = categoryData.reduce((sum, cat) => sum + cat.value, 0)

  if (purchasesQuery.isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
        <h3 className="text-neutral-100 mb-4">Análisis por Categoría</h3>
        <div className="animate-pulse bg-neutral-800 rounded-lg h-80"></div>
      </div>
    )
  }

  if (purchasesQuery.isError) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
        <h3 className="text-neutral-100 mb-4">Análisis por Categoría</h3>
        <div className="bg-red-950 border border-red-800 rounded-lg p-4">
          <p className="text-red-400">Error al cargar datos de categorías</p>
        </div>
      </div>
    )
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-neutral-100">Análisis por Categoría</h3>
        <span className="text-neutral-400 text-sm">Distribución de gastos</span>
      </div>

      {categoryData.length === 0 ? (
        <div className="text-center text-neutral-400 py-8">No hay datos de categorías</div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* PieChart */}
          <div>
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={top5Categories}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                  outerRadius={100}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {top5Categories.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#171717',
                    border: '1px solid #404040',
                    borderRadius: '0.5rem',
                    color: '#f5f5f5',
                  }}
                  formatter={(value: number) => formatCurrency(value)}
                />
                <Legend
                  wrapperStyle={{ color: '#a3a3a3', fontSize: '0.875rem' }}
                  iconType="circle"
                />
              </PieChart>
            </ResponsiveContainer>
          </div>

          {/* Lista Top 5 */}
          <div className="space-y-3">
            <h4 className="text-neutral-300 font-medium mb-3">Top 5 Categorías</h4>
            {top5Categories.map((category, index) => {
              const percentage = totalAmount > 0 ? (category.value / totalAmount) * 100 : 0

              return (
                <div
                  key={index}
                  className="bg-neutral-800 border border-neutral-700 rounded-lg p-3"
                >
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <div
                        className="w-3 h-3 rounded-full"
                        style={{ backgroundColor: COLORS[index % COLORS.length] }}
                      />
                      <span className="text-neutral-100 font-medium">{category.name}</span>
                    </div>
                    <span className="text-neutral-100 font-semibold">
                      {formatCurrency(category.value)}
                    </span>
                  </div>

                  <div className="mb-2">
                    <div
                      className="bg-neutral-700 rounded-full h-1.5"
                      style={{ overflow: 'hidden' }}
                    >
                      <div
                        className="h-1.5 transition-all duration-300"
                        style={{
                          width: `${percentage}%`,
                          backgroundColor: COLORS[index % COLORS.length],
                        }}
                      />
                    </div>
                  </div>

                  <div className="flex items-center justify-between text-xs text-neutral-400">
                    <span>{category.count} órdenes</span>
                    <span>{percentage.toFixed(1)}% del total</span>
                  </div>
                </div>
              )
            })}

            {categoryData.length > 5 && (
              <div className="text-neutral-400 text-sm text-center mt-3">
                +{categoryData.length - 5} categorías más
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

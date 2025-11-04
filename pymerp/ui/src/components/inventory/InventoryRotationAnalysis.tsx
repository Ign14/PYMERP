import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { listProducts } from '../../services/client'
import { createCurrencyFormatter } from '../../utils/currency'

export default function InventoryRotationAnalysis() {
  const currencyFormatter = useMemo(() => createCurrencyFormatter(), [])
  const formatCurrency = (value: number) => currencyFormatter.format(value ?? 0)

  const productsQuery = useQuery({
    queryKey: ['products-rotation'],
    queryFn: () => listProducts({ size: 200, status: 'all' }),
  })

  const products = productsQuery.data?.content ?? []

  const analysis = useMemo(() => {
    const productsWithValue = products.map(p => {
      const stock = Number(p.stock ?? 0)
      // Usar currentPrice del producto (precio de venta actual)
      const price = Number(p.currentPrice ?? 0)
      // Para el costo, deberíamos obtenerlo de los lotes de inventario
      // Por ahora, estimamos el costo como 70% del precio de venta (markup estándar 30%)
      const estimatedCost = price * 0.7
      const value = stock * estimatedCost
      // Simular rotación (en producción vendría de ventas)
      const rotation = Math.random() * 20 // Ventas simuladas

      return {
        id: p.id,
        name: p.name ?? 'Sin nombre',
        stock,
        value,
        rotation,
        category: p.category ?? 'General',
      }
    })

    const totalValue = productsWithValue.reduce((sum, p) => sum + p.value, 0)

    // Clasificación ABC
    const sortedByValue = [...productsWithValue].sort((a, b) => b.value - a.value)
    let accumulated = 0
    const abc = sortedByValue.map(p => {
      accumulated += p.value
      const percentage = totalValue > 0 ? (accumulated / totalValue) * 100 : 0
      return {
        ...p,
        class: percentage <= 80 ? 'A' : percentage <= 95 ? 'B' : 'C',
      }
    })

    const classA = abc.filter(p => p.class === 'A')
    const classB = abc.filter(p => p.class === 'B')
    const classC = abc.filter(p => p.class === 'C')

    // Top y Slow movers
    const topMovers = [...productsWithValue].sort((a, b) => b.rotation - a.rotation).slice(0, 5)
    const slowMovers = [...productsWithValue].sort((a, b) => a.rotation - b.rotation).slice(0, 5)

    return { classA, classB, classC, topMovers, slowMovers, totalValue }
  }, [products])

  if (productsQuery.isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
        <h3 className="text-neutral-100 mb-4">Análisis de Rotación</h3>
        <div className="animate-pulse bg-neutral-800 rounded-lg h-80"></div>
      </div>
    )
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
      <h3 className="text-neutral-100 mb-4">Análisis de Rotación (ABC)</h3>

      {/* Clasificación ABC */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div className="bg-green-950 border border-green-800 rounded-lg p-4">
          <h4 className="text-green-400 font-medium mb-2">📊 Clase A (80%)</h4>
          <p className="text-green-100 text-2xl font-bold">{analysis.classA.length}</p>
          <p className="text-green-300 text-sm">Productos críticos</p>
        </div>
        <div className="bg-yellow-950 border border-yellow-800 rounded-lg p-4">
          <h4 className="text-yellow-400 font-medium mb-2">📈 Clase B (15%)</h4>
          <p className="text-yellow-100 text-2xl font-bold">{analysis.classB.length}</p>
          <p className="text-yellow-300 text-sm">Productos importantes</p>
        </div>
        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
          <h4 className="text-neutral-300 font-medium mb-2">📉 Clase C (5%)</h4>
          <p className="text-neutral-100 text-2xl font-bold">{analysis.classC.length}</p>
          <p className="text-neutral-400 text-sm">Baja prioridad</p>
        </div>
      </div>

      {/* Top & Slow Movers */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div>
          <h4 className="text-neutral-300 font-medium mb-3">🔥 Top Movers</h4>
          <div className="space-y-2">
            {analysis.topMovers.map((p, idx) => (
              <div key={p.id} className="bg-neutral-800 border border-neutral-700 rounded-lg p-3">
                <div className="flex items-center justify-between">
                  <span className="text-neutral-100 font-medium">
                    {idx === 0 ? '🥇' : idx === 1 ? '🥈' : idx === 2 ? '🥉' : `${idx + 1}.`}{' '}
                    {p.name}
                  </span>
                  <span className="text-green-400 font-semibold">
                    {p.rotation.toFixed(1)} u/mes
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div>
          <h4 className="text-neutral-300 font-medium mb-3">🐌 Slow Movers</h4>
          <div className="space-y-2">
            {analysis.slowMovers.map(p => (
              <div key={p.id} className="bg-neutral-800 border border-neutral-700 rounded-lg p-3">
                <div className="flex items-center justify-between">
                  <span className="text-neutral-100 font-medium">{p.name}</span>
                  <span className="text-red-400 font-semibold">{p.rotation.toFixed(1)} u/mes</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="mt-4 bg-blue-950 border border-blue-800 rounded-lg p-3 text-blue-400 text-sm">
        💡 <strong>Insight:</strong> Clasificación ABC ayuda a priorizar gestión de inventario según
        valor
      </div>
    </div>
  )
}

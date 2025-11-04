import { useQuery } from '@tanstack/react-query'
import { listSuppliers, Supplier, getSupplierMetrics, SupplierMetrics } from '../services/client'
import { useMemo, useState } from 'react'

export default function SupplierPerformancePanel() {
  const [selectedSupplierId, setSelectedSupplierId] = useState<string | null>(null)

  const suppliersQuery = useQuery<Supplier[], Error>({
    queryKey: ['suppliers', true], // Solo activos
    queryFn: () => listSuppliers(undefined, true),
    refetchOnWindowFocus: false,
  })

  const metricsQuery = useQuery<SupplierMetrics, Error>({
    queryKey: ['supplier-metrics', selectedSupplierId],
    queryFn: () => getSupplierMetrics(selectedSupplierId!),
    enabled: !!selectedSupplierId,
    refetchOnWindowFocus: false,
  })

  const activeSuppliers = suppliersQuery.data ?? []
  const selectedSupplier = activeSuppliers.find(s => s.id === selectedSupplierId)

  // Calcular tendencia mes a mes
  const trend = useMemo(() => {
    if (!metricsQuery.data) return null
    const { purchasesLastMonth, purchasesPreviousMonth, amountLastMonth, amountPreviousMonth } =
      metricsQuery.data

    const purchasesChange =
      purchasesPreviousMonth > 0
        ? ((purchasesLastMonth - purchasesPreviousMonth) / purchasesPreviousMonth) * 100
        : 0

    const amountChange =
      amountPreviousMonth > 0
        ? ((amountLastMonth - amountPreviousMonth) / amountPreviousMonth) * 100
        : 0

    return { purchasesChange, amountChange }
  }, [metricsQuery.data])

  // Calcular días desde última compra
  const daysSinceLastPurchase = useMemo(() => {
    if (!metricsQuery.data?.lastPurchaseDate) return null
    const lastDate = new Date(metricsQuery.data.lastPurchaseDate)
    const today = new Date()
    const diffTime = Math.abs(today.getTime() - lastDate.getTime())
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24))
    return diffDays
  }, [metricsQuery.data])

  return (
    <div className="card-content">
      <h2 className="text-lg font-semibold text-neutral-100 mb-4">📈 Desempeño de Proveedores</h2>

      {/* Selector de proveedor */}
      <div className="mb-4">
        <label className="block text-sm font-medium text-neutral-300 mb-2">
          Selecciona un proveedor
        </label>
        <select
          value={selectedSupplierId ?? ''}
          onChange={e => setSelectedSupplierId(e.target.value || null)}
          className="w-full px-3 py-2 bg-neutral-900 border border-neutral-700 rounded-lg text-neutral-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">-- Seleccionar proveedor --</option>
          {activeSuppliers.map(supplier => (
            <option key={supplier.id} value={supplier.id}>
              {supplier.name}
            </option>
          ))}
        </select>
      </div>

      {/* Estado de carga */}
      {!selectedSupplierId && (
        <div className="text-center py-8 text-neutral-400">
          <p className="text-sm">Selecciona un proveedor para ver sus métricas de desempeño</p>
        </div>
      )}

      {selectedSupplierId && metricsQuery.isLoading && (
        <div className="text-center py-8 text-neutral-400">
          <p className="text-sm">Cargando métricas...</p>
        </div>
      )}

      {selectedSupplierId && metricsQuery.isError && (
        <div className="rounded-lg border border-red-800 bg-red-950/30 p-4 text-sm text-red-400">
          Error al cargar métricas: {metricsQuery.error.message}
        </div>
      )}

      {/* Métricas */}
      {selectedSupplierId && metricsQuery.data && (
        <div className="space-y-4">
          {/* Header con nombre del proveedor */}
          <div className="rounded-lg border border-neutral-800 bg-neutral-900/50 p-4">
            <h3 className="text-base font-semibold text-neutral-100">{selectedSupplier?.name}</h3>
            {selectedSupplier?.businessActivity && (
              <p className="text-xs text-neutral-400 mt-1">{selectedSupplier.businessActivity}</p>
            )}
          </div>

          {/* KPIs principales */}
          <div className="grid grid-cols-2 gap-3">
            {/* Total de compras */}
            <div className="rounded-lg border border-neutral-800 bg-neutral-900/50 p-3">
              <div className="text-xs text-neutral-400 mb-1">Total Compras</div>
              <div className="text-xl font-bold text-neutral-100">
                {metricsQuery.data.totalPurchases}
              </div>
              <div className="text-xs text-neutral-500 mt-1">Históricas</div>
            </div>

            {/* Monto total */}
            <div className="rounded-lg border border-green-800 bg-green-950/30 p-3">
              <div className="text-xs text-green-400 mb-1">Monto Total</div>
              <div className="text-xl font-bold text-green-300">
                ${metricsQuery.data.totalAmount.toLocaleString('es-CL')}
              </div>
              <div className="text-xs text-green-500 mt-1">Acumulado</div>
            </div>

            {/* Valor promedio por orden */}
            <div className="rounded-lg border border-blue-800 bg-blue-950/30 p-3">
              <div className="text-xs text-blue-400 mb-1">Promedio por Orden</div>
              <div className="text-xl font-bold text-blue-300">
                ${metricsQuery.data.averageOrderValue.toLocaleString('es-CL')}
              </div>
              <div className="text-xs text-blue-500 mt-1">AOV</div>
            </div>

            {/* Última compra */}
            <div
              className={`rounded-lg border p-3 ${
                daysSinceLastPurchase && daysSinceLastPurchase > 90
                  ? 'border-yellow-800 bg-yellow-950/30'
                  : 'border-neutral-800 bg-neutral-900/50'
              }`}
            >
              <div
                className={`text-xs mb-1 ${
                  daysSinceLastPurchase && daysSinceLastPurchase > 90
                    ? 'text-yellow-400'
                    : 'text-neutral-400'
                }`}
              >
                Última Compra
              </div>
              <div
                className={`text-lg font-bold ${
                  daysSinceLastPurchase && daysSinceLastPurchase > 90
                    ? 'text-yellow-300'
                    : 'text-neutral-100'
                }`}
              >
                {metricsQuery.data.lastPurchaseDate
                  ? new Date(metricsQuery.data.lastPurchaseDate).toLocaleDateString('es-CL')
                  : 'Sin compras'}
              </div>
              <div
                className={`text-xs mt-1 ${
                  daysSinceLastPurchase && daysSinceLastPurchase > 90
                    ? 'text-yellow-500'
                    : 'text-neutral-500'
                }`}
              >
                {daysSinceLastPurchase !== null ? `Hace ${daysSinceLastPurchase} días` : 'N/A'}
              </div>
            </div>
          </div>

          {/* Actividad mensual */}
          <div className="rounded-lg border border-neutral-800 bg-neutral-900/50 p-4">
            <h4 className="text-sm font-medium text-neutral-300 mb-3">Actividad Reciente</h4>

            <div className="space-y-3">
              {/* Último mes */}
              <div>
                <div className="flex items-center justify-between mb-1">
                  <span className="text-xs text-neutral-400">Último mes</span>
                  {trend && trend.purchasesChange !== 0 && (
                    <span
                      className={`text-xs font-medium ${
                        trend.purchasesChange > 0 ? 'text-green-400' : 'text-red-400'
                      }`}
                    >
                      {trend.purchasesChange > 0 ? '↑' : '↓'}{' '}
                      {Math.abs(Math.round(trend.purchasesChange))}%
                    </span>
                  )}
                </div>
                <div className="flex items-baseline gap-3">
                  <span className="text-sm text-neutral-300">
                    {metricsQuery.data.purchasesLastMonth} compra
                    {metricsQuery.data.purchasesLastMonth !== 1 ? 's' : ''}
                  </span>
                  <span className="text-sm font-medium text-neutral-100">
                    ${metricsQuery.data.amountLastMonth.toLocaleString('es-CL')}
                  </span>
                </div>
              </div>

              {/* Mes anterior */}
              <div>
                <div className="flex items-center justify-between mb-1">
                  <span className="text-xs text-neutral-400">Mes anterior</span>
                </div>
                <div className="flex items-baseline gap-3">
                  <span className="text-sm text-neutral-300">
                    {metricsQuery.data.purchasesPreviousMonth} compra
                    {metricsQuery.data.purchasesPreviousMonth !== 1 ? 's' : ''}
                  </span>
                  <span className="text-sm font-medium text-neutral-100">
                    ${metricsQuery.data.amountPreviousMonth.toLocaleString('es-CL')}
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Alertas */}
          {daysSinceLastPurchase && daysSinceLastPurchase > 90 && (
            <div className="rounded-lg border border-yellow-800 bg-yellow-950/30 p-3 text-sm text-yellow-400">
              <div className="flex items-start gap-2">
                <span>⚠️</span>
                <p>Sin compras hace {daysSinceLastPurchase} días - Considerar contacto</p>
              </div>
            </div>
          )}

          {metricsQuery.data.totalPurchases === 0 && (
            <div className="rounded-lg border border-blue-800 bg-blue-950/30 p-3 text-sm text-blue-400">
              <div className="flex items-start gap-2">
                <span>ℹ️</span>
                <p>Este proveedor no tiene compras registradas aún</p>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

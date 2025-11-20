import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  getSupplierForecast,
  listSuppliers,
  type Supplier,
  type PurchaseForecast as PurchaseForecastType,
} from '../services/client'

export default function PurchaseForecast() {
  const [selectedSupplierId, setSelectedSupplierId] = useState<string>('')

  // Lista de proveedores para selector
  const { data: suppliers = [] } = useQuery({
    queryKey: ['suppliers', { active: true }],
    queryFn: () => listSuppliers(undefined, true),
  })

  // Forecast del proveedor seleccionado
  const {
    data: forecast,
    isLoading,
    error,
  } = useQuery({
    queryKey: ['supplier-forecast', selectedSupplierId],
    queryFn: () => getSupplierForecast(selectedSupplierId),
    enabled: !!selectedSupplierId,
  })

  // Calcular totales
  const historicalMonths = forecast?.monthlyForecasts.filter(m => !m.isForecast) ?? []
  const forecastMonths = forecast?.monthlyForecasts.filter(m => m.isForecast) ?? []

  const totalHistorical = historicalMonths.reduce((sum, m) => sum + (m.actualSpend ?? 0), 0)
  const totalForecast = forecastMonths.reduce((sum, m) => sum + (m.forecastSpend ?? 0), 0)

  // Iconos de tendencia
  const trendIcon =
    forecast?.trend === 'INCREASING' ? '📈' : forecast?.trend === 'DECREASING' ? '📉' : '➡️'

  const trendColor =
    forecast?.trend === 'INCREASING'
      ? 'text-green-400'
      : forecast?.trend === 'DECREASING'
        ? 'text-red-400'
        : 'text-blue-400'

  const trendLabel =
    forecast?.trend === 'INCREASING'
      ? 'En Aumento'
      : forecast?.trend === 'DECREASING'
        ? 'Decreciente'
        : 'Estable'

  return (
    <div className="rounded-lg border border-neutral-800 bg-neutral-900 p-6">
      <h2 className="mb-4 text-lg font-medium text-neutral-100">
        📊 Forecast de Compras por Proveedor
      </h2>

      {/* Selector de proveedor */}
      <div className="mb-6">
        <label className="mb-2 block text-sm text-neutral-400">Seleccionar Proveedor</label>
        <select
          value={selectedSupplierId}
          onChange={e => setSelectedSupplierId(e.target.value)}
          className="w-full rounded border border-neutral-700 bg-neutral-800 px-3 py-2 text-neutral-100 focus:border-blue-500 focus:outline-none"
        >
          <option value="">-- Seleccione un proveedor --</option>
          {suppliers.map((supplier: Supplier) => (
            <option key={supplier.id} value={supplier.id}>
              {supplier.name}
            </option>
          ))}
        </select>
      </div>

      {!selectedSupplierId && (
        <p className="text-center text-neutral-500">Seleccione un proveedor para ver el forecast</p>
      )}

      {selectedSupplierId && isLoading && (
        <p className="text-center text-neutral-400">Cargando forecast...</p>
      )}

      {selectedSupplierId && error && (
        <div className="text-center p-4 bg-red-950 border border-red-800 rounded">
          <p className="text-red-400 font-semibold mb-1">⚠️ Error al cargar forecast</p>
          <p className="text-sm text-neutral-300">
            {error instanceof Error ? error.message : 'Error desconocido'}
          </p>
        </div>
      )}

      {forecast && (
        <div className="space-y-6">
          {/* Stats resumen */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div className="rounded border border-neutral-800 bg-neutral-950 p-4">
              <p className="text-sm text-neutral-400">Promedio Mensual</p>
              <p className="mt-1 text-2xl font-semibold text-neutral-100">
                ${forecast.averageMonthlySpend.toLocaleString('es-CL')}
              </p>
            </div>

            <div className="rounded border border-neutral-800 bg-neutral-950 p-4">
              <p className="text-sm text-neutral-400">Proyección Próximo Mes</p>
              <p className="mt-1 text-2xl font-semibold text-blue-400">
                ${forecast.projectedNextMonthSpend.toLocaleString('es-CL')}
              </p>
            </div>

            <div className="rounded border border-neutral-800 bg-neutral-950 p-4">
              <p className="text-sm text-neutral-400">Órdenes Promedio</p>
              <p className="mt-1 text-2xl font-semibold text-neutral-100">
                {forecast.averageMonthlyOrders}
              </p>
            </div>

            <div className="rounded border border-neutral-800 bg-neutral-950 p-4">
              <p className="text-sm text-neutral-400">Tendencia</p>
              <p className={`mt-1 text-2xl font-semibold ${trendColor}`}>
                {trendIcon} {trendLabel}
              </p>
            </div>
          </div>

          {/* Gráfico de barras simple */}
          <div className="rounded border border-neutral-800 bg-neutral-950 p-4">
            <h3 className="mb-4 text-sm font-medium text-neutral-300">
              Gasto Mensual (Últimos 6 Meses + Proyección 3 Meses)
            </h3>

            <div className="space-y-3">
              {forecast.monthlyForecasts.map((month, idx) => {
                const spend = month.isForecast
                  ? (month.forecastSpend ?? 0)
                  : (month.actualSpend ?? 0)
                const maxSpend = Math.max(
                  ...forecast.monthlyForecasts.map(m =>
                    m.isForecast ? (m.forecastSpend ?? 0) : (m.actualSpend ?? 0)
                  )
                )
                const widthPercent = maxSpend > 0 ? (spend / maxSpend) * 100 : 0

                return (
                  <div key={idx} className="flex items-center gap-3">
                    <div className="w-20 text-right text-sm text-neutral-400">{month.month}</div>
                    <div className="flex-1">
                      <div
                        className={`rounded px-3 py-2 text-sm ${
                          month.isForecast
                            ? 'bg-blue-500/30 text-blue-300'
                            : 'bg-green-500/30 text-green-300'
                        }`}
                        style={{ width: `${widthPercent}%` }}
                      >
                        ${spend.toLocaleString('es-CL')}
                        {month.isForecast && ' (proyectado)'}
                      </div>
                    </div>
                    {!month.isForecast && (
                      <div className="w-24 text-right text-xs text-neutral-500">
                        {month.actualOrders} órdenes
                      </div>
                    )}
                    {month.isForecast && (
                      <div className="w-24 text-right text-xs text-neutral-500">
                        ~{month.forecastOrders} órdenes
                      </div>
                    )}
                  </div>
                )
              })}
            </div>

            {/* Leyenda */}
            <div className="mt-4 flex gap-4 border-t border-neutral-800 pt-3 text-xs">
              <div className="flex items-center gap-2">
                <div className="h-3 w-3 rounded bg-green-500/30"></div>
                <span className="text-neutral-400">Real</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="h-3 w-3 rounded bg-blue-500/30"></div>
                <span className="text-neutral-400">Proyectado</span>
              </div>
            </div>
          </div>

          {/* Recomendación */}
          {forecast.recommendation && (
            <div className="rounded border border-blue-500/30 bg-blue-500/10 p-4">
              <p className="text-sm text-blue-300">
                💡 <strong>Recomendación:</strong> {forecast.recommendation}
              </p>
            </div>
          )}

          {/* Tabla detallada */}
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="border-b border-neutral-800 text-neutral-400">
                <tr>
                  <th className="pb-2 text-left font-medium">Mes</th>
                  <th className="pb-2 text-right font-medium">Gasto Real</th>
                  <th className="pb-2 text-right font-medium">Gasto Proyectado</th>
                  <th className="pb-2 text-right font-medium">Órdenes Reales</th>
                  <th className="pb-2 text-right font-medium">Órdenes Proyectadas</th>
                  <th className="pb-2 text-center font-medium">Tipo</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-800">
                {forecast.monthlyForecasts.map((month, idx) => (
                  <tr key={idx} className="text-neutral-300">
                    <td className="py-2">{month.month}</td>
                    <td className="py-2 text-right">
                      {month.actualSpend !== null
                        ? `$${month.actualSpend.toLocaleString('es-CL')}`
                        : '-'}
                    </td>
                    <td className="py-2 text-right">
                      {month.forecastSpend !== null
                        ? `$${month.forecastSpend.toLocaleString('es-CL')}`
                        : '-'}
                    </td>
                    <td className="py-2 text-right">
                      {month.actualOrders !== null ? month.actualOrders : '-'}
                    </td>
                    <td className="py-2 text-right">
                      {month.forecastOrders !== null ? month.forecastOrders : '-'}
                    </td>
                    <td className="py-2 text-center">
                      {month.isForecast ? (
                        <span className="rounded-full bg-blue-500/20 px-2 py-0.5 text-xs text-blue-400">
                          Proyección
                        </span>
                      ) : (
                        <span className="rounded-full bg-green-500/20 px-2 py-0.5 text-xs text-green-400">
                          Real
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
              <tfoot className="border-t-2 border-neutral-700 text-neutral-200">
                <tr>
                  <td className="pt-2 font-medium">Totales</td>
                  <td className="pt-2 text-right font-medium">
                    ${totalHistorical.toLocaleString('es-CL')}
                  </td>
                  <td className="pt-2 text-right font-medium">
                    ${totalForecast.toLocaleString('es-CL')}
                  </td>
                  <td className="pt-2 text-right font-medium">
                    {historicalMonths.reduce((sum, m) => sum + (m.actualOrders ?? 0), 0)}
                  </td>
                  <td className="pt-2 text-right font-medium">
                    {forecastMonths.reduce((sum, m) => sum + (m.forecastOrders ?? 0), 0)}
                  </td>
                  <td></td>
                </tr>
              </tfoot>
            </table>
          </div>

          {/* Ayuda expandible */}
          <details className="rounded border border-neutral-800 bg-neutral-950 p-4">
            <summary className="cursor-pointer text-sm text-neutral-400 hover:text-neutral-300">
              ¿Cómo funciona el forecast?
            </summary>
            <div className="mt-3 space-y-2 text-sm text-neutral-500">
              <p>
                El forecast analiza las compras de los últimos 6 meses para proyectar las
                necesidades futuras del proveedor seleccionado.
              </p>
              <ul className="list-inside list-disc space-y-1 pl-2">
                <li>
                  <strong>Tendencia:</strong> Compara los últimos 3 meses vs los 3 anteriores para
                  detectar si la demanda está aumentando, disminuyendo o estable.
                </li>
                <li>
                  <strong>Proyección:</strong> Usa promedios móviles simples ajustados por tendencia
                  para estimar los próximos 3 meses.
                </li>
                <li>
                  <strong>Precisión:</strong> Esta es una estimación estadística básica. Considere
                  factores estacionales y cambios de negocio al tomar decisiones.
                </li>
              </ul>
            </div>
          </details>
        </div>
      )}
    </div>
  )
}

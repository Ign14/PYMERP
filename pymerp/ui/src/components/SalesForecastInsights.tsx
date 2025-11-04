import { useQuery } from '@tanstack/react-query'
import { getSalesProductForecast } from '../services/client'

export default function SalesForecastInsights() {
  const { data: forecastData = [], isLoading } = useQuery({
    queryKey: ['salesProductForecast'],
    queryFn: () => getSalesProductForecast(),
    refetchInterval: 5 * 60 * 1000,
  })

  if (isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
        <div className="animate-pulse space-y-4">
          <div className="h-6 bg-neutral-800 rounded w-1/3"></div>
          <div className="h-32 bg-neutral-800 rounded"></div>
          <div className="h-32 bg-neutral-800 rounded"></div>
        </div>
      </div>
    )
  }

  // Análisis de insights
  const increasingTrend = forecastData.filter(item => item.trend === 'increasing')
  const decreasingTrend = forecastData.filter(item => item.trend === 'decreasing')
  const lowConfidence = forecastData.filter(item => item.confidence < 60)
  const highVolume = forecastData.filter(item => {
    const avgDemand =
      forecastData.reduce((sum, p) => sum + p.forecastedDemand, 0) / forecastData.length
    return item.forecastedDemand > avgDemand * 2
  })

  const totalHistorical = forecastData.reduce((sum, item) => sum + item.historicalAverage, 0)
  const totalForecasted = forecastData.reduce((sum, item) => sum + item.forecastedDemand, 0)
  const overallChange =
    totalHistorical > 0 ? ((totalForecasted - totalHistorical) / totalHistorical) * 100 : 0

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
      <h3 className="text-lg font-semibold text-white mb-6">Insights y Alertas de Pronóstico</h3>

      <div className="space-y-4 mb-6">
        {/* Alertas de tendencia creciente */}
        {increasingTrend.length > 0 && (
          <div className="bg-red-950 border border-red-900 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-3">
              <span className="text-lg">📈</span>
              <h4 className="text-base font-semibold text-red-100">
                Demanda Creciente ({increasingTrend.length} productos)
              </h4>
            </div>
            <p className="text-sm text-red-200 mb-3">
              Los siguientes productos muestran tendencia de aumento en la demanda. Considere
              incrementar stock para evitar quiebres.
            </p>
            <div className="space-y-2">
              {increasingTrend.slice(0, 5).map(item => {
                const increase =
                  item.historicalAverage > 0
                    ? ((item.forecastedDemand - item.historicalAverage) / item.historicalAverage) *
                      100
                    : 0
                return (
                  <div key={item.productId} className="flex items-center justify-between text-sm">
                    <span className="text-red-100">{item.productName}</span>
                    <span className="text-red-300 font-semibold">+{increase.toFixed(1)}%</span>
                  </div>
                )
              })}
            </div>
            <div className="mt-3 text-xs text-red-300 bg-red-900/30 rounded p-2">
              💡 <strong>Acción:</strong> Revisar contratos con proveedores y ajustar órdenes de
              compra anticipadamente.
            </div>
          </div>
        )}

        {/* Alertas de baja confianza */}
        {lowConfidence.length > 0 && (
          <div className="bg-amber-950 border border-amber-900 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-3">
              <span className="text-lg">⚠️</span>
              <h4 className="text-base font-semibold text-amber-100">
                Baja Confianza en Pronóstico ({lowConfidence.length} productos)
              </h4>
            </div>
            <p className="text-sm text-amber-200 mb-3">
              Estos productos tienen datos históricos limitados, lo que reduce la precisión del
              pronóstico.
            </p>
            <div className="space-y-2">
              {lowConfidence.slice(0, 5).map(item => (
                <div key={item.productId} className="flex items-center justify-between text-sm">
                  <span className="text-amber-100">{item.productName}</span>
                  <span className="text-amber-300 font-semibold">
                    Confianza: {item.confidence.toFixed(0)}%
                  </span>
                </div>
              ))}
            </div>
            <div className="mt-3 text-xs text-amber-300 bg-amber-900/30 rounded p-2">
              💡 <strong>Acción:</strong> Recopilar más datos históricos. Utilizar métodos de
              validación cruzada con ventas similares.
            </div>
          </div>
        )}

        {/* Alertas de tendencia decreciente */}
        {decreasingTrend.length > 0 && (
          <div className="bg-green-950 border border-green-900 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-3">
              <span className="text-lg">📉</span>
              <h4 className="text-base font-semibold text-green-100">
                Demanda Decreciente ({decreasingTrend.length} productos)
              </h4>
            </div>
            <p className="text-sm text-green-200 mb-3">
              Productos con disminución en demanda pronosticada. Evaluar causas y ajustar
              inventario.
            </p>
            <div className="space-y-2">
              {decreasingTrend.slice(0, 5).map(item => {
                const decrease =
                  item.historicalAverage > 0
                    ? ((item.forecastedDemand - item.historicalAverage) / item.historicalAverage) *
                      100
                    : 0
                return (
                  <div key={item.productId} className="flex items-center justify-between text-sm">
                    <span className="text-green-100">{item.productName}</span>
                    <span className="text-green-300 font-semibold">{decrease.toFixed(1)}%</span>
                  </div>
                )
              })}
            </div>
            <div className="mt-3 text-xs text-green-300 bg-green-900/30 rounded p-2">
              💡 <strong>Acción:</strong> Reducir pedidos futuros. Considerar promociones para
              liquidar excedentes.
            </div>
          </div>
        )}

        {/* Productos de alto volumen */}
        {highVolume.length > 0 && (
          <div className="bg-purple-950 border border-purple-900 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-3">
              <span className="text-lg">🔥</span>
              <h4 className="text-base font-semibold text-purple-100">
                Alto Volumen Pronosticado ({highVolume.length} productos)
              </h4>
            </div>
            <p className="text-sm text-purple-200 mb-3">
              Productos con demanda significativamente superior al promedio. Requieren atención
              prioritaria.
            </p>
            <div className="space-y-2">
              {highVolume.slice(0, 5).map(item => (
                <div key={item.productId} className="flex items-center justify-between text-sm">
                  <span className="text-purple-100">{item.productName}</span>
                  <span className="text-purple-300 font-semibold">
                    {item.forecastedDemand.toFixed(0)} u/mes
                  </span>
                </div>
              ))}
            </div>
            <div className="mt-3 text-xs text-purple-300 bg-purple-900/30 rounded p-2">
              💡 <strong>Acción:</strong> Asegurar disponibilidad con proveedores. Evaluar
              descuentos por volumen.
            </div>
          </div>
        )}
      </div>

      {/* Resumen global */}
      <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
        <h4 className="text-sm font-semibold text-white mb-3">📊 Resumen Global del Pronóstico</h4>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs text-neutral-400">Demanda Histórica Total</span>
              <span className="text-sm font-semibold text-white">
                {totalHistorical.toFixed(0)} u/mes
              </span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-xs text-neutral-400">Demanda Pronosticada Total</span>
              <span className="text-sm font-semibold text-white">
                {totalForecasted.toFixed(0)} u/mes
              </span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-xs text-neutral-400">Variación Global</span>
              <span
                className={`text-sm font-semibold ${
                  overallChange > 5
                    ? 'text-red-400'
                    : overallChange < -5
                      ? 'text-green-400'
                      : 'text-neutral-300'
                }`}
              >
                {overallChange > 0 ? '+' : ''}
                {overallChange.toFixed(1)}%
              </span>
            </div>
          </div>

          <div className="space-y-2">
            {overallChange > 10 && (
              <p className="text-xs text-red-300">
                ⚠️ Se espera un <strong>aumento significativo</strong> en la demanda global.
                Preparar recursos y capacidad operativa.
              </p>
            )}
            {overallChange >= -10 && overallChange <= 10 && (
              <p className="text-xs text-neutral-300">
                ℹ️ Demanda global <strong>estable</strong>. Mantener niveles actuales de inventario
                y operación.
              </p>
            )}
            {overallChange < -10 && (
              <p className="text-xs text-green-300">
                ℹ️ Se espera una <strong>disminución</strong> en la demanda global. Ajustar
                inventario y evaluar estrategias de marketing.
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Recomendaciones estratégicas */}
      <div className="mt-6 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-3">
          <div className="text-xs text-neutral-400 mb-1">Productos Analizados</div>
          <div className="text-xl font-bold text-white">{forecastData.length}</div>
        </div>
        <div className="bg-red-950 border border-red-900 rounded-lg p-3">
          <div className="text-xs text-red-300 mb-1">Demanda Creciente</div>
          <div className="text-xl font-bold text-white">{increasingTrend.length}</div>
        </div>
        <div className="bg-green-950 border border-green-900 rounded-lg p-3">
          <div className="text-xs text-green-300 mb-1">Demanda Decreciente</div>
          <div className="text-xl font-bold text-white">{decreasingTrend.length}</div>
        </div>
        <div className="bg-amber-950 border border-amber-900 rounded-lg p-3">
          <div className="text-xs text-amber-300 mb-1">Baja Confianza</div>
          <div className="text-xl font-bold text-white">{lowConfidence.length}</div>
        </div>
      </div>
    </div>
  )
}

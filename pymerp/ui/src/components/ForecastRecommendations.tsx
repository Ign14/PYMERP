import { useQuery } from "@tanstack/react-query";
import { getForecastAnalysis } from "../services/client";

export default function ForecastRecommendations() {
  const { data: forecasts, isLoading } = useQuery({
    queryKey: ["forecastAnalysis"],
    queryFn: () => getForecastAnalysis(),
    refetchInterval: 5 * 60 * 1000,
  });

  if (isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6 animate-pulse">
        <div className="h-8 bg-neutral-800 rounded w-1/3 mb-4"></div>
        <div className="space-y-3">
          <div className="h-20 bg-neutral-800 rounded"></div>
          <div className="h-20 bg-neutral-800 rounded"></div>
          <div className="h-20 bg-neutral-800 rounded"></div>
        </div>
      </div>
    );
  }

  if (!forecasts || forecasts.length === 0) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
        <p className="text-neutral-400">No hay recomendaciones disponibles</p>
      </div>
    );
  }

  // Productos críticos (stock bajo)
  const criticalProducts = forecasts
    .filter(f => f.stockStatus === "understocked")
    .sort((a, b) => a.daysOfStock - b.daysOfStock)
    .slice(0, 5);

  // Productos con tendencia creciente
  const growingProducts = forecasts
    .filter(f => f.trend === "increasing")
    .sort((a, b) => b.predictedDemand - a.predictedDemand)
    .slice(0, 3);

  // Productos sobre-stock
  const overstockedProducts = forecasts
    .filter(f => f.stockStatus === "overstocked")
    .sort((a, b) => b.daysOfStock - a.daysOfStock)
    .slice(0, 3);

  // Calcular orden de compra total recomendada
  const totalRecommendedOrder = forecasts
    .filter(f => f.recommendedOrderQty > 0)
    .reduce((sum, f) => sum + f.recommendedOrderQty, 0);

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6 space-y-6">
      <h3 className="text-xl font-semibold text-white">💡 Recomendaciones Estratégicas</h3>

      {/* Alertas Críticas */}
      {criticalProducts.length > 0 && (
        <div className="bg-red-900/20 border border-red-800 rounded-lg p-4 space-y-3">
          <div className="flex items-center gap-2">
            <span className="text-2xl">🚨</span>
            <h4 className="text-lg font-semibold text-red-400">Acción Urgente Requerida</h4>
          </div>
          <div className="space-y-2">
            {criticalProducts.map((product, index) => (
              <div key={index} className="bg-neutral-900/50 rounded-lg p-3 border border-red-800/30">
                <div className="flex justify-between items-start">
                  <div>
                    <div className="font-medium text-white">{product.productName}</div>
                    <div className="text-sm text-red-400 mt-1">
                      Stock actual: {product.currentStock.toFixed(0)} unidades ({product.daysOfStock} días)
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="text-sm text-neutral-400">Ordenar</div>
                    <div className="text-lg font-bold text-yellow-400">{product.recommendedOrderQty.toFixed(0)} unidades</div>
                  </div>
                </div>
              </div>
            ))}
          </div>
          <div className="pt-2 border-t border-red-800/30">
            <div className="text-sm text-red-300">
              ⚠️ Estos productos necesitan reposición inmediata para evitar quiebres de stock
            </div>
          </div>
        </div>
      )}

      {/* Productos en Tendencia Creciente */}
      {growingProducts.length > 0 && (
        <div className="bg-green-900/20 border border-green-800 rounded-lg p-4 space-y-3">
          <div className="flex items-center gap-2">
            <span className="text-2xl">📈</span>
            <h4 className="text-lg font-semibold text-green-400">Productos con Demanda Creciente</h4>
          </div>
          <div className="space-y-2">
            {growingProducts.map((product, index) => (
              <div key={index} className="flex justify-between items-center bg-neutral-900/50 rounded-lg p-3">
                <div>
                  <div className="font-medium text-white">{product.productName}</div>
                  <div className="text-sm text-green-400 mt-1">
                    Demanda predicha: {product.predictedDemand.toFixed(0)} unidades (↗️ Aumentando)
                  </div>
                </div>
                <div className="text-right">
                  <div className="text-sm text-neutral-400">Promedio diario</div>
                  <div className="text-white font-semibold">{product.historicalAverage.toFixed(1)}</div>
                </div>
              </div>
            ))}
          </div>
          <div className="pt-2 border-t border-green-800/30">
            <div className="text-sm text-green-300">
              💡 Considere aumentar niveles de stock de seguridad para estos productos
            </div>
          </div>
        </div>
      )}

      {/* Productos Sobre-Stock */}
      {overstockedProducts.length > 0 && (
        <div className="bg-orange-900/20 border border-orange-800 rounded-lg p-4 space-y-3">
          <div className="flex items-center gap-2">
            <span className="text-2xl">⚠️</span>
            <h4 className="text-lg font-semibold text-orange-400">Optimización de Inventario</h4>
          </div>
          <div className="space-y-2">
            {overstockedProducts.map((product, index) => (
              <div key={index} className="flex justify-between items-center bg-neutral-900/50 rounded-lg p-3">
                <div>
                  <div className="font-medium text-white">{product.productName}</div>
                  <div className="text-sm text-orange-400 mt-1">
                    Stock excesivo: {product.daysOfStock} días de inventario
                  </div>
                </div>
                <div className="text-right">
                  <div className="text-sm text-neutral-400">Stock actual</div>
                  <div className="text-white font-semibold">{product.currentStock.toFixed(0)}</div>
                </div>
              </div>
            ))}
          </div>
          <div className="pt-2 border-t border-orange-800/30">
            <div className="text-sm text-orange-300">
              💡 Evalúe promociones o redistribución para optimizar capital de trabajo
            </div>
          </div>
        </div>
      )}

      {/* Resumen de Orden de Compra */}
      {totalRecommendedOrder > 0 && (
        <div className="bg-blue-900/20 border border-blue-800 rounded-lg p-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="text-2xl">🛒</span>
              <div>
                <h4 className="text-lg font-semibold text-blue-400">Orden de Compra Sugerida</h4>
                <div className="text-sm text-neutral-400 mt-1">Total de unidades a ordenar</div>
              </div>
            </div>
            <div className="text-right">
              <div className="text-3xl font-bold text-blue-400">{totalRecommendedOrder.toFixed(0)}</div>
              <div className="text-sm text-neutral-400">unidades</div>
            </div>
          </div>
        </div>
      )}

      {/* Mejores Prácticas */}
      <div className="bg-neutral-800/50 border border-neutral-700 rounded-lg p-4 space-y-2">
        <h4 className="font-semibold text-white flex items-center gap-2">
          <span>📋</span>
          Mejores Prácticas de Gestión Predictiva
        </h4>
        <ul className="space-y-2 text-sm text-neutral-300">
          <li className="flex items-start gap-2">
            <span className="text-blue-400 mt-0.5">✓</span>
            <span>Revisar pronósticos semanalmente para ajustar niveles de stock</span>
          </li>
          <li className="flex items-start gap-2">
            <span className="text-blue-400 mt-0.5">✓</span>
            <span>Priorizar reposición de productos con menos de 15 días de inventario</span>
          </li>
          <li className="flex items-start gap-2">
            <span className="text-blue-400 mt-0.5">✓</span>
            <span>Considerar estacionalidad y eventos especiales en planificación</span>
          </li>
          <li className="flex items-start gap-2">
            <span className="text-blue-400 mt-0.5">✓</span>
            <span>Validar precisión del modelo comparando pronósticos con demanda real</span>
          </li>
          <li className="flex items-start gap-2">
            <span className="text-blue-400 mt-0.5">✓</span>
            <span>Optimizar frecuencia de pedidos para productos de alta rotación</span>
          </li>
        </ul>
      </div>
    </div>
  );
}

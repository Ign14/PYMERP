import { useQuery } from "@tanstack/react-query";
import { getPurchaseSupplierForecast } from "../../services/client";

export default function PurchaseForecastInsights() {
  const { data: forecasts } = useQuery({
    queryKey: ["purchaseSupplierForecast"],
    queryFn: () => getPurchaseSupplierForecast(),
    refetchInterval: 5 * 60 * 1000,
  });

  if (!forecasts || forecasts.length === 0) {
    return null;
  }

  const totalForecasted = forecasts.reduce((sum, f) => sum + f.forecastedSpending, 0);
  const totalHistorical = forecasts.reduce((sum, f) => sum + f.historicalAverage, 0);
  const globalVariation = totalHistorical > 0 ? ((totalForecasted - totalHistorical) / totalHistorical) * 100 : 0;

  const increasingTrends = forecasts.filter((f) => f.trend === "increasing");
  const decreasingTrends = forecasts.filter((f) => f.trend === "decreasing");
  const lowConfidence = forecasts.filter((f) => f.confidence < 60);
  const highSpenders = forecasts.filter((f) => f.forecastedSpending > totalForecasted / forecasts.length * 2);

  const formatCurrency = (value: number) => {
    return `$${value.toLocaleString("es-CL", { maximumFractionDigits: 0 })}`;
  };

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
      <h3 className="text-lg font-semibold text-white mb-6">🔍 Insights y Alertas del Pronóstico</h3>

      {/* Resumen Global */}
      <div className="bg-gradient-to-r from-blue-900/20 to-purple-900/20 border border-blue-800 rounded-lg p-5 mb-6">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <p className="text-neutral-400 text-sm mb-1">Gasto Histórico Mensual</p>
            <p className="text-2xl font-bold text-white">{formatCurrency(totalHistorical)}</p>
          </div>
          <div>
            <p className="text-neutral-400 text-sm mb-1">Pronóstico Próximo Mes</p>
            <p className="text-2xl font-bold text-white">{formatCurrency(totalForecasted)}</p>
          </div>
          <div>
            <p className="text-neutral-400 text-sm mb-1">Variación Esperada</p>
            <p className={`text-2xl font-bold ${globalVariation >= 0 ? "text-red-400" : "text-green-400"}`}>
              {globalVariation >= 0 ? "+" : ""}{globalVariation.toFixed(1)}%
            </p>
          </div>
        </div>
      </div>

      {/* Alertas */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
        {/* Tendencias Crecientes */}
        {increasingTrends.length > 0 && (
          <div className="bg-red-900/20 border border-red-800 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-3">
              <span className="text-2xl">⚠️</span>
              <div>
                <h4 className="text-red-400 font-semibold">Proveedores con Tendencia Creciente</h4>
                <p className="text-xs text-red-300">{increasingTrends.length} proveedores</p>
              </div>
            </div>
            <div className="space-y-2">
              {increasingTrends.slice(0, 3).map((f) => {
                const increase = f.historicalAverage > 0
                  ? ((f.forecastedSpending - f.historicalAverage) / f.historicalAverage) * 100
                  : 0;
                return (
                  <div key={f.supplierId} className="flex justify-between items-center text-sm">
                    <span className="text-red-200">{f.supplierName.substring(0, 25)}</span>
                    <span className="text-red-400 font-bold">+{increase.toFixed(0)}%</span>
                  </div>
                );
              })}
              {increasingTrends.length > 3 && (
                <p className="text-xs text-red-300 mt-2">Y {increasingTrends.length - 3} más...</p>
              )}
            </div>
            <div className="mt-3 pt-3 border-t border-red-800">
              <p className="text-xs text-red-200">
                💡 <strong>Acción:</strong> Revisar contratos y negociar mejores condiciones antes del aumento
              </p>
            </div>
          </div>
        )}

        {/* Baja Confianza */}
        {lowConfidence.length > 0 && (
          <div className="bg-amber-900/20 border border-amber-800 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-3">
              <span className="text-2xl">⚡</span>
              <div>
                <h4 className="text-amber-400 font-semibold">Pronósticos de Baja Confianza</h4>
                <p className="text-xs text-amber-300">{lowConfidence.length} proveedores</p>
              </div>
            </div>
            <div className="space-y-2">
              {lowConfidence.slice(0, 3).map((f) => (
                <div key={f.supplierId} className="flex justify-between items-center text-sm">
                  <span className="text-amber-200">{f.supplierName.substring(0, 25)}</span>
                  <span className="text-amber-400 font-bold">{f.confidence.toFixed(0)}%</span>
                </div>
              ))}
              {lowConfidence.length > 3 && (
                <p className="text-xs text-amber-300 mt-2">Y {lowConfidence.length - 3} más...</p>
              )}
            </div>
            <div className="mt-3 pt-3 border-t border-amber-800">
              <p className="text-xs text-amber-200">
                💡 <strong>Razón:</strong> Pocos datos históricos. Aumentar frecuencia de compras para mejorar predicción
              </p>
            </div>
          </div>
        )}

        {/* Tendencias Decrecientes */}
        {decreasingTrends.length > 0 && (
          <div className="bg-green-900/20 border border-green-800 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-3">
              <span className="text-2xl">✅</span>
              <div>
                <h4 className="text-green-400 font-semibold">Proveedores con Tendencia Decreciente</h4>
                <p className="text-xs text-green-300">{decreasingTrends.length} proveedores</p>
              </div>
            </div>
            <div className="space-y-2">
              {decreasingTrends.slice(0, 3).map((f) => {
                const decrease = f.historicalAverage > 0
                  ? ((f.forecastedSpending - f.historicalAverage) / f.historicalAverage) * 100
                  : 0;
                return (
                  <div key={f.supplierId} className="flex justify-between items-center text-sm">
                    <span className="text-green-200">{f.supplierName.substring(0, 25)}</span>
                    <span className="text-green-400 font-bold">{decrease.toFixed(0)}%</span>
                  </div>
                );
              })}
              {decreasingTrends.length > 3 && (
                <p className="text-xs text-green-300 mt-2">Y {decreasingTrends.length - 3} más...</p>
              )}
            </div>
            <div className="mt-3 pt-3 border-t border-green-800">
              <p className="text-xs text-green-200">
                💡 <strong>Oportunidad:</strong> Optimización exitosa o reducción de demanda. Evaluar causas
              </p>
            </div>
          </div>
        )}

        {/* Grandes Gastadores */}
        {highSpenders.length > 0 && (
          <div className="bg-purple-900/20 border border-purple-800 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-3">
              <span className="text-2xl">💰</span>
              <div>
                <h4 className="text-purple-400 font-semibold">Proveedores de Alto Gasto</h4>
                <p className="text-xs text-purple-300">{highSpenders.length} proveedores</p>
              </div>
            </div>
            <div className="space-y-2">
              {highSpenders.slice(0, 3).map((f) => (
                <div key={f.supplierId} className="flex justify-between items-center text-sm">
                  <span className="text-purple-200">{f.supplierName.substring(0, 25)}</span>
                  <span className="text-purple-400 font-bold">{formatCurrency(f.forecastedSpending)}</span>
                </div>
              ))}
              {highSpenders.length > 3 && (
                <p className="text-xs text-purple-300 mt-2">Y {highSpenders.length - 3} más...</p>
              )}
            </div>
            <div className="mt-3 pt-3 border-t border-purple-800">
              <p className="text-xs text-purple-200">
                💡 <strong>Recomendación:</strong> Negociar descuentos por volumen o contratos anuales
              </p>
            </div>
          </div>
        )}
      </div>

      {/* Recomendaciones Generales */}
      <div className="bg-gradient-to-br from-blue-900/30 to-blue-800/20 border border-blue-800 rounded-lg p-5">
        <h4 className="text-white font-semibold mb-4 flex items-center gap-2">
          <span>📋</span> Recomendaciones Estratégicas
        </h4>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
          <div className="bg-neutral-900/50 rounded-lg p-3">
            <p className="text-blue-300 font-medium mb-2">🎯 Planificación de Flujo de Caja</p>
            <p className="text-neutral-300">
              Basándose en el pronóstico total de {formatCurrency(totalForecasted)}, asegúrese de tener liquidez 
              suficiente para el próximo mes.
            </p>
          </div>
          <div className="bg-neutral-900/50 rounded-lg p-3">
            <p className="text-blue-300 font-medium mb-2">📊 Optimización de Inventario</p>
            <p className="text-neutral-300">
              Use las cantidades sugeridas como base para evitar sobre-stock o quiebres de inventario.
            </p>
          </div>
          <div className="bg-neutral-900/50 rounded-lg p-3">
            <p className="text-blue-300 font-medium mb-2">⏰ Programación de Compras</p>
            <p className="text-neutral-300">
              Revise las fechas estimadas de próxima compra para planificar pedidos con anticipación.
            </p>
          </div>
          <div className="bg-neutral-900/50 rounded-lg p-3">
            <p className="text-blue-300 font-medium mb-2">🔄 Actualización Continua</p>
            <p className="text-neutral-300">
              Los pronósticos se actualizan cada 5 minutos. Revise regularmente para decisiones informadas.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

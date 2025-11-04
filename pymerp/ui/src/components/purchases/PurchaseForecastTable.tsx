import { useQuery } from "@tanstack/react-query";
import { getPurchaseSupplierForecast } from "../../services/client";

export default function PurchaseForecastTable() {
  const { data: forecasts, isLoading } = useQuery({
    queryKey: ["purchaseSupplierForecast"],
    queryFn: () => getPurchaseSupplierForecast(),
    refetchInterval: 5 * 60 * 1000,
  });

  if (isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6 animate-pulse">
        <div className="h-8 bg-neutral-800 rounded w-1/3 mb-4"></div>
        <div className="space-y-3">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="h-16 bg-neutral-800 rounded"></div>
          ))}
        </div>
      </div>
    );
  }

  if (!forecasts || forecasts.length === 0) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-white mb-4">📊 Tabla de Pronósticos Detallada</h3>
        <p className="text-neutral-400 text-center py-8">No hay pronósticos disponibles</p>
      </div>
    );
  }

  const formatCurrency = (value: number) => {
    return `$${value.toLocaleString("es-CL", { maximumFractionDigits: 0 })}`;
  };

  const getTrendBadge = (trend: string) => {
    const styles = {
      increasing: "bg-red-900/30 text-red-400 border-red-800",
      stable: "bg-neutral-800 text-neutral-400 border-neutral-700",
      decreasing: "bg-green-900/30 text-green-400 border-green-800",
    };
    const labels = {
      increasing: "↗️ Creciente",
      stable: "→ Estable",
      decreasing: "↘️ Decreciente",
    };
    return {
      style: styles[trend as keyof typeof styles] || styles.stable,
      label: labels[trend as keyof typeof labels] || trend,
    };
  };

  const getConfidenceColor = (confidence: number) => {
    if (confidence >= 80) return "text-green-400";
    if (confidence >= 60) return "text-amber-400";
    return "text-red-400";
  };

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
      <div className="flex items-center justify-between mb-6">
        <h3 className="text-lg font-semibold text-white">📊 Pronósticos Detallados por Proveedor</h3>
        <span className="text-sm text-neutral-400">{forecasts.length} proveedores analizados</span>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="border-b border-neutral-800">
              <th className="text-left py-3 px-4 text-sm font-semibold text-neutral-400">Proveedor</th>
              <th className="text-right py-3 px-4 text-sm font-semibold text-neutral-400">Promedio Histórico</th>
              <th className="text-right py-3 px-4 text-sm font-semibold text-neutral-400">Pronóstico</th>
              <th className="text-center py-3 px-4 text-sm font-semibold text-neutral-400">Tendencia</th>
              <th className="text-center py-3 px-4 text-sm font-semibold text-neutral-400">Confianza</th>
              <th className="text-center py-3 px-4 text-sm font-semibold text-neutral-400">Próxima Compra</th>
              <th className="text-right py-3 px-4 text-sm font-semibold text-neutral-400">Cantidad Sugerida</th>
            </tr>
          </thead>
          <tbody>
            {forecasts.map((forecast, index) => {
              const trendBadge = getTrendBadge(forecast.trend);
              const variation = forecast.historicalAverage > 0
                ? ((forecast.forecastedSpending - forecast.historicalAverage) / forecast.historicalAverage) * 100
                : 0;

              return (
                <tr
                  key={forecast.supplierId}
                  className={`border-b border-neutral-800 hover:bg-neutral-800/50 transition-colors ${
                    index >= 10 ? "opacity-60" : ""
                  }`}
                >
                  <td className="py-3 px-4">
                    <div className="text-white font-medium">{forecast.supplierName}</div>
                    <div className="text-xs text-neutral-500">ID: {forecast.supplierId.substring(0, 8)}</div>
                  </td>
                  <td className="py-3 px-4 text-right">
                    <span className="text-neutral-300">{formatCurrency(forecast.historicalAverage)}</span>
                    <div className="text-xs text-neutral-500">por mes</div>
                  </td>
                  <td className="py-3 px-4 text-right">
                    <span className="text-white font-semibold">{formatCurrency(forecast.forecastedSpending)}</span>
                    <div className={`text-xs ${variation >= 0 ? "text-red-400" : "text-green-400"}`}>
                      {variation >= 0 ? "+" : ""}{variation.toFixed(1)}%
                    </div>
                  </td>
                  <td className="py-3 px-4 text-center">
                    <span className={`inline-flex px-2 py-1 rounded text-xs font-medium border ${trendBadge.style}`}>
                      {trendBadge.label}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-center">
                    <span className={`font-bold ${getConfidenceColor(forecast.confidence)}`}>
                      {forecast.confidence.toFixed(0)}%
                    </span>
                  </td>
                  <td className="py-3 px-4 text-center">
                    <span className="text-neutral-300 text-sm">
                      {new Date(forecast.nextPurchaseDate).toLocaleDateString("es-CL")}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-right">
                    <span className="text-blue-400 font-medium">
                      {forecast.recommendedOrderQuantity.toFixed(2)} un.
                    </span>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {forecasts.length > 10 && (
        <div className="mt-4 text-center text-sm text-neutral-500">
          Mostrando todos los {forecasts.length} proveedores (primeros 10 resaltados)
        </div>
      )}
    </div>
  );
}

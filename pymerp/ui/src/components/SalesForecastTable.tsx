import { useQuery } from "@tanstack/react-query";
import { getSalesProductForecast } from "../services/client";

export default function SalesForecastTable() {
  const { data: forecastData = [], isLoading } = useQuery({
    queryKey: ["salesProductForecast"],
    queryFn: () => getSalesProductForecast(),
    refetchInterval: 5 * 60 * 1000,
  });

  if (isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
        <div className="animate-pulse">
          <div className="h-6 bg-neutral-800 rounded w-1/3 mb-4"></div>
          <div className="space-y-3">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="h-12 bg-neutral-800 rounded"></div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  const getTrendBadge = (trend: string) => {
    if (trend === "increasing") {
      return <span className="px-2 py-1 rounded text-xs font-semibold bg-red-950 border-red-900 text-red-200">↗ Creciente</span>;
    }
    if (trend === "decreasing") {
      return <span className="px-2 py-1 rounded text-xs font-semibold bg-green-950 border-green-900 text-green-200">↘ Decreciente</span>;
    }
    return <span className="px-2 py-1 rounded text-xs font-semibold bg-neutral-800 border-neutral-700 text-neutral-300">→ Estable</span>;
  };

  const getConfidenceBadge = (confidence: number) => {
    if (confidence >= 70) {
      return <span className="text-green-400 font-semibold">{confidence.toFixed(0)}%</span>;
    }
    if (confidence >= 50) {
      return <span className="text-amber-400 font-semibold">{confidence.toFixed(0)}%</span>;
    }
    return <span className="text-red-400 font-semibold">{confidence.toFixed(0)}%</span>;
  };

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
      <div className="flex items-center justify-between mb-6">
        <h3 className="text-lg font-semibold text-white">Pronóstico Detallado de Productos</h3>
        <span className="text-sm text-neutral-400">{forecastData.length} productos</span>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-neutral-800">
              <th className="text-left py-3 px-2 text-neutral-400 font-medium">Producto</th>
              <th className="text-right py-3 px-2 text-neutral-400 font-medium">Histórico</th>
              <th className="text-right py-3 px-2 text-neutral-400 font-medium">Pronóstico</th>
              <th className="text-right py-3 px-2 text-neutral-400 font-medium">Variación</th>
              <th className="text-center py-3 px-2 text-neutral-400 font-medium">Tendencia</th>
              <th className="text-center py-3 px-2 text-neutral-400 font-medium">Confianza</th>
              <th className="text-right py-3 px-2 text-neutral-400 font-medium">Stock Rec.</th>
              <th className="text-left py-3 px-2 text-neutral-400 font-medium">Próxima Venta</th>
            </tr>
          </thead>
          <tbody>
            {forecastData.map((item, index) => {
              const variation = item.historicalAverage > 0 
                ? ((item.forecastedDemand - item.historicalAverage) / item.historicalAverage) * 100 
                : 0;
              const isTopTen = index < 10;

              return (
                <tr
                  key={item.productId}
                  className={`border-b border-neutral-800 hover:bg-neutral-800/50 transition-colors ${
                    isTopTen ? "bg-neutral-800/30" : ""
                  }`}
                >
                  <td className="py-3 px-2">
                    <div className="flex flex-col">
                      <span className="text-white font-medium">{item.productName}</span>
                      <span className="text-xs text-neutral-400">ID: {item.productId}</span>
                    </div>
                  </td>
                  <td className="py-3 px-2 text-right text-neutral-300">
                    {item.historicalAverage.toFixed(1)} u/mes
                  </td>
                  <td className="py-3 px-2 text-right text-white font-medium">
                    {item.forecastedDemand.toFixed(1)} u/mes
                  </td>
                  <td className="py-3 px-2 text-right">
                    <span className={`font-semibold ${
                      variation > 10 ? "text-red-400" : 
                      variation < -10 ? "text-green-400" : "text-neutral-400"
                    }`}>
                      {variation > 0 ? "+" : ""}{variation.toFixed(1)}%
                    </span>
                  </td>
                  <td className="py-3 px-2 text-center">
                    {getTrendBadge(item.trend)}
                  </td>
                  <td className="py-3 px-2 text-center">
                    {getConfidenceBadge(item.confidence)}
                  </td>
                  <td className="py-3 px-2 text-right text-neutral-300">
                    {item.recommendedStock.toFixed(0)} u
                  </td>
                  <td className="py-3 px-2 text-neutral-400 text-xs">
                    {item.nextSaleDate ? new Date(item.nextSaleDate).toLocaleDateString("es-CL") : "N/A"}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {forecastData.length === 0 && (
        <div className="text-center py-12 text-neutral-500">
          No hay datos de pronóstico disponibles
        </div>
      )}

      {forecastData.length > 0 && (
        <div className="mt-4 text-xs text-neutral-500 flex items-center gap-2">
          <div className="w-3 h-3 bg-neutral-800/30 rounded"></div>
          <span>Top 10 productos destacados</span>
        </div>
      )}
    </div>
  );
}

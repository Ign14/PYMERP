import { useQuery } from "@tanstack/react-query";
import { getForecastAnalysis } from "../services/client";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, Cell } from "recharts";

export default function ForecastChart() {
  const { data: forecasts, isLoading, error } = useQuery({
    queryKey: ["forecastAnalysis"],
    queryFn: () => getForecastAnalysis(),
    refetchInterval: 5 * 60 * 1000, // 5 minutos
  });

  if (isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6 animate-pulse">
        <div className="h-8 bg-neutral-800 rounded w-1/3 mb-4"></div>
        <div className="h-64 bg-neutral-800 rounded"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
        <p className="text-red-400">Error al cargar pronósticos: {String(error)}</p>
      </div>
    );
  }

  if (!forecasts || forecasts.length === 0) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
        <p className="text-neutral-400">No hay datos suficientes para generar pronósticos</p>
      </div>
    );
  }

  // Estadísticas de resumen
  const totalProducts = forecasts.length;
  const understocked = forecasts.filter(f => f.stockStatus === "understocked").length;
  const overstocked = forecasts.filter(f => f.stockStatus === "overstocked").length;
  const optimal = forecasts.filter(f => f.stockStatus === "optimal").length;

  const totalPredictedDemand = forecasts.reduce((sum, f) => sum + f.predictedDemand, 0);
  const totalCurrentStock = forecasts.reduce((sum, f) => sum + f.currentStock, 0);
  const totalRecommendedOrder = forecasts.reduce((sum, f) => sum + f.recommendedOrderQty, 0);

  const avgConfidence = forecasts.reduce((sum, f) => sum + f.confidence, 0) / totalProducts;

  // Preparar datos para gráfico (Top 10 productos con mayor demanda predicha)
  const chartData = forecasts
    .slice(0, 10)
    .map(f => ({
      name: f.productName.length > 20 ? f.productName.substring(0, 20) + "..." : f.productName,
      demandaPredicha: f.predictedDemand,
      stockActual: f.currentStock,
      status: f.stockStatus,
    }));

  const getStatusColor = (status: string) => {
    switch (status) {
      case "understocked": return "#ef4444"; // red-500
      case "overstocked": return "#f97316"; // orange-500
      case "optimal": return "#22c55e"; // green-500
      default: return "#6b7280"; // neutral-500
    }
  };

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6 space-y-6">
      <h3 className="text-xl font-semibold text-white">📈 Pronóstico de Demanda (Próximos 30 días)</h3>

      {/* Estadísticas de resumen */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-neutral-800/50 border border-neutral-700 rounded-lg p-4">
          <div className="text-sm text-neutral-400">Productos Analizados</div>
          <div className="text-2xl font-bold text-white mt-1">{totalProducts}</div>
        </div>

        <div className="bg-red-900/20 border border-red-800 rounded-lg p-4">
          <div className="text-sm text-red-400">Stock Bajo</div>
          <div className="text-2xl font-bold text-red-400 mt-1">{understocked}</div>
          <div className="text-xs text-red-500 mt-1">Requieren reposición</div>
        </div>

        <div className="bg-green-900/20 border border-green-800 rounded-lg p-4">
          <div className="text-sm text-green-400">Stock Óptimo</div>
          <div className="text-2xl font-bold text-green-400 mt-1">{optimal}</div>
          <div className="text-xs text-green-500 mt-1">Niveles adecuados</div>
        </div>

        <div className="bg-orange-900/20 border border-orange-800 rounded-lg p-4">
          <div className="text-sm text-orange-400">Sobre-stock</div>
          <div className="text-2xl font-bold text-orange-400 mt-1">{overstocked}</div>
          <div className="text-xs text-orange-500 mt-1">Exceso de inventario</div>
        </div>
      </div>

      {/* Métricas adicionales */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-neutral-800/50 border border-neutral-700 rounded-lg p-4">
          <div className="text-sm text-neutral-400">Demanda Predicha Total</div>
          <div className="text-xl font-bold text-white mt-1">{totalPredictedDemand.toFixed(0)} unidades</div>
        </div>

        <div className="bg-neutral-800/50 border border-neutral-700 rounded-lg p-4">
          <div className="text-sm text-neutral-400">Stock Actual Total</div>
          <div className="text-xl font-bold text-white mt-1">{totalCurrentStock.toFixed(0)} unidades</div>
        </div>

        <div className="bg-neutral-800/50 border border-neutral-700 rounded-lg p-4">
          <div className="text-sm text-neutral-400">Orden Recomendada Total</div>
          <div className="text-xl font-bold text-blue-400 mt-1">{totalRecommendedOrder.toFixed(0)} unidades</div>
        </div>
      </div>

      {/* Gráfico de barras comparativo */}
      <div className="bg-neutral-800/30 rounded-lg p-4">
        <h4 className="text-lg font-semibold text-white mb-4">
          Top 10 Productos: Demanda Predicha vs Stock Actual
        </h4>
        <ResponsiveContainer width="100%" height={400}>
          <BarChart data={chartData} margin={{ top: 20, right: 30, left: 20, bottom: 60 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#404040" />
            <XAxis 
              dataKey="name" 
              angle={-45} 
              textAnchor="end" 
              height={100}
              stroke="#a3a3a3"
              style={{ fontSize: '12px' }}
            />
            <YAxis stroke="#a3a3a3" />
            <Tooltip 
              contentStyle={{ 
                backgroundColor: '#262626', 
                border: '1px solid #404040',
                borderRadius: '8px'
              }}
              labelStyle={{ color: '#fff' }}
            />
            <Legend 
              wrapperStyle={{ paddingTop: '20px' }}
              iconType="circle"
            />
            <Bar dataKey="demandaPredicha" name="Demanda Predicha" fill="#3b82f6" />
            <Bar dataKey="stockActual" name="Stock Actual">
              {chartData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={getStatusColor(entry.status)} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* Indicador de confianza */}
      <div className="bg-blue-900/20 border border-blue-800 rounded-lg p-4">
        <div className="flex items-center justify-between">
          <div>
            <div className="text-sm text-blue-400">Confianza Promedio del Modelo</div>
            <div className="text-xs text-neutral-400 mt-1">Basado en historial de 90 días</div>
          </div>
          <div className="text-3xl font-bold text-blue-400">{avgConfidence.toFixed(0)}%</div>
        </div>
      </div>
    </div>
  );
}

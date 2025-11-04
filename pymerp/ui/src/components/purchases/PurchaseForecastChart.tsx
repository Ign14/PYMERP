import { useQuery } from "@tanstack/react-query";
import { getPurchaseSupplierForecast } from "../../services/client";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from "recharts";

export default function PurchaseForecastChart() {
  const { data: forecasts, isLoading } = useQuery({
    queryKey: ["purchaseSupplierForecast"],
    queryFn: () => getPurchaseSupplierForecast(),
    refetchInterval: 5 * 60 * 1000,
  });

  if (isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6 h-96 animate-pulse">
        <div className="h-8 bg-neutral-800 rounded w-1/3 mb-4"></div>
        <div className="h-full bg-neutral-800 rounded"></div>
      </div>
    );
  }

  if (!forecasts || forecasts.length === 0) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-white mb-4">📈 Pronóstico de Gastos por Proveedor</h3>
        <p className="text-neutral-400 text-center py-8">No hay datos suficientes para generar pronósticos</p>
      </div>
    );
  }

  // Tomar los top 5 proveedores por gasto pronosticado
  const topForecasts = forecasts.slice(0, 5);

  // Preparar datos para el gráfico
  const chartData = topForecasts.map((f) => ({
    nombre: f.supplierName.length > 20 ? f.supplierName.substring(0, 20) + "..." : f.supplierName,
    historico: f.historicalAverage,
    pronostico: f.forecastedSpending,
    confianza: f.confidence,
  }));

  const formatCurrency = (value: number) => {
    return `$${value.toLocaleString("es-CL", { maximumFractionDigits: 0 })}`;
  };

  const getTrendIcon = (trend: string) => {
    if (trend === "increasing") return "↗️";
    if (trend === "decreasing") return "↘️";
    return "→";
  };

  const getTrendColor = (trend: string) => {
    if (trend === "increasing") return "text-red-400";
    if (trend === "decreasing") return "text-green-400";
    return "text-neutral-400";
  };

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
      <h3 className="text-lg font-semibold text-white mb-6">📈 Pronóstico de Gastos (Top 5 Proveedores)</h3>

      <div className="grid grid-cols-1 md:grid-cols-5 gap-3 mb-6">
        {topForecasts.map((forecast, idx) => (
          <div key={forecast.supplierId} className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
            <div className="text-xs text-neutral-400 mb-1">#{idx + 1} - {forecast.supplierName}</div>
            <div className="text-xl font-bold text-white mb-1">{formatCurrency(forecast.forecastedSpending)}</div>
            <div className={`text-sm font-medium ${getTrendColor(forecast.trend)}`}>
              {getTrendIcon(forecast.trend)} {forecast.trend}
            </div>
            <div className="text-xs text-neutral-500 mt-2">
              Confianza: {forecast.confidence.toFixed(0)}%
            </div>
          </div>
        ))}
      </div>

      <ResponsiveContainer width="100%" height={350}>
        <LineChart data={chartData}>
          <CartesianGrid strokeDasharray="3 3" stroke="#404040" />
          <XAxis dataKey="nombre" stroke="#a3a3a3" tick={{ fill: "#a3a3a3", fontSize: 12 }} />
          <YAxis stroke="#a3a3a3" tick={{ fill: "#a3a3a3" }} tickFormatter={(value) => formatCurrency(value)} />
          <Tooltip
            contentStyle={{ backgroundColor: "#171717", border: "1px solid #404040", borderRadius: "8px" }}
            labelStyle={{ color: "#fff" }}
            formatter={(value: number, name: string) => {
              if (name === "confianza") return `${value.toFixed(0)}%`;
              return formatCurrency(value);
            }}
          />
          <Legend wrapperStyle={{ color: "#a3a3a3" }} />
          <Line
            type="monotone"
            dataKey="historico"
            stroke="#3b82f6"
            strokeWidth={2}
            name="Promedio Histórico"
            dot={{ fill: "#3b82f6", r: 5 }}
          />
          <Line
            type="monotone"
            dataKey="pronostico"
            stroke="#f59e0b"
            strokeWidth={2}
            strokeDasharray="5 5"
            name="Pronóstico Próximo Mes"
            dot={{ fill: "#f59e0b", r: 5 }}
          />
        </LineChart>
      </ResponsiveContainer>

      <div className="mt-4 bg-purple-900/20 border border-purple-800 rounded-lg p-4">
        <p className="text-sm text-purple-300">
          💡 <strong>Algoritmo de Pronóstico:</strong> Usa media móvil de los últimos 90 días y análisis de tendencia 
          (comparando primera vs segunda mitad del período) para predecir gastos del próximo mes.
        </p>
      </div>
    </div>
  );
}

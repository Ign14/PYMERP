import { useMemo } from "react";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, ReferenceLine } from "recharts";

type ForecastPoint = {
  date: string;
  actual?: number;
  predicted: number;
  confidence: "high" | "medium" | "low";
};

type SalesForecastPanelProps = {
  days?: 7 | 30;
};

function formatCurrency(value: number): string {
  return `$${Math.round(value).toLocaleString("es-CL")}`;
}

// Algoritmo simple de pronóstico basado en promedio móvil y tendencia
function generateForecast(days: number): ForecastPoint[] {
  const today = new Date();
  const forecast: ForecastPoint[] = [];
  
  // Datos históricos simulados (últimos 7 días)
  const historical = [
    { date: formatDate(-7), actual: 2800000 },
    { date: formatDate(-6), actual: 3200000 },
    { date: formatDate(-5), actual: 2900000 },
    { date: formatDate(-4), actual: 3500000 },
    { date: formatDate(-3), actual: 3100000 },
    { date: formatDate(-2), actual: 3800000 },
    { date: formatDate(-1), actual: 3400000 },
  ];
  
  // Calcular tendencia (promedio de los últimos 7 días)
  const avg = historical.reduce((sum, p) => sum + p.actual, 0) / historical.length;
  const trend = (historical[historical.length - 1].actual - historical[0].actual) / historical.length;
  
  // Agregar históricos al forecast
  historical.forEach(h => {
    forecast.push({
      date: h.date,
      actual: h.actual,
      predicted: h.actual,
      confidence: "high",
    });
  });
  
  // Generar pronóstico futuro
  for (let i = 0; i < days; i++) {
    const predicted = avg + (trend * (historical.length + i)) + (Math.random() * 200000 - 100000);
    const confidence = i < 3 ? "high" : i < 7 ? "medium" : "low";
    
    forecast.push({
      date: formatDate(i),
      predicted: Math.max(0, predicted),
      confidence,
    });
  }
  
  return forecast;
}

function formatDate(daysOffset: number): string {
  const date = new Date();
  date.setDate(date.getDate() + daysOffset);
  return date.toLocaleDateString("es-CL", { day: "2-digit", month: "short" });
}

export default function SalesForecastPanel({ days = 7 }: SalesForecastPanelProps) {
  const forecast = useMemo(() => generateForecast(days), [days]);
  
  const historicalData = forecast.filter(f => f.actual !== undefined);
  const futureData = forecast.filter(f => f.actual === undefined);
  const totalPredicted = futureData.reduce((sum, f) => sum + f.predicted, 0);
  const avgPredicted = totalPredicted / futureData.length;
  const historicalAvg = historicalData.reduce((sum, h) => sum + (h.actual || 0), 0) / historicalData.length;
  const growthRate = ((avgPredicted - historicalAvg) / historicalAvg) * 100;

  const chartData = forecast.map(f => ({
    fecha: f.date,
    real: f.actual ? f.actual / 1000000 : null,
    proyeccion: f.predicted / 1000000,
  }));

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5 mb-6">
      <header className="mb-4">
        <h2 className="text-xl font-semibold text-neutral-100 mb-1">🔮 Pronóstico de Ventas</h2>
        <p className="text-sm text-neutral-400">
          Proyección próximos {days} días basada en tendencia histórica
        </p>
      </header>

      {/* KPIs del pronóstico */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
          <h3 className="text-xs font-medium text-neutral-400 mb-1">Ventas Proyectadas</h3>
          <p className="text-2xl font-bold text-neutral-100">{formatCurrency(totalPredicted)}</p>
          <p className="text-xs text-neutral-400 mt-1">Próximos {days} días</p>
        </div>
        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
          <h3 className="text-xs font-medium text-neutral-400 mb-1">Promedio Diario</h3>
          <p className="text-2xl font-bold text-neutral-100">{formatCurrency(avgPredicted)}</p>
          <p className="text-xs text-neutral-400 mt-1">Estimado futuro</p>
        </div>
        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
          <h3 className="text-xs font-medium text-neutral-400 mb-1">Crecimiento Esperado</h3>
          <p className={`text-2xl font-bold ${growthRate >= 0 ? "text-green-400" : "text-red-400"}`}>
            {growthRate >= 0 ? "+" : ""}{growthRate.toFixed(1)}%
          </p>
          <p className="text-xs text-neutral-400 mt-1">vs promedio histórico</p>
        </div>
        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
          <h3 className="text-xs font-medium text-neutral-400 mb-1">Confianza</h3>
          <div className="flex items-center gap-2 mt-1">
            {futureData.filter(f => f.confidence === "high").length > 0 && (
              <span className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs font-medium bg-green-950 text-green-400 border border-green-800">
                🟢 Alta
              </span>
            )}
            {futureData.filter(f => f.confidence === "medium").length > 0 && (
              <span className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs font-medium bg-yellow-950 text-yellow-400 border border-yellow-800">
                🟡 Media
              </span>
            )}
            {futureData.filter(f => f.confidence === "low").length > 0 && (
              <span className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs font-medium bg-red-950 text-red-400 border border-red-800">
                🔴 Baja
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Gráfico de pronóstico */}
      <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
        <div style={{ height: 320 }} role="img" aria-label="Gráfico de pronóstico de ventas">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={chartData} margin={{ top: 10, right: 10, left: 10, bottom: 10 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#404040" />
              <XAxis dataKey="fecha" stroke="#a3a3a3" tick={{ fill: "#a3a3a3", fontSize: 11 }} />
              <YAxis stroke="#a3a3a3" tick={{ fill: "#a3a3a3", fontSize: 12 }} />
              <Tooltip
                contentStyle={{
                  backgroundColor: "#171717",
                  border: "1px solid #404040",
                  borderRadius: "0.5rem",
                  color: "#f5f5f5",
                }}
                formatter={(value: number, name: string) => {
                  if (name === "real") return [`${value.toFixed(2)}M`, "Ventas reales"];
                  if (name === "proyeccion") return [`${value.toFixed(2)}M`, "Proyección"];
                  return [value, name];
                }}
              />
              {/* Línea de referencia entre histórico y proyección */}
              <ReferenceLine
                x={historicalData[historicalData.length - 1]?.date}
                stroke="#fb923c"
                strokeDasharray="5 5"
                strokeWidth={2}
              />
              {/* Línea de ventas reales */}
              <Line
                type="monotone"
                dataKey="real"
                stroke="#22d3ee"
                strokeWidth={3}
                dot={{ r: 5, fill: "#0891b2", stroke: "#22d3ee", strokeWidth: 2 }}
                connectNulls={false}
              />
              {/* Línea de proyección */}
              <Line
                type="monotone"
                dataKey="proyeccion"
                stroke="#a78bfa"
                strokeWidth={2}
                strokeDasharray="5 5"
                dot={{ r: 4, fill: "#7c3aed", stroke: "#a78bfa", strokeWidth: 2 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Advertencias y notas */}
      <div className="mt-4 bg-blue-950 border border-blue-800 rounded-lg p-4">
        <div className="flex items-start gap-3">
          <span className="text-lg">ℹ️</span>
          <div className="flex-1">
            <p className="text-sm font-medium text-blue-400 mb-1">Acerca del pronóstico</p>
            <ul className="text-xs text-blue-300 space-y-1">
              <li>• Basado en promedio móvil de últimos 7 días y tendencia detectada</li>
              <li>• La confianza disminuye con días más lejanos (alta: 1-3 días, media: 4-7 días, baja: 8+ días)</li>
              <li>• Factores externos (promociones, temporadas, eventos) pueden afectar la precisión</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}

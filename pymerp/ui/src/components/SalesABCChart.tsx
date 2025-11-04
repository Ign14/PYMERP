import { useQuery } from "@tanstack/react-query";
import { getSalesABCAnalysis } from "../services/client";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, Cell } from "recharts";

export default function SalesABCChart() {
  const { data: abcData = [], isLoading } = useQuery({
    queryKey: ["salesABCAnalysis"],
    queryFn: () => getSalesABCAnalysis(),
    refetchInterval: 5 * 60 * 1000,
  });

  if (isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
        <div className="animate-pulse">
          <div className="h-6 bg-neutral-800 rounded w-1/3 mb-4"></div>
          <div className="h-64 bg-neutral-800 rounded"></div>
        </div>
      </div>
    );
  }

  // Agrupar por clasificación
  const classA = abcData.filter(item => item.classification === "A");
  const classB = abcData.filter(item => item.classification === "B");
  const classC = abcData.filter(item => item.classification === "C");

  const totalRevenue = abcData.reduce((sum, item) => sum + item.totalRevenue, 0);
  const revenueA = classA.reduce((sum, item) => sum + item.totalRevenue, 0);
  const revenueB = classB.reduce((sum, item) => sum + item.totalRevenue, 0);
  const revenueC = classC.reduce((sum, item) => sum + item.totalRevenue, 0);

  // Datos para el gráfico
  const chartData = [
    {
      class: "Clase A",
      count: classA.length,
      revenue: revenueA,
      percentage: totalRevenue > 0 ? (revenueA / totalRevenue) * 100 : 0,
    },
    {
      class: "Clase B",
      count: classB.length,
      revenue: revenueB,
      percentage: totalRevenue > 0 ? (revenueB / totalRevenue) * 100 : 0,
    },
    {
      class: "Clase C",
      count: classC.length,
      revenue: revenueC,
      percentage: totalRevenue > 0 ? (revenueC / totalRevenue) * 100 : 0,
    },
  ];

  const classColors = {
    "Clase A": "#ef4444",
    "Clase B": "#f59e0b",
    "Clase C": "#10b981",
  };

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
      <h3 className="text-lg font-semibold text-white mb-6">Distribución ABC de Productos</h3>

      {/* Tarjetas resumen */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        {/* Clase A */}
        <div className="bg-red-950 border border-red-900 rounded-lg p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm text-red-200">Clase A - Productos Estrella</span>
            <span className="text-xs bg-red-900 text-red-200 px-2 py-1 rounded">
              {classA.length} productos
            </span>
          </div>
          <div className="text-2xl font-bold text-white mb-1">
            ${revenueA.toLocaleString("es-CL")}
          </div>
          <div className="text-sm text-red-300">
            {totalRevenue > 0 ? ((revenueA / totalRevenue) * 100).toFixed(1) : 0}% de ingresos
          </div>
        </div>

        {/* Clase B */}
        <div className="bg-amber-950 border border-amber-900 rounded-lg p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm text-amber-200">Clase B - Productos Importantes</span>
            <span className="text-xs bg-amber-900 text-amber-200 px-2 py-1 rounded">
              {classB.length} productos
            </span>
          </div>
          <div className="text-2xl font-bold text-white mb-1">
            ${revenueB.toLocaleString("es-CL")}
          </div>
          <div className="text-sm text-amber-300">
            {totalRevenue > 0 ? ((revenueB / totalRevenue) * 100).toFixed(1) : 0}% de ingresos
          </div>
        </div>

        {/* Clase C */}
        <div className="bg-emerald-950 border border-emerald-900 rounded-lg p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm text-emerald-200">Clase C - Productos Ocasionales</span>
            <span className="text-xs bg-emerald-900 text-emerald-200 px-2 py-1 rounded">
              {classC.length} productos
            </span>
          </div>
          <div className="text-2xl font-bold text-white mb-1">
            ${revenueC.toLocaleString("es-CL")}
          </div>
          <div className="text-sm text-emerald-300">
            {totalRevenue > 0 ? ((revenueC / totalRevenue) * 100).toFixed(1) : 0}% de ingresos
          </div>
        </div>
      </div>

      {/* Gráfico de barras */}
      <ResponsiveContainer width="100%" height={300}>
        <BarChart data={chartData}>
          <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
          <XAxis dataKey="class" stroke="#9ca3af" />
          <YAxis stroke="#9ca3af" />
          <Tooltip
            contentStyle={{
              backgroundColor: "#1f2937",
              border: "1px solid #374151",
              borderRadius: "0.375rem",
              color: "#fff",
            }}
            formatter={(value: number, name: string) => {
              if (name === "revenue") return [`$${value.toLocaleString("es-CL")}`, "Ingresos"];
              if (name === "percentage") return [`${value.toFixed(1)}%`, "% del Total"];
              return [value, name];
            }}
          />
          <Legend />
          <Bar dataKey="revenue" name="Ingresos" radius={[4, 4, 0, 0]}>
            {chartData.map((entry, index) => (
              <Cell key={`cell-${index}`} fill={classColors[entry.class as keyof typeof classColors]} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>

      {/* Panel explicativo del Principio de Pareto */}
      <div className="mt-6 bg-neutral-800 border border-neutral-700 rounded-lg p-4">
        <h4 className="text-sm font-semibold text-white mb-2">📊 Principio de Pareto (80-15-5)</h4>
        <p className="text-xs text-neutral-300 leading-relaxed">
          El análisis ABC clasifica productos según su contribución a los ingresos. <strong className="text-white">Clase A</strong> (≤80% acumulado) representa los productos estrella que generan la mayor parte de los ingresos. <strong className="text-white">Clase B</strong> (80-95% acumulado) son productos importantes con potencial de crecimiento. <strong className="text-white">Clase C</strong> (&gt;95%) son productos ocasionales que aportan poco, candidatos para evaluación o descontinuación.
        </p>
      </div>
    </div>
  );
}

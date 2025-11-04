import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { getInventorySummary } from "../../services/client";
import { createCurrencyFormatter } from "../../utils/currency";

export default function InventoryValuationChart() {
  const currencyFormatter = useMemo(() => createCurrencyFormatter(), []);
  const formatCurrency = (value: number) => currencyFormatter.format(value ?? 0);

  const summaryQuery = useQuery({
    queryKey: ["inventory-summary"],
    queryFn: getInventorySummary,
  });

  const summary = summaryQuery.data;
  const totalValue = Number(summary?.totalValue ?? 0);

  // Simular evolución (en producción vendría de histórico)
  const historicalData = useMemo(() => {
    return [
      { month: "Hace 3m", value: totalValue * 0.75 },
      { month: "Hace 2m", value: totalValue * 0.85 },
      { month: "Hace 1m", value: totalValue * 0.92 },
      { month: "Actual", value: totalValue },
    ];
  }, [totalValue]);

  const maxValue = Math.max(...historicalData.map((d) => d.value));

  if (summaryQuery.isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
        <h3 className="text-neutral-100 mb-4">Evolución de Valorización</h3>
        <div className="animate-pulse bg-neutral-800 rounded-lg h-64"></div>
      </div>
    );
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-neutral-100">Evolución de Valorización</h3>
        <span className="px-3 py-1 bg-blue-950 text-blue-400 border border-blue-800 rounded-full text-sm">
          📈 Tendencia: +{((totalValue / (totalValue * 0.75) - 1) * 100).toFixed(1)}%
        </span>
      </div>

      {/* Gráfico de barras simple */}
      <div className="space-y-4 mb-6">
        {historicalData.map((data, idx) => {
          const widthPercentage = maxValue > 0 ? (data.value / maxValue) * 100 : 0;
          const isActual = idx === historicalData.length - 1;

          return (
            <div key={data.month}>
              <div className="flex items-center justify-between mb-1">
                <span className="text-neutral-300 text-sm">{data.month}</span>
                <span className="text-neutral-100 font-semibold">{formatCurrency(data.value)}</span>
              </div>
              <div className="w-full bg-neutral-800 rounded-full h-3 overflow-hidden">
                <div
                  className={`h-full ${
                    isActual ? "bg-gradient-to-r from-blue-600 to-blue-400" : "bg-neutral-600"
                  } transition-all`}
                  style={{ width: `${widthPercentage}%` }}
                ></div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Indicadores clave */}
      <div className="grid grid-cols-2 gap-4">
        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-3">
          <p className="text-neutral-400 text-sm">Valor Total</p>
          <p className="text-neutral-100 text-xl font-bold">{formatCurrency(totalValue)}</p>
        </div>
        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-3">
          <p className="text-neutral-400 text-sm">Productos</p>
          <p className="text-neutral-100 text-xl font-bold">{summary?.activeProducts ?? 0}</p>
        </div>
      </div>
    </div>
  );
}

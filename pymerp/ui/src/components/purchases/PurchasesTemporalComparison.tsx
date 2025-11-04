import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { listPurchases } from "../../services/client";
import { createCurrencyFormatter } from "../../utils/currency";

interface PurchasesTemporalComparisonProps {
  startDate: string;
  endDate: string;
}

export function PurchasesTemporalComparison({ startDate, endDate }: PurchasesTemporalComparisonProps) {
  const currencyFormatter = useMemo(() => createCurrencyFormatter(), []);
  const formatCurrency = (value: number) => currencyFormatter.format(value ?? 0);

  // Obtener compras del período actual
  const { data: currentPurchases } = useQuery({
    queryKey: ["purchases", "current", startDate, endDate],
    queryFn: () =>
      listPurchases({
        from: startDate ? new Date(startDate).toISOString() : undefined,
        to: endDate ? new Date(endDate).toISOString() : undefined,
        size: 10000,
      }),
  });

  // Obtener compras del período anterior (mismo rango de días)
  const { data: previousPurchases } = useQuery({
    queryKey: ["purchases", "previous", startDate, endDate],
    queryFn: async () => {
      if (!startDate || !endDate) return null;

      const start = new Date(startDate);
      const end = new Date(endDate);
      const diffDays = Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));

      const prevEnd = new Date(start);
      prevEnd.setDate(prevEnd.getDate() - 1);
      const prevStart = new Date(prevEnd);
      prevStart.setDate(prevStart.getDate() - diffDays);

      return listPurchases({
        from: prevStart.toISOString(),
        to: prevEnd.toISOString(),
        size: 10000,
      });
    },
    enabled: !!startDate && !!endDate,
  });

  const stats = useMemo(() => {
    if (!currentPurchases?.content || !previousPurchases?.content) {
      return null;
    }

    const currentTotal = currentPurchases.content.reduce((sum, p) => sum + p.total, 0);
    const currentCount = currentPurchases.content.length;
    const currentAvg = currentCount > 0 ? currentTotal / currentCount : 0;

    const previousTotal = previousPurchases.content.reduce((sum, p) => sum + p.total, 0);
    const previousCount = previousPurchases.content.length;
    const previousAvg = previousCount > 0 ? previousTotal / previousCount : 0;

    const totalChange = previousTotal !== 0 ? ((currentTotal - previousTotal) / previousTotal) * 100 : 0;
    const countChange = previousCount !== 0 ? ((currentCount - previousCount) / previousCount) * 100 : 0;
    const avgChange = previousAvg !== 0 ? ((currentAvg - previousAvg) / previousAvg) * 100 : 0;

    // Proyección mes-a-fecha (asumiendo que el período es de un mes)
    const start = new Date(startDate);
    const end = new Date(endDate);
    const daysInPeriod = Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
    const today = new Date();
    const daysRemaining = Math.max(0, Math.ceil((end.getTime() - today.getTime()) / (1000 * 60 * 60 * 24)));
    const dailyAvg = daysInPeriod > 0 ? currentTotal / daysInPeriod : 0;
    const projection = currentTotal + (dailyAvg * daysRemaining);

    return {
      currentTotal,
      currentCount,
      currentAvg,
      previousTotal,
      previousCount,
      previousAvg,
      totalChange,
      countChange,
      avgChange,
      projection,
      daysRemaining,
    };
  }, [currentPurchases, previousPurchases, startDate, endDate]);

  if (!stats) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-6">
        <h3 className="text-lg font-semibold mb-4 text-neutral-100">Comparación Temporal</h3>
        <div className="text-center py-8 text-neutral-400">
          <p>Cargando datos de comparación...</p>
        </div>
      </div>
    );
  }

  const getTrendIcon = (change: number) => {
    if (change > 5) return "📈"; // Aumento significativo
    if (change < -5) return "📉"; // Disminución significativa
    return "➡️"; // Estable
  };

  const getTrendColor = (change: number) => {
    if (change > 5) return "text-red-400";
    if (change < -5) return "text-green-400";
    return "text-neutral-400";
  };

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-6">
      <h3 className="text-lg font-semibold mb-4 flex items-center gap-2 text-neutral-100">
        <span className="text-2xl">📊</span>
        Comparación Temporal
      </h3>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        {/* Total gastado */}
        <div className="p-4 bg-neutral-950 border border-neutral-800 rounded-lg">
          <div className="text-sm text-neutral-400 mb-1">Total Gastado</div>
          <div className="text-2xl font-bold mb-2 text-neutral-100">{formatCurrency(stats.currentTotal)}</div>
          <div className={`flex items-center gap-2 text-sm ${getTrendColor(stats.totalChange)}`}>
            <span className="text-xl">{getTrendIcon(stats.totalChange)}</span>
            <span className="font-semibold">
              {stats.totalChange > 0 ? "+" : ""}
              {stats.totalChange.toFixed(1)}%
            </span>
            <span className="text-neutral-400">vs período anterior</span>
          </div>
        </div>

        {/* Cantidad de órdenes */}
        <div className="p-4 bg-neutral-950 border border-neutral-800 rounded-lg">
          <div className="text-sm text-neutral-400 mb-1">Cantidad de Órdenes</div>
          <div className="text-2xl font-bold mb-2 text-neutral-100">{stats.currentCount}</div>
          <div className={`flex items-center gap-2 text-sm ${getTrendColor(stats.countChange)}`}>
            <span className="text-xl">{getTrendIcon(stats.countChange)}</span>
            <span className="font-semibold">
              {stats.countChange > 0 ? "+" : ""}
              {stats.countChange.toFixed(1)}%
            </span>
            <span className="text-neutral-400">vs período anterior</span>
          </div>
        </div>

        {/* Promedio por orden */}
        <div className="p-4 bg-neutral-950 border border-neutral-800 rounded-lg">
          <div className="text-sm text-neutral-400 mb-1">Promedio por Orden</div>
          <div className="text-2xl font-bold mb-2 text-neutral-100">{formatCurrency(stats.currentAvg)}</div>
          <div className={`flex items-center gap-2 text-sm ${getTrendColor(stats.avgChange)}`}>
            <span className="text-xl">{getTrendIcon(stats.avgChange)}</span>
            <span className="font-semibold">
              {stats.avgChange > 0 ? "+" : ""}
              {stats.avgChange.toFixed(1)}%
            </span>
            <span className="text-neutral-400">vs período anterior</span>
          </div>
        </div>
      </div>

      {/* Proyección */}
      {stats.daysRemaining > 0 && (
        <div className="p-4 bg-blue-950 border border-blue-800 rounded-lg">
          <div className="flex items-center gap-2 mb-2">
            <span className="text-2xl">🔮</span>
            <h4 className="font-semibold text-blue-300">Proyección de Fin de Período</h4>
          </div>
          <div className="text-sm text-blue-400 mb-1">
            Basado en el gasto diario actual, se proyecta un total de:
          </div>
          <div className="text-3xl font-bold text-blue-300">{formatCurrency(stats.projection)}</div>
          <div className="text-sm text-blue-400 mt-2">
            ({stats.daysRemaining} día{stats.daysRemaining !== 1 ? "s" : ""} restante{stats.daysRemaining !== 1 ? "s" : ""})
          </div>
        </div>
      )}
    </div>
  );
}

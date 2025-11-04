import { useQuery } from "@tanstack/react-query";
import { getInventoryKPIs } from "../services/client";

export default function InventoryStatsCard() {
  const { data: kpis, isLoading, error } = useQuery({
    queryKey: ["inventoryKPIs"],
    queryFn: getInventoryKPIs,
    refetchInterval: 60000, // Refetch every minute
  });

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {Array.from({ length: 6 }).map((_, i) => (
          <div
            key={i}
            className="bg-neutral-900 border border-neutral-800 rounded-lg p-6 animate-pulse"
          >
            <div className="h-4 bg-neutral-800 rounded w-2/3 mb-3"></div>
            <div className="h-8 bg-neutral-800 rounded w-1/2 mb-2"></div>
            <div className="h-3 bg-neutral-800 rounded w-3/4"></div>
          </div>
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-900/20 border border-red-800 rounded-lg p-6 text-center">
        <p className="text-red-400">Error al cargar KPIs de inventario</p>
      </div>
    );
  }

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat("es-CL", {
      style: "currency",
      currency: "CLP",
      minimumFractionDigits: 0,
    }).format(value);
  };

  const formatNumber = (value: number) => {
    return new Intl.NumberFormat("es-CL").format(value);
  };

  const cards = [
    {
      title: "Cobertura de Stock",
      value: kpis?.stockCoverageDays !== null && kpis?.stockCoverageDays !== undefined
        ? `${kpis.stockCoverageDays.toFixed(1)} días`
        : "N/A",
      subtitle: "Duración estimada del inventario",
      color: "text-blue-400",
      bgColor: "bg-blue-900/20",
      borderColor: "border-blue-800",
    },
    {
      title: "Ratio de Rotación",
      value: kpis?.turnoverRatio ? kpis.turnoverRatio.toFixed(2) : "0.00",
      subtitle: "Veces por año (anualizado)",
      color: "text-green-400",
      bgColor: "bg-green-900/20",
      borderColor: "border-green-800",
    },
    {
      title: "Stock Muerto",
      value: formatCurrency(kpis?.deadStockValue || 0),
      subtitle: `${kpis?.deadStockCount || 0} productos sin movimiento >90 días`,
      color: "text-orange-400",
      bgColor: "bg-orange-900/20",
      borderColor: "border-orange-800",
    },
    {
      title: "Tiempo de Reposición",
      value: `${kpis?.averageLeadTimeDays || 0} días`,
      subtitle: "Tiempo promedio de lead time",
      color: "text-purple-400",
      bgColor: "bg-purple-900/20",
      borderColor: "border-purple-800",
    },
    {
      title: "Stock Crítico",
      value: formatNumber(kpis?.criticalStockProducts || 0),
      subtitle: "Productos bajo umbral",
      color: "text-red-400",
      bgColor: "bg-red-900/20",
      borderColor: "border-red-800",
    },
    {
      title: "Sobrestock",
      value: formatCurrency(kpis?.overstockValue || 0),
      subtitle: `${kpis?.overstockCount || 0} productos excedidos`,
      color: "text-yellow-400",
      bgColor: "bg-yellow-900/20",
      borderColor: "border-yellow-800",
    },
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      {cards.map((card, index) => (
        <div
          key={index}
          className={`${card.bgColor} border ${card.borderColor} rounded-lg p-6 transition-all hover:scale-[1.02]`}
        >
          <div className="flex items-start justify-between mb-3">
            <h3 className="text-sm font-medium text-neutral-400">{card.title}</h3>
            <div className={`w-10 h-10 rounded-lg ${card.bgColor} border ${card.borderColor} flex items-center justify-center`}>
              <span className={`text-xl ${card.color}`}>●</span>
            </div>
          </div>
          <div className={`text-2xl font-bold ${card.color} mb-1`}>
            {card.value}
          </div>
          <p className="text-xs text-neutral-500">{card.subtitle}</p>
        </div>
      ))}
    </div>
  );
}

import { useQuery } from "@tanstack/react-query";
import { getSalesKPIs } from "../../services/client";

export default function SalesAdvancedKPIs() {
  // KPIs de últimos 30 días por defecto
  const { data: kpis, isLoading, error } = useQuery({
    queryKey: ["salesKPIs"],
    queryFn: () => getSalesKPIs(),
    refetchInterval: 5 * 60 * 1000, // 5 minutos
  });

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 animate-pulse">
        {[1, 2, 3, 4, 5, 6, 7, 8].map((i) => (
          <div key={i} className="bg-neutral-900 border border-neutral-800 rounded-lg p-6 h-32"></div>
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
        <p className="text-red-400">Error al cargar KPIs de ventas</p>
      </div>
    );
  }

  if (!kpis) {
    return null;
  }

  const formatCurrency = (value: number) => {
    return `$${value.toLocaleString("es-CL", { maximumFractionDigits: 0 })}`;
  };

  const formatPercent = (value: number) => {
    return `${value.toFixed(1)}%`;
  };

  const getGrowthColor = (growth: number) => {
    if (growth > 0) return "text-green-400";
    if (growth < 0) return "text-red-400";
    return "text-neutral-400";
  };

  const getGrowthIcon = (growth: number) => {
    if (growth > 0) return "↗️";
    if (growth < 0) return "↘️";
    return "→";
  };

  return (
    <div className="space-y-6">
      {/* Título */}
      <h2 className="text-2xl font-semibold text-white">📊 KPIs Avanzados de Ventas</h2>

      {/* Grid de KPIs principales */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Total Revenue */}
        <div className="bg-gradient-to-br from-blue-900/30 to-blue-800/20 border border-blue-800 rounded-lg p-6">
          <div className="text-sm text-blue-400 mb-1">Ingresos Totales</div>
          <div className="text-3xl font-bold text-white">{formatCurrency(kpis.totalRevenue)}</div>
          <div className="text-xs text-blue-300 mt-2">
            {kpis.totalOrders} órdenes emitidas
          </div>
        </div>

        {/* Gross Profit */}
        <div className="bg-gradient-to-br from-green-900/30 to-green-800/20 border border-green-800 rounded-lg p-6">
          <div className="text-sm text-green-400 mb-1">Utilidad Bruta</div>
          <div className="text-3xl font-bold text-white">{formatCurrency(kpis.grossProfit)}</div>
          <div className="text-xs text-green-300 mt-2">
            Margen: {formatPercent(kpis.profitMargin)}
          </div>
        </div>

        {/* Average Ticket */}
        <div className="bg-gradient-to-br from-purple-900/30 to-purple-800/20 border border-purple-800 rounded-lg p-6">
          <div className="text-sm text-purple-400 mb-1">Ticket Promedio</div>
          <div className="text-3xl font-bold text-white">{formatCurrency(kpis.averageTicket)}</div>
          <div className="text-xs text-purple-300 mt-2">
            Por orden emitida
          </div>
        </div>

        {/* Sales Growth */}
        <div className="bg-gradient-to-br from-orange-900/30 to-orange-800/20 border border-orange-800 rounded-lg p-6">
          <div className="text-sm text-orange-400 mb-1">Crecimiento</div>
          <div className={`text-3xl font-bold ${getGrowthColor(kpis.salesGrowth)}`}>
            {getGrowthIcon(kpis.salesGrowth)} {formatPercent(Math.abs(kpis.salesGrowth))}
          </div>
          <div className="text-xs text-orange-300 mt-2">
            vs período anterior
          </div>
        </div>
      </div>

      {/* Grid de KPIs secundarios */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Unique Customers */}
        <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-5">
          <div className="text-sm text-neutral-400 mb-1">Clientes Únicos</div>
          <div className="text-2xl font-bold text-white">{kpis.uniqueCustomers}</div>
          <div className="text-xs text-neutral-500 mt-1">
            Retención: {formatPercent(kpis.customerRetentionRate)}
          </div>
        </div>

        {/* Conversion Rate */}
        <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-5">
          <div className="text-sm text-neutral-400 mb-1">Tasa de Conversión</div>
          <div className="text-2xl font-bold text-white">{formatPercent(kpis.conversionRate)}</div>
          <div className="text-xs text-neutral-500 mt-1">
            Emitidas vs total
          </div>
        </div>

        {/* Top Product */}
        <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-5">
          <div className="text-sm text-neutral-400 mb-1">Top Producto</div>
          <div className="text-lg font-semibold text-white truncate" title={kpis.topProductName}>
            {kpis.topProductName}
          </div>
          <div className="text-sm text-green-400 mt-1">
            {formatCurrency(kpis.topProductRevenue)}
          </div>
        </div>

        {/* Top Customer */}
        <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-5">
          <div className="text-sm text-neutral-400 mb-1">Top Cliente</div>
          <div className="text-lg font-semibold text-white truncate" title={kpis.topCustomerName}>
            {kpis.topCustomerName}
          </div>
          <div className="text-sm text-blue-400 mt-1">
            {formatCurrency(kpis.topCustomerRevenue)}
          </div>
        </div>
      </div>

      {/* Barra de costos vs ingresos */}
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-white mb-4">Desglose de Ingresos</h3>
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-neutral-400">Ingresos Totales</span>
            <span className="text-white font-semibold">{formatCurrency(kpis.totalRevenue)}</span>
          </div>
          <div className="w-full bg-neutral-800 rounded-full h-3">
            <div 
              className="bg-blue-500 h-3 rounded-full" 
              style={{ width: '100%' }}
            ></div>
          </div>

          <div className="flex items-center justify-between">
            <span className="text-neutral-400">Costo Estimado</span>
            <span className="text-red-400 font-semibold">{formatCurrency(kpis.totalCost)}</span>
          </div>
          <div className="w-full bg-neutral-800 rounded-full h-3">
            <div 
              className="bg-red-500 h-3 rounded-full" 
              style={{ width: `${(kpis.totalCost / kpis.totalRevenue) * 100}%` }}
            ></div>
          </div>

          <div className="flex items-center justify-between">
            <span className="text-neutral-400">Utilidad Bruta</span>
            <span className="text-green-400 font-bold text-lg">{formatCurrency(kpis.grossProfit)}</span>
          </div>
          <div className="w-full bg-neutral-800 rounded-full h-3">
            <div 
              className="bg-green-500 h-3 rounded-full" 
              style={{ width: `${kpis.profitMargin}%` }}
            ></div>
          </div>
        </div>
      </div>

      {/* Período de análisis */}
      <div className="bg-blue-900/20 border border-blue-800 rounded-lg p-4">
        <div className="flex items-center justify-between">
          <span className="text-sm text-blue-400">
            📅 Período de análisis: {new Date(kpis.periodStart).toLocaleDateString("es-CL")} - {new Date(kpis.periodEnd).toLocaleDateString("es-CL")}
          </span>
          <span className="text-xs text-blue-300">
            Actualizado automáticamente cada 5 minutos
          </span>
        </div>
      </div>
    </div>
  );
}

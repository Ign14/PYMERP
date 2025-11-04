import { useQuery } from "@tanstack/react-query";
import { getPurchaseKPIs } from "../../services/client";

export default function PurchasesAdvancedKPIs() {
  // KPIs de últimos 30 días por defecto
  const { data: kpis, isLoading, error } = useQuery({
    queryKey: ["purchaseKPIs"],
    queryFn: () => getPurchaseKPIs(),
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
        <p className="text-red-400">Error al cargar KPIs de compras</p>
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
    if (growth > 0) return "text-red-400"; // Compras creciendo es "malo" (más gasto)
    if (growth < 0) return "text-green-400"; // Compras bajando es "bueno" (ahorro)
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
      <h2 className="text-2xl font-semibold text-white">📦 KPIs Avanzados de Compras</h2>

      {/* Grid de KPIs principales */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Total Spent */}
        <div className="bg-gradient-to-br from-purple-900/30 to-purple-800/20 border border-purple-800 rounded-lg p-6">
          <div className="text-sm text-purple-400 mb-1">Gasto Total</div>
          <div className="text-3xl font-bold text-white">{formatCurrency(kpis.totalSpent)}</div>
          <div className="text-xs text-purple-300 mt-2">
            {kpis.totalOrders} órdenes recibidas
          </div>
        </div>

        {/* Average Order Value */}
        <div className="bg-gradient-to-br from-blue-900/30 to-blue-800/20 border border-blue-800 rounded-lg p-6">
          <div className="text-sm text-blue-400 mb-1">Promedio por Orden</div>
          <div className="text-3xl font-bold text-white">{formatCurrency(kpis.averageOrderValue)}</div>
          <div className="text-xs text-blue-300 mt-2">
            {kpis.totalQuantity.toLocaleString("es-CL")} unidades
          </div>
        </div>

        {/* Purchase Growth */}
        <div className="bg-gradient-to-br from-orange-900/30 to-orange-800/20 border border-orange-800 rounded-lg p-6">
          <div className="text-sm text-orange-400 mb-1">Variación de Gasto</div>
          <div className={`text-3xl font-bold ${getGrowthColor(kpis.purchaseGrowth)}`}>
            {getGrowthIcon(kpis.purchaseGrowth)} {formatPercent(Math.abs(kpis.purchaseGrowth))}
          </div>
          <div className="text-xs text-orange-300 mt-2">
            vs período anterior
          </div>
        </div>

        {/* Cost per Unit */}
        <div className="bg-gradient-to-br from-green-900/30 to-green-800/20 border border-green-800 rounded-lg p-6">
          <div className="text-sm text-green-400 mb-1">Costo Unitario</div>
          <div className="text-3xl font-bold text-white">{formatCurrency(kpis.costPerUnit)}</div>
          <div className="text-xs text-green-300 mt-2">
            Promedio por unidad
          </div>
        </div>
      </div>

      {/* Grid de KPIs secundarios */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Unique Suppliers */}
        <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-5">
          <div className="text-sm text-neutral-400 mb-1">Proveedores Activos</div>
          <div className="text-2xl font-bold text-white">{kpis.uniqueSuppliers}</div>
          <div className="text-xs text-neutral-500 mt-1">
            Órdenes recibidas
          </div>
        </div>

        {/* Supplier Concentration */}
        <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-5">
          <div className="text-sm text-neutral-400 mb-1">Concentración</div>
          <div className="text-2xl font-bold text-white">{formatPercent(kpis.supplierConcentration)}</div>
          <div className="text-xs text-neutral-500 mt-1">
            Top proveedor
          </div>
        </div>

        {/* Top Supplier */}
        <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-5">
          <div className="text-sm text-neutral-400 mb-1">Top Proveedor</div>
          <div className="text-lg font-semibold text-white truncate" title={kpis.topSupplierName}>
            {kpis.topSupplierName}
          </div>
          <div className="text-sm text-purple-400 mt-1">
            {formatCurrency(kpis.topSupplierSpent)}
          </div>
        </div>

        {/* Pending Orders */}
        <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-5">
          <div className="text-sm text-neutral-400 mb-1">Órdenes Pendientes</div>
          <div className="text-2xl font-bold text-white">{kpis.pendingOrders}</div>
          <div className="text-xs text-neutral-500 mt-1">
            Entrega a tiempo: {formatPercent(kpis.onTimeDeliveryRate)}
          </div>
        </div>
      </div>

      {/* Análisis de gastos */}
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-white mb-4">Análisis de Gastos</h3>
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-neutral-400">Gasto Total</span>
            <span className="text-white font-semibold">{formatCurrency(kpis.totalSpent)}</span>
          </div>
          <div className="w-full bg-neutral-800 rounded-full h-3">
            <div 
              className="bg-purple-500 h-3 rounded-full" 
              style={{ width: '100%' }}
            ></div>
          </div>

          <div className="flex items-center justify-between">
            <span className="text-neutral-400">Gasto Top Proveedor ({kpis.topSupplierName})</span>
            <span className="text-purple-400 font-semibold">{formatCurrency(kpis.topSupplierSpent)}</span>
          </div>
          <div className="w-full bg-neutral-800 rounded-full h-3">
            <div 
              className="bg-purple-400 h-3 rounded-full" 
              style={{ width: `${kpis.supplierConcentration}%` }}
            ></div>
          </div>

          <div className="flex items-center justify-between">
            <span className="text-neutral-400">Otros Proveedores</span>
            <span className="text-blue-400 font-semibold">
              {formatCurrency(kpis.totalSpent - kpis.topSupplierSpent)}
            </span>
          </div>
          <div className="w-full bg-neutral-800 rounded-full h-3">
            <div 
              className="bg-blue-500 h-3 rounded-full" 
              style={{ width: `${100 - kpis.supplierConcentration}%` }}
            ></div>
          </div>

          {/* Alerta de concentración */}
          {kpis.supplierConcentration > 50 && (
            <div className="bg-yellow-900/20 border border-yellow-800 rounded-lg p-3 mt-4">
              <p className="text-sm text-yellow-400">
                ⚠️ Alta concentración en un proveedor ({formatPercent(kpis.supplierConcentration)}). 
                Considere diversificar para reducir riesgo de dependencia.
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Período de análisis */}
      <div className="bg-purple-900/20 border border-purple-800 rounded-lg p-4">
        <div className="flex items-center justify-between">
          <span className="text-sm text-purple-400">
            📅 Período de análisis: {new Date(kpis.periodStart).toLocaleDateString("es-CL")} - {new Date(kpis.periodEnd).toLocaleDateString("es-CL")}
          </span>
          <span className="text-xs text-purple-300">
            Actualizado automáticamente cada 5 minutos
          </span>
        </div>
      </div>
    </div>
  );
}

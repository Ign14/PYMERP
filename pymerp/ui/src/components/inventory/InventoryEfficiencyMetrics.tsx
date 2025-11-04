import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { getInventorySummary, listProducts } from "../../services/client";
import { createCurrencyFormatter } from "../../utils/currency";

export default function InventoryEfficiencyMetrics() {
  const currencyFormatter = useMemo(() => createCurrencyFormatter(), []);
  const formatCurrency = (value: number) => currencyFormatter.format(value ?? 0);

  const summaryQuery = useQuery({
    queryKey: ["inventory-summary"],
    queryFn: getInventorySummary,
  });

  const productsQuery = useQuery({
    queryKey: ["products-efficiency"],
    queryFn: () => listProducts({ size: 200, status: "all" }),
  });

  const summary = summaryQuery.data;
  const products = productsQuery.data?.content ?? [];
  const totalValue = Number(summary?.totalValue ?? 0);

  const metrics = useMemo(() => {
    // Simular datos de eficiencia (en producción vendrían de API)
    const totalProducts = products.length;
    const dailyConsumption = 15; // Simulado
    const totalStock = products.reduce((sum, p) => sum + Number(p.stock ?? 0), 0);
    const daysOfCoverage = dailyConsumption > 0 ? totalStock / dailyConsumption : 0;

    // Stock-out rate (productos sin stock / total)
    const outOfStock = products.filter((p) => Number(p.stock ?? 0) === 0).length;
    const stockOutRate = totalProducts > 0 ? (outOfStock / totalProducts) * 100 : 0;

    // Inventory accuracy (ajustes / movimientos totales)
    const adjustments = 12; // Simulado
    const totalMovements = 150; // Simulado
    const inventoryAccuracy = totalMovements > 0 ? ((totalMovements - adjustments) / totalMovements) * 100 : 100;

    // Costo de almacenamiento estimado (5% anual del valor)
    const annualStorageCostRate = 0.05;
    const estimatedStorageCost = totalValue * annualStorageCostRate;
    const monthlyStorageCost = estimatedStorageCost / 12;

    return {
      daysOfCoverage,
      stockOutRate,
      inventoryAccuracy,
      monthlyStorageCost,
      outOfStock,
      totalProducts,
    };
  }, [products, totalValue]);

  if (summaryQuery.isLoading || productsQuery.isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
        <h3 className="text-neutral-100 mb-4">Métricas de Eficiencia</h3>
        <div className="animate-pulse bg-neutral-800 rounded-lg h-64"></div>
      </div>
    );
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
      <h3 className="text-neutral-100 mb-4">Métricas de Eficiencia</h3>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Días de cobertura */}
        <div className="bg-blue-950 border border-blue-800 rounded-lg p-4">
          <div className="flex items-center justify-between mb-2">
            <h4 className="text-blue-400 font-medium">📅 Días de Cobertura</h4>
            <span className="text-blue-300 text-2xl font-bold">{metrics.daysOfCoverage.toFixed(0)}</span>
          </div>
          <p className="text-blue-300 text-sm">Stock actual / Consumo diario promedio</p>
          <div className="mt-2 w-full bg-blue-900 rounded-full h-2">
            <div
              className="bg-blue-500 h-full rounded-full transition-all"
              style={{ width: `${Math.min((metrics.daysOfCoverage / 60) * 100, 100)}%` }}
            ></div>
          </div>
        </div>

        {/* Stock-out rate */}
        <div className="bg-red-950 border border-red-800 rounded-lg p-4">
          <div className="flex items-center justify-between mb-2">
            <h4 className="text-red-400 font-medium">📉 Tasa de Quiebre</h4>
            <span className="text-red-300 text-2xl font-bold">{metrics.stockOutRate.toFixed(1)}%</span>
          </div>
          <p className="text-red-300 text-sm">
            {metrics.outOfStock} de {metrics.totalProducts} productos sin stock
          </p>
          <div className="mt-2 w-full bg-red-900 rounded-full h-2">
            <div
              className="bg-red-500 h-full rounded-full transition-all"
              style={{ width: `${metrics.stockOutRate}%` }}
            ></div>
          </div>
        </div>

        {/* Inventory accuracy */}
        <div className="bg-green-950 border border-green-800 rounded-lg p-4">
          <div className="flex items-center justify-between mb-2">
            <h4 className="text-green-400 font-medium">✅ Precisión Inventario</h4>
            <span className="text-green-300 text-2xl font-bold">{metrics.inventoryAccuracy.toFixed(1)}%</span>
          </div>
          <p className="text-green-300 text-sm">Basado en movimientos vs ajustes</p>
          <div className="mt-2 w-full bg-green-900 rounded-full h-2">
            <div
              className="bg-green-500 h-full rounded-full transition-all"
              style={{ width: `${metrics.inventoryAccuracy}%` }}
            ></div>
          </div>
        </div>

        {/* Storage cost */}
        <div className="bg-purple-950 border border-purple-800 rounded-lg p-4">
          <div className="flex items-center justify-between mb-2">
            <h4 className="text-purple-400 font-medium">💰 Costo Almacenamiento</h4>
            <span className="text-purple-300 text-2xl font-bold">{formatCurrency(metrics.monthlyStorageCost)}</span>
          </div>
          <p className="text-purple-300 text-sm">Estimado mensual (~5% anual del valor)</p>
        </div>
      </div>

      <div className="mt-4 bg-neutral-800 border border-neutral-700 rounded-lg p-4">
        <h4 className="text-neutral-100 font-medium mb-2">Recomendaciones</h4>
        <ul className="space-y-1 text-neutral-300 text-sm">
          {metrics.stockOutRate > 10 && (
            <li className="flex items-start gap-2">
              <span className="text-red-400">⚠️</span>
              <span>Tasa de quiebre alta. Revisar puntos de reorden.</span>
            </li>
          )}
          {metrics.daysOfCoverage > 90 && (
            <li className="flex items-start gap-2">
              <span className="text-yellow-400">⚠️</span>
              <span>Sobrestock detectado. Evaluar reducción de inventario.</span>
            </li>
          )}
          {metrics.inventoryAccuracy < 95 && (
            <li className="flex items-start gap-2">
              <span className="text-orange-400">⚠️</span>
              <span>Baja precisión. Realizar conteo cíclico.</span>
            </li>
          )}
          {metrics.stockOutRate <= 5 && metrics.inventoryAccuracy >= 95 && (
            <li className="flex items-start gap-2">
              <span className="text-green-400">✅</span>
              <span>Inventario en niveles óptimos. ¡Buen trabajo!</span>
            </li>
          )}
        </ul>
      </div>
    </div>
  );
}

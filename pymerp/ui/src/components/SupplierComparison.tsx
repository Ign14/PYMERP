import { useQuery } from "@tanstack/react-query";
import { listSuppliers, getSupplierMetrics, Supplier, SupplierMetrics } from "../services/client";
import { useState, useMemo } from "react";

export default function SupplierComparison() {
  const [selectedSupplierIds, setSelectedSupplierIds] = useState<string[]>([]);

  const suppliersQuery = useQuery<Supplier[], Error>({
    queryKey: ["suppliers"],
    queryFn: () => listSuppliers(),
    refetchOnWindowFocus: false,
  });

  const activeSuppliers = useMemo(() => {
    return (suppliersQuery.data ?? []).filter(s => s.active !== false);
  }, [suppliersQuery.data]);

  // Queries para las métricas de cada proveedor seleccionado
  const metricsQueries = selectedSupplierIds.map(supplierId => 
    useQuery<SupplierMetrics, Error>({
      queryKey: ["supplier-metrics", supplierId],
      queryFn: () => getSupplierMetrics(supplierId),
      enabled: !!supplierId,
      refetchOnWindowFocus: false,
    })
  );

  const handleToggleSupplier = (supplierId: string) => {
    if (selectedSupplierIds.includes(supplierId)) {
      setSelectedSupplierIds(selectedSupplierIds.filter(id => id !== supplierId));
    } else {
      if (selectedSupplierIds.length < 4) {
        setSelectedSupplierIds([...selectedSupplierIds, supplierId]);
      }
    }
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat("es-CL", {
      style: "currency",
      currency: "CLP",
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  };

  const formatDate = (dateStr: string | null) => {
    if (!dateStr) return "Nunca";
    return new Date(dateStr).toLocaleDateString("es-CL", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  };

  const getWinnerForMetric = (metricName: keyof SupplierMetrics, metrics: (SupplierMetrics | undefined)[]) => {
    const validMetrics = metrics.filter(m => m !== undefined) as SupplierMetrics[];
    if (validMetrics.length === 0) return -1;

    const values = validMetrics.map(m => {
      const val = m[metricName];
      if (typeof val === "number") return val;
      if (typeof val === "string") return new Date(val).getTime();
      return 0;
    });

    // Para lastPurchaseDate, más reciente es mejor (mayor timestamp)
    // Para otros, mayor es mejor
    const maxValue = Math.max(...values);
    return values.indexOf(maxValue);
  };

  const selectedMetrics = metricsQueries.map(q => q.data);
  const allLoaded = metricsQueries.every(q => !q.isLoading);
  const hasError = metricsQueries.some(q => q.isError);

  return (
    <div className="card-content">
      <h2 className="text-lg font-semibold text-neutral-100 mb-4">⚖️ Comparación de Proveedores</h2>

      {/* Selector de proveedores */}
      <div className="mb-4">
        <div className="flex items-center justify-between mb-2">
          <label className="text-xs text-neutral-400">
            Seleccionar proveedores para comparar (máx. 4)
          </label>
          <span className="text-xs text-neutral-500">
            {selectedSupplierIds.length}/4 seleccionados
          </span>
        </div>
        <div className="flex flex-wrap gap-2">
          {activeSuppliers.map((supplier) => {
            const isSelected = selectedSupplierIds.includes(supplier.id);
            const canSelect = selectedSupplierIds.length < 4 || isSelected;

            return (
              <button
                key={supplier.id}
                onClick={() => handleToggleSupplier(supplier.id)}
                disabled={!canSelect}
                className={`px-3 py-1.5 text-sm rounded border transition-colors ${
                  isSelected
                    ? "bg-blue-600 text-white border-blue-600"
                    : canSelect
                      ? "bg-neutral-900 text-neutral-300 border-neutral-700 hover:border-blue-600"
                      : "bg-neutral-900/50 text-neutral-600 border-neutral-800 cursor-not-allowed"
                }`}
              >
                {supplier.name}
              </button>
            );
          })}
        </div>
      </div>

      {/* Sin selección */}
      {selectedSupplierIds.length === 0 && (
        <div className="text-center py-12 bg-neutral-900/50 rounded border border-neutral-800">
          <p className="text-neutral-400">Selecciona al menos 2 proveedores para compararlos</p>
          <p className="text-xs text-neutral-500 mt-2">Compara métricas lado a lado para tomar mejores decisiones</p>
        </div>
      )}

      {/* Solo 1 seleccionado */}
      {selectedSupplierIds.length === 1 && (
        <div className="text-center py-8 bg-neutral-900/50 rounded border border-neutral-800">
          <p className="text-neutral-400">Selecciona al menos un proveedor más para comparar</p>
        </div>
      )}

      {/* Cargando */}
      {selectedSupplierIds.length >= 2 && !allLoaded && (
        <div className="text-center py-8">
          <p className="text-neutral-400">Cargando métricas...</p>
        </div>
      )}

      {/* Error */}
      {hasError && (
        <div className="text-center py-8">
          <p className="text-red-400">Error al cargar algunas métricas</p>
        </div>
      )}

      {/* Tabla de comparación */}
      {selectedSupplierIds.length >= 2 && allLoaded && !hasError && (
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-neutral-800">
                <th className="text-left py-3 px-3 text-neutral-400 font-medium">Métrica</th>
                {selectedSupplierIds.map((supplierId, idx) => {
                  const supplier = activeSuppliers.find(s => s.id === supplierId);
                  return (
                    <th key={supplierId} className="text-right py-3 px-3 text-neutral-200 font-semibold">
                      <div className="flex flex-col items-end">
                        <span>{supplier?.name}</span>
                        <button
                          onClick={() => handleToggleSupplier(supplierId)}
                          className="text-xs text-red-400 hover:text-red-300 mt-1"
                        >
                          ✕ Quitar
                        </button>
                      </div>
                    </th>
                  );
                })}
              </tr>
            </thead>
            <tbody>
              {/* Total Compras */}
              <tr className="border-b border-neutral-800/50 hover:bg-neutral-900/30">
                <td className="py-3 px-3 text-neutral-300">Total Compras</td>
                {selectedMetrics.map((metrics, idx) => {
                  const isWinner = getWinnerForMetric("totalPurchases", selectedMetrics) === idx;
                  return (
                    <td key={idx} className={`py-3 px-3 text-right font-medium ${
                      isWinner ? "text-yellow-400" : "text-neutral-200"
                    }`}>
                      {metrics?.totalPurchases.toLocaleString("es-CL") ?? "-"}
                      {isWinner && " 🏆"}
                    </td>
                  );
                })}
              </tr>

              {/* Monto Total */}
              <tr className="border-b border-neutral-800/50 hover:bg-neutral-900/30">
                <td className="py-3 px-3 text-neutral-300">Monto Total</td>
                {selectedMetrics.map((metrics, idx) => {
                  const isWinner = getWinnerForMetric("totalAmount", selectedMetrics) === idx;
                  return (
                    <td key={idx} className={`py-3 px-3 text-right font-medium ${
                      isWinner ? "text-yellow-400" : "text-neutral-200"
                    }`}>
                      {metrics ? formatCurrency(metrics.totalAmount) : "-"}
                      {isWinner && " 🏆"}
                    </td>
                  );
                })}
              </tr>

              {/* Valor Promedio Orden */}
              <tr className="border-b border-neutral-800/50 hover:bg-neutral-900/30">
                <td className="py-3 px-3 text-neutral-300">Valor Promedio Orden</td>
                {selectedMetrics.map((metrics, idx) => {
                  const isWinner = getWinnerForMetric("averageOrderValue", selectedMetrics) === idx;
                  return (
                    <td key={idx} className={`py-3 px-3 text-right font-medium ${
                      isWinner ? "text-yellow-400" : "text-neutral-200"
                    }`}>
                      {metrics ? formatCurrency(metrics.averageOrderValue) : "-"}
                      {isWinner && " 🏆"}
                    </td>
                  );
                })}
              </tr>

              {/* Última Compra */}
              <tr className="border-b border-neutral-800/50 hover:bg-neutral-900/30">
                <td className="py-3 px-3 text-neutral-300">Última Compra</td>
                {selectedMetrics.map((metrics, idx) => {
                  const isWinner = getWinnerForMetric("lastPurchaseDate", selectedMetrics) === idx;
                  return (
                    <td key={idx} className={`py-3 px-3 text-right ${
                      isWinner ? "text-yellow-400 font-medium" : "text-neutral-300"
                    }`}>
                      {formatDate(metrics?.lastPurchaseDate ?? null)}
                      {isWinner && " 🏆"}
                    </td>
                  );
                })}
              </tr>

              {/* Compras último mes */}
              <tr className="border-b border-neutral-800/50 hover:bg-neutral-900/30">
                <td className="py-3 px-3 text-neutral-300">Compras Último Mes</td>
                {selectedMetrics.map((metrics, idx) => {
                  const isWinner = getWinnerForMetric("purchasesLastMonth", selectedMetrics) === idx;
                  return (
                    <td key={idx} className={`py-3 px-3 text-right font-medium ${
                      isWinner ? "text-yellow-400" : "text-neutral-200"
                    }`}>
                      {metrics?.purchasesLastMonth.toLocaleString("es-CL") ?? "-"}
                      {isWinner && " 🏆"}
                    </td>
                  );
                })}
              </tr>

              {/* Monto último mes */}
              <tr className="border-b border-neutral-800/50 hover:bg-neutral-900/30">
                <td className="py-3 px-3 text-neutral-300">Monto Último Mes</td>
                {selectedMetrics.map((metrics, idx) => {
                  const isWinner = getWinnerForMetric("amountLastMonth", selectedMetrics) === idx;
                  return (
                    <td key={idx} className={`py-3 px-3 text-right font-medium ${
                      isWinner ? "text-yellow-400" : "text-neutral-200"
                    }`}>
                      {metrics ? formatCurrency(metrics.amountLastMonth) : "-"}
                      {isWinner && " 🏆"}
                    </td>
                  );
                })}
              </tr>

              {/* Compras mes anterior */}
              <tr className="border-b border-neutral-800/50 hover:bg-neutral-900/30">
                <td className="py-3 px-3 text-neutral-300">Compras Mes Anterior</td>
                {selectedMetrics.map((metrics, idx) => {
                  return (
                    <td key={idx} className="py-3 px-3 text-right text-neutral-300">
                      {metrics?.purchasesPreviousMonth.toLocaleString("es-CL") ?? "-"}
                    </td>
                  );
                })}
              </tr>

              {/* Monto mes anterior */}
              <tr className="border-b border-neutral-800/50 hover:bg-neutral-900/30">
                <td className="py-3 px-3 text-neutral-300">Monto Mes Anterior</td>
                {selectedMetrics.map((metrics, idx) => {
                  return (
                    <td key={idx} className="py-3 px-3 text-right text-neutral-300">
                      {metrics ? formatCurrency(metrics.amountPreviousMonth) : "-"}
                    </td>
                  );
                })}
              </tr>
            </tbody>
          </table>

          <div className="mt-4 pt-3 border-t border-neutral-800 text-xs text-neutral-500 flex items-center gap-2">
            <span>🏆</span>
            <span>Indica el mejor valor en cada métrica</span>
          </div>
        </div>
      )}
    </div>
  );
}

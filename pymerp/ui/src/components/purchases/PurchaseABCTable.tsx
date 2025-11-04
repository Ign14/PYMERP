import { useQuery } from "@tanstack/react-query";
import { getPurchaseABCAnalysis } from "../../services/client";

export default function PurchaseABCTable() {
  const { data: classifications, isLoading } = useQuery({
    queryKey: ["purchaseABCAnalysis"],
    queryFn: () => getPurchaseABCAnalysis(),
    refetchInterval: 5 * 60 * 1000,
  });

  if (isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6 animate-pulse">
        <div className="h-8 bg-neutral-800 rounded w-1/3 mb-4"></div>
        <div className="space-y-3">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="h-16 bg-neutral-800 rounded"></div>
          ))}
        </div>
      </div>
    );
  }

  if (!classifications || classifications.length === 0) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-white mb-4">📋 Detalle de Proveedores por Clasificación</h3>
        <p className="text-neutral-400 text-center py-8">No hay proveedores para clasificar</p>
      </div>
    );
  }

  const formatCurrency = (value: number) => {
    return `$${value.toLocaleString("es-CL", { maximumFractionDigits: 0 })}`;
  };

  const getClassBadge = (classification: string) => {
    const styles = {
      A: "bg-red-900/30 text-red-400 border-red-800",
      B: "bg-amber-900/30 text-amber-400 border-amber-800",
      C: "bg-green-900/30 text-green-400 border-green-800",
    };
    return styles[classification as keyof typeof styles] || styles.C;
  };

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
      <div className="flex items-center justify-between mb-6">
        <h3 className="text-lg font-semibold text-white">📋 Detalle de Proveedores por Clasificación ABC</h3>
        <span className="text-sm text-neutral-400">{classifications.length} proveedores</span>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="border-b border-neutral-800">
              <th className="text-left py-3 px-4 text-sm font-semibold text-neutral-400">Clase</th>
              <th className="text-left py-3 px-4 text-sm font-semibold text-neutral-400">Proveedor</th>
              <th className="text-right py-3 px-4 text-sm font-semibold text-neutral-400">Gasto Total</th>
              <th className="text-center py-3 px-4 text-sm font-semibold text-neutral-400">Compras</th>
              <th className="text-right py-3 px-4 text-sm font-semibold text-neutral-400">% del Total</th>
              <th className="text-right py-3 px-4 text-sm font-semibold text-neutral-400">Acumulado</th>
              <th className="text-left py-3 px-4 text-sm font-semibold text-neutral-400">Acción Recomendada</th>
            </tr>
          </thead>
          <tbody>
            {classifications.map((item, index) => (
              <tr
                key={item.supplierId}
                className={`border-b border-neutral-800 hover:bg-neutral-800/50 transition-colors ${
                  index < 10 ? "" : "opacity-60"
                }`}
              >
                <td className="py-3 px-4">
                  <span className={`inline-flex px-2 py-1 rounded text-xs font-bold border ${getClassBadge(item.classification)}`}>
                    {item.classification}
                  </span>
                </td>
                <td className="py-3 px-4">
                  <div className="text-white font-medium">{item.supplierName}</div>
                  <div className="text-xs text-neutral-500">
                    Promedio: {formatCurrency(item.averageOrderValue)}
                  </div>
                </td>
                <td className="py-3 px-4 text-right">
                  <span className="text-white font-semibold">{formatCurrency(item.totalSpent)}</span>
                </td>
                <td className="py-3 px-4 text-center">
                  <span className="text-neutral-300">{item.purchaseCount}</span>
                </td>
                <td className="py-3 px-4 text-right">
                  <span className="text-blue-400 font-medium">{item.percentageOfTotal.toFixed(1)}%</span>
                </td>
                <td className="py-3 px-4 text-right">
                  <span className="text-neutral-400">{item.cumulativePercentage.toFixed(1)}%</span>
                </td>
                <td className="py-3 px-4">
                  <span className="text-xs text-neutral-400">{item.recommendedAction}</span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {classifications.length > 10 && (
        <div className="mt-4 text-center text-sm text-neutral-500">
          Mostrando todos los {classifications.length} proveedores (primeros 10 resaltados)
        </div>
      )}
    </div>
  );
}

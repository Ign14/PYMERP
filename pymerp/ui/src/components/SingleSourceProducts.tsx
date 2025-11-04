import { useQuery } from "@tanstack/react-query";
import { getSingleSourceProducts, SingleSourceProduct as ProductType } from "../services/client";

export default function SingleSourceProducts() {
  const productsQuery = useQuery<ProductType[], Error>({
    queryKey: ["single-source-products"],
    queryFn: () => getSingleSourceProducts(),
    refetchOnWindowFocus: false,
  });

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat("es-CL", {
      style: "currency",
      currency: "CLP",
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString("es-CL", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  };

  const getRiskColor = (risk: string) => {
    if (risk === "CRITICAL") return "bg-red-950/50 text-red-400 border-red-800";
    if (risk === "HIGH") return "bg-orange-950/50 text-orange-400 border-orange-800";
    if (risk === "MEDIUM") return "bg-yellow-950/50 text-yellow-400 border-yellow-800";
    return "bg-neutral-800/50 text-neutral-400 border-neutral-700";
  };

  const getRiskIcon = (risk: string) => {
    if (risk === "CRITICAL") return "🔴";
    if (risk === "HIGH") return "🟠";
    if (risk === "MEDIUM") return "🟡";
    return "🟢";
  };

  const getRiskLabel = (risk: string) => {
    if (risk === "CRITICAL") return "Crítico";
    if (risk === "HIGH") return "Alto";
    if (risk === "MEDIUM") return "Medio";
    return "Bajo";
  };

  if (productsQuery.isLoading) {
    return (
      <div className="card-content">
        <h2 className="text-lg font-semibold text-neutral-100">⚠️ Productos con Proveedor Único</h2>
        <p className="text-neutral-400 mt-4">Cargando productos...</p>
      </div>
    );
  }

  if (productsQuery.isError) {
    return (
      <div className="card-content">
        <h2 className="text-lg font-semibold text-neutral-100">⚠️ Productos con Proveedor Único</h2>
        <p className="text-red-400 mt-4">{productsQuery.error?.message ?? "Error al cargar productos"}</p>
      </div>
    );
  }

  const products = productsQuery.data ?? [];
  const criticalCount = products.filter(p => p.riskLevel === "CRITICAL" || p.riskLevel === "HIGH").length;
  const totalExposure = products.reduce((sum, p) => sum + p.totalSpentLast12Months, 0);

  return (
    <div className="card-content">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-semibold text-neutral-100">⚠️ Productos con Proveedor Único</h2>
        {products.length > 0 && (
          <div className="flex items-center gap-3">
            <div className="text-right">
              <div className="text-xs text-neutral-500">Exposición Total</div>
              <div className="text-lg font-bold text-orange-400">{formatCurrency(totalExposure)}</div>
            </div>
            {criticalCount > 0 && (
              <span className="px-2 py-1 text-xs font-medium rounded bg-red-950/50 text-red-400 border border-red-800">
                {criticalCount} crítico{criticalCount !== 1 ? "s" : ""}
              </span>
            )}
          </div>
        )}
      </div>

      {products.length === 0 ? (
        <div className="text-center py-12 bg-neutral-900/50 rounded border border-neutral-800">
          <p className="text-neutral-400">✅ Excelente diversificación de proveedores</p>
          <p className="text-xs text-neutral-500 mt-2">
            Todos los productos tienen múltiples proveedores alternativos
          </p>
        </div>
      ) : (
        <>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-neutral-800">
                  <th className="text-left py-2 px-2 text-neutral-400 font-medium">Riesgo</th>
                  <th className="text-left py-2 px-2 text-neutral-400 font-medium">Producto</th>
                  <th className="text-left py-2 px-2 text-neutral-400 font-medium">Proveedor Único</th>
                  <th className="text-right py-2 px-2 text-neutral-400 font-medium">Precio Actual</th>
                  <th className="text-right py-2 px-2 text-neutral-400 font-medium">Compras (12m)</th>
                  <th className="text-right py-2 px-2 text-neutral-400 font-medium">Gasto Total</th>
                  <th className="text-right py-2 px-2 text-neutral-400 font-medium">Última Compra</th>
                  <th className="text-left py-2 px-2 text-neutral-400 font-medium">Recomendación</th>
                </tr>
              </thead>
              <tbody>
                {products.map((product, idx) => (
                  <tr 
                    key={idx}
                    className="border-b border-neutral-800/50 hover:bg-neutral-900/30 transition-colors"
                  >
                    <td className="py-3 px-2">
                      <div className="flex items-center gap-2">
                        <span className="text-lg">{getRiskIcon(product.riskLevel)}</span>
                        <span className={`px-2 py-0.5 text-xs font-medium rounded border ${getRiskColor(product.riskLevel)}`}>
                          {getRiskLabel(product.riskLevel)}
                        </span>
                      </div>
                    </td>
                    <td className="py-3 px-2 text-neutral-200 font-medium">
                      {product.productName}
                    </td>
                    <td className="py-3 px-2 text-neutral-300">
                      {product.supplierName}
                    </td>
                    <td className="py-3 px-2 text-right text-neutral-200">
                      {formatCurrency(product.currentPrice)}
                    </td>
                    <td className="py-3 px-2 text-right text-neutral-300">
                      {product.purchasesLast12Months.toLocaleString("es-CL")}
                    </td>
                    <td className="py-3 px-2 text-right">
                      <span className={`font-bold ${
                        product.riskLevel === "CRITICAL" ? "text-red-400" :
                        product.riskLevel === "HIGH" ? "text-orange-400" :
                        "text-neutral-200"
                      }`}>
                        {formatCurrency(product.totalSpentLast12Months)}
                      </span>
                    </td>
                    <td className="py-3 px-2 text-right text-neutral-400 text-xs">
                      {formatDate(product.lastPurchaseDate)}
                    </td>
                    <td className="py-3 px-2 text-neutral-400 text-xs max-w-xs">
                      {product.recommendation}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Resumen y ayuda */}
          <div className="mt-4 pt-3 border-t border-neutral-800">
            <div className="grid grid-cols-4 gap-4 mb-3">
              <div className="text-center">
                <div className="text-2xl font-bold text-neutral-200">{products.length}</div>
                <div className="text-xs text-neutral-500">Productos Afectados</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-red-400">
                  {products.filter(p => p.riskLevel === "CRITICAL").length}
                </div>
                <div className="text-xs text-neutral-500">Riesgo Crítico</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-orange-400">
                  {products.filter(p => p.riskLevel === "HIGH").length}
                </div>
                <div className="text-xs text-neutral-500">Riesgo Alto</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-orange-400">{formatCurrency(totalExposure)}</div>
                <div className="text-xs text-neutral-500">Exposición Total</div>
              </div>
            </div>

            <details className="text-xs text-neutral-500">
              <summary className="cursor-pointer hover:text-neutral-400 transition-colors">
                ℹ️ ¿Por qué es importante diversificar proveedores?
              </summary>
              <div className="mt-2 space-y-1 pl-4">
                <p>• <strong>Continuidad del negocio</strong>: Si el único proveedor falla, no hay alternativas inmediatas</p>
                <p>• <strong>Poder de negociación</strong>: Proveedores únicos pueden subir precios sin competencia</p>
                <p>• <strong>Calidad y servicio</strong>: Múltiples proveedores permiten comparar y exigir mejor calidad</p>
                <p>• <strong>Resiliencia ante crisis</strong>: Diversificación reduce impacto de problemas externos</p>
                <p className="mt-2">• <strong>Niveles de riesgo</strong>:</p>
                <p className="pl-2">· <strong>Crítico</strong>: Gasto &gt; $500,000 | <strong>Alto</strong>: $200,000-$500,000</p>
                <p className="pl-2">· <strong>Medio</strong>: $50,000-$200,000 | <strong>Bajo</strong>: &lt; $50,000</p>
                <p className="mt-2">💡 <strong>Acción recomendada</strong>: Buscar proveedores alternativos para productos de riesgo crítico/alto</p>
              </div>
            </details>
          </div>
        </>
      )}
    </div>
  );
}

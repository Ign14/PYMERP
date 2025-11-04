import { useQuery } from "@tanstack/react-query";
import { getNegotiationOpportunities, NegotiationOpportunity as OpportunityType } from "../services/client";

export default function NegotiationOpportunities() {
  const opportunitiesQuery = useQuery<OpportunityType[], Error>({
    queryKey: ["negotiation-opportunities"],
    queryFn: () => getNegotiationOpportunities(),
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

  const getPriorityColor = (priority: string) => {
    if (priority === "HIGH") return "bg-red-950/50 text-red-400 border-red-800";
    if (priority === "MEDIUM") return "bg-orange-950/50 text-orange-400 border-orange-800";
    return "bg-yellow-950/50 text-yellow-400 border-yellow-800";
  };

  const getPriorityIcon = (priority: string) => {
    if (priority === "HIGH") return "🔴";
    if (priority === "MEDIUM") return "🟠";
    return "🟡";
  };

  const getPriorityLabel = (priority: string) => {
    if (priority === "HIGH") return "Alta";
    if (priority === "MEDIUM") return "Media";
    return "Baja";
  };

  if (opportunitiesQuery.isLoading) {
    return (
      <div className="card-content">
        <h2 className="text-lg font-semibold text-neutral-100">💰 Oportunidades de Negociación</h2>
        <p className="text-neutral-400 mt-4">Cargando oportunidades...</p>
      </div>
    );
  }

  if (opportunitiesQuery.isError) {
    return (
      <div className="card-content">
        <h2 className="text-lg font-semibold text-neutral-100">💰 Oportunidades de Negociación</h2>
        <p className="text-red-400 mt-4">{opportunitiesQuery.error?.message ?? "Error al cargar oportunidades"}</p>
      </div>
    );
  }

  const opportunities = opportunitiesQuery.data ?? [];
  const totalSavings = opportunities.reduce((sum, opp) => sum + opp.potentialSavings, 0);
  const highPriorityCount = opportunities.filter(o => o.priority === "HIGH").length;

  return (
    <div className="card-content">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-semibold text-neutral-100">💰 Oportunidades de Negociación</h2>
        {opportunities.length > 0 && (
          <div className="flex items-center gap-3">
            <div className="text-right">
              <div className="text-xs text-neutral-500">Savings Potenciales</div>
              <div className="text-lg font-bold text-green-400">{formatCurrency(totalSavings)}</div>
            </div>
            {highPriorityCount > 0 && (
              <span className="px-2 py-1 text-xs font-medium rounded bg-red-950/50 text-red-400 border border-red-800">
                {highPriorityCount} urgente{highPriorityCount !== 1 ? "s" : ""}
              </span>
            )}
          </div>
        )}
      </div>

      {opportunities.length === 0 ? (
        <div className="text-center py-12 bg-neutral-900/50 rounded border border-neutral-800">
          <p className="text-neutral-400">✅ No se detectaron oportunidades de negociación</p>
          <p className="text-xs text-neutral-500 mt-2">
            Tus precios están alineados con el mercado o no hay suficientes proveedores para comparar
          </p>
        </div>
      ) : (
        <>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-neutral-800">
                  <th className="text-left py-2 px-2 text-neutral-400 font-medium">Prioridad</th>
                  <th className="text-left py-2 px-2 text-neutral-400 font-medium">Proveedor</th>
                  <th className="text-left py-2 px-2 text-neutral-400 font-medium">Producto</th>
                  <th className="text-right py-2 px-2 text-neutral-400 font-medium">Precio Actual</th>
                  <th className="text-right py-2 px-2 text-neutral-400 font-medium">Promedio Mercado</th>
                  <th className="text-right py-2 px-2 text-neutral-400 font-medium">% Sobre</th>
                  <th className="text-right py-2 px-2 text-neutral-400 font-medium">Compras (12m)</th>
                  <th className="text-right py-2 px-2 text-neutral-400 font-medium">Savings Potencial</th>
                  <th className="text-left py-2 px-2 text-neutral-400 font-medium">Recomendación</th>
                </tr>
              </thead>
              <tbody>
                {opportunities.map((opp, idx) => (
                  <tr 
                    key={idx}
                    className="border-b border-neutral-800/50 hover:bg-neutral-900/30 transition-colors"
                  >
                    <td className="py-3 px-2">
                      <div className="flex items-center gap-2">
                        <span className="text-lg">{getPriorityIcon(opp.priority)}</span>
                        <span className={`px-2 py-0.5 text-xs font-medium rounded border ${getPriorityColor(opp.priority)}`}>
                          {getPriorityLabel(opp.priority)}
                        </span>
                      </div>
                    </td>
                    <td className="py-3 px-2 text-neutral-200 font-medium">
                      {opp.supplierName}
                    </td>
                    <td className="py-3 px-2 text-neutral-300">
                      {opp.productName}
                    </td>
                    <td className="py-3 px-2 text-right text-neutral-200 font-medium">
                      {formatCurrency(opp.currentPrice)}
                    </td>
                    <td className="py-3 px-2 text-right text-green-400">
                      {formatCurrency(opp.marketAverage)}
                    </td>
                    <td className="py-3 px-2 text-right">
                      <span className="text-red-400 font-semibold">
                        +{opp.pricePercentageAboveMarket.toFixed(1)}%
                      </span>
                    </td>
                    <td className="py-3 px-2 text-right text-neutral-300">
                      {opp.purchasesLast12Months.toLocaleString("es-CL")}
                    </td>
                    <td className="py-3 px-2 text-right">
                      <div className="flex flex-col items-end">
                        <span className="text-green-400 font-bold">
                          {formatCurrency(opp.potentialSavings)}
                        </span>
                        <span className="text-xs text-neutral-500">
                          si iguala mercado
                        </span>
                      </div>
                    </td>
                    <td className="py-3 px-2 text-neutral-400 text-xs max-w-xs">
                      {opp.recommendation}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Resumen y ayuda */}
          <div className="mt-4 pt-3 border-t border-neutral-800">
            <div className="grid grid-cols-3 gap-4 mb-3">
              <div className="text-center">
                <div className="text-2xl font-bold text-neutral-200">{opportunities.length}</div>
                <div className="text-xs text-neutral-500">Oportunidades Detectadas</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-green-400">{formatCurrency(totalSavings)}</div>
                <div className="text-xs text-neutral-500">Savings Potenciales Totales</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-red-400">{highPriorityCount}</div>
                <div className="text-xs text-neutral-500">Prioridad Alta (Urgente)</div>
              </div>
            </div>

            <details className="text-xs text-neutral-500">
              <summary className="cursor-pointer hover:text-neutral-400 transition-colors">
                ℹ️ ¿Cómo se calculan las oportunidades?
              </summary>
              <div className="mt-2 space-y-1 pl-4">
                <p>• Se comparan los precios de cada proveedor contra el promedio del mercado (otros proveedores del mismo producto)</p>
                <p>• Se detectan casos donde el precio actual está <strong>&gt;10%</strong> por encima del promedio</p>
                <p>• <strong>Savings potenciales</strong> = Diferencia de precio × Cantidad comprada en últimos 12 meses</p>
                <p>• <strong>Prioridad Alta</strong>: Savings &gt; $100,000 | <strong>Media</strong>: $50,000-$100,000 | <strong>Baja</strong>: &lt; $50,000</p>
                <p className="mt-2">💡 <strong>Acción recomendada</strong>: Contactar proveedores de prioridad alta y negociar reducción de precios</p>
              </div>
            </details>
          </div>
        </>
      )}
    </div>
  );
}

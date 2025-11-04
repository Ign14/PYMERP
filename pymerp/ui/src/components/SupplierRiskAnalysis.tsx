import { useQuery } from "@tanstack/react-query";
import { getSupplierRiskAnalysis, SupplierRiskAnalysis as RiskAnalysisType } from "../services/client";

export default function SupplierRiskAnalysis() {
  const riskQuery = useQuery<RiskAnalysisType, Error>({
    queryKey: ["supplier-risk-analysis"],
    queryFn: () => getSupplierRiskAnalysis(),
    refetchOnWindowFocus: false,
  });

  if (riskQuery.isLoading) {
    return (
      <div className="card-content">
        <h2 className="text-lg font-semibold text-neutral-100">⚠️ Análisis de Riesgo ABC</h2>
        <p className="text-neutral-400 mt-4">Cargando análisis de riesgo...</p>
      </div>
    );
  }

  if (riskQuery.isError) {
    return (
      <div className="card-content">
        <h2 className="text-lg font-semibold text-neutral-100">⚠️ Análisis de Riesgo ABC</h2>
        <p className="text-red-400 mt-4">{riskQuery.error?.message ?? "Error al cargar análisis"}</p>
      </div>
    );
  }

  const analysis = riskQuery.data;

  if (!analysis) {
    return (
      <div className="card-content">
        <h2 className="text-lg font-semibold text-neutral-100">⚠️ Análisis de Riesgo ABC</h2>
        <p className="text-neutral-400 mt-4">No hay datos disponibles</p>
      </div>
    );
  }

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat("es-CL", {
      style: "currency",
      currency: "CLP",
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  };

  const totalSuppliers = 
    analysis.categoryA.length + 
    analysis.categoryB.length + 
    analysis.categoryC.length;

  const getCategoryInfo = (category: "A" | "B" | "C") => {
    const configs = {
      A: {
        label: "Categoría A - Críticos",
        description: "Top 80% del volumen",
        color: "text-red-400",
        bgColor: "bg-red-950/50",
        borderColor: "border-red-800",
        icon: "🔴",
        suppliers: analysis.categoryA,
      },
      B: {
        label: "Categoría B - Importantes",
        description: "15% del volumen",
        color: "text-yellow-400",
        bgColor: "bg-yellow-950/50",
        borderColor: "border-yellow-800",
        icon: "🟡",
        suppliers: analysis.categoryB,
      },
      C: {
        label: "Categoría C - Ocasionales",
        description: "5% del volumen",
        color: "text-neutral-400",
        bgColor: "bg-neutral-800/50",
        borderColor: "border-neutral-700",
        icon: "⚪",
        suppliers: analysis.categoryC,
      },
    };
    return configs[category];
  };

  const isHighConcentration = analysis.concentrationIndex > 0.25;
  const concentrationLevel = 
    analysis.concentrationIndex > 0.4 ? "Muy Alto" :
    analysis.concentrationIndex > 0.25 ? "Alto" :
    analysis.concentrationIndex > 0.15 ? "Moderado" : "Bajo";

  const concentrationColor = 
    analysis.concentrationIndex > 0.4 ? "text-red-400" :
    analysis.concentrationIndex > 0.25 ? "text-orange-400" :
    analysis.concentrationIndex > 0.15 ? "text-yellow-400" : "text-green-400";

  return (
    <div className="card-content">
      <h2 className="text-lg font-semibold text-neutral-100 mb-4">⚠️ Análisis de Riesgo ABC</h2>

      {/* Resumen de concentración */}
      <div className={`p-3 rounded border mb-4 ${
        isHighConcentration 
          ? "bg-orange-950/30 border-orange-800/50" 
          : "bg-neutral-900/50 border-neutral-800"
      }`}>
        <div className="flex items-center justify-between mb-2">
          <span className="text-sm text-neutral-400">Índice de Concentración (Herfindahl)</span>
          <span className={`text-lg font-bold ${concentrationColor}`}>
            {(analysis.concentrationIndex * 100).toFixed(1)}%
          </span>
        </div>
        <div className="flex items-center justify-between">
          <span className="text-xs text-neutral-500">Nivel de riesgo:</span>
          <span className={`text-sm font-medium ${concentrationColor}`}>
            {concentrationLevel}
          </span>
        </div>
        {isHighConcentration && (
          <div className="mt-2 pt-2 border-t border-orange-800/30">
            <p className="text-xs text-orange-300">
              ⚠️ Alta concentración detectada. Considere diversificar proveedores para reducir riesgo.
            </p>
          </div>
        )}
      </div>

      {/* Estadísticas generales */}
      <div className="grid grid-cols-3 gap-3 mb-4">
        <div className="p-3 rounded bg-neutral-900/50 border border-neutral-800">
          <div className="text-xs text-neutral-500 mb-1">Total Proveedores</div>
          <div className="text-xl font-bold text-neutral-200">{totalSuppliers}</div>
        </div>
        <div className="p-3 rounded bg-neutral-900/50 border border-neutral-800">
          <div className="text-xs text-neutral-500 mb-1">Volumen Total</div>
          <div className="text-sm font-bold text-neutral-200">
            {formatCurrency(analysis.totalPurchaseVolume)}
          </div>
        </div>
        <div className="p-3 rounded bg-neutral-900/50 border border-neutral-800">
          <div className="text-xs text-neutral-500 mb-1">Productos Exclusivos</div>
          <div className="text-xl font-bold text-neutral-200">
            {analysis.singleSourceProductsCount}
          </div>
        </div>
      </div>

      {/* Categorías ABC */}
      <div className="space-y-3">
        {(["A", "B", "C"] as const).map((category) => {
          const info = getCategoryInfo(category);
          const percentage = info.suppliers.reduce((sum, s) => sum + s.percentage, 0);
          
          return (
            <div 
              key={category}
              className={`p-3 rounded border ${info.bgColor} ${info.borderColor}`}
            >
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2">
                  <span className="text-lg">{info.icon}</span>
                  <div>
                    <div className={`text-sm font-semibold ${info.color}`}>
                      {info.label}
                    </div>
                    <div className="text-xs text-neutral-500">{info.description}</div>
                  </div>
                </div>
                <div className="text-right">
                  <div className={`text-lg font-bold ${info.color}`}>
                    {info.suppliers.length}
                  </div>
                  <div className="text-xs text-neutral-500">
                    {percentage.toFixed(1)}%
                  </div>
                </div>
              </div>

              {info.suppliers.length > 0 && (
                <div className="mt-3 pt-3 border-t border-current/10">
                  <div className="text-xs text-neutral-400 mb-2">
                    Top proveedores en esta categoría:
                  </div>
                  <div className="space-y-1.5 max-h-32 overflow-y-auto">
                    {info.suppliers.slice(0, 5).map((supplier) => (
                      <div 
                        key={supplier.supplierId}
                        className="flex items-center justify-between text-xs py-1 px-2 rounded bg-black/20"
                      >
                        <span className="text-neutral-300 truncate flex-1 mr-2">
                          {supplier.supplierName}
                        </span>
                        <div className="flex items-center gap-2 flex-shrink-0">
                          <span className="text-neutral-400">
                            {formatCurrency(supplier.purchaseAmount)}
                          </span>
                          <span className={`font-medium ${info.color}`}>
                            {supplier.percentage.toFixed(1)}%
                          </span>
                        </div>
                      </div>
                    ))}
                    {info.suppliers.length > 5 && (
                      <div className="text-xs text-neutral-500 text-center pt-1">
                        +{info.suppliers.length - 5} proveedor{info.suppliers.length - 5 !== 1 ? "es" : ""} más
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Ayuda interpretación */}
      <div className="mt-4 pt-3 border-t border-neutral-800">
        <details className="text-xs text-neutral-500">
          <summary className="cursor-pointer hover:text-neutral-400 transition-colors">
            ℹ️ ¿Qué significa el análisis ABC?
          </summary>
          <div className="mt-2 space-y-1 pl-4">
            <p>• <strong>Categoría A (Críticos):</strong> Proveedores que representan el 80% del volumen de compras. Requieren gestión prioritaria.</p>
            <p>• <strong>Categoría B (Importantes):</strong> Proveedores que representan el 15% del volumen. Gestión regular.</p>
            <p>• <strong>Categoría C (Ocasionales):</strong> Proveedores que representan el 5% del volumen. Gestión simplificada.</p>
            <p className="mt-2">• <strong>Índice de Concentración:</strong> Mide qué tan dependiente es la empresa de pocos proveedores. &gt;25% indica alta concentración de riesgo.</p>
          </div>
        </details>
      </div>
    </div>
  );
}

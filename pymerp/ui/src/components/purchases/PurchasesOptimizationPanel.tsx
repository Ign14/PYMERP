import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { listPurchases } from "../../services/client";
import { createCurrencyFormatter } from "../../utils/currency";

interface PurchasesOptimizationPanelProps {
  startDate: string;
  endDate: string;
}

interface OptimizationInsight {
  id: string;
  type: "consolidation" | "duplicate" | "best-price";
  title: string;
  description: string;
  potentialSaving?: number;
  icon: string;
}

export function PurchasesOptimizationPanel({ startDate, endDate }: PurchasesOptimizationPanelProps) {
  const currencyFormatter = useMemo(() => createCurrencyFormatter(), []);
  const formatCurrency = (value: number) => currencyFormatter.format(value ?? 0);

  const { data: purchases } = useQuery({
    queryKey: ["purchases", "optimization", startDate, endDate],
    queryFn: () =>
      listPurchases({
        from: startDate ? new Date(startDate).toISOString() : undefined,
        to: endDate ? new Date(endDate).toISOString() : undefined,
        size: 10000,
      }),
  });

  const insights = useMemo(() => {
    if (!purchases?.content) return [];

    const result: OptimizationInsight[] = [];

    // 1. Oportunidades de consolidación de proveedores
    const supplierGroups = new Map<string, { count: number; total: number; name: string }>();
    
    purchases.content.forEach((p) => {
      if (!p.supplierId) return;
      const group = supplierGroups.get(p.supplierId) || { count: 0, total: 0, name: p.supplierName || '' };
      group.count += 1;
      group.total += p.total;
      supplierGroups.set(p.supplierId, group);
    });

    // Si hay muchos proveedores pequeños, sugerir consolidación
    const smallSuppliers = Array.from(supplierGroups.entries())
      .filter(([_, data]) => data.count <= 2 && data.total < 50000)
      .length;

    if (smallSuppliers > 5) {
      result.push({
        id: "consolidation-1",
        type: "consolidation",
        title: "Oportunidad de consolidación de proveedores",
        description: `Se detectaron ${smallSuppliers} proveedores con bajo volumen de compra. Consolidar podría reducir costos administrativos y mejorar poder de negociación.`,
        potentialSaving: smallSuppliers * 5000, // Estimación de ahorro
        icon: "🤝",
      });
    }

    // 2. Detección de posibles duplicados (mismo proveedor, mismo día, montos similares)
    const purchasesByDate = new Map<string, typeof purchases.content>();
    
    purchases.content.forEach((p) => {
      const date = p.issuedAt.split('T')[0];
      const key = `${p.supplierId}-${date}`;
      const existing = purchasesByDate.get(key) || [];
      existing.push(p);
      purchasesByDate.set(key, existing);
    });

    let duplicateCount = 0;
    purchasesByDate.forEach((group) => {
      if (group.length > 1) {
        // Verificar si hay montos similares (dentro del 10%)
        for (let i = 0; i < group.length; i++) {
          for (let j = i + 1; j < group.length; j++) {
            const diff = Math.abs(group[i].total - group[j].total) / group[i].total;
            if (diff < 0.1) {
              duplicateCount++;
            }
          }
        }
      }
    });

    if (duplicateCount > 0) {
      result.push({
        id: "duplicate-1",
        type: "duplicate",
        title: "Posibles órdenes duplicadas detectadas",
        description: `Se encontraron ${duplicateCount} par${duplicateCount > 1 ? "es" : ""} de órdenes sospechosas (mismo proveedor, misma fecha, montos similares). Revisar para evitar pagos duplicados.`,
        icon: "⚠️",
      });
    }

    // 3. Análisis de mejores precios por proveedor
    const priceAnalysis = new Map<string, { min: number; max: number; avg: number; count: number; name: string }>();
    
    purchases.content.forEach((p) => {
      if (!p.supplierId) return;
      const analysis = priceAnalysis.get(p.supplierId) || {
        min: Infinity,
        max: -Infinity,
        avg: 0,
        count: 0,
        name: p.supplierName || '',
      };
      
      analysis.min = Math.min(analysis.min, p.total);
      analysis.max = Math.max(analysis.max, p.total);
      analysis.avg = ((analysis.avg * analysis.count) + p.total) / (analysis.count + 1);
      analysis.count += 1;
      
      priceAnalysis.set(p.supplierId, analysis);
    });

    // Encontrar proveedores con alta variabilidad de precios
    const highVariability = Array.from(priceAnalysis.entries())
      .filter(([_, data]) => {
        if (data.count < 3) return false;
        const variance = (data.max - data.min) / data.avg;
        return variance > 0.3; // 30% de variación
      });

    if (highVariability.length > 0) {
      const [supplierId, data] = highVariability[0];
      const potentialSaving = (data.max - data.min) * data.count * 0.5; // Estimación conservadora

      result.push({
        id: "best-price-1",
        type: "best-price",
        title: "Variación de precios detectada",
        description: `${data.name} tiene una variación de precios del ${(((data.max - data.min) / data.avg) * 100).toFixed(1)}% en ${data.count} órdenes. Negociar precios consistentes podría generar ahorros.`,
        potentialSaving,
        icon: "💰",
      });
    }

    // 4. Sugerencias adicionales basadas en volumen
    const totalVolume = purchases.content.reduce((sum, p) => sum + p.total, 0);
    const avgOrderValue = totalVolume / purchases.content.length;

    if (avgOrderValue < 10000 && purchases.content.length > 20) {
      result.push({
        id: "consolidation-2",
        type: "consolidation",
        title: "Órdenes de bajo valor detectadas",
        description: `El promedio por orden es ${formatCurrency(avgOrderValue)}. Consolidar órdenes pequeñas podría reducir costos de envío y administrativos.`,
        potentialSaving: purchases.content.length * 500, // Estimación de ahorro por reducir transacciones
        icon: "📦",
      });
    }

    return result;
  }, [purchases, formatCurrency]);

  const totalPotentialSaving = insights.reduce((sum, insight) => sum + (insight.potentialSaving || 0), 0);

  if (insights.length === 0) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-6">
        <h3 className="text-lg font-semibold mb-4 text-neutral-100">Optimización de Compras</h3>
        <div className="text-center py-8 text-neutral-400">
          <div className="text-6xl mb-2">✓</div>
          <p>No se detectaron oportunidades de optimización significativas.</p>
          <p className="text-sm mt-2">El proceso de compras está funcionando eficientemente.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-6">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold flex items-center gap-2 text-neutral-100">
          <span className="text-2xl">🎯</span>
          Oportunidades de Optimización
        </h3>
        {totalPotentialSaving > 0 && (
          <div className="text-right">
            <div className="text-xs text-neutral-400">Ahorro potencial estimado</div>
            <div className="text-lg font-bold text-green-400">{formatCurrency(totalPotentialSaving)}</div>
          </div>
        )}
      </div>

      <div className="space-y-3">
        {insights.map((insight) => {
          const typeColors = {
            consolidation: "bg-blue-950 border-blue-800",
            duplicate: "bg-red-950 border-red-800",
            "best-price": "bg-green-950 border-green-800",
          };

          return (
            <div
              key={insight.id}
              className={`border rounded-lg p-4 ${typeColors[insight.type]}`}
            >
              <div className="flex items-start gap-3">
                <span className="text-3xl flex-shrink-0">{insight.icon}</span>
                <div className="flex-1 min-w-0">
                  <h4 className="font-semibold text-sm mb-1 text-neutral-100">{insight.title}</h4>
                  <p className="text-sm opacity-90 mb-2 text-neutral-300">{insight.description}</p>
                  {insight.potentialSaving && insight.potentialSaving > 0 && (
                    <div className="inline-block bg-neutral-800 bg-opacity-60 px-2 py-1 rounded text-xs font-semibold text-neutral-300">
                      💵 Ahorro estimado: {formatCurrency(insight.potentialSaving)}
                    </div>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

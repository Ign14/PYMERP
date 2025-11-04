import { useQuery } from "@tanstack/react-query";
import { getSalesABCAnalysis } from "../services/client";

export default function SalesABCRecommendations() {
  const { data: abcData = [], isLoading } = useQuery({
    queryKey: ["salesABCAnalysis"],
    queryFn: () => getSalesABCAnalysis(),
    refetchInterval: 5 * 60 * 1000,
  });

  if (isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
        <div className="animate-pulse space-y-4">
          <div className="h-6 bg-neutral-800 rounded w-1/3"></div>
          <div className="h-32 bg-neutral-800 rounded"></div>
          <div className="h-32 bg-neutral-800 rounded"></div>
        </div>
      </div>
    );
  }

  const classA = abcData.filter(item => item.classification === "A");
  const classB = abcData.filter(item => item.classification === "B");
  const classC = abcData.filter(item => item.classification === "C");

  const totalRevenue = abcData.reduce((sum, item) => sum + item.totalRevenue, 0);
  const revenueA = classA.reduce((sum, item) => sum + item.totalRevenue, 0);
  const revenueB = classB.reduce((sum, item) => sum + item.totalRevenue, 0);
  const revenueC = classC.reduce((sum, item) => sum + item.totalRevenue, 0);

  const concentrationRisk = totalRevenue > 0 ? (revenueA / totalRevenue) * 100 : 0;
  const optimizationPotential = classC.length;

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
      <h3 className="text-lg font-semibold text-white mb-6">Recomendaciones Estratégicas por Clase</h3>

      <div className="space-y-4 mb-6">
        {/* Estrategias Clase A */}
        <div className="bg-red-950 border border-red-900 rounded-lg p-4">
          <div className="flex items-center gap-2 mb-3">
            <span className="text-lg">⭐</span>
            <h4 className="text-base font-semibold text-red-100">
              Clase A - Productos Estrella ({classA.length} productos)
            </h4>
          </div>
          <ul className="space-y-2 text-sm text-red-200">
            <li className="flex items-start gap-2">
              <span className="text-red-400 mt-0.5">▪</span>
              <span>
                <strong>Stock prioritario:</strong> Mantener niveles de inventario elevados para evitar quiebres de stock
              </span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-red-400 mt-0.5">▪</span>
              <span>
                <strong>Promoción activa:</strong> Destacar en campañas de marketing, vitrinas y canales digitales
              </span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-red-400 mt-0.5">▪</span>
              <span>
                <strong>Análisis de precio:</strong> Optimizar márgenes sin sacrificar volumen de ventas
              </span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-red-400 mt-0.5">▪</span>
              <span>
                <strong>Relación con proveedores:</strong> Negociar condiciones preferenciales y continuidad de suministro
              </span>
            </li>
          </ul>
        </div>

        {/* Estrategias Clase B */}
        <div className="bg-amber-950 border border-amber-900 rounded-lg p-4">
          <div className="flex items-center gap-2 mb-3">
            <span className="text-lg">📈</span>
            <h4 className="text-base font-semibold text-amber-100">
              Clase B - Productos Importantes ({classB.length} productos)
            </h4>
          </div>
          <ul className="space-y-2 text-sm text-amber-200">
            <li className="flex items-start gap-2">
              <span className="text-amber-400 mt-0.5">▪</span>
              <span>
                <strong>Potencial de crecimiento:</strong> Evaluar estrategias para promover a Clase A mediante promociones dirigidas
              </span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-amber-400 mt-0.5">▪</span>
              <span>
                <strong>Revisión de pricing:</strong> Ajustar precios para mejorar competitividad sin afectar márgenes
              </span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-amber-400 mt-0.5">▪</span>
              <span>
                <strong>Optimización de inventario:</strong> Mantener stock equilibrado basado en demanda histórica
              </span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-amber-400 mt-0.5">▪</span>
              <span>
                <strong>Cross-selling:</strong> Bundling con productos Clase A para aumentar ticket promedio
              </span>
            </li>
          </ul>
        </div>

        {/* Estrategias Clase C */}
        <div className="bg-emerald-950 border border-emerald-900 rounded-lg p-4">
          <div className="flex items-center gap-2 mb-3">
            <span className="text-lg">🔍</span>
            <h4 className="text-base font-semibold text-emerald-100">
              Clase C - Productos Ocasionales ({classC.length} productos)
            </h4>
          </div>
          <ul className="space-y-2 text-sm text-emerald-200">
            <li className="flex items-start gap-2">
              <span className="text-emerald-400 mt-0.5">▪</span>
              <span>
                <strong>Evaluación de continuidad:</strong> Considerar descontinuar productos con baja rotación y bajo margen
              </span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-emerald-400 mt-0.5">▪</span>
              <span>
                <strong>Reducción de stock:</strong> Minimizar inventario para liberar capital de trabajo
              </span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-emerald-400 mt-0.5">▪</span>
              <span>
                <strong>Promociones liquidación:</strong> Lanzar ofertas especiales para rotar inventario estancado
              </span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-emerald-400 mt-0.5">▪</span>
              <span>
                <strong>Reemplazo estratégico:</strong> Identificar alternativas con mejor potencial de ventas
              </span>
            </li>
          </ul>
        </div>
      </div>

      {/* Insights Globales */}
      <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
        <h4 className="text-sm font-semibold text-white mb-3">💡 Análisis Global</h4>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs text-neutral-400">Concentración en Clase A</span>
              <span className="text-sm font-semibold text-white">{concentrationRisk.toFixed(1)}%</span>
            </div>
            {concentrationRisk > 85 && (
              <p className="text-xs text-red-300">
                ⚠️ Alta concentración de ingresos en pocos productos. Considerar diversificar portafolio.
              </p>
            )}
            {concentrationRisk >= 70 && concentrationRisk <= 85 && (
              <p className="text-xs text-amber-300">
                ℹ️ Concentración balanceada. Monitorear continuidad de productos Clase A.
              </p>
            )}
            {concentrationRisk < 70 && (
              <p className="text-xs text-emerald-300">
                ✓ Portafolio diversificado. Evaluar oportunidades de consolidación.
              </p>
            )}
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs text-neutral-400">Productos con bajo impacto (Clase C)</span>
              <span className="text-sm font-semibold text-white">{optimizationPotential}</span>
            </div>
            {optimizationPotential > 20 && (
              <p className="text-xs text-emerald-300">
                💡 Oportunidad: Revisar {optimizationPotential} productos para optimizar recursos y simplificar catálogo.
              </p>
            )}
            {optimizationPotential >= 10 && optimizationPotential <= 20 && (
              <p className="text-xs text-neutral-300">
                ℹ️ Portafolio manejable. Evaluar periódicamente productos de baja rotación.
              </p>
            )}
            {optimizationPotential < 10 && (
              <p className="text-xs text-neutral-300">
                ✓ Catálogo optimizado con pocos productos de baja rotación.
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Resumen Estratégico */}
      <div className="mt-6 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-3">
          <div className="text-xs text-neutral-400 mb-1">Total Productos</div>
          <div className="text-xl font-bold text-white">{abcData.length}</div>
        </div>
        <div className="bg-red-950 border border-red-900 rounded-lg p-3">
          <div className="text-xs text-red-300 mb-1">Clase A</div>
          <div className="text-xl font-bold text-white">
            {classA.length} ({totalRevenue > 0 ? ((revenueA / totalRevenue) * 100).toFixed(0) : 0}%)
          </div>
        </div>
        <div className="bg-amber-950 border border-amber-900 rounded-lg p-3">
          <div className="text-xs text-amber-300 mb-1">Clase B</div>
          <div className="text-xl font-bold text-white">
            {classB.length} ({totalRevenue > 0 ? ((revenueB / totalRevenue) * 100).toFixed(0) : 0}%)
          </div>
        </div>
        <div className="bg-emerald-950 border border-emerald-900 rounded-lg p-3">
          <div className="text-xs text-emerald-300 mb-1">Clase C</div>
          <div className="text-xl font-bold text-white">
            {classC.length} ({totalRevenue > 0 ? ((revenueC / totalRevenue) * 100).toFixed(0) : 0}%)
          </div>
        </div>
      </div>
    </div>
  );
}

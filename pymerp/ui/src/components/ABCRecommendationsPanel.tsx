import { useQuery } from "@tanstack/react-query";
import { getABCAnalysis } from "../services/client";

export default function ABCRecommendationsPanel() {
  const { data: analysis, isLoading } = useQuery({
    queryKey: ["abcAnalysis"],
    queryFn: () => getABCAnalysis(),
    refetchInterval: 300000,
  });

  if (isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6 animate-pulse">
        <div className="h-6 bg-neutral-800 rounded w-2/3 mb-4"></div>
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="h-24 bg-neutral-800 rounded"></div>
          ))}
        </div>
      </div>
    );
  }

  const classA = analysis?.filter(item => item.classification === "A") || [];
  const classB = analysis?.filter(item => item.classification === "B") || [];
  const classC = analysis?.filter(item => item.classification === "C") || [];

  const recommendations = [
    {
      classification: "A",
      title: "Productos Clase A - Alta Prioridad",
      color: "green",
      bgColor: "bg-green-900/20",
      borderColor: "border-green-800",
      textColor: "text-green-400",
      count: classA.length,
      strategies: [
        {
          icon: "📊",
          title: "Monitoreo Continuo",
          description: "Control diario de niveles de inventario y rotación",
        },
        {
          icon: "🎯",
          title: "Stock Óptimo",
          description: "Mantener niveles óptimos para evitar quiebres",
        },
        {
          icon: "🔄",
          title: "Reposición Frecuente",
          description: "Revisión y pedidos cada 1-3 días según demanda",
        },
        {
          icon: "📈",
          title: "Previsión Exacta",
          description: "Usar modelos de forecasting para proyectar demanda",
        },
      ],
    },
    {
      classification: "B",
      title: "Productos Clase B - Prioridad Media",
      color: "yellow",
      bgColor: "bg-yellow-900/20",
      borderColor: "border-yellow-800",
      textColor: "text-yellow-400",
      count: classB.length,
      strategies: [
        {
          icon: "📅",
          title: "Control Semanal",
          description: "Revisión de inventario cada 5-7 días",
        },
        {
          icon: "⚖️",
          title: "Stock Balanceado",
          description: "Mantener inventario moderado sin excesos",
        },
        {
          icon: "🔔",
          title: "Alertas Automáticas",
          description: "Configurar notificaciones de stock bajo",
        },
        {
          icon: "📊",
          title: "Análisis Trimestral",
          description: "Evaluar tendencias y ajustar políticas",
        },
      ],
    },
    {
      classification: "C",
      title: "Productos Clase C - Baja Prioridad",
      color: "orange",
      bgColor: "bg-orange-900/20",
      borderColor: "border-orange-800",
      textColor: "text-orange-400",
      count: classC.length,
      strategies: [
        {
          icon: "📆",
          title: "Revisión Mensual",
          description: "Control básico mensual o bimestral",
        },
        {
          icon: "📉",
          title: "Minimizar Inventario",
          description: "Reducir stock al mínimo necesario",
        },
        {
          icon: "🔍",
          title: "Evaluar Descontinuación",
          description: "Considerar eliminar productos de baja rotación",
        },
        {
          icon: "💡",
          title: "Pedidos por Demanda",
          description: "Comprar solo cuando hay orden confirmada",
        },
      ],
    },
  ];

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
      <div className="mb-6">
        <h3 className="text-lg font-semibold text-neutral-100 mb-2">
          Estrategias de Gestión por Clasificación
        </h3>
        <p className="text-xs text-neutral-400">
          Recomendaciones operativas basadas en el análisis ABC
        </p>
      </div>

      <div className="space-y-4">
        {recommendations.map((rec) => (
          <div
            key={rec.classification}
            className={`${rec.bgColor} border ${rec.borderColor} rounded-lg p-4`}
          >
            <div className="flex items-center justify-between mb-3">
              <h4 className={`text-sm font-semibold ${rec.textColor}`}>
                {rec.title}
              </h4>
              <span className={`px-2 py-1 rounded text-xs font-semibold ${rec.bgColor} border ${rec.borderColor} ${rec.textColor}`}>
                {rec.count} productos
              </span>
            </div>

            <div className="space-y-2">
              {rec.strategies.map((strategy, index) => (
                <div
                  key={index}
                  className="flex items-start gap-3 bg-neutral-900/50 rounded-lg p-3"
                >
                  <div className="text-xl flex-shrink-0">{strategy.icon}</div>
                  <div className="flex-1 min-w-0">
                    <div className="text-xs font-medium text-neutral-200 mb-1">
                      {strategy.title}
                    </div>
                    <div className="text-xs text-neutral-400">
                      {strategy.description}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>

      {/* Best Practices Summary */}
      <div className="mt-6 bg-blue-900/20 border border-blue-800 rounded-lg p-4">
        <div className="flex items-start gap-3">
          <div className="text-2xl">💡</div>
          <div>
            <h4 className="text-sm font-semibold text-blue-400 mb-2">
              Mejores Prácticas
            </h4>
            <ul className="text-xs text-neutral-300 space-y-1">
              <li>• Concentrar esfuerzos en productos Clase A (80% del valor)</li>
              <li>• Automatizar controles para productos Clase B</li>
              <li>• Simplificar gestión de productos Clase C</li>
              <li>• Revisar clasificación ABC cada 3-6 meses</li>
              <li>• Ajustar estrategias según estacionalidad</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}

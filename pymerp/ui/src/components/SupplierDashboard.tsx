import { useQuery } from "@tanstack/react-query";
import {
  listSuppliers,
  getSupplierRanking,
  getNegotiationOpportunities,
  getSingleSourceProducts,
  getSupplierAlerts,
  type Supplier,
  type SupplierRanking,
  type SupplierAlert
} from "../services/client";

export default function SupplierDashboard() {
  // Datos de múltiples endpoints
  const { data: allSuppliers = [] } = useQuery({
    queryKey: ["suppliers"],
    queryFn: () => listSuppliers(),
  });

  const { data: ranking = [] } = useQuery<SupplierRanking[]>({
    queryKey: ["suppliers-ranking"],
    queryFn: () => getSupplierRanking(),
  });

  const { data: opportunities = [] } = useQuery({
    queryKey: ["negotiation-opportunities"],
    queryFn: getNegotiationOpportunities,
  });

  const { data: singleSource = [] } = useQuery({
    queryKey: ["single-source-products"],
    queryFn: getSingleSourceProducts,
  });

  const { data: alerts = [] } = useQuery<SupplierAlert[]>({
    queryKey: ["supplier-alerts"],
    queryFn: getSupplierAlerts,
  });

  // Calcular KPIs ejecutivos
  const totalSuppliers = allSuppliers.length;
  const activeSuppliers = allSuppliers.filter(s => s.active !== false).length;
  // avgOnTimeRate calculado desde ranking (promedio de reliability)
  const avgOnTimeRate = ranking.length > 0
    ? ranking.reduce((sum, s) => sum + s.reliability, 0) / ranking.length
    : 0;

  const classASuppliers = ranking.filter(s => s.category === "A").length;
  const criticalRisks = singleSource.filter(p => p.riskLevel === "CRITICAL").length;
  const highPriorityOpportunities = opportunities.filter(o => o.priority === "HIGH").length;
  
  const potentialSavings = opportunities
    .filter(o => o.priority === "HIGH" || o.priority === "MEDIUM")
    .reduce((sum, o) => sum + o.potentialSavings, 0);

  // Top 3 acciones urgentes
  const urgentActions: Array<{
    icon: string;
    title: string;
    description: string;
    severity: "critical" | "high" | "medium";
    link?: string;
  }> = [];

  if (criticalRisks > 0) {
    urgentActions.push({
      icon: "⚠️",
      title: `${criticalRisks} Productos en Riesgo Crítico`,
      description: "Productos con un solo proveedor y alta exposición. Buscar alternativas.",
      severity: "critical",
      link: "#single-source"
    });
  }

  if (highPriorityOpportunities > 0) {
    urgentActions.push({
      icon: "💰",
      title: `${highPriorityOpportunities} Oportunidades de Ahorro`,
      description: `Potencial de ahorro: $${potentialSavings.toLocaleString("es-CL")}`,
      severity: "high",
      link: "#negotiation"
    });
  }

  const criticalAlerts = alerts.filter(a => a.severity === "CRITICAL");
  if (criticalAlerts.length > 0) {
    urgentActions.push({
      icon: "🔴",
      title: `${criticalAlerts.length} Alertas Críticas`,
      description: criticalAlerts[0]?.message ?? "Requieren atención inmediata",
      severity: "critical",
      link: "#alerts"
    });
  }

  const warningAlerts = alerts.filter(a => a.severity === "WARNING");
  if (warningAlerts.length > 0 && urgentActions.length < 3) {
    urgentActions.push({
      icon: "🟠",
      title: `${warningAlerts.length} Alertas de Advertencia`,
      description: warningAlerts[0]?.message ?? "Requieren revisión pronta",
      severity: "high",
      link: "#alerts"
    });
  }

  // Recomendaciones estratégicas
  const recommendations: Array<{
    icon: string;
    text: string;
    action: string;
  }> = [];

  if (classASuppliers < 5) {
    recommendations.push({
      icon: "📊",
      text: `Solo ${classASuppliers} proveedores clase A. Considere expandir su base estratégica.`,
      action: "Ver ranking completo"
    });
  }

  if (avgOnTimeRate < 90) {
    recommendations.push({
      icon: "⏱️",
      text: `Tasa de entrega a tiempo: ${avgOnTimeRate.toFixed(1)}%. Revisar SLAs con proveedores.`,
      action: "Ver performance"
    });
  }

  if (singleSource.length > 10) {
    recommendations.push({
      icon: "🔄",
      text: `${singleSource.length} productos con único proveedor. Riesgo de dependencia.`,
      action: "Diversificar fuentes"
    });
  }

  const severityColors = {
    critical: "border-red-500/30 bg-red-500/10 text-red-300",
    high: "border-orange-500/30 bg-orange-500/10 text-orange-300",
    medium: "border-yellow-500/30 bg-yellow-500/10 text-yellow-300"
  };

  return (
    <div className="rounded-lg border border-neutral-800 bg-neutral-900 p-6">
      <h2 className="mb-6 text-xl font-semibold text-neutral-100">
        🎯 Dashboard Ejecutivo de Proveedores
      </h2>

      {/* KPIs principales */}
      <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded border border-neutral-800 bg-neutral-950 p-4">
          <p className="text-sm text-neutral-400">Total Proveedores</p>
          <p className="mt-1 text-3xl font-bold text-neutral-100">{totalSuppliers}</p>
          <p className="mt-1 text-xs text-neutral-500">{activeSuppliers} activos</p>
        </div>

        <div className="rounded border border-neutral-800 bg-neutral-950 p-4">
          <p className="text-sm text-neutral-400">Proveedores Clase A</p>
          <p className="mt-1 text-3xl font-bold text-green-400">{classASuppliers}</p>
          <p className="mt-1 text-xs text-neutral-500">Estratégicos (80% volumen)</p>
        </div>

        <div className="rounded border border-neutral-800 bg-neutral-950 p-4">
          <p className="text-sm text-neutral-400">Entrega a Tiempo</p>
          <p className="mt-1 text-3xl font-bold text-blue-400">{avgOnTimeRate.toFixed(1)}%</p>
          <p className="mt-1 text-xs text-neutral-500">Promedio últimos 12 meses</p>
        </div>

        <div className="rounded border border-neutral-800 bg-neutral-950 p-4">
          <p className="text-sm text-neutral-400">Ahorro Potencial</p>
          <p className="mt-1 text-3xl font-bold text-yellow-400">
            ${(potentialSavings / 1000).toFixed(0)}k
          </p>
          <p className="mt-1 text-xs text-neutral-500">Oportunidades detectadas</p>
        </div>
      </div>

      {/* Acciones urgentes */}
      {urgentActions.length > 0 && (
        <div className="mb-6">
          <h3 className="mb-3 text-sm font-medium text-neutral-300">
            🚨 Acciones Prioritarias
          </h3>
          <div className="space-y-2">
            {urgentActions.slice(0, 3).map((action, idx) => (
              <div
                key={idx}
                className={`flex items-start gap-3 rounded border p-3 ${severityColors[action.severity]}`}
              >
                <span className="text-2xl">{action.icon}</span>
                <div className="flex-1">
                  <p className="font-medium">{action.title}</p>
                  <p className="mt-1 text-sm opacity-80">{action.description}</p>
                </div>
                {action.link && (
                  <a
                    href={action.link}
                    className="rounded bg-neutral-900/50 px-3 py-1 text-sm hover:bg-neutral-900"
                  >
                    Ver
                  </a>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Distribución de clasificación ABC */}
      <div className="mb-6 rounded border border-neutral-800 bg-neutral-950 p-4">
        <h3 className="mb-3 text-sm font-medium text-neutral-300">
          📊 Clasificación ABC de Proveedores
        </h3>
        <div className="space-y-2">
          {["A", "B", "C"].map(classification => {
            const count = ranking.filter(s => s.category === classification).length;
            const percentage = totalSuppliers > 0 ? (count / totalSuppliers) * 100 : 0;
            const colors = {
              A: "bg-green-500",
              B: "bg-yellow-500",
              C: "bg-blue-500"
            };

            return (
              <div key={classification} className="flex items-center gap-3">
                <div className="w-16 text-sm text-neutral-400">
                  Clase {classification}
                </div>
                <div className="flex-1">
                  <div className="h-6 w-full rounded bg-neutral-800">
                    <div
                      className={`h-full rounded ${colors[classification as keyof typeof colors]} flex items-center justify-center text-xs font-medium text-white`}
                      style={{ width: `${percentage}%` }}
                    >
                      {percentage > 5 && `${count} (${percentage.toFixed(0)}%)`}
                    </div>
                  </div>
                </div>
                <div className="w-20 text-right text-sm text-neutral-400">
                  {count} prov.
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Recomendaciones estratégicas */}
      {recommendations.length > 0 && (
        <div className="mb-6">
          <h3 className="mb-3 text-sm font-medium text-neutral-300">
            💡 Recomendaciones Estratégicas
          </h3>
          <div className="space-y-2">
            {recommendations.map((rec, idx) => (
              <div
                key={idx}
                className="flex items-start gap-3 rounded border border-blue-500/30 bg-blue-500/10 p-3 text-blue-300"
              >
                <span className="text-xl">{rec.icon}</span>
                <div className="flex-1">
                  <p className="text-sm">{rec.text}</p>
                  <p className="mt-1 text-xs opacity-70">{rec.action}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Resumen de riesgos */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div className="rounded border border-neutral-800 bg-neutral-950 p-4">
          <h3 className="mb-2 text-sm font-medium text-neutral-300">
            Productos en Riesgo
          </h3>
          <div className="space-y-2">
            {["CRITICAL", "HIGH", "MEDIUM", "LOW"].map(level => {
              const count = singleSource.filter(p => p.riskLevel === level).length;
              const colors = {
                CRITICAL: "text-red-400",
                HIGH: "text-orange-400",
                MEDIUM: "text-yellow-400",
                LOW: "text-green-400"
              };

              return count > 0 ? (
                <div key={level} className="flex justify-between text-sm">
                  <span className="text-neutral-400">{level}</span>
                  <span className={colors[level as keyof typeof colors]}>{count}</span>
                </div>
              ) : null;
            })}
          </div>
        </div>

        <div className="rounded border border-neutral-800 bg-neutral-950 p-4">
          <h3 className="mb-2 text-sm font-medium text-neutral-300">
            Oportunidades de Ahorro
          </h3>
          <div className="space-y-2">
            {["HIGH", "MEDIUM", "LOW"].map(priority => {
              const count = opportunities.filter(o => o.priority === priority).length;
              const savings = opportunities
                .filter(o => o.priority === priority)
                .reduce((sum, o) => sum + o.potentialSavings, 0);
              const colors = {
                HIGH: "text-red-400",
                MEDIUM: "text-orange-400",
                LOW: "text-yellow-400"
              };

              return count > 0 ? (
                <div key={priority} className="flex justify-between text-sm">
                  <span className="text-neutral-400">{priority} ({count})</span>
                  <span className={colors[priority as keyof typeof colors]}>
                    ${(savings / 1000).toFixed(0)}k
                  </span>
                </div>
              ) : null;
            })}
          </div>
        </div>
      </div>

      {/* Ayuda */}
      <details className="mt-6 rounded border border-neutral-800 bg-neutral-950 p-4">
        <summary className="cursor-pointer text-sm text-neutral-400 hover:text-neutral-300">
          ℹ️ Acerca de este dashboard
        </summary>
        <div className="mt-3 space-y-2 text-sm text-neutral-500">
          <p>
            Este dashboard ejecutivo consolida los principales indicadores de gestión
            de proveedores para facilitar la toma de decisiones estratégicas.
          </p>
          <ul className="list-inside list-disc space-y-1 pl-2">
            <li>
              <strong>Acciones Prioritarias:</strong> Identifica automáticamente las acciones
              más urgentes basadas en riesgos y oportunidades detectadas.
            </li>
            <li>
              <strong>Clasificación ABC:</strong> Proveedores A (80% volumen), B (15%), C (5%).
            </li>
            <li>
              <strong>Riesgos:</strong> Productos con único proveedor clasificados por exposición.
            </li>
            <li>
              <strong>Ahorro:</strong> Oportunidades de negociación por precios sobre mercado.
            </li>
          </ul>
        </div>
      </details>
    </div>
  );
}

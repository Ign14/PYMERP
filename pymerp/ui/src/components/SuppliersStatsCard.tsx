import { useQuery } from "@tanstack/react-query";
import { listSuppliers, Supplier, getSupplierAlerts, SupplierAlert } from "../services/client";
import { useMemo } from "react";

export default function SuppliersStatsCard() {
  const suppliersQuery = useQuery<Supplier[], Error>({
    queryKey: ["suppliers"],
    queryFn: () => listSuppliers(),
    refetchOnWindowFocus: false,
  });

  const alertsQuery = useQuery<SupplierAlert[], Error>({
    queryKey: ["supplier-alerts"],
    queryFn: () => getSupplierAlerts(),
    refetchOnWindowFocus: false,
  });

  const stats = useMemo(() => {
    const suppliers = suppliersQuery.data ?? [];
    const total = suppliers.length;
    const active = suppliers.filter((s) => s.active !== false).length;
    const inactive = total - active;

    // Calcular tendencia (simulada para primera versión - se puede mejorar con datos históricos)
    const newThisMonth = 0; // TODO: agregar campo createdAt en frontend para calcular

    return { total, active, inactive, newThisMonth };
  }, [suppliersQuery.data]);

  const criticalAlerts = useMemo(() => {
    return (alertsQuery.data ?? []).filter(a => a.severity === "CRITICAL" || a.severity === "WARNING");
  }, [alertsQuery.data]);

  if (suppliersQuery.isLoading) {
    return (
      <div className="card-content">
        <h2>Estadísticas de Proveedores</h2>
        <p className="text-neutral-400">Cargando estadísticas...</p>
      </div>
    );
  }

  if (suppliersQuery.isError) {
    return (
      <div className="card-content">
        <h2>Estadísticas de Proveedores</h2>
        <p className="text-red-400">{suppliersQuery.error?.message ?? "Error al cargar estadísticas"}</p>
      </div>
    );
  }

  const suppliers = suppliersQuery.data ?? [];

  // Agrupar por comuna
  const byCommuneMap = new Map<string, number>();
  suppliers.forEach((supplier) => {
    const commune = supplier.commune?.trim() || "Sin especificar";
    byCommuneMap.set(commune, (byCommuneMap.get(commune) || 0) + 1);
  });
  const byCommune = Array.from(byCommuneMap.entries())
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5);

  // Agrupar por actividad
  const byActivityMap = new Map<string, number>();
  suppliers.forEach((supplier) => {
    const activity = supplier.businessActivity?.trim() || "Sin especificar";
    byActivityMap.set(activity, (byActivityMap.get(activity) || 0) + 1);
  });
  const byActivity = Array.from(byActivityMap.entries())
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5);

  return (
    <div className="card-content">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-semibold text-neutral-100">📊 Estadísticas de Proveedores</h2>
        {criticalAlerts.length > 0 && (
          <span className="px-2 py-1 text-xs font-medium rounded bg-yellow-950/50 text-yellow-400 border border-yellow-800">
            {criticalAlerts.length} alerta{criticalAlerts.length !== 1 ? 's' : ''}
          </span>
        )}
      </div>

      {/* Alertas críticas */}
      {criticalAlerts.length > 0 && (
        <div className="mb-4 space-y-2">
          {criticalAlerts.slice(0, 2).map((alert, idx) => (
            <div 
              key={idx}
              className={`rounded-lg p-3 text-sm border ${
                alert.severity === "CRITICAL" 
                  ? "bg-red-950/30 border-red-800 text-red-400"
                  : "bg-yellow-950/30 border-yellow-800 text-yellow-400"
              }`}
            >
              <div className="flex items-start gap-2">
                <span className="text-lg">{alert.severity === "CRITICAL" ? "🔴" : "⚠️"}</span>
                <div className="flex-1">
                  <p className="font-medium">{alert.message}</p>
                  {alert.supplierName && alert.supplierName !== "Concentración de Compras" && (
                    <p className="text-xs mt-1 opacity-80">{alert.supplierName}</p>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* KPIs principales */}
      <div className="grid grid-cols-3 gap-3 mb-4">
        <div className="rounded-lg border border-neutral-800 bg-neutral-900/50 p-3">
          <div className="text-xs text-neutral-400 mb-1">Total</div>
          <div className="text-2xl font-bold text-neutral-100">{stats.total}</div>
          {stats.newThisMonth > 0 && (
            <div className="text-xs text-green-400 mt-1">+{stats.newThisMonth} este mes</div>
          )}
        </div>
        <div className="rounded-lg border border-green-800 bg-green-950/30 p-3">
          <div className="text-xs text-green-400 mb-1">Activos</div>
          <div className="text-2xl font-bold text-green-300">{stats.active}</div>
          <div className="text-xs text-green-500 mt-1">{stats.total > 0 ? Math.round((stats.active / stats.total) * 100) : 0}% del total</div>
        </div>
        <div className="rounded-lg border border-neutral-700 bg-neutral-800/30 p-3">
          <div className="text-xs text-neutral-400 mb-1">Inactivos</div>
          <div className="text-2xl font-bold text-neutral-300">{stats.inactive}</div>
          <div className="text-xs text-neutral-500 mt-1">{stats.total > 0 ? Math.round((stats.inactive / stats.total) * 100) : 0}% del total</div>
        </div>
      </div>

      {/* Advertencia si hay más inactivos que activos */}
      {stats.inactive > stats.active && stats.total > 0 && (
        <div className="mb-4 rounded-lg border border-orange-800 bg-orange-950/30 p-3 text-sm text-orange-400">
          <div className="flex items-start gap-2">
            <span>⚠️</span>
            <p>Más proveedores inactivos que activos - Revisar cartera</p>
          </div>
        </div>
      )}

      {/* Top 5 por comuna */}
      {byCommune.length > 0 && (
        <div className="mb-4">
          <h3 className="text-sm font-medium text-neutral-300 mb-2">📍 Por Comuna (Top 5)</h3>
          <ul className="space-y-1.5">
            {byCommune.map(([commune, count]) => (
              <li key={commune} className="flex items-center justify-between text-sm">
                <span className="text-neutral-400">{commune}</span>
                <span className="px-2 py-0.5 rounded bg-neutral-800 text-neutral-100 font-medium text-xs">
                  {count}
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Top 5 por actividad */}
      {byActivity.length > 0 && (
        <div>
          <h3 className="text-sm font-medium text-neutral-300 mb-2">🏢 Por Actividad (Top 5)</h3>
          <ul className="space-y-1.5">
            {byActivity.map(([activity, count]) => (
              <li key={activity} className="flex items-center justify-between text-sm">
                <span className="text-neutral-400 truncate flex-1">{activity}</span>
                <span className="px-2 py-0.5 rounded bg-neutral-800 text-neutral-100 font-medium text-xs ml-2">
                  {count}
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

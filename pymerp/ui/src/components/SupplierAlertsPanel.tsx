import { useQuery } from "@tanstack/react-query";
import { getSupplierAlerts, SupplierAlert } from "../services/client";
import { useMemo } from "react";

export default function SupplierAlertsPanel() {
  const alertsQuery = useQuery<SupplierAlert[], Error>({
    queryKey: ["supplier-alerts"],
    queryFn: () => getSupplierAlerts(),
    refetchOnWindowFocus: false,
  });

  const groupedAlerts = useMemo(() => {
    const alerts = alertsQuery.data ?? [];
    return {
      critical: alerts.filter(a => a.severity === "CRITICAL"),
      warning: alerts.filter(a => a.severity === "WARNING"),
      info: alerts.filter(a => a.severity === "INFO"),
    };
  }, [alertsQuery.data]);

  const totalAlerts = (alertsQuery.data ?? []).length;

  if (alertsQuery.isLoading) {
    return (
      <div className="card-content">
        <h2 className="text-lg font-semibold text-neutral-100 mb-4">🔔 Alertas de Proveedores</h2>
        <p className="text-neutral-400 text-sm">Cargando alertas...</p>
      </div>
    );
  }

  if (alertsQuery.isError) {
    return (
      <div className="card-content">
        <h2 className="text-lg font-semibold text-neutral-100 mb-4">🔔 Alertas de Proveedores</h2>
        <div className="rounded-lg border border-red-800 bg-red-950/30 p-4 text-sm text-red-400">
          Error al cargar alertas: {alertsQuery.error.message}
        </div>
      </div>
    );
  }

  const getSeverityStyles = (severity: SupplierAlert["severity"]) => {
    switch (severity) {
      case "CRITICAL":
        return {
          container: "bg-red-950/30 border-red-800",
          text: "text-red-400",
          icon: "🔴",
        };
      case "WARNING":
        return {
          container: "bg-yellow-950/30 border-yellow-800",
          text: "text-yellow-400",
          icon: "⚠️",
        };
      case "INFO":
        return {
          container: "bg-blue-950/30 border-blue-800",
          text: "text-blue-400",
          icon: "ℹ️",
        };
    }
  };

  const getTypeLabel = (type: SupplierAlert["type"]) => {
    switch (type) {
      case "NO_RECENT_PURCHASES":
        return "Sin compras recientes";
      case "INACTIVE_SUPPLIER":
        return "Proveedor inactivo";
      case "HIGH_CONCENTRATION":
        return "Alta concentración";
      case "SINGLE_SOURCE":
        return "Fuente única";
      default:
        return type;
    }
  };

  return (
    <div className="card-content">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-semibold text-neutral-100">🔔 Alertas de Proveedores</h2>
        {totalAlerts > 0 && (
          <span className="px-2.5 py-1 text-xs font-medium rounded-full bg-neutral-800 text-neutral-100">
            {totalAlerts} alerta{totalAlerts !== 1 ? 's' : ''}
          </span>
        )}
      </div>

      {totalAlerts === 0 && (
        <div className="text-center py-8">
          <div className="text-4xl mb-2">✅</div>
          <p className="text-sm text-neutral-400">No hay alertas pendientes</p>
          <p className="text-xs text-neutral-500 mt-1">Todos los proveedores están en buen estado</p>
        </div>
      )}

      {totalAlerts > 0 && (
        <div className="space-y-3">
          {/* Alertas críticas */}
          {groupedAlerts.critical.length > 0 && (
            <div>
              <h3 className="text-sm font-medium text-red-400 mb-2 flex items-center gap-2">
                <span>🔴</span>
                <span>Críticas ({groupedAlerts.critical.length})</span>
              </h3>
              <div className="space-y-2">
                {groupedAlerts.critical.map((alert, idx) => {
                  const styles = getSeverityStyles(alert.severity);
                  return (
                    <div
                      key={idx}
                      className={`rounded-lg border p-3 ${styles.container}`}
                    >
                      <div className="flex items-start gap-2">
                        <span className="text-lg flex-shrink-0">{styles.icon}</span>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 mb-1">
                            <span className={`text-xs font-medium px-2 py-0.5 rounded ${styles.container}`}>
                              {getTypeLabel(alert.type)}
                            </span>
                          </div>
                          <p className={`text-sm font-medium ${styles.text} mb-1`}>
                            {alert.message}
                          </p>
                          {alert.supplierName && alert.type !== "HIGH_CONCENTRATION" && (
                            <p className="text-xs text-neutral-400 mb-2">
                              Proveedor: {alert.supplierName}
                            </p>
                          )}
                          {alert.daysWithoutPurchases !== null && (
                            <p className="text-xs text-neutral-500">
                              {alert.daysWithoutPurchases} días sin actividad
                            </p>
                          )}
                          {alert.concentrationPercentage !== null && (
                            <p className="text-xs text-neutral-500">
                              {alert.concentrationPercentage.toFixed(1)}% de concentración
                            </p>
                          )}
                          {alert.actionLabel && (
                            <button className="mt-2 text-xs font-medium underline hover:no-underline">
                              {alert.actionLabel}
                            </button>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Alertas de advertencia */}
          {groupedAlerts.warning.length > 0 && (
            <div>
              <h3 className="text-sm font-medium text-yellow-400 mb-2 flex items-center gap-2">
                <span>⚠️</span>
                <span>Advertencias ({groupedAlerts.warning.length})</span>
              </h3>
              <div className="space-y-2">
                {groupedAlerts.warning.map((alert, idx) => {
                  const styles = getSeverityStyles(alert.severity);
                  return (
                    <div
                      key={idx}
                      className={`rounded-lg border p-3 ${styles.container}`}
                    >
                      <div className="flex items-start gap-2">
                        <span className="text-lg flex-shrink-0">{styles.icon}</span>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 mb-1">
                            <span className={`text-xs font-medium px-2 py-0.5 rounded ${styles.container}`}>
                              {getTypeLabel(alert.type)}
                            </span>
                          </div>
                          <p className={`text-sm font-medium ${styles.text} mb-1`}>
                            {alert.message}
                          </p>
                          {alert.supplierName && alert.type !== "HIGH_CONCENTRATION" && (
                            <p className="text-xs text-neutral-400 mb-2">
                              Proveedor: {alert.supplierName}
                            </p>
                          )}
                          {alert.daysWithoutPurchases !== null && (
                            <p className="text-xs text-neutral-500">
                              {alert.daysWithoutPurchases} días sin actividad
                            </p>
                          )}
                          {alert.concentrationPercentage !== null && (
                            <p className="text-xs text-neutral-500">
                              {alert.concentrationPercentage.toFixed(1)}% de concentración
                            </p>
                          )}
                          {alert.actionLabel && (
                            <button className="mt-2 text-xs font-medium underline hover:no-underline">
                              {alert.actionLabel}
                            </button>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Alertas informativas */}
          {groupedAlerts.info.length > 0 && (
            <div>
              <h3 className="text-sm font-medium text-blue-400 mb-2 flex items-center gap-2">
                <span>ℹ️</span>
                <span>Informativas ({groupedAlerts.info.length})</span>
              </h3>
              <div className="space-y-2">
                {groupedAlerts.info.slice(0, 3).map((alert, idx) => {
                  const styles = getSeverityStyles(alert.severity);
                  return (
                    <div
                      key={idx}
                      className={`rounded-lg border p-3 ${styles.container}`}
                    >
                      <div className="flex items-start gap-2">
                        <span className="text-lg flex-shrink-0">{styles.icon}</span>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 mb-1">
                            <span className={`text-xs font-medium px-2 py-0.5 rounded ${styles.container}`}>
                              {getTypeLabel(alert.type)}
                            </span>
                          </div>
                          <p className={`text-sm ${styles.text} mb-1`}>
                            {alert.message}
                          </p>
                          {alert.supplierName && (
                            <p className="text-xs text-neutral-400">
                              Proveedor: {alert.supplierName}
                            </p>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
                {groupedAlerts.info.length > 3 && (
                  <p className="text-xs text-neutral-500 text-center py-2">
                    +{groupedAlerts.info.length - 3} alerta{groupedAlerts.info.length - 3 !== 1 ? 's' : ''} más...
                  </p>
                )}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { createCurrencyFormatter } from "../../utils/currency";
import { listPurchases, PurchaseSummary } from "../../services/client";

type PurchasesDashboardOverviewProps = {
  startDate: string;
  endDate: string;
  statusFilter?: string;
};

export default function PurchasesDashboardOverview({ 
  startDate, 
  endDate,
  statusFilter 
}: PurchasesDashboardOverviewProps) {
  const currencyFormatter = useMemo(() => createCurrencyFormatter(), []);
  const formatCurrency = (value: number) => currencyFormatter.format(value ?? 0);

  // Calcular días en el rango actual
  const days = useMemo(() => {
    const start = new Date(startDate);
    const end = new Date(endDate);
    const diffTime = Math.abs(end.getTime() - start.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
    return diffDays;
  }, [startDate, endDate]);

  // Calcular rango anterior (mismo número de días antes del startDate)
  const previousRange = useMemo(() => {
    const start = new Date(startDate);
    const prevEnd = new Date(start);
    prevEnd.setDate(prevEnd.getDate() - 1);
    const prevStart = new Date(prevEnd);
    prevStart.setDate(prevStart.getDate() - days + 1);
    return {
      start: prevStart.toISOString().split('T')[0],
      end: prevEnd.toISOString().split('T')[0]
    };
  }, [startDate, days]);

  // Obtener todas las compras del rango actual (sin paginación)
  const purchasesQuery = useQuery({
    queryKey: ["purchases-stats", startDate, endDate, statusFilter],
    queryFn: async () => {
      const result = await listPurchases({
        page: 0,
        size: 10000,
        status: statusFilter || undefined,
        from: new Date(startDate + "T00:00:00").toISOString(),
        to: new Date(endDate + "T23:59:59").toISOString(),
      });
      return result.content ?? [];
    },
  });

  // Obtener compras del período anterior para comparación
  const previousPurchasesQuery = useQuery({
    queryKey: ["purchases-stats-previous", previousRange.start, previousRange.end, statusFilter],
    queryFn: async () => {
      const result = await listPurchases({
        page: 0,
        size: 10000,
        status: statusFilter || undefined,
        from: new Date(previousRange.start + "T00:00:00").toISOString(),
        to: new Date(previousRange.end + "T23:59:59").toISOString(),
      });
      return result.content ?? [];
    },
  });

  const purchases = purchasesQuery.data ?? [];
  const previousPurchases = previousPurchasesQuery.data ?? [];

  const stats = useMemo(() => {
    // Estadísticas del período actual
    const total = purchases.reduce((sum, p) => sum + (p.total ?? 0), 0);
    const received = purchases.filter(p => p.status?.toLowerCase() === 'received');
    const cancelled = purchases.filter(p => p.status?.toLowerCase() === 'cancelled');
    const pending = purchases.filter(p => p.status?.toLowerCase() !== 'received' && p.status?.toLowerCase() !== 'cancelled');
    
    const dailyAverage = days > 0 ? total / days : 0;

    // Estadísticas del período anterior
    const previousTotal = previousPurchases.reduce((sum, p) => sum + (p.total ?? 0), 0);
    const previousCount = previousPurchases.length;
    const previousDailyAverage = days > 0 ? previousTotal / days : 0;
    const previousReceived = previousPurchases.filter(p => p.status?.toLowerCase() === 'received').length;

    // Calcular variaciones porcentuales
    const totalVariation = previousTotal > 0 ? ((total - previousTotal) / previousTotal) * 100 : 0;
    const countVariation = previousCount > 0 ? ((purchases.length - previousCount) / previousCount) * 100 : 0;
    const dailyVariation = previousDailyAverage > 0 ? ((dailyAverage - previousDailyAverage) / previousDailyAverage) * 100 : 0;
    const receivedVariation = previousReceived > 0 ? ((received.length - previousReceived) / previousReceived) * 100 : 0;

    // Calcular top proveedores
    const supplierTotals = new Map<string, { name: string; total: number; count: number }>();
    purchases.forEach(p => {
      const key = p.supplierId ?? 'unknown';
      const name = p.supplierName ?? 'Sin proveedor';
      const existing = supplierTotals.get(key) ?? { name, total: 0, count: 0 };
      supplierTotals.set(key, {
        name,
        total: existing.total + (p.total ?? 0),
        count: existing.count + 1,
      });
    });

    const topSuppliers = Array.from(supplierTotals.values())
      .sort((a, b) => b.total - a.total)
      .slice(0, 5);

    // Alertas tempranas
    const alerts = [];
    if (totalVariation > 20) alerts.push({ type: 'warning', message: 'Gasto aumentó >20%' });
    if (cancelled.length > purchases.length * 0.1) alerts.push({ type: 'error', message: 'Alta tasa de cancelación' });
    if (pending.length > received.length) alerts.push({ type: 'info', message: 'Muchas órdenes pendientes' });

    return {
      total,
      count: purchases.length,
      dailyAverage,
      received: received.length,
      cancelled: cancelled.length,
      pending: pending.length,
      topSuppliers,
      totalVariation,
      countVariation,
      dailyVariation,
      receivedVariation,
      alerts,
    };
  }, [purchases, previousPurchases, days]);

  const rangeLabel = `Últimos ${days} días`;

  if (purchasesQuery.isLoading || previousPurchasesQuery.isLoading) {
    return (
      <section className="dashboard-overview">
        <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5" style={{ marginBottom: "1.5rem" }}>
          <header className="card-header">
            <h2 className="text-neutral-100">Resumen de compras</h2>
          </header>
          <div className="kpi-grid" style={{ marginBottom: "1.5rem" }}>
            <div className="animate-pulse bg-neutral-800 rounded-lg h-32"></div>
            <div className="animate-pulse bg-neutral-800 rounded-lg h-32"></div>
            <div className="animate-pulse bg-neutral-800 rounded-lg h-32"></div>
            <div className="animate-pulse bg-neutral-800 rounded-lg h-32"></div>
          </div>
        </div>
      </section>
    );
  }

  if (purchasesQuery.isError || previousPurchasesQuery.isError) {
    return (
      <section className="dashboard-overview">
        <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5" style={{ marginBottom: "1.5rem" }}>
          <header className="card-header">
            <h2 className="text-neutral-100">Resumen de compras</h2>
          </header>
          <div className="bg-red-950 border border-red-800 rounded-lg p-4">
            <p className="text-red-400">Error al cargar métricas de compras</p>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="dashboard-overview">
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5" style={{ marginBottom: "1.5rem" }}>
        <header className="card-header" style={{ marginBottom: "1rem" }}>
          <h2 className="text-neutral-100">Resumen de compras</h2>
        </header>

        {/* Alertas tempranas */}
        {stats.alerts.length > 0 && (
          <div style={{ marginBottom: "1.5rem", display: "flex", flexDirection: "column", gap: "0.5rem" }}>
            {stats.alerts.map((alert, idx) => (
              <div
                key={idx}
                className={`rounded-lg p-3 border ${
                  alert.type === 'error'
                    ? 'bg-red-950 border-red-800 text-red-400'
                    : alert.type === 'warning'
                    ? 'bg-yellow-950 border-yellow-800 text-yellow-400'
                    : 'bg-blue-950 border-blue-800 text-blue-400'
                }`}
                style={{ fontSize: "0.875rem" }}
              >
                <span>{alert.type === 'error' ? '🔴' : alert.type === 'warning' ? '🟡' : 'ℹ️'}</span> {alert.message}
              </div>
            ))}
          </div>
        )}

        <div className="kpi-grid" style={{ marginBottom: "1.5rem" }}>
          {/* Gasto acumulado */}
          <article className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5 stat">
            <h3 className="text-neutral-100">Gasto acumulado</h3>
            <p className="stat-value text-neutral-100">{formatCurrency(stats.total)}</p>
            <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", marginTop: "0.5rem" }}>
              <span
                className={`inline-flex items-center px-2 py-1 rounded-md text-xs font-medium border ${
                  stats.totalVariation > 0
                    ? 'bg-red-950 text-red-400 border-red-800'
                    : stats.totalVariation < 0
                    ? 'bg-green-950 text-green-400 border-green-800'
                    : 'bg-neutral-800 text-neutral-400 border-neutral-700'
                }`}
              >
                {stats.totalVariation > 0 ? '↑' : stats.totalVariation < 0 ? '↓' : '−'}{' '}
                {Math.abs(stats.totalVariation).toFixed(1)}%
              </span>
              <span className="text-neutral-400 text-xs">vs período anterior</span>
            </div>
            <span className="muted small text-neutral-400" style={{ display: "block", marginTop: "0.5rem" }}>
              {stats.count} órdenes registradas
            </span>
          </article>

          {/* Promedio diario */}
          <article className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5 stat">
            <h3 className="text-neutral-100">Promedio diario</h3>
            <p className="stat-value text-neutral-100">{formatCurrency(stats.dailyAverage)}</p>
            <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", marginTop: "0.5rem" }}>
              <span
                className={`inline-flex items-center px-2 py-1 rounded-md text-xs font-medium border ${
                  stats.dailyVariation > 0
                    ? 'bg-red-950 text-red-400 border-red-800'
                    : stats.dailyVariation < 0
                    ? 'bg-green-950 text-green-400 border-green-800'
                    : 'bg-neutral-800 text-neutral-400 border-neutral-700'
                }`}
              >
                {stats.dailyVariation > 0 ? '↑' : stats.dailyVariation < 0 ? '↓' : '−'}{' '}
                {Math.abs(stats.dailyVariation).toFixed(1)}%
              </span>
              <span className="text-neutral-400 text-xs">vs período anterior</span>
            </div>
          </article>

          {/* Estado de órdenes */}
          <article className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5 stat">
            <h3 className="text-neutral-100">Órdenes recibidas</h3>
            <p className="stat-value stat-success text-neutral-100">{stats.received}</p>
            <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", marginTop: "0.5rem" }}>
              <span
                className={`inline-flex items-center px-2 py-1 rounded-md text-xs font-medium border ${
                  stats.receivedVariation > 0
                    ? 'bg-green-950 text-green-400 border-green-800'
                    : stats.receivedVariation < 0
                    ? 'bg-red-950 text-red-400 border-red-800'
                    : 'bg-neutral-800 text-neutral-400 border-neutral-700'
                }`}
              >
                {stats.receivedVariation > 0 ? '↑' : stats.receivedVariation < 0 ? '↓' : '−'}{' '}
                {Math.abs(stats.receivedVariation).toFixed(1)}%
              </span>
              <span className="text-neutral-400 text-xs">vs período anterior</span>
            </div>
            <div style={{ display: "flex", gap: "1rem", marginTop: "0.5rem", fontSize: "0.875rem" }}>
              <span className="muted text-neutral-400">⏳ Pendientes: {stats.pending}</span>
              <span className="muted text-neutral-400">🔴 Canceladas: {stats.cancelled}</span>
            </div>
          </article>

          {/* Top proveedores */}
          <article className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5 stat">
            <h3 className="text-neutral-100">Top proveedores</h3>
            <div style={{ marginTop: "0.5rem" }}>
              {stats.topSuppliers.length > 0 ? (
                <div style={{ fontSize: "0.875rem" }}>
                  {stats.topSuppliers.slice(0, 3).map((supplier, idx) => (
                    <div key={idx} style={{ marginBottom: "0.5rem" }}>
                      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "0.25rem" }}>
                        <span className="text-neutral-400">
                          {idx === 0 ? '🥇' : idx === 1 ? '🥈' : '🥉'} {supplier.name}
                        </span>
                        <span style={{ fontWeight: 600 }} className="text-neutral-100">
                          {formatCurrency(supplier.total)}
                        </span>
                      </div>
                      <div className="bg-neutral-800 rounded-full h-1.5" style={{ overflow: "hidden" }}>
                        <div
                          className="bg-blue-500 h-1.5"
                          style={{
                            width: `${(supplier.total / stats.topSuppliers[0].total) * 100}%`,
                            transition: "width 0.3s ease",
                          }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <span className="muted small text-neutral-400">No hay datos</span>
              )}
            </div>
          </article>
        </div>
      </div>
    </section>
  );
}

import { useMemo } from "react";
import { useQueryClient } from "@tanstack/react-query";
import DailyAvgCard from "./DailyAvgCard";
import Total14DaysCard from "./Total14DaysCard";
import SalesTrendChart from "./SalesTrendChart";
import { createCurrencyFormatter } from "../../utils/currency";
import { useSalesDashboard } from "../../hooks/useSalesDashboard";

type SalesDashboardOverviewProps = {
  days?: number;
};

export default function SalesDashboardOverview({ days = 14 }: SalesDashboardOverviewProps) {
  const queryClient = useQueryClient();
  const { summary, series, points, computedTotal } = useSalesDashboard(days);

  const currencyFormatter = useMemo(() => createCurrencyFormatter(), []);
  const formatCurrency = (value: number) => currencyFormatter.format(value ?? 0);

  const totalValue = summary.data?.total14d ?? computedTotal;
  const averageValue = summary.data?.avgDaily14d ?? (days > 0 ? totalValue / days : 0);

  const isInitialLoading = summary.isLoading && !summary.data && series.isLoading && !series.data;
  const isRefreshing = (summary.isFetching || series.isFetching) && !isInitialLoading;
  const hasError = summary.isError || series.isError;

  const handleRetry = () => {
    queryClient.invalidateQueries({ queryKey: ["sales-summary", days] });
    queryClient.invalidateQueries({ queryKey: ["sales-series", days] });
  };

  const chartPoints = series.isSuccess || summary.isSuccess ? points : points;

  return (
    <section aria-labelledby="sales-dashboard-overview-heading" className="dashboard-overview">
      <div className="card" style={{ marginBottom: "1.5rem" }}>
        <header className="card-header">
          <div>
            <h2 id="sales-dashboard-overview-heading">Ventas últimas {days} jornadas</h2>
            <p className="muted">Promedio diario, total acumulado y tendencia por día.</p>
          </div>
          {isRefreshing && !hasError && (
            <span className="muted" role="status">
              Actualizando…
            </span>
          )}
        </header>

        {hasError && (
          <div className="error-banner" role="alert">
            <p>No se pudieron cargar las métricas de ventas de los últimos {days} días.</p>
            <button className="btn" type="button" onClick={handleRetry}>
              Reintentar
            </button>
          </div>
        )}

        <div className="kpi-grid" style={{ marginBottom: "1.5rem" }}>
          {isInitialLoading ? (
            <SkeletonCard title="Promedio venta diaria" description={`Últimos ${days} días`} />
          ) : (
            <DailyAvgCard value={averageValue} days={days} formatter={formatCurrency} />
          )}
          {isInitialLoading ? (
            <SkeletonCard title="Total de ventas" description={`Incluye impuestos · Últimos ${days} días`} />
          ) : (
            <Total14DaysCard value={totalValue} days={days} formatter={formatCurrency} />
          )}
        </div>

        <div className="chart-card">
          {isInitialLoading ? (
            <div className="skeleton skeleton-chart" aria-hidden="true" />
          ) : (
            <SalesTrendChart points={chartPoints} formatter={formatCurrency} />
          )}
        </div>
      </div>
    </section>
  );
}

type SkeletonCardProps = {
  title: string;
  description: string;
};

function SkeletonCard({ title, description }: SkeletonCardProps) {
  return (
    <article className="card stat" aria-busy="true">
      <h3>{title}</h3>
      <div className="skeleton skeleton-text" aria-hidden="true" />
      <span className="stat-trend">{description}</span>
    </article>
  );
}

import { useEffect, useMemo, useState } from "react";
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
  const { series, points } = useSalesDashboard(days);

  const [chartRange, setChartRange] = useState<{ start: string; end: string } | null>(null);

  const currencyFormatter = useMemo(() => createCurrencyFormatter(), []);
  const formatCurrency = (value: number) => currencyFormatter.format(value ?? 0);

  const seriesInitialLoading = series.isLoading && !series.data;
  const seriesRefreshing = series.isFetching && !seriesInitialLoading;

  const seriesHasError = series.isError;

  const handleRetry = () => {
    queryClient.invalidateQueries({ queryKey: ["sales-series", days] });
  };

  const minDate = points[0]?.date ?? "";
  const maxDate = points.length > 0 ? points[points.length - 1]?.date ?? "" : "";

  useEffect(() => {
    if (!minDate || !maxDate) {
      setChartRange(null);
      return;
    }

    setChartRange((prev) => {
      if (!prev) {
        return { start: minDate, end: maxDate };
      }

      const normalizedStart = prev.start < minDate ? minDate : prev.start;
      const normalizedEnd = prev.end > maxDate ? maxDate : prev.end;

      if (normalizedStart > normalizedEnd) {
        return { start: minDate, end: maxDate };
      }

      if (normalizedStart !== prev.start || normalizedEnd !== prev.end) {
        return { start: normalizedStart, end: normalizedEnd };
      }

      return prev;
    });
  }, [minDate, maxDate]);

  const filteredChartPoints = useMemo(() => {
    if (!chartRange) {
      return points;
    }

    const { start, end } = chartRange;
    if (!start || !end) {
      return points;
    }

    return points.filter((point) => point.date >= start && point.date <= end);
  }, [chartRange, points]);

  const filteredTotal = useMemo(
    () => filteredChartPoints.reduce((acc, point) => acc + point.total, 0),
    [filteredChartPoints],
  );

  const filteredAverage = useMemo(() => {
    if (filteredChartPoints.length === 0) {
      return 0;
    }
    return filteredTotal / filteredChartPoints.length;
  }, [filteredTotal, filteredChartPoints.length]);

  const rangeStart = chartRange?.start ?? minDate;
  const rangeEnd = chartRange?.end ?? maxDate;

  const rangeLabel = rangeStart && rangeEnd ? formatRange(rangeStart, rangeEnd) : "Sin rango";

  const handleStartChange = (value: string) => {
    if (!value) {
      return;
    }
    setChartRange((prev) => {
      const safeEnd = prev?.end && prev.end >= value ? prev.end : value;
      return { start: value, end: safeEnd ?? value };
    });
  };

  const handleEndChange = (value: string) => {
    if (!value) {
      return;
    }
    setChartRange((prev) => {
      const safeStart = prev?.start && prev.start <= value ? prev.start : value;
      return { start: safeStart ?? value, end: value };
    });
  };

  return (
    <section aria-labelledby="sales-dashboard-overview-heading" className="dashboard-overview">
      <div className="card" style={{ marginBottom: "1.5rem" }}>
        <header className="card-header">
          <div>
            <h2 id="sales-dashboard-overview-heading">Resumen de ventas</h2>
          </div>
          {seriesRefreshing && !seriesHasError && (
            <span className="muted" role="status">
              Actualizando…
            </span>
          )}
        </header>

        {seriesHasError && (
          <div className="error-banner" role="alert">
            <p>No se pudieron cargar los indicadores de ventas.</p>
            <button className="btn" type="button" onClick={handleRetry}>
              Reintentar
            </button>
          </div>
        )}

        <div className="kpi-grid" style={{ marginBottom: "1.5rem" }}>
          {seriesInitialLoading || seriesHasError ? (
            <SkeletonCard title="Promedio diario" description={`Rango seleccionado: ${rangeLabel}`} />
          ) : (
            <DailyAvgCard
              value={filteredAverage}
              rangeLabel={rangeLabel}
              formatter={formatCurrency}
            />
          )}
          {seriesInitialLoading || seriesHasError ? (
            <SkeletonCard title="Ventas acumuladas" description={`Rango seleccionado: ${rangeLabel}`} />
          ) : (
            <Total14DaysCard
              value={filteredTotal}
              rangeLabel={rangeLabel}
              formatter={formatCurrency}
            />
          )}
        </div>
      </div>

      <div className="card">
        <header className="card-header" style={{ gap: "1rem", flexWrap: "wrap" }}>
          <div style={{ display: "flex", flexDirection: "column", gap: "0.25rem" }}>
            <h2>Tendencia de ventas</h2>
            {seriesRefreshing && !seriesHasError && (
              <span className="muted" role="status">
                Actualizando…
              </span>
            )}
          </div>
          <div className="field-group" style={{ display: "flex", gap: "0.75rem", alignItems: "center" }}>
            <label className="field" style={{ display: "flex", flexDirection: "column", gap: "0.25rem" }}>
              <span className="muted small">Desde</span>
              <input
                className="input"
                type="date"
                value={chartRange?.start ?? minDate}
                min={minDate}
                max={chartRange?.end ?? maxDate}
                onChange={(event) => handleStartChange(event.target.value)}
                disabled={seriesInitialLoading || seriesHasError || !minDate}
              />
            </label>
            <label className="field" style={{ display: "flex", flexDirection: "column", gap: "0.25rem" }}>
              <span className="muted small">Hasta</span>
              <input
                className="input"
                type="date"
                value={chartRange?.end ?? maxDate}
                min={chartRange?.start ?? minDate}
                max={maxDate}
                onChange={(event) => handleEndChange(event.target.value)}
                disabled={seriesInitialLoading || seriesHasError || !maxDate}
              />
            </label>
          </div>
        </header>

        {seriesHasError && (
          <div className="error-banner" role="alert">
            <p>No se pudo cargar la tendencia de ventas.</p>
            <button className="btn" type="button" onClick={handleRetry}>
              Reintentar
            </button>
          </div>
        )}

        <div className="chart-card">
          {seriesInitialLoading ? (
            <div className="skeleton skeleton-chart" aria-hidden="true" />
          ) : filteredChartPoints.length > 0 ? (
            <SalesTrendChart points={filteredChartPoints} formatter={formatCurrency} />
          ) : (
            <p className="muted" role="status">
              No hay ventas registradas en el rango seleccionado.
            </p>
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

function formatRange(start: string, end: string) {
  if (!start) {
    return end;
  }

  if (!end || start === end) {
    return start;
  }

  return `${start} a ${end}`;
}

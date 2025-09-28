import { useEffect, useMemo, useState } from "react";
import DailyAvgCard from "./DailyAvgCard";
import Total14DaysCard from "./Total14DaysCard";
import SalesTrendChart from "./SalesTrendChart";
import { createCurrencyFormatter } from "../../utils/currency";
import { getSampleSalesTimeseries } from "../../data/sampleSalesTimeseries";

type SalesDashboardOverviewProps = {
  days?: number;
};

type ChartRange = { start: string; end: string } | null;

export default function SalesDashboardOverview({ days = 14 }: SalesDashboardOverviewProps) {
  const points = useMemo(() => getSampleSalesTimeseries(days), [days]);
  const [chartRange, setChartRange] = useState<ChartRange>(null);

  const currencyFormatter = useMemo(() => createCurrencyFormatter(), []);
  const formatCurrency = (value: number) => currencyFormatter.format(value ?? 0);

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
  }, [filteredChartPoints, filteredTotal]);

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
        </header>

        <div className="kpi-grid" style={{ marginBottom: "1.5rem" }}>
          <DailyAvgCard value={filteredAverage} rangeLabel={rangeLabel} formatter={formatCurrency} />

          <Total14DaysCard value={filteredTotal} rangeLabel={rangeLabel} formatter={formatCurrency} />
        </div>
      </div>

      <div className="card">
        <header className="card-header" style={{ gap: "1rem", flexWrap: "wrap" }}>
          <div style={{ display: "flex", flexDirection: "column", gap: "0.25rem" }}>
            <h2>Tendencia de ventas</h2>
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
                disabled={!minDate}
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
                disabled={!maxDate}
              />
            </label>
          </div>
        </header>

        <div className="chart-card">
          {filteredChartPoints.length > 0 ? (
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

function formatRange(start: string, end: string) {
  if (!start) {
    return end;
  }

  if (!end || start === end) {
    return start;
  }

  return `${start} a ${end}`;
}

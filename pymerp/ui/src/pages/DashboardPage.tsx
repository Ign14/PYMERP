import type { ChangeEvent } from "react";
import { useEffect, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import PageHeader from "../components/layout/PageHeader";
import {
  type DashboardSalesMetrics,
  type DashboardSalesMetricsParams,
  type TrendSeriesResponse,
  getDashboardSalesMetrics,
  getPurchaseSaleTrend,
} from "../services/client";

type DateRange = { from: string; to: string };

const DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

function addDays(base: Date, amount: number): Date {
  const copy = new Date(base);
  copy.setDate(copy.getDate() + amount);
  return copy;
}

function formatAsInputDate(date: Date): string {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function buildDefaultRange(): DateRange {
  const today = new Date();
  const start = addDays(today, -13);
  return { from: formatAsInputDate(start), to: formatAsInputDate(today) };
}

function normalizeRange(
  fromValue: string | null,
  toValue: string | null,
  fallback: DateRange,
): DateRange {
  const from = DATE_REGEX.test(fromValue ?? "") ? fromValue! : fallback.from;
  const to = DATE_REGEX.test(toValue ?? "") ? toValue! : fallback.to;
  if (from > to) {
    return { from, to: from };
  }
  return { from, to };
}

export default function DashboardPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const defaultRange = useMemo(buildDefaultRange, []);

  const rawRange = useMemo(
    () => ({
      from: searchParams.get("from"),
      to: searchParams.get("to"),
    }),
    [searchParams],
  );

  const range = useMemo(
    () => normalizeRange(rawRange.from, rawRange.to, defaultRange),
    [rawRange.from, rawRange.to, defaultRange],
  );

  useEffect(() => {
    if (rawRange.from !== range.from || rawRange.to !== range.to) {
      setSearchParams({ from: range.from, to: range.to }, { replace: true });
    }
  }, [rawRange.from, rawRange.to, range.from, range.to, setSearchParams]);

  const handleRangeChange = (field: "from" | "to") => (event: ChangeEvent<HTMLInputElement>) => {
    const value = event.target.value;
    if (!DATE_REGEX.test(value)) {
      return;
    }
    let nextFrom = field === "from" ? value : range.from;
    let nextTo = field === "to" ? value : range.to;
    if (nextFrom > nextTo) {
      if (field === "from") {
        nextTo = value;
      } else {
        nextFrom = value;
      }
    }
    setSearchParams({ from: nextFrom, to: nextTo });
  };

  const { from, to } = range;

  const metricsQuery = useQuery<DashboardSalesMetrics, Error>({
    queryKey: ["salesMetrics", from, to],
    queryFn: () => getDashboardSalesMetrics({ from, to } satisfies DashboardSalesMetricsParams),
    enabled: Boolean(from && to),
  });

  const trendQuery = useQuery<TrendSeriesResponse, Error>({
    queryKey: ["trend", from, to, "purchase-sale"],
    queryFn: () => getPurchaseSaleTrend({ from, to, series: "purchase,sale" }),
    enabled: Boolean(from && to),
  });

  const currencyFormatter = useMemo(
    () => new Intl.NumberFormat("es-CL", { style: "currency", currency: "CLP", maximumFractionDigits: 0 }),
    [],
  );

  const formatCurrency = (value: number | null | undefined) => currencyFormatter.format(value ?? 0);

  const chartData = useMemo(() => {
    if (!trendQuery.data) {
      return [] as Array<{ date: string; purchase: number; sale: number }>;
    }
    const points = new Map<string, { date: string; purchase: number; sale: number }>();
    trendQuery.data.purchase.forEach((point) => {
      const entry = points.get(point.date) ?? { date: point.date, purchase: 0, sale: 0 };
      entry.purchase = point.value ?? 0;
      points.set(point.date, entry);
    });
    trendQuery.data.sale.forEach((point) => {
      const entry = points.get(point.date) ?? { date: point.date, purchase: 0, sale: 0 };
      entry.sale = point.value ?? 0;
      points.set(point.date, entry);
    });
    return Array.from(points.values()).sort((a, b) => a.date.localeCompare(b.date));
  }, [trendQuery.data]);

  const paymentMethods = metricsQuery.data?.topPaymentMethods ?? [];
  const topMethods = paymentMethods.slice(0, 4);
  const isUpdating = metricsQuery.isFetching || trendQuery.isFetching;

  return (
    <div className="dashboard-panel">
      <PageHeader
        title="Panel de control"
        description="Visualiza tus KPIs diarios y compara la tendencia de compras versus ventas."
      />

      <section className="card panel-filter" aria-labelledby="panel-filter-title">
        <div className="panel-filter-header">
          <h3 id="panel-filter-title">Filtro por fechas</h3>
          <span className={`panel-filter-status ${isUpdating ? "busy" : ""}`} role="status">
            {isUpdating ? "Actualizando datos..." : "Datos al día"}
          </span>
        </div>
        <div className="date-filter" role="group" aria-label="Rango de fechas">
          <label>
            <span>Desde</span>
            <input
              type="date"
              value={from}
              max={to}
              onChange={handleRangeChange("from")}
            />
          </label>
          <label>
            <span>Hasta</span>
            <input
              type="date"
              value={to}
              min={from}
              onChange={handleRangeChange("to")}
            />
          </label>
        </div>
        <p className="muted small">Período seleccionado: {from} a {to}</p>
      </section>

      <section className="panel-kpis" aria-label="Indicadores clave">
        <article className="card kpi-card">
          <h3>Ventas del día</h3>
          {metricsQuery.isLoading ? (
            <p className="muted">Cargando...</p>
          ) : metricsQuery.isError ? (
            <p className="panel-error">No se pudieron cargar los indicadores.</p>
          ) : (
            <>
              <p className="kpi-value">{formatCurrency(metricsQuery.data?.totalDay ?? 0)}</p>
              <p className="kpi-meta">Total emitido el {to}</p>
            </>
          )}
        </article>

        <article className="card kpi-card">
          <h3>Producto más vendido</h3>
          {metricsQuery.isLoading ? (
            <p className="muted">Cargando...</p>
          ) : metricsQuery.isError ? (
            <p className="panel-error">No se pudieron cargar los indicadores.</p>
          ) : metricsQuery.data?.topProduct ? (
            <>
              <p className="kpi-value">{metricsQuery.data.topProduct.name}</p>
              <p className="kpi-meta">{metricsQuery.data.topProduct.qty} unidades</p>
            </>
          ) : (
            <p className="muted">Sin datos para el período seleccionado.</p>
          )}
        </article>

        <article className="card kpi-card">
          <h3>Formas de pago más utilizadas</h3>
          {metricsQuery.isLoading ? (
            <p className="muted">Cargando...</p>
          ) : metricsQuery.isError ? (
            <p className="panel-error">No se pudieron cargar los indicadores.</p>
          ) : topMethods.length > 0 ? (
            <ul className="kpi-list">
              {topMethods.map((method) => (
                <li key={method.method}>
                  <span className="kpi-list-label">{method.method}</span>
                  <span className="kpi-list-value">{method.count} usos</span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="muted">No hay ventas registradas en este rango.</p>
          )}
        </article>
      </section>

      <section className="card panel-chart" aria-labelledby="overview-title">
        <div className="panel-chart-header">
          <div>
            <h3 id="overview-title">Vista general</h3>
            <p className="muted small">Comparativo de compras vs. ventas</p>
          </div>
        </div>
        {trendQuery.isLoading ? (
          <p className="muted">Cargando tendencia...</p>
        ) : trendQuery.isError ? (
          <p className="panel-error">No se pudo cargar la tendencia.</p>
        ) : chartData.length === 0 ? (
          <p className="muted">Sin datos para el rango seleccionado.</p>
        ) : (
          <div className="chart-wrapper">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={chartData} margin={{ top: 20, right: 24, bottom: 8, left: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1f2937" />
                <XAxis dataKey="date" stroke="#9aa0a6" tick={{ fontSize: 12 }} />
                <YAxis stroke="#9aa0a6" tickFormatter={(value) => formatCurrency(Number(value))} width={96} />
                <Tooltip formatter={(value: number) => formatCurrency(value)} labelFormatter={(label) => `Fecha: ${label}`} />
                <Legend />
                <Line type="monotone" dataKey="sale" name="Ventas" stroke="#60a5fa" strokeWidth={2} dot={false} />
                <Line type="monotone" dataKey="purchase" name="Compras" stroke="#f97316" strokeWidth={2} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        )}
      </section>
    </div>
  );
}

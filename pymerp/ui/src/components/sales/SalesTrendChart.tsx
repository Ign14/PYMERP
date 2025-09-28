import type { CSSProperties } from "react";
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { formatMoneyCLP } from "../../utils/currency";

type SalesTrendChartProps = {
  points: Array<{ date: string; total: number }>;
  formatter?: (value: number) => string;
};

const hiddenListStyles: CSSProperties = {
  position: "absolute",
  width: 1,
  height: 1,
  padding: 0,
  margin: -1,
  overflow: "hidden",
  clip: "rect(0, 0, 0, 0)",
  whiteSpace: "nowrap",
  border: 0,
};

export default function SalesTrendChart({ points, formatter = formatMoneyCLP }: SalesTrendChartProps) {
  const data = points.map((point) => ({ ...point }));
  const hasActivity = data.some((point) => point.total > 0);

  return (
    <div className="chart-container" data-testid="sales-trend-chart" data-point-count={data.length}>
      <div style={{ height: 260 }} role="img" aria-label="Tendencia de ventas diarias">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data} margin={{ top: 10, right: 24, left: 0, bottom: 0 }}>
            <defs>
              <linearGradient id="salesTrendGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#60a5fa" stopOpacity={0.85} />
                <stop offset="95%" stopColor="#60a5fa" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#1f2937" />
            <XAxis dataKey="date" stroke="#9aa0a6" tick={{ fontSize: 12 }} />
            <YAxis stroke="#9aa0a6" tickFormatter={(value) => formatter(Number(value))} width={96} />
            <Tooltip
              formatter={(value: number) => formatter(value)}
              labelFormatter={(label) => `Fecha: ${label}`}
            />
            <Area type="monotone" dataKey="total" stroke="#60a5fa" fill="url(#salesTrendGradient)" />
          </AreaChart>
        </ResponsiveContainer>
      </div>
      {!hasActivity && (
        <p className="muted" role="status">
          Sin ventas registradas en los últimos días.
        </p>
      )}
      <ul style={hiddenListStyles} data-testid="sales-trend-points">
        {data.map((point) => (
          <li key={point.date} data-testid="sales-trend-point">{`${point.date}: ${point.total}`}</li>
        ))}
      </ul>
    </div>
  );
}

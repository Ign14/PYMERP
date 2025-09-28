import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchSalesSummary, fetchSalesTimeseries } from "../services/reports";

export function useSalesDashboard(days = 14) {
  const summary = useQuery({
    queryKey: ["sales-summary", days],
    queryFn: () => fetchSalesSummary(days),
  });

  const series = useQuery({
    queryKey: ["sales-series", days],
    queryFn: () => fetchSalesTimeseries(days),
  });

  const points = useMemo(() => {
    const frame = buildFrame(days);
    const totals = new Map<string, number>();
    const source = series.data?.points ?? [];

    for (const point of source) {
      if (!point?.date) {
        continue;
      }
      const normalizedDate = point.date.trim();
      if (!normalizedDate) {
        continue;
      }
      const current = totals.get(normalizedDate) ?? 0;
      const value = typeof point.total === "number" && !Number.isNaN(point.total) ? point.total : 0;
      totals.set(normalizedDate, current + value);
    }

    return frame.map((date) => ({
      date,
      total: totals.get(date) ?? 0,
    }));
  }, [series.data, days]);

  const computedTotal = useMemo(
    () => points.reduce((acc, point) => acc + point.total, 0),
    [points],
  );

  return { summary, series, points, computedTotal };
}

function buildFrame(days: number): string[] {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const frame: string[] = [];

  for (let i = days - 1; i >= 0; i -= 1) {
    const current = new Date(today);
    current.setDate(today.getDate() - i);
    frame.push(formatLocalDate(current));
  }

  return frame;
}

function formatLocalDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { createCurrencyFormatter } from "../../utils/currency";
import { listPurchaseDaily, PurchaseDailyPoint } from "../../services/client";
import {
  ComposedChart,
  Line,
  Bar,
  CartesianGrid,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";

type PurchasesTrendSectionProps = {
  startDate: string;
  endDate: string;
  onStartDateChange: (date: string) => void;
  onEndDateChange: (date: string) => void;
};

export default function PurchasesTrendSection({ 
  startDate, 
  endDate, 
  onStartDateChange, 
  onEndDateChange 
}: PurchasesTrendSectionProps) {
  const today = new Date().toISOString().split('T')[0];
  
  const currencyFormatter = useMemo(() => createCurrencyFormatter(), []);
  const formatCurrency = (value: number) => currencyFormatter.format(value ?? 0);

  // Calcular días para la consulta
  const days = useMemo(() => {
    const start = new Date(startDate);
    const end = new Date(endDate);
    const diffTime = Math.abs(end.getTime() - start.getTime());
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
  }, [startDate, endDate]);

  const purchasesQuery = useQuery<PurchaseDailyPoint[], Error>({
    queryKey: ["purchases", "daily", days],
    queryFn: () => listPurchaseDaily(days),
    enabled: days > 0 && days <= 365,
  });

  const purchasesData = purchasesQuery.data ?? [];

  const averageDailyPurchases = useMemo(() => {
    if (purchasesData.length === 0) return 0;
    const totalPurchases = purchasesData.reduce((sum, point) => sum + (point.total ?? 0), 0);
    return totalPurchases / purchasesData.length;
  }, [purchasesData]);

  const handleStartChange = (value: string) => {
    if (!value) return;
    onStartDateChange(value);
    if (value > endDate) {
      onEndDateChange(value);
    }
  };

  const handleEndChange = (value: string) => {
    if (!value) return;
    onEndDateChange(value);
    if (value < startDate) {
      onStartDateChange(value);
    }
  };

  const chartData = useMemo(() => {
    return purchasesData.map(point => ({
      date: point.date,
      total: Number(point.total ?? 0),
      count: point.count,
      average: averageDailyPurchases,
    }));
  }, [purchasesData, averageDailyPurchases]);

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
      <header className="card-header border-b border-neutral-800 pb-4 mb-4" style={{ gap: "1rem", flexWrap: "wrap" }}>
        <div style={{ display: "flex", flexDirection: "column", gap: "0.25rem" }}>
          <h2 className="text-neutral-100">Tendencia de compras</h2>
          <p className="muted small text-neutral-400">
            Promedio diario: {formatCurrency(averageDailyPurchases)} | 
            Total período: {formatCurrency(purchasesData.reduce((sum, point) => sum + (point.total ?? 0), 0))}
          </p>
        </div>
        <div className="field-group" style={{ display: "flex", gap: "0.75rem", alignItems: "center" }}>
          <label className="field" style={{ display: "flex", flexDirection: "column", gap: "0.25rem" }}>
            <span className="muted small text-neutral-400">Desde</span>
            <input
              className="input bg-neutral-800 border-neutral-700 text-neutral-100"
              type="date"
              value={startDate}
              max={endDate}
              onChange={(event) => handleStartChange(event.target.value)}
            />
          </label>
          <label className="field" style={{ display: "flex", flexDirection: "column", gap: "0.25rem" }}>
            <span className="muted small text-neutral-400">Hasta</span>
            <input
              className="input bg-neutral-800 border-neutral-700 text-neutral-100"
              type="date"
              value={endDate}
              min={startDate}
              max={today}
              onChange={(event) => handleEndChange(event.target.value)}
            />
          </label>
        </div>
      </header>

      <div className="chart-card" style={{ height: 320 }}>
        {purchasesQuery.isLoading ? (
          <p className="muted text-neutral-400" role="status">Cargando datos de compras...</p>
        ) : purchasesQuery.isError ? (
          <p className="panel-error text-red-400" role="status">
            Error al cargar los datos de compras. {purchasesQuery.error?.message}
          </p>
        ) : chartData.length > 0 ? (
          <ResponsiveContainer width="100%" height="100%">
            <ComposedChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#404040" />
              <XAxis dataKey="date" stroke="#a3a3a3" tick={{ fontSize: 12 }} />
              <YAxis 
                yAxisId="left"
                stroke="#a3a3a3" 
                tickFormatter={(value) => `$${(value / 1000).toFixed(0)}k`} 
              />
              <YAxis 
                yAxisId="right"
                orientation="right"
                stroke="#a3a3a3" 
                tickFormatter={(value) => value.toString()}
              />
              <Tooltip 
                formatter={(value: number, name: string) => {
                  if (name === 'total' || name === 'average') {
                    return [formatCurrency(value), name === 'total' ? 'Monto' : 'Promedio'];
                  }
                  return [value, 'Órdenes'];
                }}
                contentStyle={{ backgroundColor: '#262626', border: '1px solid #404040', borderRadius: '8px' }}
                labelStyle={{ color: '#f5f5f5' }}
              />
              <Legend />
              <Bar yAxisId="left" dataKey="total" fill="#a855f7" radius={[6, 6, 0, 0]} name="Monto" />
              <Line 
                yAxisId="left"
                type="monotone" 
                dataKey="average" 
                stroke="#f59e0b" 
                strokeWidth={2}
                dot={false}
                name="Promedio"
                strokeDasharray="5 5"
              />
              <Line 
                yAxisId="right"
                type="monotone" 
                dataKey="count" 
                stroke="#10b981" 
                strokeWidth={2}
                name="Órdenes"
              />
            </ComposedChart>
          </ResponsiveContainer>
        ) : (
          <p className="muted text-neutral-400" role="status">
            No hay compras registradas en el rango seleccionado ({startDate} - {endDate}).
          </p>
        )}
      </div>
    </div>
  );
}

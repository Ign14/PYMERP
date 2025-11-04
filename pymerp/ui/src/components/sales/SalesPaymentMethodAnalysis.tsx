import { useMemo } from "react";
import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from "recharts";

type PaymentMethodStat = {
  method: string;
  total: number;
  count: number;
};

type SalesPaymentMethodAnalysisProps = {
  startDate?: string;
  endDate?: string;
};

const COLORS = ["#22d3ee", "#3b82f6", "#8b5cf6", "#ec4899", "#f59e0b", "#10b981"];

function formatCurrency(value: number): string {
  return `$${Math.round(value).toLocaleString("es-CL")}`;
}

// Datos de demostración
function generatePaymentStats(): PaymentMethodStat[] {
  return [
    { method: "Efectivo", total: 12500000, count: 145 },
    { method: "Tarjeta de crédito", total: 18900000, count: 98 },
    { method: "Tarjeta de débito", total: 15200000, count: 112 },
    { method: "Transferencia", total: 8300000, count: 42 },
    { method: "Cheque", total: 3500000, count: 18 },
    { method: "Otro", total: 1200000, count: 9 },
  ];
}

export default function SalesPaymentMethodAnalysis({ startDate, endDate }: SalesPaymentMethodAnalysisProps) {
  const paymentStats = useMemo(() => generatePaymentStats(), []);
  const totalRevenue = paymentStats.reduce((sum, stat) => sum + stat.total, 0);

  const chartData = paymentStats.map((stat) => ({
    name: stat.method,
    value: stat.total,
    count: stat.count,
  }));

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5 mb-6">
      <header className="mb-4">
        <h2 className="text-xl font-semibold text-neutral-100 mb-1">💳 Análisis de Métodos de Pago</h2>
        <p className="text-sm text-neutral-400">Distribución de ventas por forma de pago</p>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Gráfico de dona */}
        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
          <div style={{ height: 280 }} role="img" aria-label="Distribución de métodos de pago">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={chartData}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={90}
                  paddingAngle={2}
                  dataKey="value"
                  label={({ percent }) => `${(percent * 100).toFixed(1)}%`}
                  labelLine={{ stroke: "#a3a3a3", strokeWidth: 1 }}
                >
                  {chartData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip
                  contentStyle={{
                    backgroundColor: "#171717",
                    border: "1px solid #404040",
                    borderRadius: "0.5rem",
                    color: "#f5f5f5",
                  }}
                  formatter={(value: number, name, props) => [
                    formatCurrency(value),
                    `${props.payload.count} transacciones`,
                  ]}
                />
                <Legend
                  iconType="circle"
                  wrapperStyle={{ color: "#a3a3a3", fontSize: "12px" }}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Lista detallada */}
        <div className="space-y-3">
          {paymentStats.map((stat, index) => {
            const percentage = (stat.total / totalRevenue) * 100;
            return (
              <div
                key={stat.method}
                className="bg-neutral-800 border border-neutral-700 rounded-lg p-4"
              >
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <div
                      className="w-3 h-3 rounded-full"
                      style={{ backgroundColor: COLORS[index % COLORS.length] }}
                    ></div>
                    <h3 className="text-neutral-100 font-medium">{stat.method}</h3>
                  </div>
                  <span className="text-lg font-bold text-neutral-100">{formatCurrency(stat.total)}</span>
                </div>
                <div className="flex justify-between items-center text-sm">
                  <span className="text-neutral-400">{stat.count} transacciones</span>
                  <span className="text-neutral-300">{percentage.toFixed(1)}%</span>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

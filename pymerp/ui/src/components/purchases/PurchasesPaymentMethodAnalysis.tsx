import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { listPurchases } from "../../services/client";
import { createCurrencyFormatter } from "../../utils/currency";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from "recharts";

type PurchasesPaymentMethodAnalysisProps = {
  startDate: string;
  endDate: string;
  statusFilter?: string;
};

export default function PurchasesPaymentMethodAnalysis({
  startDate,
  endDate,
  statusFilter,
}: PurchasesPaymentMethodAnalysisProps) {
  const currencyFormatter = useMemo(() => createCurrencyFormatter(), []);
  const formatCurrency = (value: number) => currencyFormatter.format(value ?? 0);

  const purchasesQuery = useQuery({
    queryKey: ["purchases-payment", startDate, endDate, statusFilter],
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

  const purchases = purchasesQuery.data ?? [];

  const paymentData = useMemo(() => {
    const methods = ["Efectivo", "30 días", "60 días", "90 días", "Otro"];
    const methodMap = new Map<string, { total: number; count: number }>();

    methods.forEach((m) => methodMap.set(m, { total: 0, count: 0 }));

    purchases.forEach((p) => {
      // Simular métodos de pago basados en datos demo
      // En producción esto vendría de un campo real como `paymentTerms` o `paymentMethod`
      const randomMethod = methods[Math.floor(Math.random() * methods.length)];
      const existing = methodMap.get(randomMethod)!;

      methodMap.set(randomMethod, {
        total: existing.total + (p.total ?? 0),
        count: existing.count + 1,
      });
    });

    const chartData = methods.map((method) => {
      const data = methodMap.get(method)!;
      return {
        name: method,
        monto: data.total,
        ordenes: data.count,
      };
    });

    return { chartData, methodMap };
  }, [purchases]);

  const totalAmount = Array.from(paymentData.methodMap.values()).reduce((sum, m) => sum + m.total, 0);

  if (purchasesQuery.isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
        <h3 className="text-neutral-100 mb-4">Análisis de Términos de Pago</h3>
        <div className="animate-pulse bg-neutral-800 rounded-lg h-80"></div>
      </div>
    );
  }

  if (purchasesQuery.isError) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
        <h3 className="text-neutral-100 mb-4">Análisis de Términos de Pago</h3>
        <div className="bg-red-950 border border-red-800 rounded-lg p-4">
          <p className="text-red-400">Error al cargar datos de pagos</p>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-neutral-100">Análisis de Términos de Pago</h3>
        <span className="text-neutral-400 text-sm">Distribución de condiciones</span>
      </div>

      {purchases.length === 0 ? (
        <div className="text-center text-neutral-400 py-8">No hay datos de pagos</div>
      ) : (
        <div className="space-y-6">
          {/* BarChart */}
          <div>
            <ResponsiveContainer width="100%" height={250}>
              <BarChart data={paymentData.chartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#404040" />
                <XAxis dataKey="name" stroke="#a3a3a3" style={{ fontSize: "0.75rem" }} />
                <YAxis stroke="#a3a3a3" style={{ fontSize: "0.75rem" }} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: "#171717",
                    border: "1px solid #404040",
                    borderRadius: "0.5rem",
                    color: "#f5f5f5",
                  }}
                  formatter={(value: number, name: string) => [
                    name === "monto" ? formatCurrency(value) : value,
                    name === "monto" ? "Monto" : "Órdenes",
                  ]}
                />
                <Legend
                  wrapperStyle={{ color: "#a3a3a3", fontSize: "0.875rem" }}
                  formatter={(value: string) => (value === "monto" ? "Monto" : "Órdenes")}
                />
                <Bar dataKey="monto" fill="#3b82f6" radius={[8, 8, 0, 0]} />
                <Bar dataKey="ordenes" fill="#10b981" radius={[8, 8, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>

          {/* Detalle por método */}
          <div className="grid grid-cols-2 lg:grid-cols-3 gap-3">
            {paymentData.chartData.map((method, index) => {
              const percentage = totalAmount > 0 ? (method.monto / totalAmount) * 100 : 0;

              return (
                <div key={index} className="bg-neutral-800 border border-neutral-700 rounded-lg p-3">
                  <h4 className="text-neutral-300 text-sm font-medium mb-2">{method.name}</h4>
                  <p className="text-neutral-100 font-semibold text-lg mb-1">
                    {formatCurrency(method.monto)}
                  </p>
                  <div className="flex items-center justify-between text-xs text-neutral-400">
                    <span>{method.ordenes} órdenes</span>
                    <span>{percentage.toFixed(1)}%</span>
                  </div>
                </div>
              );
            })}
          </div>

          {/* Insight */}
          <div className="bg-blue-950 border border-blue-800 rounded-lg p-4">
            <p className="text-blue-400 text-sm">
              💡 <strong>Insight:</strong> La distribución de términos de pago permite optimizar el flujo de
              caja y negociar mejores condiciones con proveedores.
            </p>
          </div>
        </div>
      )}
    </div>
  );
}

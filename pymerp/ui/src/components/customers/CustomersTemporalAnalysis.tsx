import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  AreaChart,
  Area,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";
import { getCustomerSaleHistory } from "../../services/client";

type Props = {
  customerId: string;
  customerName: string;
};

export default function CustomersTemporalAnalysis({ customerId, customerName }: Props) {
  const salesQuery = useQuery({
    queryKey: ["customers", customerId, "sales-history-analysis"],
    queryFn: () => getCustomerSaleHistory(customerId, 0, 500),
    staleTime: 60_000,
  });

  // Agrupar ventas por mes
  const monthlyData = useMemo(() => {
    if (!salesQuery.data?.content) return [];

    const monthMap: Record<string, { month: string; sales: number; revenue: number; items: number }> = {};

    salesQuery.data.content.forEach((sale) => {
      const date = new Date(sale.saleDate);
      const monthKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
      const monthLabel = date.toLocaleDateString("es-CL", { year: "numeric", month: "short" });

      if (!monthMap[monthKey]) {
        monthMap[monthKey] = { month: monthLabel, sales: 0, revenue: 0, items: 0 };
      }

      monthMap[monthKey].sales += 1;
      monthMap[monthKey].revenue += sale.total || 0;
      monthMap[monthKey].items += sale.itemCount || 0;
    });

    return Object.values(monthMap).sort((a, b) => a.month.localeCompare(b.month)).slice(-12);
  }, [salesQuery.data]);

  // Agrupar ventas por tipo de documento
  const docTypeData = useMemo(() => {
    if (!salesQuery.data?.content) return [];

    const typeMap: Record<string, { type: string; count: number; total: number }> = {};

    salesQuery.data.content.forEach((sale) => {
      const type = sale.docType || "Sin tipo";

      if (!typeMap[type]) {
        typeMap[type] = { type, count: 0, total: 0 };
      }

      typeMap[type].count += 1;
      typeMap[type].total += sale.total || 0;
    });

    return Object.values(typeMap).sort((a, b) => b.total - a.total);
  }, [salesQuery.data]);

  // Calcular estadísticas
  const stats = useMemo(() => {
    if (!salesQuery.data?.content || salesQuery.data.content.length === 0) {
      return { avgSale: 0, avgItems: 0, totalRevenue: 0, totalSales: 0 };
    }

    const totalRevenue = salesQuery.data.content.reduce((sum, sale) => sum + (sale.total || 0), 0);
    const totalSales = salesQuery.data.content.length;
    const totalItems = salesQuery.data.content.reduce((sum, sale) => sum + (sale.itemCount || 0), 0);

    return {
      avgSale: totalRevenue / totalSales,
      avgItems: totalItems / totalSales,
      totalRevenue,
      totalSales,
    };
  }, [salesQuery.data]);

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-5">
      <h3 className="text-xl font-semibold text-neutral-100 mb-4">
        📈 Análisis Temporal - {customerName}
      </h3>

      {salesQuery.isLoading && (
        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-8 text-center">
          <p className="text-neutral-400">Cargando análisis...</p>
        </div>
      )}

      {salesQuery.isError && (
        <div className="bg-red-950 border border-red-800 rounded-lg p-4">
          <p className="text-red-400">Error al cargar el análisis</p>
        </div>
      )}

      {salesQuery.data && salesQuery.data.content.length === 0 && (
        <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-8 text-center">
          <p className="text-neutral-400">No hay datos suficientes para generar análisis</p>
        </div>
      )}

      {salesQuery.data && salesQuery.data.content.length > 0 && (
        <>
          {/* Estadísticas generales */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
              <p className="text-xs text-neutral-400 mb-1">Total Ventas</p>
              <p className="text-2xl font-bold text-neutral-100">{stats.totalSales}</p>
            </div>
            <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
              <p className="text-xs text-neutral-400 mb-1">Ingresos Totales</p>
              <p className="text-2xl font-bold text-green-400">${stats.totalRevenue.toLocaleString("es-CL")}</p>
            </div>
            <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
              <p className="text-xs text-neutral-400 mb-1">Ticket Promedio</p>
              <p className="text-2xl font-bold text-blue-400">${Math.round(stats.avgSale).toLocaleString("es-CL")}</p>
            </div>
            <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
              <p className="text-xs text-neutral-400 mb-1">Productos/Venta</p>
              <p className="text-2xl font-bold text-purple-400">{stats.avgItems.toFixed(1)}</p>
            </div>
          </div>

          {/* Gráfico de tendencia mensual */}
          <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4 mb-6">
            <h4 className="text-neutral-100 font-semibold mb-4">Tendencia de Ventas (últimos 12 meses)</h4>
            <ResponsiveContainer width="100%" height={300}>
              <AreaChart data={monthlyData}>
                <defs>
                  <linearGradient id="colorRevenue" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#404040" />
                <XAxis dataKey="month" stroke="#a3a3a3" style={{ fontSize: "12px" }} />
                <YAxis stroke="#a3a3a3" style={{ fontSize: "12px" }} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: "#262626",
                    border: "1px solid #404040",
                    borderRadius: "8px",
                    color: "#e5e5e5",
                  }}
                  formatter={(value: number) => `$${value.toLocaleString("es-CL")}`}
                />
                <Legend wrapperStyle={{ color: "#e5e5e5" }} />
                <Area
                  type="monotone"
                  dataKey="revenue"
                  name="Ingresos"
                  stroke="#3b82f6"
                  strokeWidth={2}
                  fill="url(#colorRevenue)"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>

          {/* Gráfico por tipo de documento */}
          <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
            <h4 className="text-neutral-100 font-semibold mb-4">Ventas por Tipo de Documento</h4>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={docTypeData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#404040" />
                <XAxis dataKey="type" stroke="#a3a3a3" style={{ fontSize: "12px" }} />
                <YAxis stroke="#a3a3a3" style={{ fontSize: "12px" }} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: "#262626",
                    border: "1px solid #404040",
                    borderRadius: "8px",
                    color: "#e5e5e5",
                  }}
                  formatter={(value: number, name: string) =>
                    name === "total" ? `$${value.toLocaleString("es-CL")}` : value
                  }
                />
                <Legend wrapperStyle={{ color: "#e5e5e5" }} />
                <Bar dataKey="count" name="Cantidad" fill="#10b981" />
                <Bar dataKey="total" name="Total ($)" fill="#3b82f6" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </>
      )}
    </div>
  );
}

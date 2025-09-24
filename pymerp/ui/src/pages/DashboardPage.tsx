import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  listCompanies,
  listInventoryAlerts,
  listSalesDaily,
  listProducts,
  Page,
  Company,
  SalesDailyPoint,
  InventoryAlert,
  Product,
} from "../services/client";
import PageHeader from "../components/layout/PageHeader";
import HealthCard from "../components/HealthCard";
import CompaniesCard from "../components/CompaniesCard";
import CreateCompanyForm from "../components/CreateCompanyForm";
import ProductsCard from "../components/ProductsCard";
import SuppliersCard from "../components/SuppliersCard";
import CustomersCard from "../components/CustomersCard";
import {
  AreaChart,
  Area,
  ResponsiveContainer,
  CartesianGrid,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

export default function DashboardPage() {
  const companiesQuery = useQuery<Company[], Error>({ queryKey: ["companies"], queryFn: listCompanies });
  const salesMetricsQuery = useQuery<SalesDailyPoint[], Error>({
    queryKey: ["sales", "dashboard"],
    queryFn: () => listSalesDaily(14),
  });
  const alertsQuery = useQuery<InventoryAlert[], Error>({
    queryKey: ["inventory", "alerts", { dashboard: true }],
    queryFn: () => listInventoryAlerts(5),
  });
  const productsQuery = useQuery<Page<Product>, Error>({
    queryKey: ["products", { dashboard: true }],
    queryFn: () => listProducts({ size: 200 }),
  });

  const salesTotals = useMemo(() => {
    const data = salesMetricsQuery.data ?? [];
    const total = data.reduce((acc, point) => acc + Number(point.total), 0);
    const docs = data.reduce((acc, point) => acc + point.count, 0);
    const avg = data.length ? total / data.length : 0;
    return { total, docs, avg };
  }, [salesMetricsQuery.data]);

  const chartData = useMemo(() => (
    (salesMetricsQuery.data ?? []).map((point) => ({
      date: point.date,
      total: Number(point.total),
    }))
  ), [salesMetricsQuery.data]);

  const inventoryAlerts = alertsQuery.data ?? [];

  const kpiData = [
    { title: "Empresas", value: companiesQuery.data?.length ?? 0, trend: "Multi-tenant" },
    { title: "Ventas 14d", value: `$${salesTotals.total.toLocaleString()}`, trend: `${salesTotals.docs} documentos` },
    { title: "Ticket diario", value: `$${salesTotals.avg.toFixed(0)}`, trend: "Promedio 14 días" },
    { title: "Alertas stock", value: inventoryAlerts.length, trend: "Umbral configurado" },
  ];

  return (
    <div className="dashboard">
      <PageHeader
        title="Resumen ejecutivo"
        description="Monitorea indicadores clave, salud del sistema y catálogos recientes."
      />

      <section className="kpi-grid">
        {kpiData.map((item) => (
          <div key={item.title} className="card stat">
            <h3>{item.title}</h3>
            <p className="stat-value">{item.value}</p>
            <span className="stat-trend">{item.trend}</span>
          </div>
        ))}
      </section>

      <section className="card">
        <h3>Tendencia de ventas (14 días)</h3>
        <div style={{ height: 260 }}>
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={chartData}>
              <defs>
                <linearGradient id="dashboardSales" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#60a5fa" stopOpacity={0.8}/>
                  <stop offset="95%" stopColor="#60a5fa" stopOpacity={0}/>
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#1f2937" />
              <XAxis dataKey="date" stroke="#9aa0a6" tick={{ fontSize: 12 }} />
              <YAxis stroke="#9aa0a6" tickFormatter={(value) => `$${(value / 1000).toFixed(0)}k`} />
              <Tooltip formatter={(value: number) => `$${value.toLocaleString()}`}/>
              <Area type="monotone" dataKey="total" stroke="#60a5fa" fill="url(#dashboardSales)" />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </section>

      <section className="responsive-grid">
        <div className="card stretch"><HealthCard /></div>
        <div className="card"><CompaniesCard /></div>
        <div className="card"><CreateCompanyForm /></div>
      </section>

      <section className="responsive-grid large">
        <div className="card"><ProductsCard /></div>
        <div className="card"><SuppliersCard /></div>
        <div className="card"><CustomersCard /></div>
      </section>

      <section className="card table-card">
        <h3>Alertas de inventario</h3>
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>Producto</th>
                <th>Lote</th>
                <th>Disponible</th>
                <th>Registrado</th>
              </tr>
            </thead>
            <tbody>
              {inventoryAlerts.map((alert) => {
                const product = productsQuery.data?.content.find((p) => p.id === alert.productId);
                return (
                  <tr key={alert.lotId}>
                    <td>{product?.name ?? alert.productId}</td>
                    <td className="mono">{alert.lotId}</td>
                    <td className="mono">{Number(alert.qtyAvailable).toFixed(2)}</td>
                    <td className="mono small">{new Date(alert.createdAt).toLocaleDateString()}</td>
                  </tr>
                );
              })}
              {inventoryAlerts.length === 0 && (
                <tr><td colSpan={4} className="muted">Sin alertas activas</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

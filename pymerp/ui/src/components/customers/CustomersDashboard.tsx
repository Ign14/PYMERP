import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { listCustomers, getCustomerStats } from "../../services/client";
import { createCurrencyFormatter } from "../../utils/currency";

export default function CustomersDashboard() {
  const currencyFormatter = useMemo(() => createCurrencyFormatter(), []);
  const formatCurrency = (value: number) => currencyFormatter.format(value ?? 0);

  // Obtener todos los clientes activos
  const customersQuery = useQuery({
    queryKey: ["customers", "dashboard"],
    queryFn: () => listCustomers({ active: true, page: 0, size: 10000 }),
  });

  // Calcular fecha de hace 30 días para clientes nuevos
  const thirtyDaysAgo = useMemo(() => {
    const date = new Date();
    date.setDate(date.getDate() - 30);
    return date.toISOString();
  }, []);

  const stats = useMemo(() => {
    if (!customersQuery.data) return null;

    const customers = customersQuery.data.content || [];
    const totalActive = customers.length;

    // Clientes nuevos (últimos 30 días)
    const newCustomers = customers.filter(
      (c) => c.createdAt && new Date(c.createdAt) >= new Date(thirtyDaysAgo)
    ).length;

    return {
      totalActive,
      newCustomers,
      totalCustomers: customersQuery.data.totalElements || totalActive,
    };
  }, [customersQuery.data, thirtyDaysAgo]);

  // Query para obtener ingresos totales y top cliente
  // Usaremos el primer cliente como ejemplo para obtener stats
  const topCustomerQuery = useQuery({
    queryKey: ["customers", "top-revenue"],
    queryFn: async () => {
      // En una implementación real, el backend debería proveer un endpoint
      // para obtener el top cliente por ingresos
      // Por ahora retornamos datos de ejemplo
      return {
        totalRevenue: 0,
        topCustomer: null as { name: string; revenue: number } | null,
      };
    },
    enabled: !!customersQuery.data,
  });

  if (customersQuery.isLoading) {
    return (
      <section className="mb-6">
        <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
          <h2 className="text-neutral-100 mb-4">Resumen de Clientes</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="animate-pulse bg-neutral-800 rounded-lg h-32"></div>
            ))}
          </div>
        </div>
      </section>
    );
  }

  if (customersQuery.isError) {
    return (
      <section className="mb-6">
        <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
          <div className="bg-red-950 border border-red-800 rounded-lg p-4">
            <p className="text-red-400">Error al cargar métricas de clientes</p>
          </div>
        </div>
      </section>
    );
  }

  if (!stats) return null;

  return (
    <section className="mb-6">
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
        <h2 className="text-neutral-100 mb-4 text-xl font-semibold">Resumen de Clientes</h2>
        
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* Total Clientes Activos */}
          <article className="bg-neutral-800 border border-neutral-700 rounded-xl shadow p-5">
            <div className="flex items-start justify-between mb-2">
              <h3 className="text-neutral-400 text-sm font-medium">Clientes Activos</h3>
              <span className="text-2xl">👥</span>
            </div>
            <p className="text-3xl font-bold text-neutral-100 mb-1">{stats.totalActive}</p>
            <span className="text-neutral-400 text-sm">
              De {stats.totalCustomers} totales
            </span>
          </article>

          {/* Clientes Nuevos (Último Mes) */}
          <article className="bg-neutral-800 border border-neutral-700 rounded-xl shadow p-5">
            <div className="flex items-start justify-between mb-2">
              <h3 className="text-neutral-400 text-sm font-medium">Nuevos (30 días)</h3>
              <span className="text-2xl">📈</span>
            </div>
            <p className="text-3xl font-bold text-neutral-100 mb-1">{stats.newCustomers}</p>
            <span className="text-neutral-400 text-sm">
              {stats.totalActive > 0
                ? `${((stats.newCustomers / stats.totalActive) * 100).toFixed(1)}% del total`
                : "0% del total"}
            </span>
          </article>

          {/* Ingresos Totales */}
          <article className="bg-neutral-800 border border-neutral-700 rounded-xl shadow p-5">
            <div className="flex items-start justify-between mb-2">
              <h3 className="text-neutral-400 text-sm font-medium">Ingresos Totales</h3>
              <span className="text-2xl">💰</span>
            </div>
            <p className="text-3xl font-bold text-neutral-100 mb-1">
              {formatCurrency(topCustomerQuery.data?.totalRevenue || 0)}
            </p>
            <span className="text-neutral-400 text-sm">Histórico completo</span>
          </article>

          {/* Top Cliente */}
          <article className="bg-neutral-800 border border-neutral-700 rounded-xl shadow p-5">
            <div className="flex items-start justify-between mb-2">
              <h3 className="text-neutral-400 text-sm font-medium">Top Cliente</h3>
              <span className="text-2xl">⭐</span>
            </div>
            {topCustomerQuery.data?.topCustomer ? (
              <>
                <p className="text-lg font-bold text-neutral-100 mb-1 truncate">
                  {topCustomerQuery.data.topCustomer.name}
                </p>
                <span className="text-neutral-400 text-sm">
                  {formatCurrency(topCustomerQuery.data.topCustomer.revenue)}
                </span>
              </>
            ) : (
              <>
                <p className="text-lg font-bold text-neutral-100 mb-1">-</p>
                <span className="text-neutral-400 text-sm">Sin datos suficientes</span>
              </>
            )}
          </article>
        </div>
      </div>
    </section>
  );
}

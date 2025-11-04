import { useQuery } from "@tanstack/react-query";
import { getStockMovementStats } from "../services/client";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from "recharts";

export default function InventoryMovementSummary() {
  const { data: stats, isLoading, error } = useQuery({
    queryKey: ["stockMovementStats"],
    queryFn: getStockMovementStats,
    refetchInterval: 60000,
  });

  if (isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6 animate-pulse">
        <div className="h-6 bg-neutral-800 rounded w-1/3 mb-6"></div>
        <div className="h-64 bg-neutral-800 rounded"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-900/20 border border-red-800 rounded-lg p-6 text-center">
        <p className="text-red-400">Error al cargar estadísticas de movimiento</p>
      </div>
    );
  }

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat("es-CL", {
      style: "currency",
      currency: "CLP",
      minimumFractionDigits: 0,
    }).format(value);
  };

  const formatNumber = (value: number) => {
    return new Intl.NumberFormat("es-CL").format(value);
  };

  const chartData = [
    {
      name: "Entradas",
      valor: stats?.totalInflows || 0,
      transacciones: stats?.inflowTransactions || 0,
    },
    {
      name: "Salidas",
      valor: stats?.totalOutflows || 0,
      transacciones: stats?.outflowTransactions || 0,
    },
  ];

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-6">
      <div className="mb-6">
        <h2 className="text-xl font-semibold text-neutral-100 mb-2">
          Resumen de Movimientos (últimos 30 días)
        </h2>
        <p className="text-sm text-neutral-400">
          Análisis de entradas y salidas de inventario
        </p>
      </div>

      {/* Chart Section */}
      <div className="mb-8">
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={chartData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#404040" />
            <XAxis dataKey="name" stroke="#a3a3a3" />
            <YAxis stroke="#a3a3a3" tickFormatter={(value) => formatCurrency(value)} />
            <Tooltip
              contentStyle={{
                backgroundColor: "#262626",
                border: "1px solid #404040",
                borderRadius: "8px",
              }}
              labelStyle={{ color: "#f5f5f5" }}
              formatter={(value: number) => formatCurrency(value)}
            />
            <Legend />
            <Bar dataKey="valor" fill="#3b82f6" name="Valor Total" radius={[8, 8, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
        <div className="bg-green-900/20 border border-green-800 rounded-lg p-4">
          <div className="text-sm text-neutral-400 mb-1">Total Entradas</div>
          <div className="text-2xl font-bold text-green-400">
            {formatCurrency(stats?.totalInflows || 0)}
          </div>
          <div className="text-xs text-neutral-500 mt-1">
            {stats?.inflowTransactions || 0} transacciones
          </div>
        </div>
        <div className="bg-red-900/20 border border-red-800 rounded-lg p-4">
          <div className="text-sm text-neutral-400 mb-1">Total Salidas</div>
          <div className="text-2xl font-bold text-red-400">
            {formatCurrency(stats?.totalOutflows || 0)}
          </div>
          <div className="text-xs text-neutral-500 mt-1">
            {stats?.outflowTransactions || 0} transacciones
          </div>
        </div>
      </div>

      {/* Top Products Tables */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Top Inflows */}
        <div>
          <h3 className="text-sm font-semibold text-neutral-300 mb-3">
            Top 5 Entradas
          </h3>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="border-b border-neutral-800">
                <tr className="text-left text-neutral-400">
                  <th className="pb-2 font-medium">Producto</th>
                  <th className="pb-2 font-medium text-right">Cantidad</th>
                  <th className="pb-2 font-medium text-right">Valor</th>
                </tr>
              </thead>
              <tbody>
                {stats?.topInflowProducts && stats.topInflowProducts.length > 0 ? (
                  stats.topInflowProducts.map((product, index) => (
                    <tr key={index} className="border-b border-neutral-800/50">
                      <td className="py-2 text-neutral-300">{product.productName}</td>
                      <td className="py-2 text-right text-neutral-400">
                        {formatNumber(product.quantity)}
                      </td>
                      <td className="py-2 text-right text-green-400">
                        {formatCurrency(product.value)}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={3} className="py-4 text-center text-neutral-500">
                      Sin movimientos de entrada
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Top Outflows */}
        <div>
          <h3 className="text-sm font-semibold text-neutral-300 mb-3">
            Top 5 Salidas
          </h3>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="border-b border-neutral-800">
                <tr className="text-left text-neutral-400">
                  <th className="pb-2 font-medium">Producto</th>
                  <th className="pb-2 font-medium text-right">Cantidad</th>
                  <th className="pb-2 font-medium text-right">Valor</th>
                </tr>
              </thead>
              <tbody>
                {stats?.topOutflowProducts && stats.topOutflowProducts.length > 0 ? (
                  stats.topOutflowProducts.map((product, index) => (
                    <tr key={index} className="border-b border-neutral-800/50">
                      <td className="py-2 text-neutral-300">{product.productName}</td>
                      <td className="py-2 text-right text-neutral-400">
                        {formatNumber(product.quantity)}
                      </td>
                      <td className="py-2 text-right text-red-400">
                        {formatCurrency(product.value)}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={3} className="py-4 text-center text-neutral-500">
                      Sin movimientos de salida
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}

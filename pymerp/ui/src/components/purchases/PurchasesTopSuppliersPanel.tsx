import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { listPurchases } from "../../services/client";
import { createCurrencyFormatter } from "../../utils/currency";

type PurchasesTopSuppliersPanelProps = {
  startDate: string;
  endDate: string;
  statusFilter?: string;
};

export default function PurchasesTopSuppliersPanel({
  startDate,
  endDate,
  statusFilter,
}: PurchasesTopSuppliersPanelProps) {
  const currencyFormatter = useMemo(() => createCurrencyFormatter(), []);
  const formatCurrency = (value: number) => currencyFormatter.format(value ?? 0);

  const purchasesQuery = useQuery({
    queryKey: ["purchases-top-suppliers", startDate, endDate, statusFilter],
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

  const topSuppliers = useMemo(() => {
    const supplierMap = new Map<
      string,
      {
        name: string;
        total: number;
        count: number;
        avgAmount: number;
        onTimeDeliveries: number;
        totalDeliveries: number;
      }
    >();

    purchases.forEach((p) => {
      const key = p.supplierId ?? "unknown";
      const name = p.supplierName ?? "Sin proveedor";
      const existing = supplierMap.get(key) ?? {
        name,
        total: 0,
        count: 0,
        avgAmount: 0,
        onTimeDeliveries: 0,
        totalDeliveries: 0,
      };

      const isOnTime = p.status?.toLowerCase() === "received";
      const deliveryCount = ["received", "completed"].includes(p.status?.toLowerCase() ?? "") ? 1 : 0;

      supplierMap.set(key, {
        name,
        total: existing.total + (p.total ?? 0),
        count: existing.count + 1,
        avgAmount: 0,
        onTimeDeliveries: existing.onTimeDeliveries + (isOnTime ? 1 : 0),
        totalDeliveries: existing.totalDeliveries + deliveryCount,
      });
    });

    const suppliers = Array.from(supplierMap.values()).map((s) => ({
      ...s,
      avgAmount: s.count > 0 ? s.total / s.count : 0,
      onTimeRate: s.totalDeliveries > 0 ? (s.onTimeDeliveries / s.totalDeliveries) * 100 : 0,
    }));

    return suppliers.sort((a, b) => b.total - a.total).slice(0, 10);
  }, [purchases]);

  const maxTotal = topSuppliers.length > 0 ? topSuppliers[0].total : 1;

  if (purchasesQuery.isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
        <h3 className="text-neutral-100 mb-4">Top 10 Proveedores</h3>
        <div className="animate-pulse bg-neutral-800 rounded-lg h-96"></div>
      </div>
    );
  }

  if (purchasesQuery.isError) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
        <h3 className="text-neutral-100 mb-4">Top 10 Proveedores</h3>
        <div className="bg-red-950 border border-red-800 rounded-lg p-4">
          <p className="text-red-400">Error al cargar datos de proveedores</p>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-neutral-100">Top 10 Proveedores</h3>
        <span className="text-neutral-400 text-sm">Por volumen de compra</span>
      </div>

      {topSuppliers.length === 0 ? (
        <div className="text-center text-neutral-400 py-8">No hay datos de proveedores</div>
      ) : (
        <div className="space-y-4">
          {topSuppliers.map((supplier, index) => {
            const percentage = (supplier.total / maxTotal) * 100;
            const medal = index === 0 ? "🥇" : index === 1 ? "🥈" : index === 2 ? "🥉" : null;

            return (
              <div key={index} className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    {medal && <span className="text-xl">{medal}</span>}
                    <span className="text-neutral-100 font-medium">
                      #{index + 1} {supplier.name}
                    </span>
                  </div>
                  <div className="text-right">
                    <p className="text-neutral-100 font-semibold">{formatCurrency(supplier.total)}</p>
                    <span className="text-neutral-400 text-xs">{supplier.count} órdenes</span>
                  </div>
                </div>

                {/* Barra de progreso */}
                <div className="mb-3">
                  <div className="bg-neutral-700 rounded-full h-2" style={{ overflow: "hidden" }}>
                    <div
                      className="bg-blue-500 h-2 transition-all duration-300"
                      style={{ width: `${percentage}%` }}
                    />
                  </div>
                </div>

                {/* Métricas adicionales */}
                <div className="grid grid-cols-3 gap-2 text-xs">
                  <div className="bg-neutral-900 rounded-lg p-2">
                    <p className="text-neutral-400 mb-1">Promedio</p>
                    <p className="text-neutral-100 font-medium">{formatCurrency(supplier.avgAmount)}</p>
                  </div>
                  <div className="bg-neutral-900 rounded-lg p-2">
                    <p className="text-neutral-400 mb-1">Entregas</p>
                    <p className="text-neutral-100 font-medium">
                      {supplier.onTimeDeliveries}/{supplier.totalDeliveries}
                    </p>
                  </div>
                  <div className="bg-neutral-900 rounded-lg p-2">
                    <p className="text-neutral-400 mb-1">Puntualidad</p>
                    <p
                      className={`font-medium ${
                        supplier.onTimeRate >= 90
                          ? "text-green-400"
                          : supplier.onTimeRate >= 70
                          ? "text-yellow-400"
                          : "text-red-400"
                      }`}
                    >
                      {supplier.onTimeRate.toFixed(0)}%
                    </p>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

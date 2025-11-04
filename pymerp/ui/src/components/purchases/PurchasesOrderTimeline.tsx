import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { listPurchases } from "../../services/client";
import { createCurrencyFormatter } from "../../utils/currency";

type PurchasesOrderTimelineProps = {
  startDate: string;
  endDate: string;
  statusFilter?: string;
};

export default function PurchasesOrderTimeline({
  startDate,
  endDate,
  statusFilter,
}: PurchasesOrderTimelineProps) {
  const currencyFormatter = useMemo(() => createCurrencyFormatter(), []);
  const formatCurrency = (value: number) => currencyFormatter.format(value ?? 0);

  const [supplierFilter, setSupplierFilter] = useState<string>("");

  const purchasesQuery = useQuery({
    queryKey: ["purchases-timeline", startDate, endDate, statusFilter],
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

  // Extraer lista de proveedores únicos
  const suppliers = useMemo(() => {
    const uniqueSuppliers = new Set<string>();
    purchases.forEach((p) => {
      if (p.supplierName) uniqueSuppliers.add(p.supplierName);
    });
    return Array.from(uniqueSuppliers).sort();
  }, [purchases]);

  // Filtrar por proveedor seleccionado
  const filteredPurchases = useMemo(() => {
    if (!supplierFilter) return purchases;
    return purchases.filter((p) => p.supplierName === supplierFilter);
  }, [purchases, supplierFilter]);

  // Ordenar por fecha descendente (más recientes primero)
  const sortedPurchases = useMemo(() => {
    return [...filteredPurchases].sort((a, b) => {
      const dateA = new Date(a.issuedAt ?? 0).getTime();
      const dateB = new Date(b.issuedAt ?? 0).getTime();
      return dateB - dateA;
    });
  }, [filteredPurchases]);

  const getStatusConfig = (status: string) => {
    const statusLower = status.toLowerCase();
    const configs: Record<string, { icon: string; label: string; color: string; bgColor: string }> = {
      received: { icon: "🟢", label: "Recibida", color: "text-green-400", bgColor: "bg-green-950" },
      pending: { icon: "🟡", label: "Pendiente", color: "text-yellow-400", bgColor: "bg-yellow-950" },
      cancelled: { icon: "🔴", label: "Cancelada", color: "text-red-400", bgColor: "bg-red-950" },
      intransit: { icon: "⏳", label: "En tránsito", color: "text-blue-400", bgColor: "bg-blue-950" },
      completed: { icon: "✅", label: "Completada", color: "text-neutral-300", bgColor: "bg-neutral-700" },
    };
    return (
      configs[statusLower] ?? {
        icon: "⚪",
        label: statusLower,
        color: "text-neutral-400",
        bgColor: "bg-neutral-800",
      }
    );
  };

  // Detectar órdenes con demora (más de 7 días sin recibir)
  const getDelayAlert = (purchase: (typeof purchases)[0]) => {
    if (purchase.status?.toLowerCase() === "received" || purchase.status?.toLowerCase() === "cancelled") {
      return null;
    }
    const issuedDate = new Date(purchase.issuedAt ?? 0);
    const now = new Date();
    const daysDiff = Math.floor((now.getTime() - issuedDate.getTime()) / (1000 * 60 * 60 * 24));
    if (daysDiff > 7) {
      return `⚠️ Demora: ${daysDiff} días desde emisión`;
    }
    return null;
  };

  if (purchasesQuery.isLoading) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
        <h3 className="text-neutral-100 mb-4">Timeline de órdenes</h3>
        <div className="animate-pulse bg-neutral-800 rounded-lg h-64"></div>
      </div>
    );
  }

  if (purchasesQuery.isError) {
    return (
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
        <h3 className="text-neutral-100 mb-4">Timeline de órdenes</h3>
        <div className="bg-red-950 border border-red-800 rounded-lg p-4">
          <p className="text-red-400">Error al cargar órdenes</p>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-neutral-100">Timeline de órdenes</h3>
        <div className="flex items-center gap-2">
          <label className="text-neutral-400 text-sm">Filtrar por proveedor:</label>
          <select
            className="bg-neutral-800 border border-neutral-700 text-neutral-100 rounded-lg px-3 py-1.5 text-sm focus:ring-2 focus:ring-blue-500 outline-none"
            value={supplierFilter}
            onChange={(e) => setSupplierFilter(e.target.value)}
          >
            <option value="">Todos</option>
            {suppliers.map((supplier) => (
              <option key={supplier} value={supplier}>
                {supplier}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="text-neutral-400 text-sm mb-4">
        Mostrando {sortedPurchases.length} de {purchases.length} órdenes
      </div>

      {sortedPurchases.length === 0 ? (
        <div className="text-center text-neutral-400 py-8">No hay órdenes en el período seleccionado</div>
      ) : (
        <div className="relative">
          {/* Línea vertical del timeline */}
          <div className="absolute left-6 top-0 bottom-0 w-0.5 bg-neutral-700"></div>

          <div className="space-y-6">
            {sortedPurchases.map((purchase, index) => {
              const config = getStatusConfig(purchase.status ?? "");
              const delayAlert = getDelayAlert(purchase);

              return (
                <div key={purchase.id} className="relative pl-16">
                  {/* Círculo del timeline */}
                  <div
                    className={`absolute left-3.5 top-2 w-5 h-5 rounded-full border-2 border-neutral-800 flex items-center justify-center ${config.bgColor}`}
                  >
                    <span className="text-xs">{config.icon}</span>
                  </div>

                  {/* Tarjeta de orden */}
                  <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4 hover:border-neutral-600 transition-colors">
                    <div className="flex items-start justify-between mb-2">
                      <div>
                        <h4 className="text-neutral-100 font-medium">
                          {purchase.docType ?? "Factura"} {purchase.docNumber ?? "-"}
                        </h4>
                        <p className="text-neutral-400 text-sm">
                          {purchase.supplierName ?? "Sin proveedor"}
                        </p>
                      </div>
                      <div className="text-right">
                        <p className="text-neutral-100 font-semibold">{formatCurrency(purchase.total ?? 0)}</p>
                        <span
                          className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-xs font-medium border ${config.color} border-neutral-700`}
                        >
                          {config.icon} {config.label}
                        </span>
                      </div>
                    </div>

                    <div className="flex items-center justify-between text-sm">
                      <span className="text-neutral-400">
                        📅 {new Date(purchase.issuedAt ?? 0).toLocaleDateString("es-ES")}
                      </span>
                      {delayAlert && (
                        <span className="text-yellow-400 font-medium">{delayAlert}</span>
                      )}
                    </div>

                    <div className="mt-2 flex gap-4 text-xs text-neutral-500">
                      <span>Neto: {formatCurrency(purchase.net ?? 0)}</span>
                      <span>IVA: {formatCurrency(purchase.vat ?? 0)}</span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}

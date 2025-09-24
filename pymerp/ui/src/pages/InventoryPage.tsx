import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  getInventorySettings,
  getInventorySummary,
  listInventoryAlerts,
  listProducts,
  updateInventorySettings,
  InventoryAlert,
  InventorySettings,
  InventorySummary,
  Page,
  Product,
} from "../services/client";
import PageHeader from "../components/layout/PageHeader";
import ProductsCard from "../components/ProductsCard";
import InventoryAdjustmentDialog from "../components/dialogs/InventoryAdjustmentDialog";

const FALLBACK_THRESHOLD = 10;

function formatCurrency(value: number | string | null | undefined) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) {
    return "$0";
  }
  return `$${numeric.toLocaleString("es-CL", { maximumFractionDigits: 0 })}`;
}

export default function InventoryPage() {
  const queryClient = useQueryClient();
  const [adjustDialogOpen, setAdjustDialogOpen] = useState(false);
  const [thresholdValue, setThresholdValue] = useState<number | null>(null);
  const [thresholdInput, setThresholdInput] = useState<string>("");

  const productsQuery = useQuery<Page<Product>, Error>({
    queryKey: ["products", { view: "inventory" }],
    queryFn: () => listProducts({ size: 200, status: "all" }),
  });

  const settingsQuery = useQuery<InventorySettings, Error>({
    queryKey: ["inventory", "settings"],
    queryFn: getInventorySettings,
  });

  useEffect(() => {
    if (settingsQuery.data) {
      const numeric = Number(settingsQuery.data.lowStockThreshold ?? 0);
      if (Number.isFinite(numeric) && numeric > 0) {
        if (thresholdValue === null) {
          setThresholdValue(numeric);
        }
        setThresholdInput((prev) => (prev ? prev : String(numeric)));
        return;
      }
    }
    if (!settingsQuery.isLoading && thresholdValue === null) {
      setThresholdValue(FALLBACK_THRESHOLD);
      setThresholdInput(String(FALLBACK_THRESHOLD));
    }
  }, [settingsQuery.data, settingsQuery.isLoading, thresholdValue]);

  const summaryQuery = useQuery<InventorySummary, Error>({
    queryKey: ["inventory", "summary"],
    queryFn: getInventorySummary,
  });

  const alertsQuery = useQuery<InventoryAlert[], Error>({
    queryKey: ["inventory", "alerts", thresholdValue],
    enabled: thresholdValue !== null,
    queryFn: () => listInventoryAlerts(thresholdValue ?? undefined),
  });

  const settingsMutation = useMutation({
    mutationFn: (value: number) => updateInventorySettings({ lowStockThreshold: value }),
    onSuccess: (data) => {
      const numeric = Number(data.lowStockThreshold ?? 0);
      if (Number.isFinite(numeric) && numeric > 0) {
        setThresholdValue(numeric);
        setThresholdInput(String(numeric));
      }
      queryClient.invalidateQueries({ queryKey: ["inventory", "summary"] });
      queryClient.invalidateQueries({ queryKey: ["inventory", "alerts"] });
      queryClient.setQueryData(["inventory", "settings"], data);
    },
  });

  const handleSaveThreshold = () => {
    const value = Number(thresholdInput);
    if (!Number.isFinite(value) || value <= 0) {
      window.alert("Ingresa un umbral mayor a cero");
      return;
    }
    settingsMutation.mutate(value);
  };

  const handleAdjustmentApplied = () => {
    queryClient.invalidateQueries({ queryKey: ["inventory", "summary"] });
    queryClient.invalidateQueries({ queryKey: ["inventory", "alerts"] });
    queryClient.invalidateQueries({ queryKey: ["products"], exact: false });
    setAdjustDialogOpen(false);
  };

  const productsIndex = useMemo(
    () => new Map((productsQuery.data?.content ?? []).map((product) => [product.id, product] as const)),
    [productsQuery.data]
  );

  const summary = summaryQuery.data;
  const totalValue = formatCurrency(summary?.totalValue ?? 0);
  const activeProducts = summary?.activeProducts ?? 0;
  const lowStockAlerts = summary?.lowStockAlerts ?? alertsQuery.data?.length ?? 0;
  const configuredThreshold = summary?.lowStockThreshold ?? thresholdValue ?? Number(thresholdInput || 0);

  return (
    <div className="page-section">
      <PageHeader
        title="Inventario"
        description="Visibiliza catalogo, lotes y alertas de stock para garantizar disponibilidad."
        actions={<button className="btn" onClick={() => setAdjustDialogOpen(true)}>+ Ajuste de stock</button>}
      />

      <section className="kpi-grid">
        <div className="card stat">
          <h3>Valor inventario</h3>
          <p className="stat-value">{summaryQuery.isLoading ? "-" : totalValue}</p>
          <span className="stat-trend">Costo total disponible</span>
        </div>
        <div className="card stat">
          <h3>Productos activos</h3>
          <p className="stat-value">{summaryQuery.isLoading ? "-" : activeProducts}</p>
          <span className="stat-trend">Inventario sincronizado</span>
        </div>
        <div className="card stat">
          <h3>Alertas stock</h3>
          <p className="stat-value">{alertsQuery.isLoading ? "-" : lowStockAlerts}</p>
          <span className="stat-trend">Umbral {configuredThreshold ?? "-"}</span>
        </div>
      </section>

      <section className="responsive-grid">
        <div className="card">
          <ProductsCard />
        </div>
        <div className="card table-card">
          <h3>Lotes con stock critico</h3>
          <div className="inline-actions" style={{ marginBottom: "0.75rem" }}>
            <label className="muted small" htmlFor="low-stock-threshold">Umbral</label>
            <input
              id="low-stock-threshold"
              className="input"
              type="number"
              step="0.1"
              min="0.1"
              value={thresholdInput}
              onChange={(e) => setThresholdInput(e.target.value)}
              disabled={settingsMutation.isPending}
            />
            <button className="btn" type="button" onClick={handleSaveThreshold} disabled={settingsMutation.isPending}>
              {settingsMutation.isPending ? "Guardando..." : "Guardar"}
            </button>
          </div>
          {settingsMutation.isError && (
            <p className="error">{(settingsMutation.error as Error)?.message ?? "No se pudo actualizar el umbral"}</p>
          )}
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th>Producto</th>
                  <th>Lote</th>
                  <th>Disponible</th>
                  <th>Expira</th>
                  <th>Creado</th>
                </tr>
              </thead>
              <tbody>
                {alertsQuery.isLoading && (
                  <tr>
                    <td colSpan={5} className="muted">Cargando alertas...</td>
                  </tr>
                )}
                {alertsQuery.isError && (
                  <tr>
                    <td colSpan={5} className="error">{alertsQuery.error?.message ?? "No se pudieron obtener alertas"}</td>
                  </tr>
                )}
                {!alertsQuery.isLoading && !alertsQuery.isError && (alertsQuery.data ?? []).map((alert) => {
                  const product = productsIndex.get(alert.productId);
                  const expDate = alert.expDate ? new Date(alert.expDate).toLocaleDateString() : "-";
                  return (
                    <tr key={alert.lotId}>
                      <td>{product?.name ?? alert.productId}</td>
                      <td className="mono">{alert.lotId}</td>
                      <td className="mono">{Number(alert.qtyAvailable).toFixed(2)}</td>
                      <td className="mono">{expDate}</td>
                      <td className="mono small">{new Date(alert.createdAt).toLocaleDateString()}</td>
                    </tr>
                  );
                })}
                {!alertsQuery.isLoading && !alertsQuery.isError && (alertsQuery.data ?? []).length === 0 && (
                  <tr>
                    <td colSpan={5} className="muted">Sin alertas</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <InventoryAdjustmentDialog
        open={adjustDialogOpen}
        onClose={() => setAdjustDialogOpen(false)}
        onApplied={handleAdjustmentApplied}
      />
    </div>
  );
}

import { useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import PageHeader from "../components/layout/PageHeader";
import SuppliersCard, { SuppliersCardHandle } from "../components/SuppliersCard";
import SupplierFormDialog from "../components/dialogs/SupplierFormDialog";
import SuppliersStatsCard from "../components/SuppliersStatsCard";
import SupplierPerformancePanel from "../components/SupplierPerformancePanel";
import SupplierAlertsPanel from "../components/SupplierAlertsPanel";
import SuppliersRanking from "../components/SuppliersRanking";
import SupplierRiskAnalysis from "../components/SupplierRiskAnalysis";
import SupplierPriceHistory from "../components/SupplierPriceHistory";
import SupplierComparison from "../components/SupplierComparison";
import NegotiationOpportunities from "../components/NegotiationOpportunities";
import SingleSourceProducts from "../components/SingleSourceProducts";
import PurchaseForecast from "../components/PurchaseForecast";
import SupplierDashboard from "../components/SupplierDashboard";
import { Supplier, exportSuppliersToCSV, importSuppliersFromCSV } from "../services/client";

const mockContracts = [
  { supplier: "Distribuidora Norte", nextReview: "2025-10-15", status: "Activo" },
  { supplier: "Logística Express", nextReview: "2025-11-01", status: "Renegociar" }
];

export default function SuppliersPage() {
  const cardRef = useRef<SuppliersCardHandle>(null);
  const queryClient = useQueryClient();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingSupplier, setEditingSupplier] = useState<Supplier | null>(null);
  const [importing, setImporting] = useState(false);
  const [importError, setImportError] = useState<string | null>(null);
  const [importSuccess, setImportSuccess] = useState<string | null>(null);

  const handleOpenCreateDialog = () => {
    setEditingSupplier(null);
    setDialogOpen(true);
  };

  const handleOpenEditDialog = (supplier: Supplier) => {
    setEditingSupplier(supplier);
    setDialogOpen(true);
  };

  const handleCloseDialog = () => {
    setDialogOpen(false);
    setEditingSupplier(null);
  };

  const handleSaved = () => {
    queryClient.invalidateQueries({ queryKey: ["suppliers"] });
    handleCloseDialog();
  };

  const handleExport = async () => {
    try {
      const blob = await exportSuppliersToCSV();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `proveedores_${new Date().toISOString().split("T")[0]}.csv`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error("Error exportando proveedores:", error);
      alert("Error al exportar proveedores");
    }
  };

  const handleImport = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setImporting(true);
    setImportError(null);
    setImportSuccess(null);

    try {
      const result = await importSuppliersFromCSV(file);
      queryClient.invalidateQueries({ queryKey: ["suppliers"] });
      
      if (result.errors.length > 0) {
        setImportError(`Se crearon ${result.created} proveedores con ${result.errors.length} errores`);
      } else {
        setImportSuccess(`Se importaron ${result.created} proveedores exitosamente`);
      }
    } catch (error) {
      setImportError((error as Error)?.message ?? "Error al importar proveedores");
    } finally {
      setImporting(false);
      event.target.value = "";
    }
  };

  return (
    <div className="page-section">
      <PageHeader
        title="Proveedores"
        description="Consolida contratos, contactos y desempeño de abastecimiento."
        actions={(
          <>
            <button className="btn btn-ghost" onClick={handleExport}>
              📥 Exportar CSV
            </button>
            <label className="btn btn-ghost" style={{ cursor: "pointer" }}>
              📤 Importar CSV
              <input
                type="file"
                accept=".csv"
                onChange={handleImport}
                disabled={importing}
                style={{ display: "none" }}
              />
            </label>
            <button className="btn" onClick={handleOpenCreateDialog}>
              + Nuevo proveedor
            </button>
          </>
        )}
      />

      {importError && <div className="alert alert-error">{importError}</div>}
      {importSuccess && <div className="alert alert-success">{importSuccess}</div>}

      <SupplierFormDialog
        open={dialogOpen}
        supplier={editingSupplier}
        onClose={handleCloseDialog}
        onSaved={handleSaved}
      />

      <section className="responsive-grid">
        <div className="card">
          <SuppliersCard
            ref={cardRef}
            onOpenCreateDialog={handleOpenCreateDialog}
            onOpenEditDialog={handleOpenEditDialog}
          />
        </div>
        <div className="card">
          <SuppliersStatsCard />
        </div>
      </section>

      {/* Nueva sección: Análisis de Desempeño y Alertas */}
      <section className="responsive-grid mt-6">
        <div className="card">
          <SupplierPerformancePanel />
        </div>
        <div className="card">
          <SupplierAlertsPanel />
        </div>
      </section>

      {/* Sprint 2: Ranking y Análisis de Riesgo ABC */}
      <section className="responsive-grid mt-6">
        <div className="card">
          <SuppliersRanking />
        </div>
        <div className="card">
          <SupplierRiskAnalysis />
        </div>
      </section>

      {/* Sprint 2: Historial de Precios y Comparación */}
      <section className="responsive-grid mt-6">
        <div className="card">
          <SupplierPriceHistory />
        </div>
        <div className="card">
          <SupplierComparison />
        </div>
      </section>

      {/* Sprint 3: Optimización de Compras */}
      <section className="responsive-grid mt-6">
        <div className="card">
          <NegotiationOpportunities />
        </div>
        <div className="card">
          <SingleSourceProducts />
        </div>
      </section>

      {/* Sprint 3: Dashboard Ejecutivo y Forecast */}
      <section className="responsive-grid mt-6">
        <div className="card">
          <SupplierDashboard />
        </div>
        <div className="card">
          <PurchaseForecast />
        </div>
      </section>
    </div>
  );
}

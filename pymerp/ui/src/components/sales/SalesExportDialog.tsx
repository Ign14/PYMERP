import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { exportSalesToCSV } from "../../services/client";

type ExportFormat = "csv" | "json";
type ExportScope = "current" | "all" | "filtered";

type SalesExportDialogProps = {
  isOpen: boolean;
  onClose: () => void;
  filters: {
    status?: string;
    docType?: string;
    paymentMethod?: string;
    search?: string;
    startDate?: string;
    endDate?: string;
  };
  totalRecords: number;
};

export default function SalesExportDialog({
  isOpen,
  onClose,
  filters,
  totalRecords,
}: SalesExportDialogProps) {
  const [format, setFormat] = useState<ExportFormat>("csv");
  const [scope, setScope] = useState<ExportScope>("filtered");
  const [includeMetadata, setIncludeMetadata] = useState(true);

  const exportMutation = useMutation({
    mutationFn: async () => {
      const params = scope === "all" ? {} : {
        status: filters.status || undefined,
        docType: filters.docType || undefined,
        paymentMethod: filters.paymentMethod || undefined,
        search: filters.search || undefined,
      };

      if (format === "csv") {
        const blob = await exportSalesToCSV(params);
        downloadBlob(blob, `ventas-${new Date().toISOString().split('T')[0]}.csv`);
      } else {
        // Para JSON, podríamos crear una exportación personalizada
        const data = {
          metadata: includeMetadata ? {
            exportDate: new Date().toISOString(),
            filters: filters,
            totalRecords: totalRecords,
            scope: scope,
          } : undefined,
          summary: {
            totalRecords: totalRecords,
            appliedFilters: Object.keys(filters).filter(k => filters[k as keyof typeof filters]),
          },
          // En producción, aquí iría la data real
          data: [],
        };
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
        downloadBlob(blob, `ventas-${new Date().toISOString().split('T')[0]}.json`);
      }
    },
    onSuccess: () => {
      onClose();
    },
  });

  function downloadBlob(blob: Blob, filename: string) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    link.rel = "noopener";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }

  if (!isOpen) return null;

  const activeFilters = Object.entries(filters)
    .filter(([_, value]) => value && value !== "")
    .map(([key, value]) => ({ key, value }));

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-2xl p-6 max-w-lg w-full mx-4">
        <header className="mb-6">
          <h2 className="text-2xl font-bold text-neutral-100 mb-2">📥 Exportar Ventas</h2>
          <p className="text-sm text-neutral-400">
            Configura las opciones de exportación para tus documentos de ventas
          </p>
        </header>

        <div className="space-y-6">
          {/* Formato de exportación */}
          <div>
            <label className="block text-sm font-medium text-neutral-100 mb-3">
              Formato de archivo
            </label>
            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => setFormat("csv")}
                className={`p-4 rounded-lg border-2 transition-all ${
                  format === "csv"
                    ? "border-cyan-500 bg-cyan-950 text-cyan-400"
                    : "border-neutral-700 bg-neutral-800 text-neutral-400 hover:border-neutral-600"
                }`}
              >
                <div className="text-2xl mb-1">📊</div>
                <div className="font-semibold">CSV</div>
                <div className="text-xs mt-1">Excel compatible</div>
              </button>
              <button
                type="button"
                onClick={() => setFormat("json")}
                className={`p-4 rounded-lg border-2 transition-all ${
                  format === "json"
                    ? "border-purple-500 bg-purple-950 text-purple-400"
                    : "border-neutral-700 bg-neutral-800 text-neutral-400 hover:border-neutral-600"
                }`}
              >
                <div className="text-2xl mb-1">🔧</div>
                <div className="font-semibold">JSON</div>
                <div className="text-xs mt-1">Desarrollo/API</div>
              </button>
            </div>
          </div>

          {/* Alcance de la exportación */}
          <div>
            <label className="block text-sm font-medium text-neutral-100 mb-3">
              Datos a exportar
            </label>
            <div className="space-y-2">
              <button
                type="button"
                onClick={() => setScope("filtered")}
                className={`w-full p-3 rounded-lg border text-left transition-all ${
                  scope === "filtered"
                    ? "border-cyan-500 bg-cyan-950 text-cyan-100"
                    : "border-neutral-700 bg-neutral-800 text-neutral-300 hover:border-neutral-600"
                }`}
              >
                <div className="font-medium">Resultados filtrados ({totalRecords})</div>
                <div className="text-xs mt-1 opacity-75">Exportar solo los registros con filtros aplicados</div>
              </button>
              <button
                type="button"
                onClick={() => setScope("all")}
                className={`w-full p-3 rounded-lg border text-left transition-all ${
                  scope === "all"
                    ? "border-cyan-500 bg-cyan-950 text-cyan-100"
                    : "border-neutral-700 bg-neutral-800 text-neutral-300 hover:border-neutral-600"
                }`}
              >
                <div className="font-medium">Todas las ventas</div>
                <div className="text-xs mt-1 opacity-75">Exportar todos los registros sin filtros</div>
              </button>
            </div>
          </div>

          {/* Opciones adicionales */}
          {format === "json" && (
            <div>
              <label className="flex items-center gap-3 p-3 rounded-lg bg-neutral-800 border border-neutral-700 cursor-pointer hover:border-neutral-600 transition-colors">
                <input
                  type="checkbox"
                  checked={includeMetadata}
                  onChange={(e) => setIncludeMetadata(e.target.checked)}
                  className="w-4 h-4"
                />
                <div className="flex-1">
                  <div className="text-sm font-medium text-neutral-100">Incluir metadatos</div>
                  <div className="text-xs text-neutral-400 mt-0.5">Fecha, filtros aplicados, estadísticas</div>
                </div>
              </label>
            </div>
          )}

          {/* Filtros activos */}
          {activeFilters.length > 0 && scope === "filtered" && (
            <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
              <h3 className="text-sm font-medium text-neutral-100 mb-2">Filtros aplicados:</h3>
              <div className="space-y-1">
                {activeFilters.map(({ key, value }) => (
                  <div key={key} className="flex items-center gap-2 text-xs">
                    <span className="inline-flex items-center px-2 py-0.5 rounded bg-neutral-700 text-neutral-300 font-medium">
                      {key}
                    </span>
                    <span className="text-neutral-400">=</span>
                    <span className="text-neutral-100">{value}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Acciones */}
        <div className="flex gap-3 mt-6 pt-6 border-t border-neutral-800">
          <button
            type="button"
            onClick={onClose}
            disabled={exportMutation.isPending}
            className="flex-1 px-4 py-2.5 rounded-lg bg-neutral-800 border border-neutral-700 text-neutral-100 hover:bg-neutral-700 transition-colors disabled:opacity-50"
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={() => exportMutation.mutate()}
            disabled={exportMutation.isPending}
            className="flex-1 px-4 py-2.5 rounded-lg bg-cyan-600 text-white font-medium hover:bg-cyan-700 transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
          >
            {exportMutation.isPending ? (
              <>
                <span className="animate-spin">⏳</span>
                Exportando...
              </>
            ) : (
              <>
                📥 Exportar {format.toUpperCase()}
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}

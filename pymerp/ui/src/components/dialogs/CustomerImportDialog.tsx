import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";

type Props = {
  open: boolean;
  onClose: () => void;
};

type ImportResult = {
  created: number;
  errors: Array<{ line: number; error: string }>;
  totalErrors: number;
};

export default function CustomerImportDialog({ open, onClose }: Props) {
  const queryClient = useQueryClient();
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<ImportResult | null>(null);
  const [isDragging, setIsDragging] = useState(false);

  const importMutation = useMutation<ImportResult, Error, File>({
    mutationFn: async (file: File) => {
      const formData = new FormData();
      formData.append("file", file);

      const response = await fetch("/api/v1/customers/import", {
        method: "POST",
        body: formData,
      });

      if (!response.ok) {
        throw new Error("Error importando archivo");
      }

      return response.json();
    },
    onSuccess: (data) => {
      setResult(data);
      queryClient.invalidateQueries({ queryKey: ["customers"] });
      queryClient.invalidateQueries({ queryKey: ["customers", "segments"] });
    },
  });

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(false);
    
    const droppedFile = e.dataTransfer.files[0];
    if (droppedFile && droppedFile.name.endsWith(".csv")) {
      setFile(droppedFile);
      setResult(null);
    }
  };

  const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0];
    if (selectedFile) {
      setFile(selectedFile);
      setResult(null);
    }
  };

  const handleImport = () => {
    if (file) {
      importMutation.mutate(file);
    }
  };

  const handleClose = () => {
    setFile(null);
    setResult(null);
    onClose();
  };

  if (!open) return null;

  return (
    <div className="dialog-overlay" onClick={handleClose}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <div className="dialog-header">
          <h2>Importar Clientes desde CSV</h2>
          <button className="close-button" onClick={handleClose}>
            ✕
          </button>
        </div>

        <div className="dialog-body">
          {!result ? (
            <>
              <div
                className={`file-drop-zone ${isDragging ? "dragging" : ""}`}
                onDrop={handleDrop}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
              >
                {file ? (
                  <div className="file-selected">
                    <div className="file-icon">📄</div>
                    <div className="file-name">{file.name}</div>
                    <div className="file-size">
                      {(file.size / 1024).toFixed(1)} KB
                    </div>
                  </div>
                ) : (
                  <div className="file-drop-message">
                    <div className="upload-icon">📥</div>
                    <p>Arrastra un archivo CSV aquí</p>
                    <p className="text-muted">o</p>
                    <label className="btn btn-secondary">
                      Seleccionar archivo
                      <input
                        type="file"
                        accept=".csv"
                        onChange={handleFileInput}
                        style={{ display: "none" }}
                      />
                    </label>
                  </div>
                )}
              </div>

              <div className="import-instructions">
                <h4>Formato esperado del CSV:</h4>
                <p>
                  El archivo debe tener las siguientes columnas en orden:
                </p>
                <ol>
                  <li>Nombre (requerido)</li>
                  <li>RUT</li>
                  <li>Email</li>
                  <li>Teléfono</li>
                  <li>Dirección</li>
                  <li>Segmento (código)</li>
                  <li>Persona de Contacto</li>
                  <li>Notas</li>
                  <li>Activo (Sí/No)</li>
                </ol>
              </div>
            </>
          ) : (
            <div className="import-result">
              <div className={`result-summary ${result.totalErrors > 0 ? "warning" : "success"}`}>
                <h3>Importación completada</h3>
                <div className="result-stats">
                  <div className="stat">
                    <span className="stat-label">Creados:</span>
                    <span className="stat-value text-success">{result.created}</span>
                  </div>
                  <div className="stat">
                    <span className="stat-label">Errores:</span>
                    <span className="stat-value text-critical">{result.totalErrors}</span>
                  </div>
                </div>
              </div>

              {result.errors.length > 0 && (
                <div className="error-list">
                  <h4>Errores detectados:</h4>
                  <div className="errors-container">
                    {result.errors.slice(0, 10).map((err, idx) => (
                      <div key={idx} className="error-item">
                        <span className="error-line">Línea {err.line}:</span>
                        <span className="error-message">{err.error}</span>
                      </div>
                    ))}
                    {result.errors.length > 10 && (
                      <p className="text-muted">
                        ... y {result.errors.length - 10} errores más
                      </p>
                    )}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        <div className="dialog-footer">
          {!result ? (
            <>
              <button className="btn btn-secondary" onClick={handleClose}>
                Cancelar
              </button>
              <button
                className="btn"
                onClick={handleImport}
                disabled={!file || importMutation.isPending}
              >
                {importMutation.isPending ? "Importando..." : "Importar"}
              </button>
            </>
          ) : (
            <button className="btn" onClick={handleClose}>
              Cerrar
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

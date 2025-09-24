import PageHeader from "../components/layout/PageHeader";

const mockReports = [
  { name: "IVA mensual", description: "Detalle compras y ventas con cr�dito fiscal", lastRun: "2025-09-01" },
  { name: "Ventas por categor�a", description: "Participaci�n de l�neas y margen", lastRun: "2025-09-18" },
  { name: "Performance proveedores", description: "Lead time, cumplimiento y descuentos", lastRun: "2025-09-10" }
];

export default function ReportsPage() {
  return (
    <div className="page-section">
      <PageHeader
        title="Reportes y an�lisis"
        description="Genera reportes financieros y operacionales listos para auditar."
        actions={<button className="btn">+ Nuevo reporte</button>}
      />

      <div className="card">
        <ul className="report-list">
          {mockReports.map((report) => (
            <li key={report.name}>
              <div>
                <strong>{report.name}</strong>
                <p className="muted small">{report.description}</p>
              </div>
              <div className="report-meta">
                <span className="muted small">�ltima ejecuci�n: {report.lastRun}</span>
                <button className="btn ghost">Generar</button>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}

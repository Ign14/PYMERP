import PageHeader from "../components/layout/PageHeader";

const cashflow = [
  { period: "Semana 38", income: "$6.2M", expense: "$4.9M", balance: "$1.3M" },
  { period: "Semana 37", income: "$5.8M", expense: "$5.1M", balance: "$0.7M" },
  { period: "Semana 36", income: "$6.5M", expense: "$5.6M", balance: "$0.9M" }
];

export default function FinancesPage() {
  return (
    <div className="page-section">
      <PageHeader
        title="Finanzas"
        description="Concilia cuentas por cobrar/pagar y planifica flujo de caja."
        actions={<button className="btn">Exportar flujo</button>}
      />

      <section className="kpi-grid">
        <div className="card stat">
          <h3>Caja disponible</h3>
          <p className="stat-value">$9.4M</p>
          <span className="stat-trend">Cobros próximos 7 días: $3.2M</span>
        </div>
        <div className="card stat">
          <h3>Cuentas por cobrar</h3>
          <p className="stat-value">$5.1M</p>
          <span className="stat-trend">18 documentos | 4 vencidos</span>
        </div>
        <div className="card stat">
          <h3>Cuentas por pagar</h3>
          <p className="stat-value">$3.7M</p>
          <span className="stat-trend">7 facturas próximas</span>
        </div>
      </section>

      <div className="card table-card">
        <h3>Flujo semanal</h3>
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>Periodo</th>
                <th>Ingresos</th>
                <th>Egresos</th>
                <th>Balance</th>
              </tr>
            </thead>
            <tbody>
              {cashflow.map((row) => (
                <tr key={row.period}>
                  <td>{row.period}</td>
                  <td className="mono">{row.income}</td>
                  <td className="mono">{row.expense}</td>
                  <td className="mono">{row.balance}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

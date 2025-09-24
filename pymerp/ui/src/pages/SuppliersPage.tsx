import { useRef } from "react";
import PageHeader from "../components/layout/PageHeader";
import SuppliersCard, { SuppliersCardHandle } from "../components/SuppliersCard";

const mockContracts = [
  { supplier: "Distribuidora Norte", nextReview: "2025-10-15", status: "Activo" },
  { supplier: "Logística Express", nextReview: "2025-11-01", status: "Renegociar" }
];

export default function SuppliersPage() {
  const cardRef = useRef<SuppliersCardHandle>(null);

  return (
    <div className="page-section">
      <PageHeader
        title="Proveedores"
        description="Consolida contratos, contactos y desempeño de abastecimiento."
        actions={(
          <button className="btn" onClick={() => cardRef.current?.openCreate()}>
            + Registrar proveedor
          </button>
        )}
      />

      <section className="responsive-grid">
        <div className="card">
          <SuppliersCard ref={cardRef} />
        </div>
        <div className="card table-card">
          <h3>Contratos prioritarios</h3>
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th>Proveedor</th>
                  <th>Revisión</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                {mockContracts.map((contract) => (
                  <tr key={contract.supplier}>
                    <td>{contract.supplier}</td>
                    <td className="mono small">{contract.nextReview}</td>
                    <td><span className={`status ${contract.status.toLowerCase()}`}>{contract.status}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </div>
  );
}

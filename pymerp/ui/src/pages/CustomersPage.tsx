import { useMemo, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import PageHeader from "../components/layout/PageHeader";
import CustomersCard, { CustomersCardHandle } from "../components/CustomersCard";
import {
  CustomerSegmentSummary,
  UNASSIGNED_SEGMENT_CODE,
  listCustomerSegments,
} from "../services/client";

export default function CustomersPage() {
  const cardRef = useRef<CustomersCardHandle>(null);
  const [activeSegmentCode, setActiveSegmentCode] = useState<string | null>(null);
  const segmentsQuery = useQuery<CustomerSegmentSummary[], Error>({
    queryKey: ["customers", "segments"],
    queryFn: listCustomerSegments,
    staleTime: 60_000,
  });

  const segments = segmentsQuery.data ?? [];
  const totalCustomers = segments.reduce((acc, item) => acc + item.total, 0);
  const activeSegment = useMemo(() => {
    if (!activeSegmentCode) return null;
    return (
      segments.find((segment) => segment.code === activeSegmentCode) ?? {
        segment: activeSegmentCode === UNASSIGNED_SEGMENT_CODE ? "Sin segmentar" : activeSegmentCode,
        code: activeSegmentCode,
        total: 0,
      }
    );
  }, [activeSegmentCode, segments]);

  const toggleSegment = (segment: CustomerSegmentSummary) => {
    setActiveSegmentCode((prev) => (prev === segment.code ? null : segment.code));
  };

  const clearSegment = () => setActiveSegmentCode(null);

  return (
    <div className="page-section">
      <PageHeader
        title="Clientes"
        description="Administra fichas, contactos y geolocalización de clientes."
        actions={(
          <button className="btn" onClick={() => cardRef.current?.focusCreate()}>
            + Nuevo cliente
          </button>
        )}
      />

      <section className="responsive-grid">
        <div className="card">
          <CustomersCard
            ref={cardRef}
            segmentFilter={activeSegmentCode}
            segmentLabel={activeSegment?.segment}
            onClearSegment={activeSegmentCode ? clearSegment : undefined}
          />
        </div>
        <div className="card">
          <h3>Segmentación</h3>
          {segmentsQuery.isLoading && <p>Loading...</p>}
          {segmentsQuery.isError && <p className="error">{segmentsQuery.error?.message ?? "No se pudieron cargar los segmentos"}</p>}
          {!segmentsQuery.isLoading && !segmentsQuery.isError && (
            <>
              <ul className="segment-list">
                {segments.map((segment) => {
                  const isActive = segment.code === activeSegmentCode;
                  return (
                    <li key={`${segment.code}-${segment.segment}`}>
                      <button
                        type="button"
                        className={`segment-chip${isActive ? " active" : ""}`}
                        onClick={() => toggleSegment(segment)}
                      >
                        <div>
                          <strong>{segment.segment}</strong>
                          <p className="muted small">{segment.total} clientes</p>
                        </div>
                        <span className="stat-trend">
                          {totalCustomers ? `${Math.round((segment.total / totalCustomers) * 100)}%` : "-"}
                        </span>
                      </button>
                    </li>
                  );
                })}
                {segments.length === 0 && <li className="muted">Sin segmentos registrados</li>}
              </ul>
              {activeSegment && (
                <button className="btn ghost" type="button" onClick={clearSegment}>
                  Limpiar filtro
                </button>
              )}
            </>
          )}
        </div>
      </section>
    </div>
  );
}

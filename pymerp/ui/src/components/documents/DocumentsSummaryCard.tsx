import { Link } from "react-router-dom";
import { useMemo, type MouseEvent, type Ref } from "react";
import { useDocuments } from "../../hooks/useDocuments";
import type { DocumentType } from "../../services/client";

const DATE_FORMATTER = new Intl.DateTimeFormat("es-CL", { dateStyle: "medium" });
const CURRENCY_FORMATTER = new Intl.NumberFormat("es-CL", {
  style: "currency",
  currency: "CLP",
  maximumFractionDigits: 0,
});

function formatDate(value?: string) {
  if (!value) {
    return "—";
  }
  const timestamp = Date.parse(value);
  if (Number.isNaN(timestamp)) {
    return value;
  }
  return DATE_FORMATTER.format(new Date(timestamp));
}

function formatCurrency(value: number | null | undefined) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "—";
  }
  return CURRENCY_FORMATTER.format(value);
}

type DocumentsSummaryCardProps = {
  title: string;
  type: DocumentType;
  viewAllTo: string;
  limit?: number;
  emptyMessage?: string;
  onViewAllClick?: (event: MouseEvent<HTMLAnchorElement>) => void;
  viewAllRef?: Ref<HTMLAnchorElement>;
};

export default function DocumentsSummaryCard({
  title,
  type,
  viewAllTo,
  limit = 5,
  emptyMessage = "No hay documentos registrados.",
  onViewAllClick,
  viewAllRef,
}: DocumentsSummaryCardProps) {
  const query = useDocuments(type, { size: limit });

  const documents = query.data?.content ?? [];
  const total = query.data?.totalElements ?? 0;

  const totalLabel = useMemo(() => {
    if (query.isLoading) {
      return "Cargando...";
    }
    if (query.isError) {
      return "—";
    }
    return total.toLocaleString("es-CL");
  }, [query.isError, query.isLoading, total]);

  return (
    <section className="card documents-card" aria-busy={query.isLoading} aria-live="polite">
      <header className="documents-card__header">
        <div>
          <h3>{title}</h3>
          <p className="documents-card__total">{totalLabel}</p>
          <span className="muted small">Total documentos</span>
        </div>
        <Link
          className="btn ghost"
          to={viewAllTo}
          ref={viewAllRef}
          onClick={(event) => {
            if (onViewAllClick) {
              event.preventDefault();
              onViewAllClick(event);
            }
          }}
        >
          Ver todos
        </Link>
      </header>

      <div className="documents-card__content">
        {query.isLoading && <p className="muted" role="status">Cargando documentos...</p>}
        {query.isError && (
          <p className="error" role="alert">{query.error?.message ?? "No se pudieron obtener los documentos."}</p>
        )}
        {!query.isLoading && !query.isError && documents.length === 0 && (
          <p className="muted">{emptyMessage}</p>
        )}
        {!query.isLoading && !query.isError && documents.length > 0 && (
          <ul className="documents-card__list">
            {documents.map((document) => (
              <li key={document.id} className="documents-card__item">
                <div className="documents-card__item-info">
                  <span className="documents-card__item-title">
                    {document.type} {document.number ?? document.id}
                  </span>
                  <span className="documents-card__item-meta">
                    <span>{formatDate(document.issuedAt)}</span>
                    {document.status && (
                      <span className={`status ${document.status.toLowerCase()}`} aria-label={document.status}>
                        {document.status}
                      </span>
                    )}
                  </span>
                </div>
                <span className="documents-card__item-total">{formatCurrency(document.total)}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}

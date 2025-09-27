import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { deleteCompany, listCompanies, type Company } from "../services/client";
import CompanyFormModal from "./CompanyFormModal";
import { parseProblemDetail } from "../utils/problemDetail";

export default function CompaniesCard() {
  const queryClient = useQueryClient();
  const { data, isLoading, isError, error, refetch, isFetching } = useQuery<Company[], Error>({
    queryKey: ["companies"],
    queryFn: listCompanies,
    refetchOnWindowFocus: false,
  });

  const [editing, setEditing] = useState<Company | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const deleteMutation = useMutation<void, Error, string>({
    mutationFn: deleteCompany,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["companies"], exact: false });
      setDeleteError(null);
    },
    onError: (err: unknown) => {
      const parsed = parseProblemDetail(err);
      setDeleteError(parsed.message ?? "No se pudo eliminar la compañía");
    },
  });

  const handleDelete = (company: Company) => {
    if (!window.confirm(`¿Eliminar ${company.businessName}?`)) {
      return;
    }
    deleteMutation.mutate(company.id);
  };

  const companies = data ?? [];
  const hasCompanies = companies.length > 0;

  return (
    <div className="card">
      <div className="card-header">
        <h2>Compañías</h2>
        <button className="btn-link" onClick={() => refetch()} disabled={isFetching}>
          {isFetching ? "Actualizando..." : "Actualizar"}
        </button>
      </div>
      <div className="card-body">
        {isLoading && <p>Cargando...</p>}
        {isError && <p className="error">{error?.message ?? "No se pudieron cargar las compañías"}</p>}
        {!isLoading && !isError && !hasCompanies && (
          <p className="muted">Aún no registras compañías.</p>
        )}
        {!isLoading && !isError && hasCompanies && (
          <ul className="list company-list">
            {companies.map((company) => {
              const isDeleting = deleteMutation.isPending && deleteMutation.variables === company.id;
              const location = [company.address, company.commune].filter(Boolean).join(", ");
              return (
                <li key={company.id}>
                  <div className="company-row">
                    <div>
                      <strong>{company.businessName}</strong>
                      <div className="mono small">RUT {company.rut}</div>
                    </div>
                    <div className="company-actions">
                      <button className="btn-link" onClick={() => setEditing(company)}>
                        Editar
                      </button>
                      <button
                        className="btn-link danger"
                        onClick={() => handleDelete(company)}
                        disabled={deleteMutation.isPending}
                      >
                        {isDeleting ? "Eliminando..." : "Eliminar"}
                      </button>
                    </div>
                  </div>
                  <div className="company-details">
                    {company.businessActivity && <span>{company.businessActivity}</span>}
                    {location && <span>{location}</span>}
                    {company.phone && <span>{company.phone}</span>}
                    {company.email && <span className="mono small">{company.email}</span>}
                  </div>
                  {company.receiptFooterMessage ? (
                    <p className="muted small">Ticket: {company.receiptFooterMessage}</p>
                  ) : null}
                </li>
              );
            })}
          </ul>
        )}
        {deleteError ? (
          <p className="error" aria-live="assertive">
            {deleteError}
          </p>
        ) : null}
      </div>
      <CompanyFormModal company={editing} open={editing !== null} onClose={() => setEditing(null)} />
    </div>
  );
}

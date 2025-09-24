import { useQuery } from "@tanstack/react-query";
import { listCompanies, type Company } from "../services/client";

type Props = {
  onSelect?: (company: Company) => void;
};

export default function CompaniesCard({ onSelect }: Props) {
  const { data, isLoading, isError, error, refetch, isFetching } = useQuery<Company[], Error>({
    queryKey: ["companies"],
    queryFn: listCompanies,
    refetchOnWindowFocus: false,
  });

  return (
    <div className="card">
      <div className="card-header">
        <h2>Companies</h2>
        <button className="btn-link" onClick={() => refetch()} disabled={isFetching}>
          {isFetching ? "Refreshing..." : "Refresh"}
        </button>
      </div>
      <div className="card-body">
        {isLoading && <p>Loading...</p>}
        {isError && <p className="error">{error?.message ?? "Could not load companies"}</p>}
        {!isLoading && !isError && data && data.length === 0 && (
          <p className="muted">No companies yet.</p>
        )}
        {!isLoading && !isError && data && data.length > 0 && (
          <ul className="list">
            {data.map((company) => (
              <li key={company.id} onClick={() => onSelect?.(company)}>
                <strong>{company.name}</strong>
                {company.rut ? <span className="mono"> rut {company.rut}</span> : null}
                <div className="mono small">{company.id}</div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

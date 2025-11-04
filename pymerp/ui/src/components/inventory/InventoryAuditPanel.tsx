import { useQuery } from "@tanstack/react-query";
import { useState } from "react";

type InventoryMovement = {
  id: string;
  productId: string;
  productName: string;
  lotId: string;
  type: string;
  qty: number;
  refType: string | null;
  refId: string | null;
  note: string | null;
  createdBy: string | null;
  userIp: string | null;
  reasonCode: string | null;
  previousQty: number | null;
  newQty: number | null;
  createdAt: string;
};

type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
};

async function listInventoryMovements(params: {
  productId?: string;
  type?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}): Promise<Page<InventoryMovement>> {
  const searchParams = new URLSearchParams();
  if (params.productId) searchParams.append("productId", params.productId);
  if (params.type) searchParams.append("type", params.type);
  if (params.from) searchParams.append("from", params.from);
  if (params.to) searchParams.append("to", params.to);
  searchParams.append("page", String(params.page ?? 0));
  searchParams.append("size", String(params.size ?? 20));

  const response = await fetch(`/api/v1/inventory/movements?${searchParams}`);
  if (!response.ok) throw new Error("Error al cargar movimientos");
  return response.json();
}

const MOVEMENT_TYPES = [
  { value: "", label: "Todos los tipos" },
  { value: "MANUAL_IN", label: "Entrada manual" },
  { value: "MANUAL_OUT", label: "Salida manual" },
  { value: "SALE_OUT", label: "Venta" },
  { value: "SALE_CANCEL", label: "Cancelación venta" },
  { value: "PURCHASE_IN", label: "Compra" },
];

export function InventoryAuditPanel() {
  const [page, setPage] = useState(0);
  const [typeFilter, setTypeFilter] = useState("");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");

  const movementsQuery = useQuery({
    queryKey: ["inventory-movements", page, typeFilter, dateFrom, dateTo],
    queryFn: () =>
      listInventoryMovements({
        type: typeFilter || undefined,
        from: dateFrom ? new Date(dateFrom).toISOString() : undefined,
        to: dateTo ? new Date(dateTo + "T23:59:59").toISOString() : undefined,
        page,
        size: 20,
      }),
  });

  const getTypeLabel = (type: string) => {
    return MOVEMENT_TYPES.find(t => t.value === type)?.label ?? type;
  };

  const getTypeBadgeClass = (type: string) => {
    switch (type) {
      case "MANUAL_IN":
      case "PURCHASE_IN":
        return "status active";
      case "MANUAL_OUT":
      case "SALE_OUT":
        return "status cancelled";
      case "SALE_CANCEL":
        return "status warning";
      default:
        return "status";
    }
  };

  return (
    <div className="card">
      <h3>Historial de Movimientos (Auditoría)</h3>
      <p className="muted" style={{ marginBottom: "1rem" }}>
        Registro completo de todos los movimientos de inventario con trazabilidad
      </p>

      {/* Filtros */}
      <div className="filter-bar" style={{ marginBottom: "1rem" }}>
        <select className="input" value={typeFilter} onChange={(e) => {
          setTypeFilter(e.target.value);
          setPage(0);
        }}>
          {MOVEMENT_TYPES.map(t => (
            <option key={t.value} value={t.value}>{t.label}</option>
          ))}
        </select>

        <input
          type="date"
          className="input"
          value={dateFrom}
          onChange={(e) => {
            setDateFrom(e.target.value);
            setPage(0);
          }}
          placeholder="Desde"
        />

        <input
          type="date"
          className="input"
          value={dateTo}
          onChange={(e) => {
            setDateTo(e.target.value);
            setPage(0);
          }}
          placeholder="Hasta"
        />

        {(typeFilter || dateFrom || dateTo) && (
          <button
            className="btn ghost"
            onClick={() => {
              setTypeFilter("");
              setDateFrom("");
              setDateTo("");
              setPage(0);
            }}
          >
            Limpiar filtros
          </button>
        )}
      </div>

      {/* Tabla */}
      <div className="table-wrapper">
        <table className="table">
          <thead>
            <tr>
              <th>Fecha</th>
              <th>Tipo</th>
              <th>Producto</th>
              <th>Cantidad</th>
              <th>Stock Anterior</th>
              <th>Stock Nuevo</th>
              <th>Usuario</th>
              <th>IP</th>
              <th>Nota</th>
            </tr>
          </thead>
          <tbody>
            {movementsQuery.isLoading && (
              <tr>
                <td colSpan={9} className="muted">Cargando movimientos...</td>
              </tr>
            )}
            {movementsQuery.isError && (
              <tr>
                <td colSpan={9} className="error">
                  Error: {(movementsQuery.error as Error).message}
                </td>
              </tr>
            )}
            {!movementsQuery.isLoading &&
              !movementsQuery.isError &&
              movementsQuery.data?.content.map((movement) => (
                <tr key={movement.id}>
                  <td className="mono small">
                    {new Date(movement.createdAt).toLocaleString("es-CL")}
                  </td>
                  <td>
                    <span className={getTypeBadgeClass(movement.type)}>
                      {getTypeLabel(movement.type)}
                    </span>
                  </td>
                  <td>{movement.productName}</td>
                  <td className="mono">
                    {movement.type.includes("OUT") ? "-" : "+"}
                    {Number(movement.qty).toFixed(2)}
                  </td>
                  <td className="mono">
                    {movement.previousQty != null
                      ? Number(movement.previousQty).toFixed(2)
                      : "-"}
                  </td>
                  <td className="mono">
                    {movement.newQty != null
                      ? Number(movement.newQty).toFixed(2)
                      : "-"}
                  </td>
                  <td className="small">{movement.createdBy ?? "Sistema"}</td>
                  <td className="mono small">{movement.userIp ?? "-"}</td>
                  <td className="small">{movement.note ?? "-"}</td>
                </tr>
              ))}
            {!movementsQuery.isLoading &&
              !movementsQuery.isError &&
              movementsQuery.data?.content.length === 0 && (
                <tr>
                  <td colSpan={9} className="muted">No hay movimientos</td>
                </tr>
              )}
          </tbody>
        </table>
      </div>

      {/* Paginación */}
      {movementsQuery.data && movementsQuery.data.totalPages > 1 && (
        <div className="pagination">
          <button
            className="btn"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            Anterior
          </button>
          <span className="muted">
            Página {page + 1} de {movementsQuery.data.totalPages}
          </span>
          <button
            className="btn"
            disabled={page + 1 >= movementsQuery.data.totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            Siguiente
          </button>
        </div>
      )}

      {/* Estadísticas */}
      {movementsQuery.data && (
        <div style={{ marginTop: "1rem", padding: "0.75rem", background: "#f5f5f5", borderRadius: "4px" }}>
          <p className="muted small">
            <strong>Total de registros:</strong> {movementsQuery.data.totalElements} movimientos
          </p>
        </div>
      )}
    </div>
  );
}

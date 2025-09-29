import { useEffect, useMemo, useState } from "react";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  cancelPurchase,
  listPurchaseDaily,
  listPurchases,
  PurchaseDailyPoint,
  PurchaseSummary,
  PurchaseUpdatePayload,
  updatePurchase,
} from "../services/client";
import PageHeader from "../components/layout/PageHeader";
import PurchaseCreateDialog from "../components/dialogs/PurchaseCreateDialog";
import DocumentsSummaryCard from "../components/documents/DocumentsSummaryCard";
import {
  ColumnDef,
  flexRender,
  getCoreRowModel,
  useReactTable,
} from "@tanstack/react-table";
import {
  BarChart,
  Bar,
  CartesianGrid,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import useDebouncedValue from "../hooks/useDebouncedValue";

const PAGE_SIZE = 10;
const STATUS_OPTIONS = [
  { value: "", label: "Todos" },
  { value: "received", label: "Recibida" },
  { value: "cancelled", label: "Cancelada" },
];

export default function PurchasesPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [statusFilter, setStatusFilter] = useState("received");
  const [searchInput, setSearchInput] = useState("");

  const debouncedSearch = useDebouncedValue(searchInput, 300);

  useEffect(() => {
    setPage(0);
  }, [statusFilter, debouncedSearch]);

  const purchasesQuery = useQuery({
    queryKey: ["purchases", page, statusFilter, debouncedSearch],
    queryFn: () =>
      listPurchases({
        page,
        size: PAGE_SIZE,
        status: statusFilter || undefined,
        search: debouncedSearch || undefined,
      }),
    placeholderData: keepPreviousData,
  });

  const metricsQuery = useQuery<PurchaseDailyPoint[], Error>({
    queryKey: ["purchases", "metrics", 14],
    queryFn: () => listPurchaseDaily(14),
  });

  const cancelMutation = useMutation({
    mutationFn: (id: string) => cancelPurchase(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["purchases"] }),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: PurchaseUpdatePayload }) => updatePurchase(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["purchases"] }),
  });

  const handleCancel = (purchase: PurchaseSummary) => {
    if (purchase.status?.toLowerCase() === "cancelled") {
      window.alert("La compra ya fue cancelada.");
      return;
    }
    if (window.confirm(`Cancelar compra ${purchase.docNumber ?? purchase.id}?`)) {
      cancelMutation.mutate(purchase.id);
    }
  };

  const handleEdit = (purchase: PurchaseSummary) => {
    const docType = window.prompt("Documento", purchase.docType ?? "");
    if (docType === null) return;
    const docNumber = window.prompt("Numero", purchase.docNumber ?? "");
    if (docNumber === null) return;
    const payload: PurchaseUpdatePayload = {
      docType: docType.trim() || purchase.docType,
      docNumber: docNumber.trim() || purchase.docNumber,
    };
    updateMutation.mutate({ id: purchase.id, payload });
  };

  const columns = useMemo<ColumnDef<PurchaseSummary>[]>(() => [
    {
      header: "Documento",
      accessorKey: "docType",
      cell: (info) => info.getValue<string>() ?? "Factura",
    },
    {
      header: "Numero",
      accessorKey: "docNumber",
      cell: (info) => info.getValue<string>() ?? "-",
    },
    {
      header: "Proveedor",
      accessorFn: (row) => row.supplierName ?? row.supplierId ?? "-",
      cell: (info) => info.getValue<string>() ?? "-",
    },
    {
      header: "Estado",
      accessorKey: "status",
      cell: (info) => {
        const value = info.getValue<string>() ?? "";
        return <span className={`status ${value.toLowerCase()}`}>{value}</span>;
      },
    },
    {
      header: "Neto",
      accessorKey: "net",
      cell: (info) => `$${Number(info.getValue<string>()).toLocaleString()}`,
    },
    {
      header: "IVA",
      accessorKey: "vat",
      cell: (info) => `$${Number(info.getValue<string>()).toLocaleString()}`,
    },
    {
      header: "Total",
      accessorKey: "total",
      cell: (info) => `$${Number(info.getValue<string>()).toLocaleString()}`,
    },
    {
      header: "Emitida",
      accessorKey: "issuedAt",
      cell: (info) => new Date(info.getValue<string>()).toLocaleString(),
    },
    {
      id: "actions",
      header: "Acciones",
      cell: ({ row }) => (
        <div className="table-actions">
          <button className="btn ghost" type="button" onClick={() => handleEdit(row.original)}>
            Editar
          </button>
          <button
            className="btn ghost"
            type="button"
            onClick={() => handleCancel(row.original)}
          >
            Cancelar
          </button>
        </div>
      ),
    },
  ], []);

  const table = useReactTable({
    data: purchasesQuery.data?.content ?? [],
    columns,
    getCoreRowModel: getCoreRowModel(),
    manualPagination: true,
    pageCount: purchasesQuery.data?.totalPages ?? -1,
    state: {
      pagination: { pageIndex: page, pageSize: PAGE_SIZE },
    },
    onPaginationChange: (updater) => {
      const next = typeof updater === "function" ? updater({ pageIndex: page, pageSize: PAGE_SIZE }).pageIndex : updater.pageIndex;
      setPage(next);
    },
  });

  const dailyData = useMemo(() => (metricsQuery.data ?? []).map((point) => ({
    date: point.date,
    total: Number(point.total ?? 0),
    count: point.count,
  })), [metricsQuery.data]);

  return (
    <div className="page-section">
      <PageHeader
        title="Compras y abastecimiento"
        description="Controla ordenes, recepciones y presupuestos para evitar quiebres."
        actions={<button className="btn" onClick={() => setDialogOpen(true)}>+ Nueva orden</button>}
      />

      <div className="card">
        <div className="filter-bar">
          <input
            className="input"
            placeholder="Buscar (doc, numero, proveedor)"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
          />
          {searchInput && (
            <button className="btn ghost" type="button" onClick={() => setSearchInput("")}>
              Limpiar
            </button>
          )}
          <select className="input" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            {STATUS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        </div>
      </div>

      <section className="kpi-grid">
        <div className="card stat">
          <h3>Ordenes en curso</h3>
          <p className="stat-value">{purchasesQuery.data?.totalElements ?? 0}</p>
          <span className="stat-trend">Total registros listados</span>
        </div>
        <div className="card stat">
          <h3>Gasto 14 dias</h3>
          <p className="stat-value">${dailyData.reduce((acc, row) => acc + row.total, 0).toLocaleString()}</p>
          <span className="stat-trend">Incluye impuestos</span>
        </div>
        <div className="card stat">
          <h3>Ordenes diarias</h3>
          <p className="stat-value">{dailyData.reduce((acc, row) => acc + row.count, 0)}</p>
          <span className="stat-trend">Ultimas 2 semanas</span>
        </div>
      </section>

      <DocumentsSummaryCard
        title="Órdenes de compra"
        type="PURCHASE"
        viewAllTo="/app/purchases?type=PURCHASE"
        emptyMessage="No hay órdenes de compra."
      />

      <div className="card">
        <h3>Tendencia de compras</h3>
        <div style={{ height: 260 }}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={dailyData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#1f2937" />
              <XAxis dataKey="date" stroke="#9aa0a6" tick={{ fontSize: 12 }} />
              <YAxis stroke="#9aa0a6" tickFormatter={(value) => `$${(value / 1000).toFixed(0)}k`} />
              <Tooltip formatter={(value: number) => `$${value.toLocaleString()}`}/>
              <Bar dataKey="total" fill="#a855f7" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="card table-card">
        <h3>Ordenes recientes</h3>
        <div className="table-wrapper">
          <table className="table">
            <thead>
              {table.getHeaderGroups().map((headerGroup) => (
                <tr key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <th key={header.id}>{flexRender(header.column.columnDef.header, header.getContext())}</th>
                  ))}
                </tr>
              ))}
            </thead>
            <tbody>
              {table.getRowModel().rows.map((row) => (
                <tr key={row.id}>
                  {row.getVisibleCells().map((cell) => (
                    <td key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="pagination">
          <button className="btn" disabled={page === 0} onClick={() => setPage((prev) => Math.max(0, prev - 1))}>Anterior</button>
          <span className="muted">Pagina {page + 1} de {purchasesQuery.data?.totalPages ?? 1}</span>
          <button className="btn" disabled={page + 1 >= (purchasesQuery.data?.totalPages ?? 1)} onClick={() => setPage((prev) => prev + 1)}>Siguiente</button>
        </div>
      </div>

      <PurchaseCreateDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        onCreated={() => queryClient.invalidateQueries({ queryKey: ["purchases"] })}
      />
    </div>
  );
}

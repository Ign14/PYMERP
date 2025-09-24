import { useEffect, useMemo, useState } from "react";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  cancelSale,
  getSaleDetail,
  listSales,
  listSalesDaily,
  Page,
  SaleDetail,
  SaleRes,
  SaleSummary,
  SalesDailyPoint,
  SaleUpdatePayload,
  updateSale,
} from "../services/client";
import PageHeader from "../components/layout/PageHeader";
import SalesCreateDialog from "../components/dialogs/SalesCreateDialog";
import Modal from "../components/dialogs/Modal";
import { ColumnDef, flexRender, getCoreRowModel, useReactTable } from "@tanstack/react-table";
import {
  AreaChart,
  Area,
  CartesianGrid,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import useDebouncedValue from "../hooks/useDebouncedValue";
import { SALE_DOCUMENT_TYPES, SALE_PAYMENT_METHODS } from "../constants/sales";

const PAGE_SIZE = 10;
const STATUS_OPTIONS = [
  { value: "", label: "Todos" },
  { value: "emitida", label: "Emitida" },
  { value: "cancelled", label: "Cancelada" },
];

const DOC_TYPE_FILTERS = [
  { value: "", label: "Todos los documentos" },
  ...SALE_DOCUMENT_TYPES.map((option) => ({ value: option.value, label: option.label })),
];

const PAYMENT_METHOD_FILTERS = [
  { value: "", label: "Todos los pagos" },
  ...SALE_PAYMENT_METHODS.map((option) => ({ value: option.value, label: option.label })),
];

function normalizedIncludes(source: string | undefined | null, term: string) {
  if (!source) {
    return false;
  }
  return source.toLowerCase().includes(term);
}

export default function SalesPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [statusFilter, setStatusFilter] = useState("emitida");
  const [docTypeFilter, setDocTypeFilter] = useState("");
  const [paymentFilter, setPaymentFilter] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const [receiptSaleId, setReceiptSaleId] = useState<string | null>(null);

  const debouncedSearch = useDebouncedValue(searchInput, 300);

  useEffect(() => {
    setPage(0);
  }, [statusFilter, docTypeFilter, paymentFilter, debouncedSearch]);

  const salesQuery = useQuery({
    queryKey: [
      "sales",
      page,
      statusFilter,
      docTypeFilter,
      paymentFilter,
      debouncedSearch,
    ],
    queryFn: () =>
      listSales({
        page,
        size: PAGE_SIZE,
        status: statusFilter || undefined,
        docType: docTypeFilter || undefined,
        paymentMethod: paymentFilter || undefined,
        search: debouncedSearch || undefined,
      }),
    placeholderData: keepPreviousData,
  });

  const metricsQuery = useQuery<SalesDailyPoint[], Error>({
    queryKey: ["sales", "metrics", 14],
    queryFn: () => listSalesDaily(14),
  });

  const saleDetailQuery = useQuery<SaleDetail, Error>({
    queryKey: ["sale-detail", receiptSaleId],
    queryFn: () => {
      if (!receiptSaleId) {
        throw new Error("Venta no seleccionada");
      }
      return getSaleDetail(receiptSaleId);
    },
    enabled: !!receiptSaleId,
  });

  const cancelMutation = useMutation({
    mutationFn: (id: string) => cancelSale(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["sales"] }),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: SaleUpdatePayload }) => updateSale(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["sales"] }),
  });

  const handleCancel = (sale: SaleSummary) => {
    if (sale.status?.toLowerCase() === "cancelled") {
      window.alert("La venta ya fue cancelada.");
      return;
    }
    if (window.confirm(`Cancelar venta ${sale.docType ?? "Factura"}?`)) {
      cancelMutation.mutate(sale.id);
    }
  };

  const handleEdit = (sale: SaleSummary) => {
    const docType = window.prompt("Documento", sale.docType ?? "");
    if (docType === null) return;
    const paymentMethod = window.prompt("Metodo de pago", sale.paymentMethod ?? "");
    if (paymentMethod === null) return;
    const payload: SaleUpdatePayload = {
      docType: docType.trim() || sale.docType,
      paymentMethod: paymentMethod.trim() || sale.paymentMethod,
    };
    updateMutation.mutate({ id: sale.id, payload });
  };

  const handleShowReceipt = (sale: SaleSummary) => {
    setReceiptSaleId(sale.id);
  };

  const handleCloseReceipt = () => {
    setReceiptSaleId(null);
  };

  const handlePrint = () => {
    const detail = saleDetailQuery.data;
    if (!detail) return;
    const printWindow = window.open("", "_blank", "width=420,height=640");
    if (!printWindow) return;
    const doc = printWindow.document;
    doc.write("<title>Comprobante de venta</title>");
    const pre = doc.createElement("pre");
    pre.style.fontFamily = "'Courier New', monospace";
    pre.style.fontSize = "12px";
    pre.style.whiteSpace = "pre-wrap";
    pre.textContent = detail.thermalTicket;
    doc.body.appendChild(pre);
    doc.close();
    printWindow.focus();
    printWindow.print();
  };

  const handleSaleCreated = (sale: SaleRes) => {
    setPage(0);
    const searchTerm = searchInput.trim().toLowerCase();
    const statusMatches = !statusFilter || sale.status.toLowerCase() === statusFilter.toLowerCase();
    const docMatches = !docTypeFilter || sale.docType === docTypeFilter;
    const paymentMatches = !paymentFilter || sale.paymentMethod === paymentFilter;
    const searchMatches =
      !searchTerm ||
      normalizedIncludes(sale.docType, searchTerm) ||
      normalizedIncludes(sale.paymentMethod, searchTerm) ||
      normalizedIncludes(sale.customerName, searchTerm) ||
      normalizedIncludes(sale.customerId, searchTerm) ||
      normalizedIncludes(sale.id, searchTerm);

    if (statusMatches && docMatches && paymentMatches && searchMatches) {
      queryClient.setQueriesData({ queryKey: ["sales"] }, (current: Page<SaleSummary> | undefined) => {
        if (!current) {
          return current;
        }
        if (current.number !== 0) {
          return current;
        }
        const summary: SaleSummary = {
          id: sale.id,
          customerId: sale.customerId ?? undefined,
          customerName: sale.customerName ?? undefined,
          docType: sale.docType,
          paymentMethod: sale.paymentMethod,
          status: sale.status,
          net: sale.net,
          vat: sale.vat,
          total: sale.total,
          issuedAt: sale.issuedAt,
        };
        const existing = current.content ?? [];
        const updatedContent = [summary, ...existing].slice(0, current.size ?? PAGE_SIZE);
        return {
          ...current,
          content: updatedContent,
          totalElements: (current.totalElements ?? 0) + 1,
        };
      });
    }

    queryClient.invalidateQueries({ queryKey: ["sales"] });
  };

  const columns = useMemo<ColumnDef<SaleSummary>[]>(() => [
    {
      header: "Documento",
      accessorKey: "docType",
      cell: (info) => info.getValue<string>() ?? "Factura",
    },
    {
      header: "Cliente",
      accessorFn: (row) => row.customerName ?? row.customerId ?? "-",
      cell: (info) => info.getValue<string>() ?? "-",
    },
    {
      header: "Pago",
      accessorKey: "paymentMethod",
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
      cell: (info) => `$${(info.getValue<number>() ?? 0).toLocaleString()}`,
    },
    {
      header: "IVA",
      accessorKey: "vat",
      cell: (info) => `$${(info.getValue<number>() ?? 0).toLocaleString()}`,
    },
    {
      header: "Total",
      accessorKey: "total",
      cell: (info) => `$${(info.getValue<number>() ?? 0).toLocaleString()}`,
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
          <button className="btn ghost" type="button" onClick={() => handleShowReceipt(row.original)}>
            Comprobante
          </button>
          <button className="btn ghost" type="button" onClick={() => handleEdit(row.original)}>
            Editar
          </button>
          <button
            className="btn ghost"
            type="button"
            onClick={() => handleCancel(row.original)}
            disabled={cancelMutation.isPending}
          >
            {cancelMutation.isPending ? "Cancelando..." : "Cancelar"}
          </button>
        </div>
      ),
    },
  ], [cancelMutation.isPending]);

  const table = useReactTable({
    data: salesQuery.data?.content ?? [],
    columns,
    getCoreRowModel: getCoreRowModel(),
    manualPagination: true,
    pageCount: salesQuery.data?.totalPages ?? -1,
    state: {
      pagination: { pageIndex: page, pageSize: PAGE_SIZE },
    },
    onPaginationChange: (updater) => {
      const next = typeof updater === "function"
        ? updater({ pageIndex: page, pageSize: PAGE_SIZE }).pageIndex
        : updater.pageIndex;
      setPage(next);
    },
  });

  const dailyData = useMemo(() => (metricsQuery.data ?? []).map((point) => ({
    date: point.date,
    total: point.total ?? 0,
    count: point.count,
  })), [metricsQuery.data]);

  const ticketPromedio = useMemo(() => {
    if (!dailyData.length) return 0;
    const totalValue = dailyData.reduce((acc, point) => acc + (point.count > 0 ? point.total / point.count : 0), 0);
    return totalValue / dailyData.length;
  }, [dailyData]);

  return (
    <div className="page-section">
      <PageHeader
        title="Ventas"
        description="Gestiona documentos, monitorea cobranzas y analiza el desempeno."
        actions={<button className="btn" onClick={() => setDialogOpen(true)}>+ Registrar venta</button>}
      />

      <div className="card">
        <div className="filter-bar">
          <input
            className="input"
            placeholder="Buscar (cliente, doc, pago)"
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
          <select className="input" value={docTypeFilter} onChange={(e) => setDocTypeFilter(e.target.value)}>
            {DOC_TYPE_FILTERS.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
          <select className="input" value={paymentFilter} onChange={(e) => setPaymentFilter(e.target.value)}>
            {PAYMENT_METHOD_FILTERS.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        </div>
      </div>

      <section className="kpi-grid">
        <div className="card stat">
          <h3>Ticket promedio</h3>
          <p className="stat-value">${ticketPromedio ? ticketPromedio.toFixed(0) : "-"}</p>
          <span className="stat-trend">Promedio diario en 14 dias</span>
        </div>
        <div className="card stat">
          <h3>Ventas 14 dias</h3>
          <p className="stat-value">${dailyData.reduce((acc, point) => acc + point.total, 0).toLocaleString()}</p>
          <span className="stat-trend">Incluye impuestos</span>
        </div>
        <div className="card stat">
          <h3>Documentos</h3>
          <p className="stat-value">{dailyData.reduce((acc, point) => acc + point.count, 0)}</p>
          <span className="stat-trend">Emitidos ultimas 2 semanas</span>
        </div>
      </section>

      <div className="card">
        <h3>Tendencia de ventas</h3>
        <div style={{ height: 260 }}>
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={dailyData}>
              <defs>
                <linearGradient id="salesGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#60a5fa" stopOpacity={0.8} />
                  <stop offset="95%" stopColor="#60a5fa" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#1f2937" />
              <XAxis dataKey="date" stroke="#9aa0a6" tick={{ fontSize: 12 }} />
              <YAxis stroke="#9aa0a6" tickFormatter={(value) => `$${(value / 1000).toFixed(0)}k`} />
              <Tooltip formatter={(value: number) => `$${value.toLocaleString()}`} />
              <Area type="monotone" dataKey="total" stroke="#60a5fa" fill="url(#salesGradient)" />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="card table-card">
        <h3>Facturas recientes</h3>
        <div className="table-wrapper">
          <table className="table">
            <thead>
              {table.getHeaderGroups().map((headerGroup) => (
                <tr key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <th key={header.id}>
                      {header.isPlaceholder ? null : flexRender(header.column.columnDef.header, header.getContext())}
                    </th>
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
          <button className="btn" disabled={page === 0} onClick={() => setPage((prev) => Math.max(0, prev - 1))}>
            Anterior
          </button>
          <span className="muted">Pagina {page + 1} de {salesQuery.data?.totalPages ?? 1}</span>
          <button
            className="btn"
            disabled={page + 1 >= (salesQuery.data?.totalPages ?? 1)}
            onClick={() => setPage((prev) => prev + 1)}
          >
            Siguiente
          </button>
        </div>
      </div>

      <SalesCreateDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        onCreated={handleSaleCreated}
      />

      <Modal open={!!receiptSaleId} onClose={handleCloseReceipt} title="Comprobante termico">
        {saleDetailQuery.isLoading && <p>Cargando comprobante...</p>}
        {saleDetailQuery.isError && (
          <p className="error">{saleDetailQuery.error?.message ?? "No se pudo cargar el comprobante"}</p>
        )}
        {saleDetailQuery.data && (
          <div className="receipt-modal">
            <div className="receipt-summary">
              <p><strong>Documento:</strong> {saleDetailQuery.data.docType}</p>
              <p><strong>Pago:</strong> {saleDetailQuery.data.paymentMethod}</p>
              {saleDetailQuery.data.customer && (
                <p><strong>Cliente:</strong> {saleDetailQuery.data.customer.name}</p>
              )}
              <p><strong>Total:</strong> ${saleDetailQuery.data.total.toLocaleString()}</p>
            </div>
            <pre className="mono small" style={{ whiteSpace: "pre-wrap" }}>
              {saleDetailQuery.data.thermalTicket}
            </pre>
            <div className="buttons">
              <button className="btn" type="button" onClick={handlePrint}>Imprimir</button>
              <button className="btn ghost" type="button" onClick={handleCloseReceipt}>Cerrar</button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}

import { useEffect, useMemo, useRef, useState } from "react";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  cancelSale,
  getSaleDetail,
  listSales,
  listSalesTrend,
  Page,
  SaleDetail,
  SaleRes,
  SaleSummary,
  SalesDailyPoint,
  SaleUpdatePayload,
  SalesPeriodSummary,
  getSalesSummaryByPeriod,
  getSalesWindowMetrics,
  listDocumentsGrouped,
  getDocumentDetail,
  fetchDocumentFile,
  DocumentSummary,
  DocumentDetail,
  DocumentsGroupedResponse,
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
const DOCUMENTS_PAGE_SIZE = 6;
const TREND_DEFAULT_DAYS = 14;
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

function formatDateInput(date: Date): string {
  return date.toISOString().slice(0, 10);
}

function subtractDays(date: Date, amount: number): Date {
  const next = new Date(date);
  next.setDate(next.getDate() - amount);
  return next;
}

function formatCurrency(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "-";
  }
  return `$${Math.round(value).toLocaleString("es-CL")}`;
}

function formatNumber(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return "-";
  }
  return value.toLocaleString("es-CL");
}

type DocumentAction = "open" | "preview" | "print" | "download";

const API_BASE = import.meta.env.VITE_API_BASE ?? "/api";

function slugifyDocumentValue(value: string): string {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-zA-Z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .toLowerCase();
}

function buildDocumentFileName(summary: DocumentSummary, extension: string): string {
  const type = summary.type ? slugifyDocumentValue(summary.type) : "documento";
  const number = summary.number ? slugifyDocumentValue(summary.number) : slugifyDocumentValue(summary.id);
  const base = [type, number].filter(Boolean).join("-") || "documento";
  return `${base}.${extension}`;
}

function getBlobExtension(type: string): string {
  if (!type) {
    return "pdf";
  }
  if (type.includes("pdf")) {
    return "pdf";
  }
  if (type.includes("html")) {
    return "html";
  }
  if (type.includes("json")) {
    return "json";
  }
  return "bin";
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
  const [trendFrom, setTrendFrom] = useState(() =>
    formatDateInput(subtractDays(new Date(), TREND_DEFAULT_DAYS - 1)),
  );
  const [trendTo, setTrendTo] = useState(() => formatDateInput(new Date()));
  const [documentsModalOpen, setDocumentsModalOpen] = useState(false);
  const [documentsSalesPage, setDocumentsSalesPage] = useState(0);
  const [documentsPurchasesPage, setDocumentsPurchasesPage] = useState(0);
  const [previewDocument, setPreviewDocument] = useState<DocumentSummary | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const documentsCloseButtonRef = useRef<HTMLButtonElement | null>(null);
  const documentsPrimaryActionRef = useRef<HTMLButtonElement | null>(null);
  const previewObjectUrlRef = useRef<string | null>(null);

  const debouncedSearch = useDebouncedValue(searchInput, 300);
  const isTrendRangeInvalid = Boolean(trendFrom && trendTo && trendFrom > trendTo);

  useEffect(() => {
    setPage(0);
  }, [statusFilter, docTypeFilter, paymentFilter, debouncedSearch]);

  useEffect(() => {
    if (!documentsModalOpen) {
      if (previewObjectUrlRef.current) {
        URL.revokeObjectURL(previewObjectUrlRef.current);
        previewObjectUrlRef.current = null;
      }
      setPreviewDocument(null);
      setPreviewUrl(null);
      return;
    }
    setDocumentsSalesPage(0);
    setDocumentsPurchasesPage(0);
  }, [documentsModalOpen]);

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

  const salesTrendQuery = useQuery<SalesDailyPoint[], Error>({
    queryKey: ["sales", "trend", trendFrom, trendTo],
    queryFn: () => listSalesTrend({ from: trendFrom, to: trendTo }),
    enabled: !isTrendRangeInvalid && Boolean(trendFrom && trendTo),
    placeholderData: keepPreviousData,
  });

  const windowMetricsQuery = useQuery({
    queryKey: ["sales", "metrics", "window", TREND_DEFAULT_DAYS],
    queryFn: () => getSalesWindowMetrics(`${TREND_DEFAULT_DAYS}d`),
  });

  const summaryTodayQuery = useQuery<SalesPeriodSummary, Error>({
    queryKey: ["sales", "metrics", "summary", "today"],
    queryFn: () => getSalesSummaryByPeriod("today"),
  });

  const summaryWeekQuery = useQuery<SalesPeriodSummary, Error>({
    queryKey: ["sales", "metrics", "summary", "week"],
    queryFn: () => getSalesSummaryByPeriod("week"),
  });

  const summaryMonthQuery = useQuery<SalesPeriodSummary, Error>({
    queryKey: ["sales", "metrics", "summary", "month"],
    queryFn: () => getSalesSummaryByPeriod("month"),
  });

  const documentsQuery = useQuery<DocumentsGroupedResponse, Error>({
    queryKey: [
      "documents",
      "grouped",
      documentsSalesPage,
      documentsPurchasesPage,
      DOCUMENTS_PAGE_SIZE,
    ],
    queryFn: () =>
      listDocumentsGrouped({
        salesPage: documentsSalesPage,
        purchasesPage: documentsPurchasesPage,
        size: DOCUMENTS_PAGE_SIZE,
      }),
    enabled: documentsModalOpen,
    placeholderData: keepPreviousData,
  });

  const documentPreviewQuery = useQuery<DocumentDetail, Error>({
    queryKey: ["documents", "detail", previewDocument?.id],
    queryFn: () =>
      getDocumentDetail(previewDocument!.id, { direction: previewDocument!.direction }),
    enabled: documentsModalOpen && Boolean(previewDocument),
    staleTime: 5 * 60 * 1000,
  });

  useEffect(() => {
    if (!previewDocument) {
      if (previewObjectUrlRef.current) {
        URL.revokeObjectURL(previewObjectUrlRef.current);
        previewObjectUrlRef.current = null;
      }
      setPreviewUrl(null);
      return;
    }
  }, [previewDocument]);

  useEffect(() => {
    const detail = documentPreviewQuery.data;
    if (!detail) {
      if (!documentPreviewQuery.isFetching && !previewDocument) {
        if (previewObjectUrlRef.current) {
          URL.revokeObjectURL(previewObjectUrlRef.current);
          previewObjectUrlRef.current = null;
        }
        setPreviewUrl(null);
      }
      return;
    }
    if (previewObjectUrlRef.current) {
      URL.revokeObjectURL(previewObjectUrlRef.current);
      previewObjectUrlRef.current = null;
    }
    if (detail.previewUrl) {
      setPreviewUrl(detail.previewUrl);
      return;
    }
    if (detail.htmlPreview) {
      const blob = new Blob([detail.htmlPreview], { type: "text/html;charset=utf-8" });
      const objectUrl = URL.createObjectURL(blob);
      previewObjectUrlRef.current = objectUrl;
      setPreviewUrl(objectUrl);
      return;
    }
    setPreviewUrl(null);
  }, [documentPreviewQuery.data, documentPreviewQuery.isFetching, previewDocument]);

  const firstDocument = useMemo(() => {
    if (!documentsQuery.data) {
      return null;
    }
    const salesDocs = documentsQuery.data.sales?.content ?? [];
    if (salesDocs.length > 0) {
      return { id: salesDocs[0].id, direction: "sales" as const };
    }
    const purchaseDocs = documentsQuery.data.purchases?.content ?? [];
    if (purchaseDocs.length > 0) {
      return { id: purchaseDocs[0].id, direction: "purchases" as const };
    }
    return null;
  }, [documentsQuery.data]);

  useEffect(() => {
    if (!documentsModalOpen) {
      return;
    }
    if (!firstDocument) {
      return;
    }
    const element = documentsPrimaryActionRef.current;
    if (element) {
      element.focus();
    }
  }, [documentsModalOpen, firstDocument]);

  useEffect(() => {
    return () => {
      if (previewObjectUrlRef.current) {
        URL.revokeObjectURL(previewObjectUrlRef.current);
      }
    };
  }, []);

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

  const documentFileMutation = useMutation<
    { blob: Blob; intent: "download" | "print"; summary: DocumentSummary },
    Error,
    { summary: DocumentSummary; intent: "download" | "print" }
  >({
    mutationFn: async ({ summary, intent }) => {
      const blob = await fetchDocumentFile(summary.id, { direction: summary.direction });
      return { blob, intent, summary };
    },
    onSuccess: ({ blob, intent, summary }) => {
      const extension = getBlobExtension(blob.type);
      const fileName = buildDocumentFileName(summary, extension);
      const blobUrl = URL.createObjectURL(blob);
      if (intent === "download") {
        const link = window.document.createElement("a");
        link.href = blobUrl;
        link.download = fileName;
        link.rel = "noopener";
        window.document.body.appendChild(link);
        link.click();
        window.document.body.removeChild(link);
        URL.revokeObjectURL(blobUrl);
        return;
      }

      const printWindow = window.open(blobUrl, "_blank", "noopener");
      if (!printWindow) {
        URL.revokeObjectURL(blobUrl);
        window.alert(
          "No se pudo abrir la ventana de impresión. Permite las ventanas emergentes e inténtalo nuevamente.",
        );
        return;
      }

      let cleaned = false;
      const cleanup = () => {
        if (!cleaned) {
          cleaned = true;
          URL.revokeObjectURL(blobUrl);
        }
      };

      const handleAfterPrint = () => {
        printWindow.removeEventListener("afterprint", handleAfterPrint);
        printWindow.close();
        cleanup();
      };

      const handleUnload = () => {
        printWindow.removeEventListener("unload", handleUnload);
        cleanup();
      };

      const handleLoad = () => {
        printWindow.removeEventListener("load", handleLoad);
        try {
          printWindow.focus();
          printWindow.print();
        } catch (error) {
          console.error("No se pudo iniciar la impresión", error);
        }
      };

      printWindow.addEventListener("load", handleLoad);
      printWindow.addEventListener("afterprint", handleAfterPrint);
      printWindow.addEventListener("unload", handleUnload);

      window.setTimeout(cleanup, 60000);
    },
    onError: (error) => {
      console.error("No se pudo obtener el documento", error);
    },
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

  const handleOpenDocumentsModal = () => {
    setDocumentsModalOpen(true);
  };

  const handleCloseDocumentsModal = () => {
    setDocumentsModalOpen(false);
    documentFileMutation.reset();
  };

  const handleDocumentAction = (document: DocumentSummary, action: DocumentAction) => {
    switch (action) {
      case "open": {
        const base = API_BASE.endsWith("/") ? API_BASE.slice(0, -1) : API_BASE;
        const url = `${base}/v1/documents/${document.id}?direction=${document.direction}`;
        const opened = window.open(url, "_blank", "noopener");
        if (!opened) {
          window.alert(
            "No se pudo abrir el documento en una nueva pestaña. Permite las ventanas emergentes e inténtalo nuevamente.",
          );
        }
        break;
      }
      case "preview": {
        if (previewDocument?.id === document.id) {
          documentPreviewQuery.refetch();
        } else {
          setPreviewDocument(document);
        }
        break;
      }
      case "print": {
        documentFileMutation.reset();
        documentFileMutation.mutate({ summary: document, intent: "print" });
        break;
      }
      case "download": {
        documentFileMutation.reset();
        documentFileMutation.mutate({ summary: document, intent: "download" });
        break;
      }
      default:
        break;
    }
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

  const dailyData = useMemo(
    () =>
      (salesTrendQuery.data ?? []).map((point) => ({
        date: point.date,
        total: point.total ?? 0,
        count: point.count ?? 0,
      })),
    [salesTrendQuery.data],
  );

  const windowMetrics = windowMetricsQuery.data;
  const salesToday = summaryTodayQuery.data;
  const salesWeek = summaryWeekQuery.data;
  const salesMonth = summaryMonthQuery.data;
  const documentsData = documentsQuery.data;
  const salesDocumentsPage = documentsData?.sales;
  const purchaseDocumentsPage = documentsData?.purchases;
  const salesDocuments = salesDocumentsPage?.content ?? [];
  const purchaseDocuments = purchaseDocumentsPage?.content ?? [];
  const documentsActionsDisabled =
    documentsQuery.isFetching || documentPreviewQuery.isFetching || documentFileMutation.isPending;
  const documentActionErrorMessage = documentFileMutation.isError
    ? documentFileMutation.error?.message ?? "No se pudo procesar la acción del documento."
    : null;
  const previewDetail = documentPreviewQuery.data;

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
          <h3>Promedio venta diaria</h3>
          <p className="stat-value">
            {windowMetricsQuery.isLoading
              ? "Cargando..."
              : windowMetricsQuery.isError
              ? "—"
              : formatCurrency(windowMetrics?.dailyAverage)}
          </p>
          <span className="stat-trend">
            {windowMetricsQuery.isError
              ? "No se pudieron obtener los indicadores."
              : `Últimos ${TREND_DEFAULT_DAYS} días`}
          </span>
        </div>
        <div className="card stat">
          <h3>Total de ventas</h3>
          <p className="stat-value">
            {windowMetricsQuery.isLoading
              ? "Cargando..."
              : windowMetricsQuery.isError
              ? "—"
              : formatCurrency(windowMetrics?.totalWithTax)}
          </p>
          <span className="stat-trend">
            {windowMetricsQuery.isError
              ? "Intenta actualizar nuevamente."
              : "Incluye impuestos - últimos 14 días"}
          </span>
        </div>
        <div className="card stat">
          <h3>Ventas del día</h3>
          <p className="stat-value">
            {summaryTodayQuery.isLoading
              ? "Cargando..."
              : summaryTodayQuery.isError
              ? "—"
              : formatCurrency(salesToday?.total)}
          </p>
          <span className="stat-trend">
            {summaryTodayQuery.isError
              ? "Sin datos disponibles."
              : `${formatNumber(salesToday?.count)} documentos`}
          </span>
        </div>
        <div className="card stat">
          <h3>Ventas de la semana</h3>
          <p className="stat-value">
            {summaryWeekQuery.isLoading
              ? "Cargando..."
              : summaryWeekQuery.isError
              ? "—"
              : formatCurrency(salesWeek?.total)}
          </p>
          <span className="stat-trend">
            {summaryWeekQuery.isError
              ? "Sin datos disponibles."
              : `${formatNumber(salesWeek?.count)} documentos`}
          </span>
        </div>
        <div className="card stat">
          <h3>Ventas del mes</h3>
          <p className="stat-value">
            {summaryMonthQuery.isLoading
              ? "Cargando..."
              : summaryMonthQuery.isError
              ? "—"
              : formatCurrency(salesMonth?.total)}
          </p>
          <span className="stat-trend">
            {summaryMonthQuery.isError
              ? "Sin datos disponibles."
              : `${formatNumber(salesMonth?.count)} documentos`}
          </span>
        </div>
        <button
          type="button"
          className="card stat"
          onClick={handleOpenDocumentsModal}
          disabled={documentsActionsDisabled || documentsModalOpen}
          aria-label="Ver documentos agrupados por compras y ventas"
        >
          <h3>Número de documentos</h3>
          <p className="stat-value">
            {windowMetricsQuery.isLoading
              ? "Cargando..."
              : windowMetricsQuery.isError
              ? "—"
              : formatNumber(windowMetrics?.documentCount)}
          </p>
          <span className="stat-trend">
            {windowMetricsQuery.isError
              ? "No se pudo calcular el total."
              : documentsQuery.isError
              ? "No se pudo cargar el listado."
              : documentsActionsDisabled
              ? "Cargando documentos..."
              : "Compras y ventas recientes"}
          </span>
        </button>
      </section>

      <div className="card">
        <h3>Tendencia de ventas</h3>
        <div className="filter-bar">
          <label>
            Desde
            <input
              className="input"
              type="date"
              value={trendFrom}
              max={trendTo || undefined}
              onChange={(event) => setTrendFrom(event.target.value)}
            />
          </label>
          <label>
            Hasta
            <input
              className="input"
              type="date"
              value={trendTo}
              min={trendFrom || undefined}
              onChange={(event) => setTrendTo(event.target.value)}
            />
          </label>
          {salesTrendQuery.isFetching && !salesTrendQuery.isLoading && !isTrendRangeInvalid && (
            <span className="muted" role="status">Actualizando…</span>
          )}
        </div>
        <div style={{ minHeight: 220 }}>
          {isTrendRangeInvalid ? (
            <p className="error">La fecha "Desde" debe ser anterior o igual a "Hasta".</p>
          ) : salesTrendQuery.isError ? (
            <p className="error">
              No se pudo cargar la tendencia: {salesTrendQuery.error?.message ?? "Intenta nuevamente."}
            </p>
          ) : dailyData.length === 0 ? (
            salesTrendQuery.isLoading ? (
              <p>Cargando tendencia...</p>
            ) : (
              <p className="muted">No hay datos en el rango seleccionado.</p>
            )
          ) : (
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
          )}
        </div>
      </div>

      <div className="card table-card">
        <h3>Documentos recientes</h3>
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

      <Modal
        open={documentsModalOpen}
        onClose={handleCloseDocumentsModal}
        title="Documentos"
        initialFocusRef={documentsCloseButtonRef}
        size="wide"
      >
        {documentsQuery.isLoading && <p>Cargando documentos...</p>}
        {documentsQuery.isError && (
          <p className="error">
            {documentsQuery.error?.message ?? "No se pudieron obtener los documentos."}
          </p>
        )}
        {documentActionErrorMessage && (
          <p className="error" role="alert">{documentActionErrorMessage}</p>
        )}
        {documentsActionsDisabled && !documentsQuery.isLoading && !documentsQuery.isError && (
          <p className="muted" role="status">Actualizando documentos...</p>
        )}
        {!documentsQuery.isLoading && !documentsQuery.isError && (
          <div className="documents-modal__content">
            <div className="documents-modal__sections">
              <section className="documents-modal__section">
                <h4>Ventas</h4>
                {salesDocuments.length === 0 ? (
                  <p className="documents-modal__empty">No hay documentos de ventas.</p>
                ) : (
                  <div className="table-wrapper">
                    <table className="table">
                      <thead>
                        <tr>
                          <th>Tipo</th>
                          <th>Número</th>
                          <th>Fecha</th>
                          <th>Total</th>
                          <th>Estado</th>
                          <th>Acciones</th>
                        </tr>
                      </thead>
                      <tbody>
                        {salesDocuments.map((document) => (
                          <tr key={document.id}>
                            <td>{document.type}</td>
                            <td>{document.number ?? "-"}</td>
                            <td>{document.issuedAt ? new Date(document.issuedAt).toLocaleDateString() : "-"}</td>
                            <td>{formatCurrency(document.total)}</td>
                            <td>{document.status}</td>
                            <td>
                              <div className="table-actions">
                                <button
                                  className="btn"
                                  type="button"
                                  onClick={() => handleDocumentAction(document, "open")}
                                  disabled={documentsActionsDisabled}
                                  ref={
                                    firstDocument &&
                                    firstDocument.direction === "sales" &&
                                    firstDocument.id === document.id
                                      ? documentsPrimaryActionRef
                                      : undefined
                                  }
                                  aria-label={`Abrir ${document.type} ${document.number ?? document.id}`}
                                >
                                  Abrir
                                </button>
                                <button
                                  className="btn ghost"
                                  type="button"
                                  onClick={() => handleDocumentAction(document, "preview")}
                                  disabled={documentsActionsDisabled}
                                  aria-label={`Ver ${document.type} ${document.number ?? document.id}`}
                                >
                                  Ver
                                </button>
                                <button
                                  className="btn ghost"
                                  type="button"
                                  onClick={() => handleDocumentAction(document, "print")}
                                  disabled={documentsActionsDisabled}
                                  aria-label={`Imprimir ${document.type} ${document.number ?? document.id}`}
                                >
                                  Imprimir
                                </button>
                                <button
                                  className="btn ghost"
                                  type="button"
                                  onClick={() => handleDocumentAction(document, "download")}
                                  disabled={documentsActionsDisabled}
                                  aria-label={`Descargar ${document.type} ${document.number ?? document.id}`}
                                >
                                  Descargar
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
                <div className="pagination">
                  <button
                    className="btn"
                    type="button"
                    disabled={(salesDocumentsPage?.number ?? 0) === 0 || documentsActionsDisabled}
                    onClick={() =>
                      setDocumentsSalesPage((prev) => Math.max(0, prev - 1))
                    }
                  >
                    Anterior
                  </button>
                  <span className="muted">
                    Página {(salesDocumentsPage?.number ?? 0) + 1} de {salesDocumentsPage?.totalPages ?? 1}
                  </span>
                  <button
                    className="btn"
                    type="button"
                    disabled={
                      documentsActionsDisabled ||
                      ((salesDocumentsPage?.number ?? 0) + 1 >= (salesDocumentsPage?.totalPages ?? 1))
                    }
                    onClick={() => setDocumentsSalesPage((prev) => prev + 1)}
                  >
                    Siguiente
                  </button>
                </div>
              </section>
              <section className="documents-modal__section">
                <h4>Compras</h4>
                {purchaseDocuments.length === 0 ? (
                  <p className="documents-modal__empty">No hay documentos de compras.</p>
                ) : (
                  <div className="table-wrapper">
                    <table className="table">
                      <thead>
                        <tr>
                          <th>Tipo</th>
                          <th>Número</th>
                          <th>Fecha</th>
                          <th>Total</th>
                          <th>Estado</th>
                          <th>Acciones</th>
                        </tr>
                      </thead>
                      <tbody>
                        {purchaseDocuments.map((document) => (
                          <tr key={document.id}>
                            <td>{document.type}</td>
                            <td>{document.number ?? "-"}</td>
                            <td>{document.issuedAt ? new Date(document.issuedAt).toLocaleDateString() : "-"}</td>
                            <td>{formatCurrency(document.total)}</td>
                            <td>{document.status}</td>
                            <td>
                              <div className="table-actions">
                                <button
                                  className="btn"
                                  type="button"
                                  onClick={() => handleDocumentAction(document, "open")}
                                  disabled={documentsActionsDisabled}
                                  ref={
                                    firstDocument &&
                                    firstDocument.direction === "purchases" &&
                                    firstDocument.id === document.id
                                      ? documentsPrimaryActionRef
                                      : undefined
                                  }
                                  aria-label={`Abrir ${document.type} ${document.number ?? document.id}`}
                                >
                                  Abrir
                                </button>
                                <button
                                  className="btn ghost"
                                  type="button"
                                  onClick={() => handleDocumentAction(document, "preview")}
                                  disabled={documentsActionsDisabled}
                                  aria-label={`Ver ${document.type} ${document.number ?? document.id}`}
                                >
                                  Ver
                                </button>
                                <button
                                  className="btn ghost"
                                  type="button"
                                  onClick={() => handleDocumentAction(document, "print")}
                                  disabled={documentsActionsDisabled}
                                  aria-label={`Imprimir ${document.type} ${document.number ?? document.id}`}
                                >
                                  Imprimir
                                </button>
                                <button
                                  className="btn ghost"
                                  type="button"
                                  onClick={() => handleDocumentAction(document, "download")}
                                  disabled={documentsActionsDisabled}
                                  aria-label={`Descargar ${document.type} ${document.number ?? document.id}`}
                                >
                                  Descargar
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
                <div className="pagination">
                  <button
                    className="btn"
                    type="button"
                    disabled={(purchaseDocumentsPage?.number ?? 0) === 0 || documentsActionsDisabled}
                    onClick={() =>
                      setDocumentsPurchasesPage((prev) => Math.max(0, prev - 1))
                    }
                  >
                    Anterior
                  </button>
                  <span className="muted">
                    Página {(purchaseDocumentsPage?.number ?? 0) + 1} de {purchaseDocumentsPage?.totalPages ?? 1}
                  </span>
                  <button
                    className="btn"
                    type="button"
                    disabled={
                      documentsActionsDisabled ||
                      ((purchaseDocumentsPage?.number ?? 0) + 1 >= (purchaseDocumentsPage?.totalPages ?? 1))
                    }
                    onClick={() => setDocumentsPurchasesPage((prev) => prev + 1)}
                  >
                    Siguiente
                  </button>
                </div>
              </section>
            </div>
            <section className="documents-modal__preview" aria-live="polite">
              <header>
                <h5>Vista previa</h5>
                <span>
                  {previewDocument
                    ? `${previewDocument.type} ${previewDocument.number ?? previewDocument.id}`
                    : "Selecciona un documento para ver su detalle."}
                </span>
              </header>
              {previewDocument && (
                <div className="documents-modal__preview-meta">
                  <span>
                    Total: <strong>{formatCurrency(previewDocument.total)}</strong>
                  </span>
                  {previewDocument.issuedAt && (
                    <span>Fecha: {new Date(previewDocument.issuedAt).toLocaleDateString()}</span>
                  )}
                  <span>Estado: {previewDocument.status}</span>
                  {previewDetail?.counterparty && (
                    <span>
                      {previewDocument.direction === "sales" ? "Cliente" : "Proveedor"}: {previewDetail.counterparty}
                    </span>
                  )}
                </div>
              )}
              {documentPreviewQuery.isLoading && previewDocument && (
                <p role="status">Cargando vista previa...</p>
              )}
              {documentPreviewQuery.isError && previewDocument && (
                <p className="error" role="alert">
                  {documentPreviewQuery.error?.message ?? "No se pudo cargar la vista previa."}
                </p>
              )}
              {previewDocument ? (
                previewUrl ? (
                  <div className="documents-modal__preview-frame">
                    <iframe
                      title={`Vista previa ${previewDocument.type} ${previewDocument.number ?? previewDocument.id}`}
                      src={previewUrl}
                    />
                  </div>
                ) : (
                  !documentPreviewQuery.isLoading &&
                  !documentPreviewQuery.isError && (
                    <p className="documents-modal__preview-empty">
                      Este documento no tiene vista previa disponible.
                    </p>
                  )
                )
              ) : (
                <p className="documents-modal__preview-empty">
                  Selecciona un documento de compras o ventas para ver el detalle.
                </p>
              )}
            </section>
          </div>
        )}
        <div className="documents-modal__footer">
          <button
            ref={documentsCloseButtonRef}
            className="btn ghost"
            type="button"
            onClick={handleCloseDocumentsModal}
          >
            Cerrar
          </button>
        </div>
      </Modal>

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

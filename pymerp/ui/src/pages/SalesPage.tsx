import { useEffect, useMemo, useRef, useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  cancelSale,
  getSaleDetail,
  listSales,
  Page,
  SaleDetail,
  SaleRes,
  SaleSummary,
  SaleUpdatePayload,
  SalesPeriodSummary,
  getSalesSummaryByPeriod,
  DocumentSummary,
  DocumentFile,
  downloadDocument,
  downloadDocumentByLink,
  getBillingDocument,
  getDocumentPreview,
  updateSale,
  exportSalesToCSV,
} from '../services/client'
import PageHeader from '../components/layout/PageHeader'
import SalesCreateDialog from '../components/dialogs/SalesCreateDialog'
import Modal from '../components/dialogs/Modal'
import { ColumnDef, flexRender, getCoreRowModel, useReactTable } from '@tanstack/react-table'
import useDebouncedValue from '../hooks/useDebouncedValue'
import { SALE_DOCUMENT_TYPES, SALE_PAYMENT_METHODS } from '../constants/sales'
import SalesDashboardOverview from '../components/sales/SalesDashboardOverview'
import SalesTrendSection from '../components/sales/SalesTrendSection'
import SaleDocumentsModal from '../components/sales/SaleDocumentsModal'
import SalesTopProductsPanel from '../components/sales/SalesTopProductsPanel'
import SalesPaymentMethodAnalysis from '../components/sales/SalesPaymentMethodAnalysis'
import SalesDocTypeAnalysis from '../components/sales/SalesDocTypeAnalysis'
import SalesPerformanceMetrics from '../components/sales/SalesPerformanceMetrics'
import SalesTopCustomersPanel from '../components/sales/SalesTopCustomersPanel'
import SalesDailyTimeline from '../components/sales/SalesDailyTimeline'
import SalesForecastPanel from '../components/sales/SalesForecastPanel'
import SalesExportDialog from '../components/sales/SalesExportDialog'
import SalesAdvancedKPIs from '../components/sales/SalesAdvancedKPIs'
import SalesABCChart from '../components/SalesABCChart'
import SalesABCTable from '../components/SalesABCTable'
import SalesABCRecommendations from '../components/SalesABCRecommendations'
import SalesForecastChart from '../components/SalesForecastChart'
import SalesForecastTable from '../components/SalesForecastTable'
import SalesForecastInsights from '../components/SalesForecastInsights'

const PAGE_SIZE = 10
const STATUS_OPTIONS = [
  { value: '', label: 'Todos' },
  { value: 'emitida', label: 'Emitida' },
  { value: 'cancelled', label: 'Cancelada' },
]

const DOC_TYPE_FILTERS = [
  { value: '', label: 'Todos los documentos' },
  ...SALE_DOCUMENT_TYPES.map(option => ({ value: option.value, label: option.label })),
]

const PAYMENT_METHOD_FILTERS = [
  { value: '', label: 'Todos los pagos' },
  ...SALE_PAYMENT_METHODS.map(option => ({ value: option.value, label: option.label })),
]

function normalizedIncludes(source: string | undefined | null, term: string) {
  if (!source) {
    return false
  }
  return source.toLowerCase().includes(term)
}

function formatCurrency(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '-'
  }
  return `$${Math.round(value).toLocaleString('es-CL')}`
}

function formatNumber(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '-'
  }
  return value.toLocaleString('es-CL')
}

function formatAdjustments(discount?: number | null, tax?: number | null): string {
  const parts: string[] = []
  if (discount && discount > 0) {
    parts.push(`- ${formatCurrency(discount)}`)
  }
  if (tax && tax > 0) {
    parts.push(`+ ${formatCurrency(tax)}`)
  }
  return parts.length > 0 ? parts.join(' / ') : '—'
}

function getSaleDocumentNumber(sale: SaleSummary): string {
  if (sale.documentNumber && sale.documentNumber.trim()) {
    return sale.documentNumber.trim()
  }
  if (sale.docNumber && sale.docNumber.trim()) {
    return sale.docNumber.trim()
  }
  if (sale.series && (sale.folio ?? '') !== '') {
    return `${sale.series}-${sale.folio}`
  }
  return sale.id
}

function sanitizeFileSegment(segment: string): string {
  return segment
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-zA-Z0-9._-]+/g, '_')
    .replace(/^_+|_+$/g, '')
}

function getDefaultExtension(mimeType?: string): string {
  if (!mimeType) {
    return '.pdf'
  }
  const lower = mimeType.toLowerCase()
  if (lower.includes('pdf')) return '.pdf'
  if (lower.includes('html')) return '.html'
  if (lower.includes('json')) return '.json'
  if (lower.includes('xml')) return '.xml'
  return '.bin'
}

function buildDocumentFilename(document: DocumentSummary, file?: DocumentFile): string {
  if (file?.filename) {
    return file.filename
  }
  const typeSegment = sanitizeFileSegment(document.type || 'documento')
  const identifierSegment = sanitizeFileSegment(document.number ?? document.id)
  const extension = getDefaultExtension(file?.mimeType)
  return `${typeSegment || 'documento'}-${identifierSegment || document.id}${extension}`
}

function formatDocumentLabel(document: DocumentSummary): string {
  return `${document.type} ${document.number ?? document.id}`
}

function triggerBrowserDownload(file: DocumentFile, filename: string) {
  const url = URL.createObjectURL(file.blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.rel = 'noopener'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

async function openFileInPrintWindow(file: DocumentFile, title: string) {
  const url = URL.createObjectURL(file.blob)
  const printWindow = window.open('', '_blank', 'noopener,noreferrer')
  if (!printWindow) {
    URL.revokeObjectURL(url)
    throw new Error(
      'No se pudo abrir la ventana de impresión. Revisa el bloqueador de ventanas emergentes.'
    )
  }

  const cleanup = () => {
    URL.revokeObjectURL(url)
  }

  printWindow.addEventListener('beforeunload', cleanup, { once: true })

  if (file.mimeType.toLowerCase().includes('pdf')) {
    printWindow.document.body.style.margin = '0'
    printWindow.document.body.style.height = '100vh'
    const iframe = printWindow.document.createElement('iframe')
    iframe.src = url
    iframe.title = title
    iframe.style.border = '0'
    iframe.style.width = '100%'
    iframe.style.height = '100%'
    printWindow.document.body.appendChild(iframe)
    iframe.onload = () => {
      printWindow.document.title = title
      printWindow.focus()
      printWindow.print()
    }
  } else {
    const textContent = await file.blob.text()
    printWindow.document.open()
    printWindow.document.write(textContent)
    printWindow.document.title = title
    printWindow.document.close()
    printWindow.focus()
    printWindow.print()
  }

  setTimeout(cleanup, 60_000)
}

export default function SalesPage() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [statusFilter, setStatusFilter] = useState('emitida')
  const [docTypeFilter, setDocTypeFilter] = useState('')
  const [paymentFilter, setPaymentFilter] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [receiptDocument, setReceiptDocument] = useState<DocumentSummary | null>(null)
  const [receiptError, setReceiptError] = useState<string | null>(null)
  const [saleDocumentsModalOpen, setSaleDocumentsModalOpen] = useState(false)
  const [exportDialogOpen, setExportDialogOpen] = useState(false)
  const documentsTriggerRef = useRef<HTMLButtonElement | null>(null)
  const receiptPrimaryActionRef = useRef<HTMLButtonElement | null>(null)
  const receiptCloseButtonRef = useRef<HTMLButtonElement | null>(null)

  // Estado para el rango de fechas compartido entre dashboard y tendencia
  const defaultDays = 14
  const today = new Date().toISOString().split('T')[0]
  const defaultStartDate = new Date(Date.now() - (defaultDays - 1) * 24 * 60 * 60 * 1000)
    .toISOString()
    .split('T')[0]
  const [startDate, setStartDate] = useState<string>(defaultStartDate)
  const [endDate, setEndDate] = useState<string>(today)

  const debouncedSearch = useDebouncedValue(searchInput, 300)

  useEffect(() => {
    setPage(0)
  }, [statusFilter, docTypeFilter, paymentFilter, debouncedSearch])

  useEffect(() => {
    if (!saleDocumentsModalOpen) {
      requestAnimationFrame(() => {
        documentsTriggerRef.current?.focus()
      })
    }
  }, [saleDocumentsModalOpen])

  const salesQuery = useQuery({
    queryKey: ['sales', page, statusFilter, docTypeFilter, paymentFilter, debouncedSearch],
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
  })

  const summaryTodayQuery = useQuery<SalesPeriodSummary, Error>({
    queryKey: ['sales', 'metrics', 'summary', 'today'],
    queryFn: () => getSalesSummaryByPeriod('today'),
  })

  const summaryWeekQuery = useQuery<SalesPeriodSummary, Error>({
    queryKey: ['sales', 'metrics', 'summary', 'week'],
    queryFn: () => getSalesSummaryByPeriod('week'),
  })

  const summaryMonthQuery = useQuery<SalesPeriodSummary, Error>({
    queryKey: ['sales', 'metrics', 'summary', 'month'],
    queryFn: () => getSalesSummaryByPeriod('month'),
  })

  const saleDetailQuery = useQuery<SaleDetail, Error>({
    queryKey: ['sale-detail', receiptDocument?.id],
    queryFn: () => {
      if (!receiptDocument) {
        throw new Error('Documento no seleccionado')
      }
      return getSaleDetail(receiptDocument.id)
    },
    enabled: !!receiptDocument,
  })

  const receiptDetail = saleDetailQuery.data ?? null

  const receiptTotals = useMemo(() => {
    if (!receiptDetail) {
      return { subtotal: 0, discount: 0, tax: 0, total: 0 }
    }
    const discount = receiptDetail.items.reduce((acc, item) => acc + (item.discount ?? 0), 0)
    const subtotal = receiptDetail.net
    const tax = receiptDetail.vat ?? 0
    const total = receiptDetail.total ?? subtotal + tax
    return { subtotal, discount, tax, total }
  }, [receiptDetail])

  useEffect(() => {
    if (!receiptDocument || !receiptDetail) {
      return
    }
    const element = receiptPrimaryActionRef.current
    if (element) {
      requestAnimationFrame(() => element.focus())
    }
  }, [receiptDocument, receiptDetail])

  const cancelMutation = useMutation({
    mutationFn: (id: string) => cancelSale(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['sales'] }),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: SaleUpdatePayload }) =>
      updateSale(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['sales'] }),
  })
  const receiptPrintMutation = useMutation({
    mutationFn: async (document: DocumentSummary) => {
      const file = await getDocumentPreview(document.id)
      await openFileInPrintWindow(file, formatDocumentLabel(document))
    },
    onMutate: () => {
      setReceiptError(null)
    },
    onError: (error: unknown) => {
      setReceiptError(
        error instanceof Error
          ? error.message
          : 'No se pudo preparar la impresión. Intenta nuevamente.'
      )
    },
  })

  const receiptDownloadMutation = useMutation({
    mutationFn: async (document: DocumentSummary) => {
      const file = await downloadDocument(document.id)
      const filename = buildDocumentFilename(document, file)
      triggerBrowserDownload(file, filename)
    },
    onMutate: () => {
      setReceiptError(null)
    },
    onError: (error: unknown) => {
      setReceiptError(
        error instanceof Error
          ? error.message
          : 'No se pudo descargar el documento. Intenta nuevamente.'
      )
    },
  })

  const quickDownloadMutation = useMutation({
    mutationFn: async (sale: SaleSummary) => {
      const document: DocumentSummary = {
        id: sale.id,
        direction: 'sales',
        type: sale.docType ?? 'Documento',
        number: getSaleDocumentNumber(sale),
        issuedAt: sale.issuedAt,
        total: sale.total,
        status: sale.status,
      }
      const file = await downloadDocument(document.id)
      const filename = buildDocumentFilename(document, file)
      triggerBrowserDownload(file, filename)
    },
  })

  const exportMutation = useMutation({
    mutationFn: async () => {
      const blob = await exportSalesToCSV({
        status: statusFilter || undefined,
        docType: docTypeFilter || undefined,
        paymentMethod: paymentFilter || undefined,
        search: debouncedSearch || undefined,
      })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `ventas-${new Date().toISOString().split('T')[0]}.csv`
      link.rel = 'noopener'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
    },
  })

  const handleCancel = (sale: SaleSummary) => {
    if (sale.status?.toLowerCase() === 'cancelled') {
      window.alert('La venta ya fue cancelada.')
      return
    }
    if (window.confirm(`Cancelar venta ${sale.docType ?? 'Factura'}?`)) {
      cancelMutation.mutate(sale.id)
    }
  }

  const handleEdit = (sale: SaleSummary) => {
    const docType = window.prompt('Documento', sale.docType ?? '')
    if (docType === null) return
    const paymentMethod = window.prompt('Metodo de pago', sale.paymentMethod ?? '')
    if (paymentMethod === null) return
    const payload: SaleUpdatePayload = {
      docType: docType.trim() || sale.docType,
      paymentMethod: paymentMethod.trim() || sale.paymentMethod,
    }
    updateMutation.mutate({ id: sale.id, payload })
  }

  const handleShowReceipt = (sale: SaleSummary) => {
    const summary: DocumentSummary = {
      id: sale.id,
      direction: 'sales',
      type: sale.docType ?? 'Documento',
      number: getSaleDocumentNumber(sale),
      issuedAt: sale.issuedAt,
      total: sale.total,
      status: sale.status,
    }
    setReceiptDocument(summary)
    setReceiptError(null)
  }

  const handleCloseReceipt = () => {
    setReceiptDocument(null)
    setReceiptError(null)
    receiptPrintMutation.reset()
    receiptDownloadMutation.reset()
  }

  const handleOpenDocumentsModal = () => {
    setSaleDocumentsModalOpen(true)
  }

  const handleCloseDocumentsModal = () => {
    setSaleDocumentsModalOpen(false)
  }

  const handleSaleCreated = (sale: SaleRes) => {
    setPage(0)
    const searchTerm = searchInput.trim().toLowerCase()
    const statusMatches = !statusFilter || sale.status.toLowerCase() === statusFilter.toLowerCase()
    const docMatches = !docTypeFilter || sale.docType === docTypeFilter
    const paymentMatches = !paymentFilter || sale.paymentMethod === paymentFilter
    const searchMatches =
      !searchTerm ||
      normalizedIncludes(sale.docType, searchTerm) ||
      normalizedIncludes(sale.paymentMethod, searchTerm) ||
      normalizedIncludes(sale.customerName, searchTerm) ||
      normalizedIncludes(sale.customerId, searchTerm) ||
      normalizedIncludes(sale.id, searchTerm)

    if (statusMatches && docMatches && paymentMatches && searchMatches) {
      queryClient.setQueriesData(
        { queryKey: ['sales'] },
        (current: Page<SaleSummary> | undefined) => {
          if (!current) {
            return current
          }
          if (current.number !== 0) {
            return current
          }
          const summary: SaleSummary = {
            id: sale.id,
            customerId: sale.customerId ?? undefined,
            customerName: sale.customerName ?? undefined,
            docType: sale.docType,
            docNumber: sale.docNumber ?? sale.documentNumber ?? sale.id,
            paymentMethod: sale.paymentMethod,
            status: sale.status,
            net: sale.net,
            vat: sale.vat,
            total: sale.total,
            issuedAt: sale.issuedAt,
          }
          const existing = current.content ?? []
          const updatedContent = [summary, ...existing].slice(0, current.size ?? PAGE_SIZE)
          return {
            ...current,
            content: updatedContent,
            totalElements: (current.totalElements ?? 0) + 1,
          }
        }
      )
    }

    queryClient.invalidateQueries({ queryKey: ['sales'] })
  }

  const columns = useMemo<ColumnDef<SaleSummary>[]>(
    () => [
      {
        header: 'Documento',
        accessorKey: 'docType',
        cell: info => info.getValue<string>() ?? 'Factura',
      },
      {
        header: 'Número de documento',
        accessorFn: row => getSaleDocumentNumber(row),
        cell: info => {
          const value = info.getValue<string>() ?? '-'
          return <span className="mono">{value}</span>
        },
      },
      {
        header: 'Cliente',
        accessorFn: row => row.customerName ?? row.customerId ?? '-',
        cell: info => info.getValue<string>() ?? '-',
      },
      {
        header: 'Pago',
        accessorKey: 'paymentMethod',
        cell: info => info.getValue<string>() ?? '-',
      },
      {
        header: 'Estado',
        accessorKey: 'status',
        cell: info => {
          const value = info.getValue<string>() ?? ''
          const statusLower = value.toLowerCase()
          let badge = ''
          if (statusLower === 'emitida') {
            badge = '🟢 Emitida'
          } else if (statusLower === 'cancelled') {
            badge = '🔴 Cancelada'
          } else {
            badge = value
          }
          return (
            <span
              className={`inline-flex items-center gap-1 px-2 py-1 rounded text-xs font-medium ${
                statusLower === 'emitida'
                  ? 'bg-green-950 text-green-400 border border-green-800'
                  : statusLower === 'cancelled'
                    ? 'bg-red-950 text-red-400 border border-red-800'
                    : 'bg-neutral-800 text-neutral-400 border border-neutral-700'
              }`}
            >
              {badge}
            </span>
          )
        },
      },
      {
        header: 'Neto',
        accessorKey: 'net',
        cell: info => (
          <span className="text-neutral-100">
            ${(info.getValue<number>() ?? 0).toLocaleString()}
          </span>
        ),
      },
      {
        header: 'IVA',
        accessorKey: 'vat',
        cell: info => (
          <span className="text-neutral-100">
            ${(info.getValue<number>() ?? 0).toLocaleString()}
          </span>
        ),
      },
      {
        header: 'Total',
        accessorKey: 'total',
        cell: info => (
          <span className="font-semibold text-neutral-100">
            ${(info.getValue<number>() ?? 0).toLocaleString()}
          </span>
        ),
      },
      {
        header: 'Emitida',
        accessorKey: 'issuedAt',
        cell: info => (
          <span className="text-neutral-400 text-sm">
            {new Date(info.getValue<string>()).toLocaleString()}
          </span>
        ),
      },
      {
        id: 'actions',
        header: 'Acciones',
        cell: ({ row }) => (
          <div className="table-actions">
            <button
              className="btn ghost"
              type="button"
              onClick={() => handleShowReceipt(row.original)}
            >
              Comprobante
            </button>
            <button
              className="btn ghost"
              type="button"
              onClick={() => quickDownloadMutation.mutate(row.original)}
              disabled={quickDownloadMutation.isPending}
            >
              {quickDownloadMutation.isPending ? 'Descargando...' : 'Descargar'}
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
              {cancelMutation.isPending ? 'Cancelando...' : 'Cancelar'}
            </button>
          </div>
        ),
      },
    ],
    [cancelMutation.isPending, quickDownloadMutation.isPending]
  )

  const table = useReactTable({
    data: salesQuery.data?.content ?? [],
    columns,
    getCoreRowModel: getCoreRowModel(),
    manualPagination: true,
    pageCount: salesQuery.data?.totalPages ?? -1,
    state: {
      pagination: { pageIndex: page, pageSize: PAGE_SIZE },
    },
    onPaginationChange: updater => {
      const next =
        typeof updater === 'function'
          ? updater({ pageIndex: page, pageSize: PAGE_SIZE }).pageIndex
          : updater.pageIndex
      setPage(next)
    },
  })

  const salesToday = summaryTodayQuery.data
  const salesWeek = summaryWeekQuery.data
  const salesMonth = summaryMonthQuery.data

  // Calcular alertas
  const alerts = useMemo(() => {
    const alertList: Array<{ id: string; type: 'warning' | 'info'; message: string }> = []

    // Alerta de ventas bajas del día
    if (salesToday && salesWeek) {
      const weekAvg = salesWeek.total / 7
      if (salesToday.total > 0 && salesToday.total < weekAvg * 0.5) {
        alertList.push({
          id: 'low-sales-today',
          type: 'warning',
          message: `⚠️ Las ventas de hoy están un 50% por debajo del promedio semanal (${formatCurrency(weekAvg)})`,
        })
      }
    }

    // Alerta de documentos cancelados hoy
    if (salesQuery.data?.content) {
      const today = new Date().toISOString().split('T')[0]
      const cancelledToday = salesQuery.data.content.filter(sale => {
        const saleDate = new Date(sale.issuedAt).toISOString().split('T')[0]
        return sale.status?.toLowerCase() === 'cancelled' && saleDate === today
      })
      if (cancelledToday.length > 0) {
        alertList.push({
          id: 'cancelled-today',
          type: 'warning',
          message: `⚠️ ${cancelledToday.length} documento${cancelledToday.length > 1 ? 's' : ''} cancelado${cancelledToday.length > 1 ? 's' : ''} hoy`,
        })
      }
    }

    // Alerta informativa de cero ventas
    if (salesToday && salesToday.total === 0 && salesToday.count === 0) {
      alertList.push({
        id: 'no-sales-today',
        type: 'info',
        message: `ℹ️ Sin ventas registradas hoy`,
      })
    }

    return alertList
  }, [salesToday, salesWeek, salesQuery.data])

  return (
    <div className="page-section bg-neutral-950">
      <PageHeader
        title="Ventas"
        description="Gestiona documentos, monitorea cobranzas y analiza el desempeno."
        actions={
          <button className="btn" onClick={() => setDialogOpen(true)}>
            + Registrar venta
          </button>
        }
      />

      {/* Sección de KPIs Avanzados */}
      <div style={{ marginBottom: '2rem' }}>
        <SalesAdvancedKPIs />
      </div>

      {/* Sección de Análisis ABC de Productos */}
      <div style={{ marginBottom: '2rem' }}>
        <h2 className="text-xl font-bold text-white mb-4">Análisis ABC de Productos</h2>
        <div className="grid grid-cols-1 gap-6">
          <SalesABCChart />
          <SalesABCTable />
          <SalesABCRecommendations />
        </div>
      </div>

      {/* Sección de Pronóstico de Demanda */}
      <div style={{ marginBottom: '2rem' }}>
        <h2 className="text-xl font-bold text-white mb-4">Pronóstico de Demanda</h2>
        <div className="grid grid-cols-1 gap-6">
          <SalesForecastChart />
          <SalesForecastTable />
          <SalesForecastInsights />
        </div>
      </div>

      <SalesDashboardOverview
        startDate={startDate}
        endDate={endDate}
        onStartDateChange={setStartDate}
        onEndDateChange={setEndDate}
      />

      <section className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
          <h3 className="text-neutral-100 text-sm font-medium mb-2">💰 Ventas del día</h3>
          <p className="text-3xl font-bold text-neutral-100 mb-1">
            {summaryTodayQuery.isLoading
              ? '...'
              : summaryTodayQuery.isError
                ? '—'
                : formatCurrency(salesToday?.total)}
          </p>
          <div className="flex gap-2 items-center">
            <span className="text-sm text-neutral-400">
              {summaryTodayQuery.isError
                ? 'Sin datos disponibles.'
                : `${formatNumber(salesToday?.count)} docs`}
            </span>
            {!summaryTodayQuery.isLoading &&
            !summaryTodayQuery.isError &&
            salesToday &&
            salesWeek &&
            salesWeek.total > 0
              ? (() => {
                  const weekAvg = salesWeek.total / 7
                  const diff = ((salesToday.total - weekAvg) / weekAvg) * 100
                  const isPositive = diff > 0
                  return (
                    <span
                      className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium ${
                        isPositive
                          ? 'bg-green-950 text-green-400 border border-green-800'
                          : 'bg-red-950 text-red-400 border border-red-800'
                      }`}
                    >
                      {isPositive ? '🟢' : '🔴'} {diff > 0 ? '+' : ''}
                      {diff.toFixed(1)}%
                    </span>
                  )
                })()
              : null}
          </div>
        </div>
        <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
          <h3 className="text-neutral-100 text-sm font-medium mb-2">📈 Ventas de la semana</h3>
          <p className="text-3xl font-bold text-neutral-100 mb-1">
            {summaryWeekQuery.isLoading
              ? '...'
              : summaryWeekQuery.isError
                ? '—'
                : formatCurrency(salesWeek?.total)}
          </p>
          <div className="flex gap-2 items-center">
            <span className="text-sm text-neutral-400">
              {summaryWeekQuery.isError
                ? 'Sin datos disponibles.'
                : `${formatNumber(salesWeek?.count)} docs`}
            </span>
            {!summaryWeekQuery.isLoading &&
            !summaryWeekQuery.isError &&
            salesWeek &&
            salesMonth &&
            salesMonth.total > 0
              ? (() => {
                  const monthAvgWeek = salesMonth.total / 4
                  const diff = ((salesWeek.total - monthAvgWeek) / monthAvgWeek) * 100
                  const isPositive = diff > 0
                  return (
                    <span
                      className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium ${
                        isPositive
                          ? 'bg-green-950 text-green-400 border border-green-800'
                          : 'bg-red-950 text-red-400 border border-red-800'
                      }`}
                    >
                      {isPositive ? '🟢' : '🔴'} {diff > 0 ? '+' : ''}
                      {diff.toFixed(1)}%
                    </span>
                  )
                })()
              : null}
          </div>
        </div>
        <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
          <h3 className="text-neutral-100 text-sm font-medium mb-2">📊 Ventas del mes</h3>
          <p className="text-3xl font-bold text-neutral-100 mb-1">
            {summaryMonthQuery.isLoading
              ? '...'
              : summaryMonthQuery.isError
                ? '—'
                : formatCurrency(salesMonth?.total)}
          </p>
          <div className="flex gap-2 items-center">
            <span className="text-sm text-neutral-400">
              {summaryMonthQuery.isError
                ? 'Sin datos disponibles.'
                : `${formatNumber(salesMonth?.count)} docs`}
            </span>
            {!summaryMonthQuery.isLoading && !summaryMonthQuery.isError && salesMonth && salesWeek
              ? (() => {
                  const monthlyTrend = salesWeek.count > 0 ? salesMonth.count / salesWeek.count : 0
                  const trendLabel =
                    monthlyTrend >= 4
                      ? 'tendencia alta'
                      : monthlyTrend >= 2
                        ? 'tendencia media'
                        : 'tendencia baja'
                  return (
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium bg-neutral-800 text-neutral-400 border border-neutral-700">
                      📊 {trendLabel}
                    </span>
                  )
                })()
              : null}
          </div>
        </div>
        <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
          <h3 className="text-neutral-100 text-sm font-medium mb-2">📄 Total documentos</h3>
          <p className="text-3xl font-bold text-neutral-100 mb-1">
            {salesQuery.isLoading
              ? '...'
              : salesQuery.isError
                ? '—'
                : (salesQuery.data?.totalElements ?? 0).toLocaleString('es-CL')}
          </p>
          <button
            type="button"
            className="text-sm text-blue-400 hover:text-blue-300 transition-colors"
            onClick={handleOpenDocumentsModal}
            ref={documentsTriggerRef}
          >
            Ver documentos →
          </button>
        </div>
      </section>

      {/* Alertas inteligentes */}
      {alerts.length > 0 && (
        <section className="mb-6 space-y-3">
          {alerts.map(alert => (
            <div
              key={alert.id}
              className={`flex items-start gap-3 p-4 rounded-xl border ${
                alert.type === 'warning'
                  ? 'bg-yellow-950 border-yellow-800 text-yellow-400'
                  : 'bg-blue-950 border-blue-800 text-blue-400'
              }`}
            >
              <span className="text-lg">{alert.type === 'warning' ? '⚠️' : 'ℹ️'}</span>
              <p className="text-sm font-medium flex-1">{alert.message}</p>
            </div>
          ))}
        </section>
      )}

      <SalesTrendSection
        startDate={startDate}
        endDate={endDate}
        onStartDateChange={setStartDate}
        onEndDateChange={setEndDate}
      />

      {/* Paneles de análisis avanzado */}
      <SalesTopProductsPanel startDate={startDate} endDate={endDate} />

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <SalesPaymentMethodAnalysis startDate={startDate} endDate={endDate} />
        <SalesDocTypeAnalysis startDate={startDate} endDate={endDate} />
      </div>

      <SalesPerformanceMetrics startDate={startDate} endDate={endDate} />

      <SalesTopCustomersPanel startDate={startDate} endDate={endDate} />

      <SalesDailyTimeline date={endDate} />

      <SalesForecastPanel days={7} />

      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5 mb-6">
        <div className="flex flex-wrap gap-3 items-center mb-4">
          <input
            className="input bg-neutral-800 border-neutral-700 text-neutral-100 flex-1 min-w-[200px]"
            placeholder="Buscar (cliente, doc, pago)"
            value={searchInput}
            onChange={e => setSearchInput(e.target.value)}
          />
          {searchInput && (
            <button
              className="btn ghost text-neutral-400 hover:text-neutral-100"
              type="button"
              onClick={() => setSearchInput('')}
            >
              Limpiar
            </button>
          )}
          <select
            className="input bg-neutral-800 border-neutral-700 text-neutral-100"
            value={statusFilter}
            onChange={e => setStatusFilter(e.target.value)}
          >
            {STATUS_OPTIONS.map(opt => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
          <select
            className="input bg-neutral-800 border-neutral-700 text-neutral-100"
            value={docTypeFilter}
            onChange={e => setDocTypeFilter(e.target.value)}
          >
            {DOC_TYPE_FILTERS.map(opt => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
          <select
            className="input bg-neutral-800 border-neutral-700 text-neutral-100"
            value={paymentFilter}
            onChange={e => setPaymentFilter(e.target.value)}
          >
            {PAYMENT_METHOD_FILTERS.map(opt => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
          <button
            className="btn bg-cyan-600 hover:bg-cyan-700 text-white font-medium"
            type="button"
            onClick={() => setExportDialogOpen(true)}
          >
            📥 Exportar
          </button>
        </div>
      </div>

      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
        <h3 className="text-neutral-100 mb-4">Documentos recientes</h3>
        <div className="table-wrapper">
          <table className="table">
            <thead>
              {table.getHeaderGroups().map(headerGroup => (
                <tr key={headerGroup.id}>
                  {headerGroup.headers.map(header => (
                    <th key={header.id}>
                      {header.isPlaceholder
                        ? null
                        : flexRender(header.column.columnDef.header, header.getContext())}
                    </th>
                  ))}
                </tr>
              ))}
            </thead>
            <tbody>
              {table.getRowModel().rows.map(row => (
                <tr key={row.id}>
                  {row.getVisibleCells().map(cell => (
                    <td key={cell.id}>
                      {flexRender(cell.column.columnDef.cell, cell.getContext())}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="flex gap-2 mt-4 justify-between items-center border-t border-neutral-700 pt-4">
          <button
            className="btn bg-neutral-800 border-neutral-700 text-neutral-100 hover:bg-neutral-700"
            disabled={page === 0}
            onClick={() => setPage(prev => Math.max(0, prev - 1))}
          >
            ← Anterior
          </button>
          <span className="text-neutral-400">
            Página {page + 1} de {salesQuery.data?.totalPages ?? 1}
          </span>
          <button
            className="btn bg-neutral-800 border-neutral-700 text-neutral-100 hover:bg-neutral-700"
            disabled={page + 1 >= (salesQuery.data?.totalPages ?? 1)}
            onClick={() => setPage(prev => prev + 1)}
          >
            Siguiente →
          </button>
        </div>
      </div>

      <SaleDocumentsModal isOpen={saleDocumentsModalOpen} onClose={handleCloseDocumentsModal} />

      <SalesCreateDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        onCreated={handleSaleCreated}
      />

      <Modal
        open={!!receiptDocument}
        onClose={handleCloseReceipt}
        title="Detalle del comprobante"
        initialFocusRef={receiptPrimaryActionRef}
        className="modal--wide"
      >
        {saleDetailQuery.isLoading && <p>Cargando comprobante...</p>}
        {saleDetailQuery.isError && (
          <p className="error">
            {saleDetailQuery.error?.message ?? 'No se pudo cargar el comprobante.'}
          </p>
        )}
        {receiptError && !saleDetailQuery.isLoading && <p className="error">{receiptError}</p>}
        {receiptDetail && (
          <div className="document-detail" aria-live="polite">
            <section className="document-detail__header">
              <div className="document-detail__headline">
                <p className="document-detail__type">
                  {receiptDetail.docType ?? receiptDocument?.type ?? 'Documento'}
                </p>
                <p className="document-detail__number">
                  #{receiptDocument?.number ?? receiptDetail.id}
                </p>
              </div>
              <div className="document-detail__meta-grid">
                <div className="document-detail__meta-item">
                  <span className="document-detail__meta-label">Fecha</span>
                  <span className="document-detail__meta-value">
                    {receiptDocument?.issuedAt
                      ? new Date(receiptDocument.issuedAt).toLocaleString()
                      : '—'}
                  </span>
                </div>
                <div className="document-detail__meta-item">
                  <span className="document-detail__meta-label">
                    {receiptDetail.customer
                      ? 'Cliente'
                      : receiptDetail.supplier
                        ? 'Proveedor'
                        : 'Contraparte'}
                  </span>
                  <span className="document-detail__meta-value">
                    {receiptDetail.customer?.name ?? receiptDetail.supplier?.name ?? '—'}
                  </span>
                </div>
                <div className="document-detail__meta-item">
                  <span className="document-detail__meta-label">Pago</span>
                  <span className="document-detail__meta-value">
                    {receiptDetail.paymentMethod ?? '—'}
                  </span>
                </div>
                <div className="document-detail__meta-item">
                  <span className="document-detail__meta-label">Estado</span>
                  <span className="document-detail__meta-value">{receiptDetail.status}</span>
                </div>
                <div className="document-detail__meta-item document-detail__meta-item--highlight">
                  <span className="document-detail__meta-label">Total</span>
                  <span className="document-detail__meta-value document-detail__meta-value--highlight">
                    {formatCurrency(receiptTotals.total)}
                  </span>
                </div>
              </div>
            </section>
            <div className="document-detail__items table-wrapper compact">
              <table className="table">
                <thead>
                  <tr>
                    <th>SKU</th>
                    <th>Descripción</th>
                    <th className="mono">Cantidad</th>
                    <th className="mono">Precio unitario</th>
                    <th className="mono">Descuentos/Impuestos</th>
                    <th className="mono">Total ítem</th>
                  </tr>
                </thead>
                <tbody>
                  {receiptDetail.items.map((item, index) => (
                    <tr key={`${item.productId ?? 'item'}-${index}`}>
                      <td className="mono">{item.productId ?? '—'}</td>
                      <td>
                        <span className="document-detail__description" title={item.productName}>
                          {item.productName}
                        </span>
                      </td>
                      <td className="mono">{formatNumber(item.qty)}</td>
                      <td className="mono">{formatCurrency(item.unitPrice)}</td>
                      <td className="mono">{formatAdjustments(item.discount, item.tax)}</td>
                      <td className="mono">{formatCurrency(item.lineTotal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="document-detail__totals">
              <div className="document-detail__totals-item">
                <span>Subtotal</span>
                <strong>{formatCurrency(receiptTotals.subtotal)}</strong>
              </div>
              <div className="document-detail__totals-item">
                <span>Descuentos</span>
                <strong>
                  {receiptTotals.discount > 0
                    ? `- ${formatCurrency(receiptTotals.discount)}`
                    : formatCurrency(receiptTotals.discount)}
                </strong>
              </div>
              <div className="document-detail__totals-item">
                <span>Impuestos</span>
                <strong>{formatCurrency(receiptTotals.tax)}</strong>
              </div>
              <div className="document-detail__totals-item document-detail__totals-item--accent">
                <span>Total</span>
                <strong>{formatCurrency(receiptTotals.total)}</strong>
              </div>
            </div>
            <div className="document-detail__actions">
              <button
                ref={receiptPrimaryActionRef}
                className="btn"
                type="button"
                onClick={() =>
                  receiptDocument && receiptDetail && receiptPrintMutation.mutate(receiptDocument)
                }
                disabled={
                  !receiptDocument ||
                  !receiptDetail ||
                  receiptPrintMutation.isPending ||
                  receiptDownloadMutation.isPending
                }
              >
                {receiptPrintMutation.isPending ? 'Preparando...' : 'Imprimir'}
              </button>
              <button
                className="btn ghost"
                type="button"
                onClick={() =>
                  receiptDocument &&
                  receiptDetail &&
                  receiptDownloadMutation.mutate(receiptDocument)
                }
                disabled={
                  !receiptDocument ||
                  !receiptDetail ||
                  receiptDownloadMutation.isPending ||
                  receiptPrintMutation.isPending
                }
              >
                {receiptDownloadMutation.isPending ? 'Descargando...' : 'Descargar'}
              </button>
              <button
                ref={receiptCloseButtonRef}
                className="btn ghost"
                type="button"
                onClick={handleCloseReceipt}
              >
                Cerrar
              </button>
            </div>
          </div>
        )}
      </Modal>

      <SalesExportDialog
        isOpen={exportDialogOpen}
        onClose={() => setExportDialogOpen(false)}
        filters={{
          status: statusFilter,
          docType: docTypeFilter,
          paymentMethod: paymentFilter,
          search: debouncedSearch,
          startDate: startDate,
          endDate: endDate,
        }}
        totalRecords={salesQuery.data?.totalElements ?? 0}
      />
    </div>
  )
}

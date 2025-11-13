import { useEffect, useMemo, useState } from 'react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  cancelPurchase,
  listPurchases,
  listSuppliers,
  PurchaseSummary,
  PurchaseUpdatePayload,
  updatePurchase,
  exportPurchasesToCSV,
  importPurchasesFromCSV,
  downloadDocument,
  DocumentSummary,
  DocumentFile,
  getPurchaseDetail,
  PurchaseDetail,
} from '../services/client'
import PageHeader from '../components/layout/PageHeader'
import Modal from '../components/dialogs/Modal'
import PurchaseCreateDialog from '../components/dialogs/PurchaseCreateDialog'
import PurchaseImportDialog from '../components/dialogs/PurchaseImportDialog'
import PurchasesAdvancedKPIs from '../components/purchases/PurchasesAdvancedKPIs'
import PurchaseABCChart from '../components/purchases/PurchaseABCChart'
import PurchaseABCTable from '../components/purchases/PurchaseABCTable'
import PurchaseABCRecommendations from '../components/purchases/PurchaseABCRecommendations'
import PurchaseForecastChart from '../components/purchases/PurchaseForecastChart'
import PurchaseForecastTable from '../components/purchases/PurchaseForecastTable'
import PurchaseForecastInsights from '../components/purchases/PurchaseForecastInsights'
import PurchasesDashboardOverview from '../components/purchases/PurchasesDashboardOverview'
import PurchasesTrendSection from '../components/purchases/PurchasesTrendSection'
import { PurchasesAlertsPanel } from '../components/purchases/PurchasesAlertsPanel'
import { PurchasesTemporalComparison } from '../components/purchases/PurchasesTemporalComparison'
import { PurchasesOptimizationPanel } from '../components/purchases/PurchasesOptimizationPanel'
import { PurchasesSupplierStatsPanel } from '../components/purchases/PurchasesSupplierStatsPanel'
import PurchasesOrderTimeline from '../components/purchases/PurchasesOrderTimeline'
import PurchasesTopSuppliersPanel from '../components/purchases/PurchasesTopSuppliersPanel'
import PurchasesCategoryAnalysis from '../components/purchases/PurchasesCategoryAnalysis'
import PurchasesPaymentMethodAnalysis from '../components/purchases/PurchasesPaymentMethodAnalysis'
import PurchasesPerformanceMetrics from '../components/purchases/PurchasesPerformanceMetrics'
import PurchasesExportDialog from '../components/purchases/PurchasesExportDialog'
import ServiceList from '../components/services/ServiceList'
import {
  ColumnDef,
  flexRender,
  getCoreRowModel,
  useReactTable,
  getSortedRowModel,
  SortingState,
} from '@tanstack/react-table'
import useDebouncedValue from '../hooks/useDebouncedValue'

const PAGE_SIZE = 10
const STATUS_OPTIONS = [
  { value: '', label: 'Todos' },
  { value: 'received', label: 'Recibida' },
  { value: 'cancelled', label: 'Cancelada' },
]

function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('es-CL', {
    style: 'currency',
    currency: 'CLP',
    minimumFractionDigits: 0,
  }).format(amount)
}

function sanitizeFileSegment(segment: string): string {
  return segment
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-zA-Z0-9._-]+/g, '_')
    .replace(/^_+|_+$/g, '')
}

function getDefaultExtension(mimeType?: string): string {
  if (!mimeType) return '.pdf'
  const lower = mimeType.toLowerCase()
  if (lower.includes('pdf')) return '.pdf'
  if (lower.includes('html')) return '.html'
  if (lower.includes('json')) return '.json'
  if (lower.includes('xml')) return '.xml'
  return '.bin'
}

function buildDocumentFilename(document: DocumentSummary, file?: DocumentFile): string {
  if (file?.filename) return file.filename
  const typeSegment = sanitizeFileSegment(document.type || 'documento')
  const identifierSegment = sanitizeFileSegment(document.number ?? document.id)
  const extension = getDefaultExtension(file?.mimeType)
  return `${typeSegment || 'documento'}-${identifierSegment || document.id}${extension}`
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

export default function PurchasesPage() {
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [importDialogOpen, setImportDialogOpen] = useState(false)
  const [exportDialogOpen, setExportDialogOpen] = useState(false)
  const [receiptPurchaseId, setReceiptPurchaseId] = useState<string | null>(null)
  const [statusFilter, setStatusFilter] = useState('received')
  const [searchInput, setSearchInput] = useState('')
  const [sorting, setSorting] = useState<SortingState>([])

  // Filtros avanzados
  const [docTypeFilter, setDocTypeFilter] = useState('')
  const [supplierFilter, setSupplierFilter] = useState('')
  const [minAmount, setMinAmount] = useState('')
  const [maxAmount, setMaxAmount] = useState('')
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false)

  // Estado para el rango de fechas
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
  }, [statusFilter, debouncedSearch, docTypeFilter])

  // Query para obtener lista de proveedores
  const suppliersQuery = useQuery({
    queryKey: ['suppliers'],
    queryFn: () => listSuppliers(),
  })

  const purchasesQuery = useQuery({
    queryKey: ['purchases', page, statusFilter, debouncedSearch, docTypeFilter],
    queryFn: () => {
      const params: any = {
        page,
        size: PAGE_SIZE,
        status: statusFilter || undefined,
        search: debouncedSearch || undefined,
        docType: docTypeFilter || undefined,
      }

      return listPurchases(params)
    },
    placeholderData: keepPreviousData,
  })

  // Aplicar filtros del lado del cliente para proveedor y montos
  const filteredPurchases = useMemo(() => {
    if (!purchasesQuery.data) return null

    let filtered = [...purchasesQuery.data.content]

    // Filtro por proveedor
    if (supplierFilter) {
      filtered = filtered.filter(p => p.supplierId === supplierFilter)
    }

    // Filtro por monto mínimo
    if (minAmount) {
      const min = parseFloat(minAmount)
      if (!isNaN(min)) {
        filtered = filtered.filter(p => {
          const total = parseFloat(p.total?.toString() ?? '0')
          return total >= min
        })
      }
    }

    // Filtro por monto máximo
    if (maxAmount) {
      const max = parseFloat(maxAmount)
      if (!isNaN(max)) {
        filtered = filtered.filter(p => {
          const total = parseFloat(p.total?.toString() ?? '0')
          return total <= max
        })
      }
    }

    return {
      ...purchasesQuery.data,
      content: filtered,
    }
  }, [purchasesQuery.data, supplierFilter, minAmount, maxAmount])

  const cancelMutation = useMutation({
    mutationFn: (id: string) => cancelPurchase(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['purchases'] }),
  })

  const purchaseDetailQuery = useQuery<PurchaseDetail, Error>({
    queryKey: ['purchaseDetail', receiptPurchaseId],
    queryFn: () => getPurchaseDetail(receiptPurchaseId!),
    enabled: !!receiptPurchaseId,
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: PurchaseUpdatePayload }) =>
      updatePurchase(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['purchases'] }),
  })

  const quickDownloadMutation = useMutation({
    mutationFn: async (purchase: PurchaseSummary) => {
      const document: DocumentSummary = {
        id: purchase.id,
        direction: 'purchases',
        type: purchase.docType ?? 'Documento',
        number: purchase.docNumber ?? purchase.id,
        issuedAt: purchase.issuedAt,
        total: purchase.total,
        status: purchase.status,
      }
      const file = await downloadDocument(document.id)
      const filename = buildDocumentFilename(document, file)
      triggerBrowserDownload(file, filename)
    },
  })

  const exportMutation = useMutation({
    mutationFn: async () => {
      const blob = await exportPurchasesToCSV({
        status: statusFilter || undefined,
        search: debouncedSearch || undefined,
        docType: docTypeFilter || undefined,
      })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `compras-${new Date().toISOString().split('T')[0]}.csv`
      link.rel = 'noopener'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
    },
  })

  const handleCancel = (purchase: PurchaseSummary) => {
    if (purchase.status?.toLowerCase() === 'cancelled') {
      window.alert('La compra ya fue cancelada.')
      return
    }
    if (window.confirm(`Cancelar compra ${purchase.docNumber ?? purchase.id}?`)) {
      cancelMutation.mutate(purchase.id)
    }
  }

  const handleEdit = (purchase: PurchaseSummary) => {
    const docType = window.prompt('Documento', purchase.docType ?? '')
    if (docType === null) return
    const docNumber = window.prompt('Numero', purchase.docNumber ?? '')
    if (docNumber === null) return
    const payload: PurchaseUpdatePayload = {
      docType: docType.trim() || purchase.docType,
      docNumber: docNumber.trim() || purchase.docNumber,
    }
    updateMutation.mutate({ id: purchase.id, payload })
  }

  const columns = useMemo<ColumnDef<PurchaseSummary>[]>(
    () => [
      {
        header: 'Fecha',
        accessorKey: 'issuedAt',
        cell: info => new Date(info.getValue<string>()).toLocaleDateString('es-ES'),
        enableSorting: true,
        sortingFn: 'datetime',
      },
      {
        header: 'Documento',
        accessorKey: 'docType',
        cell: info => info.getValue<string>() ?? 'Factura',
        enableSorting: false,
      },
      {
        header: 'Numero',
        accessorKey: 'docNumber',
        cell: info => info.getValue<string>() ?? '-',
        enableSorting: false,
      },
      {
        header: 'Proveedor',
        accessorFn: row => row.supplierName ?? row.supplierId ?? '-',
        cell: info => info.getValue<string>() ?? '-',
        enableSorting: true,
      },
      {
        header: 'Estado',
        accessorKey: 'status',
        cell: info => {
          const value = (info.getValue<string>() ?? '').toLowerCase()

          // Mapeo de estados a badges con iconos
          const statusConfig: Record<string, { icon: string; label: string; className: string }> = {
            received: {
              icon: '🟢',
              label: 'Recibida',
              className: 'bg-green-950 text-green-400 border-green-800',
            },
            pending: {
              icon: '🟡',
              label: 'Pendiente',
              className: 'bg-yellow-950 text-yellow-400 border-yellow-800',
            },
            cancelled: {
              icon: '🔴',
              label: 'Cancelada',
              className: 'bg-red-950 text-red-400 border-red-800',
            },
            intransit: {
              icon: '⏳',
              label: 'En tránsito',
              className: 'bg-blue-950 text-blue-400 border-blue-800',
            },
            completed: {
              icon: '✅',
              label: 'Completada',
              className: 'bg-neutral-700 text-neutral-300 border-neutral-600',
            },
          }

          const config = statusConfig[value] ?? {
            icon: '⚪',
            label: value || 'Desconocido',
            className: 'bg-neutral-800 text-neutral-400 border-neutral-700',
          }

          return (
            <span
              className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium border ${config.className}`}
            >
              <span>{config.icon}</span>
              <span>{config.label}</span>
            </span>
          )
        },
        enableSorting: false,
      },
      {
        header: 'Neto',
        accessorKey: 'net',
        cell: info => `$${Number(info.getValue<string>()).toLocaleString()}`,
        enableSorting: true,
        sortingFn: 'alphanumeric',
      },
      {
        header: 'IVA',
        accessorKey: 'vat',
        cell: info => `$${Number(info.getValue<string>()).toLocaleString()}`,
        enableSorting: true,
        sortingFn: 'alphanumeric',
      },
      {
        header: 'Total',
        accessorKey: 'total',
        cell: info => `$${Number(info.getValue<string>()).toLocaleString()}`,
        enableSorting: true,
        sortingFn: 'alphanumeric',
      },
      {
        id: 'actions',
        header: 'Acciones',
        enableSorting: false,
        cell: ({ row }) => (
          <div className="table-actions">
            <button
              className="btn ghost"
              type="button"
              onClick={() => setReceiptPurchaseId(row.original.id)}
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
            <button className="btn ghost" type="button" onClick={() => handleCancel(row.original)}>
              Cancelar
            </button>
          </div>
        ),
      },
    ],
    [quickDownloadMutation.isPending]
  )

  const table = useReactTable({
    data: filteredPurchases?.content ?? [],
    columns,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    manualPagination: true,
    pageCount: filteredPurchases?.totalPages ?? -1,
    state: {
      sorting,
      pagination: { pageIndex: page, pageSize: PAGE_SIZE },
    },
    onSortingChange: setSorting,
    onPaginationChange: updater => {
      const next =
        typeof updater === 'function'
          ? updater({ pageIndex: page, pageSize: PAGE_SIZE }).pageIndex
          : updater.pageIndex
      setPage(next)
    },
    enableSorting: true,
  })

  return (
    <div className="page-section bg-neutral-950">
      <PageHeader
        title="Compras y abastecimiento"
        description="Controla ordenes, recepciones y presupuestos para evitar quiebres."
        actions={
          <button className="btn" onClick={() => setDialogOpen(true)}>
            + Nueva orden
          </button>
        }
      />

      {/* KPIs Avanzados */}
      <div className="mb-8">
        <PurchasesAdvancedKPIs />
      </div>

      {/* Análisis ABC de Proveedores */}
      <div className="mb-8 space-y-6">
        <h2 className="text-2xl font-semibold text-white">📊 Análisis ABC de Proveedores</h2>
        <PurchaseABCChart />
        <PurchaseABCRecommendations />
        <PurchaseABCTable />
      </div>

      {/* Pronóstico de Demanda */}
      <div className="mb-8 space-y-6">
        <h2 className="text-2xl font-semibold text-white">🔮 Pronóstico de Demanda</h2>
        <PurchaseForecastChart />
        <PurchaseForecastInsights />
        <PurchaseForecastTable />
      </div>

      <PurchasesDashboardOverview
        startDate={startDate}
        endDate={endDate}
        statusFilter={statusFilter}
      />

      <section className="responsive-grid" style={{ marginBottom: '2rem' }}>
        <ServiceList />
      </section>

      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
        <div className="filter-bar">
          <input
            className="input bg-neutral-800 border-neutral-700 text-neutral-100"
            placeholder="Buscar (doc, numero, proveedor)"
            value={searchInput}
            onChange={e => setSearchInput(e.target.value)}
          />
          {searchInput && (
            <button className="btn ghost" type="button" onClick={() => setSearchInput('')}>
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
          <button
            className="btn ghost"
            type="button"
            onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
          >
            {showAdvancedFilters ? '🔽 Ocultar filtros' : '🔼 Más filtros'}
          </button>
          <button className="btn" type="button" onClick={() => setExportDialogOpen(true)}>
            📊 Exportar Avanzado
          </button>
          <button className="btn ghost" type="button" onClick={() => setImportDialogOpen(true)}>
            Importar CSV
          </button>
        </div>

        {/* Filtros Avanzados */}
        {showAdvancedFilters && (
          <div
            className="filter-bar"
            style={{
              marginTop: '0.5rem',
              paddingTop: '0.5rem',
              borderTop: '1px solid #404040',
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
              gap: '0.5rem',
            }}
          >
            <select
              className="input bg-neutral-800 border-neutral-700 text-neutral-100"
              value={docTypeFilter}
              onChange={e => setDocTypeFilter(e.target.value)}
            >
              <option value="">Todos los tipos de documento</option>
              <option value="Factura">Factura</option>
              <option value="Boleta">Boleta</option>
              <option value="Cotización">Cotización</option>
              <option value="Orden de Compra">Orden de Compra</option>
              <option value="Guía de Despacho">Guía de Despacho</option>
            </select>

            <select
              className="input bg-neutral-800 border-neutral-700 text-neutral-100"
              value={supplierFilter}
              onChange={e => setSupplierFilter(e.target.value)}
            >
              <option value="">Todos los proveedores</option>
              {suppliersQuery.data?.map(supplier => (
                <option key={supplier.id} value={supplier.id}>
                  {supplier.name}
                </option>
              ))}
            </select>

            <input
              className="input bg-neutral-800 border-neutral-700 text-neutral-100"
              type="number"
              placeholder="Monto mínimo"
              value={minAmount}
              onChange={e => setMinAmount(e.target.value)}
            />

            <input
              className="input bg-neutral-800 border-neutral-700 text-neutral-100"
              type="number"
              placeholder="Monto máximo"
              value={maxAmount}
              onChange={e => setMaxAmount(e.target.value)}
            />

            {(docTypeFilter || supplierFilter || minAmount || maxAmount) && (
              <button
                className="btn ghost"
                type="button"
                onClick={() => {
                  setDocTypeFilter('')
                  setSupplierFilter('')
                  setMinAmount('')
                  setMaxAmount('')
                }}
              >
                Limpiar filtros avanzados
              </button>
            )}
          </div>
        )}
      </div>

      <PurchasesTrendSection
        startDate={startDate}
        endDate={endDate}
        onStartDateChange={setStartDate}
        onEndDateChange={setEndDate}
      />

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-4">
        <PurchasesTemporalComparison startDate={startDate} endDate={endDate} />
        <PurchasesAlertsPanel startDate={startDate} endDate={endDate} />
      </div>

      <PurchasesOptimizationPanel startDate={startDate} endDate={endDate} />

      <PurchasesSupplierStatsPanel startDate={startDate} endDate={endDate} />

      {/* Nuevos componentes Fase 1-3 */}
      <PurchasesOrderTimeline startDate={startDate} endDate={endDate} statusFilter={statusFilter} />

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))',
          gap: '1.5rem',
          marginBottom: '1.5rem',
        }}
      >
        <PurchasesTopSuppliersPanel
          startDate={startDate}
          endDate={endDate}
          statusFilter={statusFilter}
        />
        <PurchasesCategoryAnalysis
          startDate={startDate}
          endDate={endDate}
          statusFilter={statusFilter}
        />
      </div>

      <PurchasesPaymentMethodAnalysis
        startDate={startDate}
        endDate={endDate}
        statusFilter={statusFilter}
      />

      <PurchasesPerformanceMetrics
        startDate={startDate}
        endDate={endDate}
        statusFilter={statusFilter}
      />

      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5 table-card">
        <h3 className="text-neutral-100">Ordenes recientes</h3>
        <div className="table-wrapper">
          <table className="table">
            <thead>
              {table.getHeaderGroups().map(headerGroup => (
                <tr key={headerGroup.id}>
                  {headerGroup.headers.map(header => (
                    <th
                      key={header.id}
                      onClick={
                        header.column.getCanSort()
                          ? header.column.getToggleSortingHandler()
                          : undefined
                      }
                      style={{
                        cursor: header.column.getCanSort() ? 'pointer' : 'default',
                        userSelect: 'none',
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        {flexRender(header.column.columnDef.header, header.getContext())}
                        {header.column.getCanSort() && (
                          <span style={{ opacity: 0.5 }}>
                            {{
                              asc: '↑',
                              desc: '↓',
                            }[header.column.getIsSorted() as string] ?? '↕'}
                          </span>
                        )}
                      </div>
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
        <div className="pagination">
          <button
            className="btn"
            disabled={page === 0}
            onClick={() => setPage(prev => Math.max(0, prev - 1))}
          >
            Anterior
          </button>
          <span className="muted text-neutral-400">
            Pagina {page + 1} de {filteredPurchases?.totalPages ?? 1}
          </span>
          <button
            className="btn"
            disabled={page + 1 >= (filteredPurchases?.totalPages ?? 1)}
            onClick={() => setPage(prev => prev + 1)}
          >
            Siguiente
          </button>
        </div>
      </div>

      <PurchaseCreateDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        onCreated={() => queryClient.invalidateQueries({ queryKey: ['purchases'] })}
      />

      <PurchaseImportDialog
        open={importDialogOpen}
        onClose={() => setImportDialogOpen(false)}
        onImported={() => queryClient.invalidateQueries({ queryKey: ['purchases'] })}
      />

      <PurchasesExportDialog
        open={exportDialogOpen}
        onClose={() => setExportDialogOpen(false)}
        startDate={startDate}
        endDate={endDate}
        statusFilter={statusFilter}
      />

      {/* Modal de Comprobante */}
      <Modal
        open={!!receiptPurchaseId}
        onClose={() => setReceiptPurchaseId(null)}
        title="Detalle del Comprobante de Compra"
      >
        {purchaseDetailQuery.isLoading && <p>Cargando detalle...</p>}
        {purchaseDetailQuery.error && (
          <p className="error">Error al cargar: {purchaseDetailQuery.error.message}</p>
        )}
        {purchaseDetailQuery.data && (
          <div className="document-detail">
            <section className="document-detail__header">
              <div className="document-detail__title-row">
                <h2 className="document-detail__title">
                  {purchaseDetailQuery.data.docType || 'Compra'}
                </h2>
                <p className="document-detail__number">#{purchaseDetailQuery.data.docNumber ?? purchaseDetailQuery.data.id}</p>
              </div>
              <div className="document-detail__meta-grid">
                <div className="document-detail__meta-item">
                  <span className="document-detail__meta-label">Fecha Emisión</span>
                  <span className="document-detail__meta-value">
                    {purchaseDetailQuery.data.issuedAt
                      ? new Date(purchaseDetailQuery.data.issuedAt).toLocaleString('es-CL')
                      : '—'}
                  </span>
                </div>
                {purchaseDetailQuery.data.receivedAt && (
                  <div className="document-detail__meta-item">
                    <span className="document-detail__meta-label">Fecha Recepción</span>
                    <span className="document-detail__meta-value">
                      {new Date(purchaseDetailQuery.data.receivedAt).toLocaleString('es-CL')}
                    </span>
                  </div>
                )}
                <div className="document-detail__meta-item">
                  <span className="document-detail__meta-label">Proveedor</span>
                  <span className="document-detail__meta-value">
                    {purchaseDetailQuery.data.supplier?.name ?? '—'}
                  </span>
                </div>
                <div className="document-detail__meta-item">
                  <span className="document-detail__meta-label">Términos de pago</span>
                  <span className="document-detail__meta-value">
                    {purchaseDetailQuery.data.paymentTermDays > 0
                      ? `${purchaseDetailQuery.data.paymentTermDays} días`
                      : 'Sin términos de pago'}
                  </span>
                </div>
                <div className="document-detail__meta-item">
                  <span className="document-detail__meta-label">Estado</span>
                  <span className="document-detail__meta-value">
                    {purchaseDetailQuery.data.status}
                  </span>
                </div>
                <div className="document-detail__meta-item document-detail__meta-item--highlight">
                  <span className="document-detail__meta-label">Total</span>
                  <span className="document-detail__meta-value document-detail__meta-value--highlight">
                    {formatCurrency(purchaseDetailQuery.data.total)}
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
                    <th className="mono">Costo Unit.</th>
                    <th className="mono">Info Adicional</th>
                    <th className="mono">Total</th>
                  </tr>
                </thead>
                <tbody>
                  {purchaseDetailQuery.data.items.map((item, index) => {
                    const subtotal = item.qty * item.unitCost
                    const name = item.productName || item.serviceName || 'Item'
                    return (
                      <tr key={`${item.id}-${index}`}>
                        <td className="mono">{item.productSku ?? '—'}</td>
                        <td>
                          <span className="document-detail__description" title={name}>
                            {name}
                          </span>
                        </td>
                        <td className="mono">{item.qty}</td>
                        <td className="mono">{formatCurrency(item.unitCost)}</td>
                        <td className="mono" style={{ fontSize: '0.85rem', color: '#666' }}>
                          {item.expDate && `Venc: ${new Date(item.expDate).toLocaleDateString('es-CL')}`}
                          {item.mfgDate && ` | Fab: ${new Date(item.mfgDate).toLocaleDateString('es-CL')}`}
                          {item.locationCode && ` | 📍 ${item.locationCode}`}
                        </td>
                        <td className="mono">{formatCurrency(subtotal)}</td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
            <section className="document-detail__totals">
              <div className="document-detail__totals-row">
                <span className="document-detail__totals-label">Neto</span>
                <span className="document-detail__totals-value mono">
                  {formatCurrency(purchaseDetailQuery.data.net)}
                </span>
              </div>
              <div className="document-detail__totals-row">
                <span className="document-detail__totals-label">IVA (19%)</span>
                <span className="document-detail__totals-value mono">
                  {formatCurrency(purchaseDetailQuery.data.vat)}
                </span>
              </div>
              <div className="document-detail__totals-row document-detail__totals-row--total">
                <span className="document-detail__totals-label">Total</span>
                <span className="document-detail__totals-value mono">
                  {formatCurrency(purchaseDetailQuery.data.total)}
                </span>
              </div>
            </section>
            <div className="document-detail__actions">
              <button className="btn" onClick={() => setReceiptPurchaseId(null)}>
                Cerrar
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}

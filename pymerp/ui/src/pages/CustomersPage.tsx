import { useEffect, useMemo, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import PageHeader from '../components/layout/PageHeader'
import CustomersCard, { CustomersCardHandle } from '../components/CustomersCard'
import CustomerCreateDialog from '../components/dialogs/CustomerCreateDialog'
import CustomerImportDialog from '../components/dialogs/CustomerImportDialog'
import CustomersDashboard from '../components/customers/CustomersDashboard'
import CustomersClusterMap from '../components/customers/CustomersClusterMap'
import CustomersActivityTimeline from '../components/customers/CustomersActivityTimeline'
import CustomersTemporalAnalysis from '../components/customers/CustomersTemporalAnalysis'
import {
  Customer,
  CustomerSegmentSummary,
  CustomerStats,
  UNASSIGNED_SEGMENT_CODE,
  listCustomerSegments,
  listCustomers,
  getCustomerStats,
} from '../services/client'

export default function CustomersPage() {
  const cardRef = useRef<CustomersCardHandle>(null)
  const [activeSegmentCode, setActiveSegmentCode] = useState<string | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [importDialogOpen, setImportDialogOpen] = useState(false)
  const [editingCustomer, setEditingCustomer] = useState<Customer | null>(null)
  const [selectedCustomerId, setSelectedCustomerId] = useState<string | null>(null)
  const [showClusterMap, setShowClusterMap] = useState(false)
  const [showTimeline, setShowTimeline] = useState(false)
  const [showAnalysis, setShowAnalysis] = useState(false)

  const segmentsQuery = useQuery<CustomerSegmentSummary[], Error>({
    queryKey: ['customers', 'segments'],
    queryFn: listCustomerSegments,
    staleTime: 60_000,
  })

  const segments = segmentsQuery.data ?? []
  const totalCustomers = segments.reduce((acc, item) => acc + item.total, 0)
  const activeSegment = useMemo(() => {
    if (!activeSegmentCode) return null
    return (
      segments.find(segment => segment.code === activeSegmentCode) ?? {
        name: activeSegmentCode === UNASSIGNED_SEGMENT_CODE ? 'Sin segmentar' : activeSegmentCode,
        segment:
          activeSegmentCode === UNASSIGNED_SEGMENT_CODE ? 'Sin segmentar' : activeSegmentCode,
        code: activeSegmentCode,
        total: 0,
        color: null,
      }
    )
  }, [activeSegmentCode, segments])

  const toggleSegment = (segment: CustomerSegmentSummary) => {
    setActiveSegmentCode(prev => (prev === segment.code ? null : segment.code))
  }

  const clearSegment = () => setActiveSegmentCode(null)

  // Query para obtener todos los clientes (para el mapa)
  const allCustomersQuery = useQuery({
    queryKey: ['customers', 'all'],
    queryFn: async () => {
      const response = await listCustomers({ page: 0, size: 1000 })
      return response.content || []
    },
    enabled: showClusterMap,
    staleTime: 60_000,
  })

  // Query para obtener stats de todos los clientes
  const allStatsQuery = useQuery({
    queryKey: ['customers', 'all-stats'],
    queryFn: async () => {
      const customers = allCustomersQuery.data || []
      const statsPromises = customers.map(c => getCustomerStats(c.id).catch(() => null))
      const stats = await Promise.all(statsPromises)
      return customers.reduce(
        (acc, customer, index) => {
          const stat = stats[index]
          if (stat) {
            acc[customer.id] = stat
          }
          return acc
        },
        {} as Record<string, CustomerStats>
      )
    },
    enabled: showClusterMap && !!allCustomersQuery.data,
    staleTime: 60_000,
  })

  const getCustomerHealthStatus = (lastSaleDate?: string) => {
    if (!lastSaleDate) {
      return {
        status: 'inactive',
        label: 'Sin ventas',
        color: 'bg-neutral-700 text-neutral-400 border-neutral-600',
        icon: '⚪',
      }
    }
    const daysSinceLastSale = Math.floor(
      (new Date().getTime() - new Date(lastSaleDate).getTime()) / (1000 * 60 * 60 * 24)
    )
    if (daysSinceLastSale <= 30) {
      return {
        status: 'healthy',
        label: 'Saludable',
        color: 'bg-green-950 text-green-400 border-green-800',
        icon: '🟢',
      }
    }
    if (daysSinceLastSale <= 90) {
      return {
        status: 'at-risk',
        label: 'En riesgo',
        color: 'bg-yellow-950 text-yellow-400 border-yellow-800',
        icon: '🟡',
      }
    } else {
      return {
        status: 'inactive',
        label: 'Inactivo',
        color: 'bg-red-950 text-red-400 border-red-800',
        icon: '🔴',
      }
    }
  }

  const handleOpenCreateDialog = () => {
    setEditingCustomer(null)
    setDialogOpen(true)
  }

  const handleOpenEditDialog = (customer: Customer) => {
    setEditingCustomer(customer)
    setDialogOpen(true)
  }

  const handleCloseDialog = () => {
    setDialogOpen(false)
    setEditingCustomer(null)
  }

  const handleExportCSV = () => {
    const filters = cardRef.current?.getFilters()
    const params = new URLSearchParams()

    if (filters?.query) params.append('query', filters.query)
    if (filters?.segment) params.append('segment', filters.segment)
    if (filters?.active !== undefined && filters.active !== null) {
      params.append('active', String(filters.active))
    }

    const url = `/api/v1/customers/export${params.toString() ? `?${params.toString()}` : ''}`

    // Crear link temporal para descargar
    const link = document.createElement('a')
    link.href = url
    link.download = 'customers.csv'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }

  return (
    <div className="page-section bg-neutral-950">
      <PageHeader
        title="Clientes"
        description="Administra fichas, contactos y geolocalización de clientes."
        actions={
          <>
            <button
              className={`btn ${showClusterMap ? 'btn-secondary' : 'btn-ghost'}`}
              onClick={() => setShowClusterMap(!showClusterMap)}
            >
              🗺️ {showClusterMap ? 'Ocultar' : 'Mostrar'} Mapa
            </button>
            <button className="btn btn-secondary" onClick={() => setImportDialogOpen(true)}>
              📤 Importar CSV
            </button>
            <button className="btn btn-secondary" onClick={handleExportCSV}>
              📥 Exportar CSV
            </button>
            <button className="btn" onClick={handleOpenCreateDialog}>
              + Nuevo cliente
            </button>
          </>
        }
      />

      <CustomerCreateDialog
        open={dialogOpen}
        onClose={handleCloseDialog}
        editingCustomer={editingCustomer}
      />

      <CustomerImportDialog open={importDialogOpen} onClose={() => setImportDialogOpen(false)} />

      <CustomersDashboard />

      {showClusterMap && (
        <CustomersClusterMap
          customers={allCustomersQuery.data || []}
          customerStats={allStatsQuery.data || {}}
          onCustomerSelect={customer => {
            setSelectedCustomerId(customer.id)
            setEditingCustomer(customer)
            setShowTimeline(true)
            setShowAnalysis(true)
          }}
          selectedCustomerId={selectedCustomerId}
          getHealthStatus={getCustomerHealthStatus}
        />
      )}

      {showTimeline && selectedCustomerId && editingCustomer && (
        <CustomersActivityTimeline
          customerId={selectedCustomerId}
          customerName={editingCustomer.name}
        />
      )}

      {showAnalysis && selectedCustomerId && editingCustomer && (
        <CustomersTemporalAnalysis
          customerId={selectedCustomerId}
          customerName={editingCustomer.name}
        />
      )}

      <section className="responsive-grid">
        <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
          <CustomersCard
            ref={cardRef}
            segmentFilter={activeSegmentCode}
            segmentLabel={activeSegment?.name || activeSegment?.segment}
            onClearSegment={activeSegmentCode ? clearSegment : undefined}
            onOpenCreateDialog={handleOpenCreateDialog}
            onOpenEditDialog={customer => {
              handleOpenEditDialog(customer)
              setSelectedCustomerId(customer.id)
              setShowTimeline(true)
              setShowAnalysis(true)
            }}
          />
        </div>
        <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
          <h3 className="text-neutral-100">Segmentación</h3>
          {segmentsQuery.isLoading && <p className="text-neutral-400">Loading...</p>}
          {segmentsQuery.isError && (
            <p className="error text-red-400">
              {segmentsQuery.error?.message ?? 'No se pudieron cargar los segmentos'}
            </p>
          )}
          {!segmentsQuery.isLoading && !segmentsQuery.isError && (
            <>
              <ul className="segment-list">
                {segments.map(segment => {
                  const isActive = segment.code === activeSegmentCode
                  const segmentName = segment.name || segment.segment
                  return (
                    <li key={`${segment.code}-${segmentName}`}>
                      <button
                        type="button"
                        className={`segment-chip${isActive ? ' active' : ''} bg-neutral-800 hover:bg-neutral-700 border border-neutral-700`}
                        onClick={() => toggleSegment(segment)}
                        style={{
                          borderLeft: segment.color ? `4px solid ${segment.color}` : undefined,
                        }}
                      >
                        <div>
                          <strong className="text-neutral-100">{segmentName}</strong>
                          <p className="muted small text-neutral-400">{segment.total} clientes</p>
                        </div>
                        <span className="stat-trend text-neutral-300">
                          {totalCustomers
                            ? `${Math.round((segment.total / totalCustomers) * 100)}%`
                            : '-'}
                        </span>
                      </button>
                    </li>
                  )
                })}
                {segments.length === 0 && (
                  <li className="muted text-neutral-400">Sin segmentos registrados</li>
                )}
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
  )
}

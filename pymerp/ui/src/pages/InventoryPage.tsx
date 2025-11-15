import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import {
  getInventorySettings,
  getInventorySummary,
  listInventoryAlerts,
  listProducts,
  InventoryAlert,
  InventorySettings,
  InventorySummary,
  Page,
  Product,
} from '../services/client'
import PageHeader from '../components/layout/PageHeader'
import LocationsPage from './inventory/LocationsPage'
import LotsListPage from './inventory/LotsListPage'
import InventoryMovementsCard from './inventory/InventoryMovementsCard'
import InventoryAdjustmentDialog from '../components/dialogs/InventoryAdjustmentDialog'
import ProductCatalogModal from '../components/dialogs/ProductCatalogModal'
import ProductDetailModal from '../components/dialogs/ProductDetailModal'
import ProductFormDialog from '../components/dialogs/ProductFormDialog'
import { InventoryAuditPanel } from '../components/inventory/InventoryAuditPanel'
import InventoryRotationAnalysis from '../components/inventory/InventoryRotationAnalysis'
import InventoryValuationChart from '../components/inventory/InventoryValuationChart'
import InventoryReplenishmentPanel from '../components/inventory/InventoryReplenishmentPanel'
import InventoryEfficiencyMetrics from '../components/inventory/InventoryEfficiencyMetrics'
import InventoryStatsCard from '../components/InventoryStatsCard'
import InventoryMovementSummary from '../components/InventoryMovementSummary'
import ABCClassificationChart from '../components/ABCClassificationChart'
import ABCProductsTable from '../components/ABCProductsTable'
import ABCRecommendationsPanel from '../components/ABCRecommendationsPanel'
import ForecastChart from '../components/ForecastChart'
import ForecastTable from '../components/ForecastTable'
import ForecastRecommendations from '../components/ForecastRecommendations'

const FALLBACK_THRESHOLD = 10

function formatCurrency(value: number | string | null | undefined) {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) {
    return '$0'
  }
  return `$${numeric.toLocaleString('es-CL', { maximumFractionDigits: 0 })}`
}

export default function InventoryPage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [activeInventoryTab, setActiveInventoryTab] = useState<'lots' | 'locations'>('lots')
  const [adjustDialogOpen, setAdjustDialogOpen] = useState(false)
  const [catalogModalOpen, setCatalogModalOpen] = useState(false)
  const [productFormOpen, setProductFormOpen] = useState(false)
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null)
  const [thresholdValue, setThresholdValue] = useState<number | null>(null)

  const productsQuery = useQuery<Page<Product>, Error>({
    queryKey: ['products', { view: 'inventory' }],
    queryFn: () => listProducts({ size: 200, status: 'all' }),
  })

  const settingsQuery = useQuery<InventorySettings, Error>({
    queryKey: ['inventory', 'settings'],
    queryFn: getInventorySettings,
  })

  useEffect(() => {
    if (settingsQuery.data) {
      const numeric = Number(settingsQuery.data.lowStockThreshold ?? 0)
      if (Number.isFinite(numeric) && numeric > 0) {
        if (thresholdValue === null) {
          setThresholdValue(numeric)
        }
        return
      }
    }
    if (!settingsQuery.isLoading && thresholdValue === null) {
      setThresholdValue(FALLBACK_THRESHOLD)
    }
  }, [settingsQuery.data, settingsQuery.isLoading, thresholdValue])

  const summaryQuery = useQuery<InventorySummary, Error>({
    queryKey: ['inventory', 'summary'],
    queryFn: getInventorySummary,
  })

  const alertsQuery = useQuery<InventoryAlert[], Error>({
    queryKey: ['inventory', 'alerts', thresholdValue],
    enabled: thresholdValue !== null,
    queryFn: () => listInventoryAlerts(thresholdValue ?? undefined),
  })

  const handleAdjustmentApplied = () => {
    queryClient.invalidateQueries({ queryKey: ['inventory', 'summary'] })
    queryClient.invalidateQueries({ queryKey: ['inventory', 'alerts'] })
    queryClient.invalidateQueries({ queryKey: ['products'], exact: false })
    setAdjustDialogOpen(false)
  }

  const handleProductSaved = (product: Product) => {
    queryClient.invalidateQueries({ queryKey: ['products'], exact: false })
    setSelectedProduct(product)
    setProductFormOpen(false)
  }

  const productsIndex = useMemo(
    () =>
      new Map((productsQuery.data?.content ?? []).map(product => [product.id, product] as const)),
    [productsQuery.data]
  )

  const summary = summaryQuery.data
  const totalValue = formatCurrency(summary?.totalValue ?? 0)
  const activeProducts = summary?.activeProducts ?? 0
  const lowStockAlerts = summary?.lowStockAlerts ?? alertsQuery.data?.length ?? 0
  const configuredThreshold =
    summary?.lowStockThreshold ?? thresholdValue ?? FALLBACK_THRESHOLD

  const handleProductSelect = (product: Product) => {
    setCatalogModalOpen(false)
    setSelectedProduct(product)
  }

  const handleCloseProductDetail = () => {
    setSelectedProduct(null)
  }

  return (
    <div className="page-section">
      <PageHeader
        title="Inventario"
        description="Visibiliza catálogo, lotes y alertas de stock para garantizar disponibilidad."
        actions={
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button className="btn ghost" onClick={() => navigate('/app/inventory/movements')}>
              📋 Ver movimientos
            </button>
            <button className="btn ghost" onClick={() => setCatalogModalOpen(true)}>
              📦 Catálogo de productos
            </button>
            <button className="btn" onClick={() => setProductFormOpen(true)}>
              + Nuevo Producto
            </button>
            <button className="btn" onClick={() => setAdjustDialogOpen(true)}>
              + Ajuste de stock
            </button>
          </div>
        }
      />

      {/* Alertas inteligentes */}
      {!summaryQuery.isLoading && (
        <div
          style={{
            marginBottom: '1.5rem',
            display: 'flex',
            flexDirection: 'column',
            gap: '0.5rem',
          }}
        >
          {lowStockAlerts > 5 && (
            <div className="bg-red-950 border border-red-800 rounded-lg p-3 text-red-400 text-sm">
              🔴 <strong>Crítico:</strong> {lowStockAlerts} productos con stock bajo - requiere
              atención inmediata
            </div>
          )}
          {activeProducts === 0 && (
            <div className="bg-yellow-950 border border-yellow-800 rounded-lg p-3 text-yellow-400 text-sm">
              ⚠️ <strong>Advertencia:</strong> No hay productos activos en el inventario
            </div>
          )}
        </div>
      )}

      {/* KPIs Avanzados */}
      <div style={{ marginBottom: '2rem' }}>
        <h2
          className="text-neutral-100"
          style={{ fontSize: '1.25rem', fontWeight: '600', marginBottom: '1rem' }}
        >
          📊 KPIs de Inventario
        </h2>
        <InventoryStatsCard />
      </div>

      {/* Resumen de Movimientos */}
      <div style={{ marginBottom: '2rem' }}>
        <InventoryMovementSummary />
      </div>

      {/* Análisis ABC de Inventario */}
      <div style={{ marginBottom: '2rem' }}>
        <h2
          className="text-neutral-100"
          style={{ fontSize: '1.25rem', fontWeight: '600', marginBottom: '1rem' }}
        >
          🎯 Análisis ABC de Inventario
        </h2>

        {/* ABC Chart - Full Width */}
        <div style={{ marginBottom: '1.5rem' }}>
          <ABCClassificationChart />
        </div>

        {/* ABC Table + Recommendations - 2 Columns */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
          <ABCProductsTable />
          <ABCRecommendationsPanel />
        </div>
      </div>

      {/* Sección de Pronóstico de Demanda */}
      <div style={{ marginBottom: '2rem', marginTop: '2rem' }}>
        <h2
          className="text-neutral-100"
          style={{ fontSize: '1.5rem', fontWeight: '600', marginBottom: '1rem' }}
        >
          📈 Pronóstico de Demanda e Inteligencia Predictiva
        </h2>

        {/* Forecast Chart - Full Width */}
        <div style={{ marginBottom: '1.25rem' }}>
          <ForecastChart />
        </div>

        {/* Forecast Table + Recommendations - 2 Columns */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
          <ForecastTable />
          <ForecastRecommendations />
        </div>
      </div>

      <section className="responsive-grid">
        <div className="card">
          <div className="card-header">
            <div>
              <h2>Operaciones logísticas</h2>
              <p className="muted small">
                Visualiza lotes, asigna ubicaciones y gestiona centros logísticos desde un solo lugar.
              </p>
            </div>
            <div className="inline-actions">
              <button
                className={`btn ghost${activeInventoryTab === 'lots' ? ' active' : ''}`}
                type="button"
                onClick={() => setActiveInventoryTab('lots')}
              >
                Lotes
              </button>
              <button
                className={`btn ghost${activeInventoryTab === 'locations' ? ' active' : ''}`}
                type="button"
                onClick={() => setActiveInventoryTab('locations')}
              >
                Ubicaciones
              </button>
            </div>
          </div>
        </div>
        <div style={{ gridColumn: '1 / -1' }}>
          {activeInventoryTab === 'lots' ? <LotsListPage /> : <LocationsPage />}
        </div>
      </section>

      {/* Nueva sección: Análisis de Inventario */}
      <div style={{ marginBottom: '2rem', marginTop: '2rem' }}>
        <h2
          className="text-neutral-100"
          style={{ fontSize: '1.25rem', fontWeight: '600', marginBottom: '1rem' }}
        >
          📊 Análisis de Inventario
        </h2>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
          <InventoryRotationAnalysis />
          <InventoryValuationChart />
        </div>
      </div>

      <ProductFormDialog
        open={productFormOpen}
        product={null}
        onClose={() => setProductFormOpen(false)}
        onSaved={handleProductSaved}
      />

      {/* Nueva sección: Gestión Operativa */}
      <div style={{ marginBottom: '2rem' }}>
        <h2
          className="text-neutral-100"
          style={{ fontSize: '1.25rem', fontWeight: '600', marginBottom: '1rem' }}
        >
          ⚙️ Gestión Operativa
        </h2>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
          <InventoryReplenishmentPanel />
          <InventoryEfficiencyMetrics />
        </div>
      </div>

      <section className="responsive-grid" style={{ marginTop: '2rem', gap: '1rem' }}>
        <LotsListPage />
        <InventoryMovementsCard />
      </section>

      <InventoryAuditPanel />

      <InventoryAdjustmentDialog
        open={adjustDialogOpen}
        onClose={() => setAdjustDialogOpen(false)}
        onApplied={handleAdjustmentApplied}
      />

      <ProductCatalogModal
        open={catalogModalOpen}
        onClose={() => setCatalogModalOpen(false)}
        onSelectProduct={handleProductSelect}
      />

      <ProductDetailModal
        open={!!selectedProduct}
        product={selectedProduct}
        onClose={handleCloseProductDetail}
      />
    </div>
  )
}

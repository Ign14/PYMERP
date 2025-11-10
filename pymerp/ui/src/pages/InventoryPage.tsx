import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import {
  getInventorySettings,
  getInventorySummary,
  listInventoryAlerts,
  listProducts,
  updateInventorySettings,
  InventoryAlert,
  InventorySettings,
  InventorySummary,
  Page,
  Product,
} from '../services/client'
import PageHeader from '../components/layout/PageHeader'
import LocationsCard from '../components/LocationsCard'
import ServicesCard from '../components/ServicesCard'
import InventoryAdjustmentDialog from '../components/dialogs/InventoryAdjustmentDialog'
import ProductCatalogModal from '../components/dialogs/ProductCatalogModal'
import ProductDetailModal from '../components/dialogs/ProductDetailModal'
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
  const [adjustDialogOpen, setAdjustDialogOpen] = useState(false)
  const [catalogModalOpen, setCatalogModalOpen] = useState(false)
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null)
  const [thresholdValue, setThresholdValue] = useState<number | null>(null)
  const [thresholdInput, setThresholdInput] = useState<string>('')

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
        setThresholdInput(prev => (prev ? prev : String(numeric)))
        return
      }
    }
    if (!settingsQuery.isLoading && thresholdValue === null) {
      setThresholdValue(FALLBACK_THRESHOLD)
      setThresholdInput(String(FALLBACK_THRESHOLD))
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

  const settingsMutation = useMutation({
    mutationFn: (value: number) => updateInventorySettings({ lowStockThreshold: value }),
    onSuccess: data => {
      const numeric = Number(data.lowStockThreshold ?? 0)
      if (Number.isFinite(numeric) && numeric > 0) {
        setThresholdValue(numeric)
        setThresholdInput(String(numeric))
      }
      queryClient.invalidateQueries({ queryKey: ['inventory', 'summary'] })
      queryClient.invalidateQueries({ queryKey: ['inventory', 'alerts'] })
      queryClient.setQueryData(['inventory', 'settings'], data)
    },
  })

  const handleSaveThreshold = () => {
    const value = Number(thresholdInput)
    if (!Number.isFinite(value) || value <= 0) {
      window.alert('Ingresa un umbral mayor a cero')
      return
    }
    settingsMutation.mutate(value)
  }

  const handleAdjustmentApplied = () => {
    queryClient.invalidateQueries({ queryKey: ['inventory', 'summary'] })
    queryClient.invalidateQueries({ queryKey: ['inventory', 'alerts'] })
    queryClient.invalidateQueries({ queryKey: ['products'], exact: false })
    setAdjustDialogOpen(false)
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
    summary?.lowStockThreshold ?? thresholdValue ?? Number(thresholdInput || 0)

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
          <LocationsCard />
        </div>
        <div className="card">
          <ServicesCard />
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

      <section className="responsive-grid">
        <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5 table-card">
          <h3 className="text-neutral-100">Lotes con stock crítico</h3>
          <div className="inline-actions" style={{ marginBottom: '0.75rem' }}>
            <label className="muted small text-neutral-400" htmlFor="low-stock-threshold">
              Umbral
            </label>
            <input
              id="low-stock-threshold"
              className="input bg-neutral-800 border-neutral-700 text-neutral-100"
              type="number"
              step="0.1"
              min="0.1"
              value={thresholdInput}
              onChange={e => setThresholdInput(e.target.value)}
              disabled={settingsMutation.isPending}
            />
            <button
              className="btn"
              type="button"
              onClick={handleSaveThreshold}
              disabled={settingsMutation.isPending}
            >
              {settingsMutation.isPending ? 'Guardando...' : 'Guardar'}
            </button>
          </div>
          {settingsMutation.isError && (
            <p className="error">
              {(settingsMutation.error as Error)?.message ?? 'No se pudo actualizar el umbral'}
            </p>
          )}
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th>Estado</th>
                  <th>Producto</th>
                  <th>Lote</th>
                  <th>Disponible</th>
                  <th>Expira</th>
                  <th>Creado</th>
                  <th>Acción</th>
                </tr>
              </thead>
              <tbody>
                {alertsQuery.isLoading && (
                  <tr>
                    <td colSpan={7} className="muted text-neutral-400">
                      Cargando alertas...
                    </td>
                  </tr>
                )}
                {alertsQuery.isError && (
                  <tr>
                    <td colSpan={7} className="error text-red-400">
                      {alertsQuery.error?.message ?? 'No se pudieron obtener alertas'}
                    </td>
                  </tr>
                )}
                {!alertsQuery.isLoading &&
                  !alertsQuery.isError &&
                  (alertsQuery.data ?? []).map(alert => {
                    const product = productsIndex.get(alert.productId)
                    const qtyAvailable = Number(alert.qtyAvailable)
                    const threshold = thresholdValue ?? 10

                    // Determinar nivel de urgencia
                    const isCritical = qtyAvailable < 5 || qtyAvailable < threshold * 0.1
                    const isLow = qtyAvailable >= 5 && qtyAvailable <= threshold

                    // Verificar si está próximo a expirar (30 días)
                    const expDate = alert.expDate ? new Date(alert.expDate) : null
                    const today = new Date()
                    const daysToExpiry = expDate
                      ? Math.floor((expDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
                      : null
                    const isExpiringSoon =
                      daysToExpiry !== null && daysToExpiry <= 30 && daysToExpiry >= 0
                    const isExpired = daysToExpiry !== null && daysToExpiry < 0

                    const statusConfig = isCritical
                      ? {
                          icon: '🔴',
                          label: 'Crítico',
                          className: 'bg-red-950 text-red-400 border-red-800',
                        }
                      : isLow
                        ? {
                            icon: '🟡',
                            label: 'Bajo',
                            className: 'bg-yellow-950 text-yellow-400 border-yellow-800',
                          }
                        : {
                            icon: '🟢',
                            label: 'Normal',
                            className: 'bg-green-950 text-green-400 border-green-800',
                          }

                    return (
                      <tr
                        key={alert.lotId}
                        className={
                          isExpired ? 'bg-red-950/20' : isExpiringSoon ? 'bg-yellow-950/20' : ''
                        }
                      >
                        <td>
                          <span
                            className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium border ${statusConfig.className}`}
                          >
                            <span>{statusConfig.icon}</span>
                            <span>{statusConfig.label}</span>
                          </span>
                        </td>
                        <td className="text-neutral-100">{product?.name ?? alert.productId}</td>
                        <td className="mono text-neutral-300">{alert.lotId}</td>
                        <td
                          className={`mono font-semibold ${isCritical ? 'text-red-400' : isLow ? 'text-yellow-400' : 'text-neutral-100'}`}
                        >
                          {qtyAvailable.toFixed(2)}
                        </td>
                        <td
                          className={`mono ${isExpired ? 'text-red-400 font-semibold' : isExpiringSoon ? 'text-yellow-400 font-semibold' : 'text-neutral-300'}`}
                        >
                          {expDate ? expDate.toLocaleDateString() : '-'}
                          {isExpired && <span className="ml-2">❌ Vencido</span>}
                          {isExpiringSoon && !isExpired && (
                            <span className="ml-2">⚠️ {daysToExpiry}d</span>
                          )}
                        </td>
                        <td className="mono small text-neutral-400">
                          {new Date(alert.createdAt).toLocaleDateString()}
                        </td>
                        <td>
                          <button
                            className="btn ghost text-xs"
                            type="button"
                            onClick={() => {
                              setAdjustDialogOpen(true)
                            }}
                            title="Reabastecimiento rápido"
                          >
                            + Stock
                          </button>
                        </td>
                      </tr>
                    )
                  })}
                {!alertsQuery.isLoading &&
                  !alertsQuery.isError &&
                  (alertsQuery.data ?? []).length === 0 && (
                    <tr>
                      <td colSpan={7} className="muted text-neutral-400 text-center py-8">
                        ✅ Sin alertas de stock crítico
                      </td>
                    </tr>
                  )}
              </tbody>
            </table>
          </div>
        </div>
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

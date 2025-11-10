import { useState, useMemo, FormEvent } from 'react'
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query'
import Modal from './Modal'
import LotTransferDialog from './LotTransferDialog'
import ProductInventoryAlertModal from './ProductInventoryAlertModal'
import {
  listProductPrices,
  fetchProductStock,
  createProductPrice,
  Product,
  ProductStock,
  ProductStockLot,
  PriceHistoryEntry,
  PriceChangePayload,
} from '../../services/client'

type Props = {
  open: boolean
  product: Product | null
  onClose: () => void
}

type TabType = 'details' | 'prices' | 'inventory'

export default function ProductDetailModal({ open, product, onClose }: Props) {
  const [activeTab, setActiveTab] = useState<TabType>('details')
  const [transferContext, setTransferContext] = useState<{
    product: Product
    lot: ProductStockLot
  } | null>(null)
  const [criticalModalOpen, setCriticalModalOpen] = useState(false)
  const [priceForm, setPriceForm] = useState<PriceChangePayload>({ price: 0 })
  const queryClient = useQueryClient()

  const pricesQuery = useQuery({
    queryKey: ['product-prices', product?.id],
    queryFn: () => {
      if (!product) {
        throw new Error('Producto no seleccionado')
      }
      return listProductPrices(product.id, { size: 10 })
    },
    enabled: !!product && open,
  })

  const stockQuery = useQuery<ProductStock>({
    queryKey: ['product', product?.id, 'stock'],
    queryFn: () => {
      if (!product) {
        throw new Error('Producto no seleccionado')
      }
      return fetchProductStock(product.id)
    },
    enabled: !!product && open,
  })

  const prices: PriceHistoryEntry[] = pricesQuery.data?.content ?? []

  const priceMutation = useMutation({
    mutationFn: async () => {
      if (!product) throw new Error('Producto no seleccionado')
      if (!priceForm.price || priceForm.price <= 0) {
        throw new Error('Ingresa un precio válido')
      }
      return createProductPrice(product.id, priceForm)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'], exact: false })
      queryClient.invalidateQueries({ queryKey: ['product-prices', product?.id] })
      setPriceForm({ price: 0 })
      setActiveTab('prices')
    },
  })

  const handleSubmitPrice = (event: FormEvent) => {
    event.preventDefault()
    if (priceMutation.isPending) return
    priceMutation.mutate()
  }

  const formatPriceLabel = (value?: string | number | null) => {
    if (typeof value === 'number') {
      return Number.isFinite(value) && value > 0 ? `$${value.toFixed(2)}` : 'Sin precio'
    }
    if (typeof value === 'string' && value.trim().length > 0) {
      const parsed = Number(value)
      return Number.isFinite(parsed) && parsed > 0 ? `$${parsed.toFixed(2)}` : 'Sin precio'
    }
    return 'Sin precio'
  }

  const currentPriceLabel = useMemo(() => {
    if (!product) return ''
    return formatPriceLabel(product.currentPrice)
  }, [product])

  const criticalStock = useMemo(() => {
    if (!product?.criticalStock) return 0
    const value = Number(product.criticalStock)
    return Number.isFinite(value) ? value : 0
  }, [product])

  if (!product) return null

  return (
    <>
      <Modal
        open={open}
        onClose={onClose}
        title={product.name}
        className="modal-wide"
      >
        <div style={{ width: '100%', maxWidth: '1000px' }}>
          {/* Tabs */}
          <div className="tabs-container">
            <button
              className={`tab-button ${activeTab === 'details' ? 'active' : ''}`}
              onClick={() => setActiveTab('details')}
              type="button"
            >
              📋 Detalles
            </button>
            <button
              className={`tab-button ${activeTab === 'prices' ? 'active' : ''}`}
              onClick={() => setActiveTab('prices')}
              type="button"
            >
              💰 Precios
            </button>
            <button
              className={`tab-button ${activeTab === 'inventory' ? 'active' : ''}`}
              onClick={() => setActiveTab('inventory')}
              type="button"
            >
              📦 Inventario
            </button>
          </div>

          {/* Tab: Detalles */}
          {activeTab === 'details' && (
            <div className="tab-content">
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                  gap: '1rem',
                  padding: '1.5rem',
                  background: 'var(--bg-secondary)',
                  borderRadius: '12px',
                  border: '1px solid var(--border-color)',
                }}
              >
                <div>
                  <p className="muted small">SKU</p>
                  <p style={{ fontWeight: 600, fontSize: '1.05rem' }}>{product.sku}</p>
                </div>
                <div>
                  <p className="muted small">Precio actual</p>
                  <p style={{ fontWeight: 600, fontSize: '1.05rem', color: 'var(--accent)' }}>
                    {currentPriceLabel}
                  </p>
                </div>
                <div>
                  <p className="muted small">Categoría</p>
                  <p style={{ fontWeight: 600, fontSize: '1.05rem' }}>{product.category || '—'}</p>
                </div>
                <div>
                  <p className="muted small">Estado</p>
                  <p style={{ fontWeight: 600, fontSize: '1.05rem' }}>
                    {product.active ? (
                      <span style={{ color: 'var(--success-color)' }}>✓ Activo</span>
                    ) : (
                      <span style={{ color: 'var(--error-color)' }}>✗ Inactivo</span>
                    )}
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* Tab: Precios */}
          {activeTab === 'prices' && (
            <div className="tab-content">
              {/* Formulario de actualización de precio */}
              <form
                onSubmit={handleSubmitPrice}
                style={{
                  padding: '1.5rem',
                  background: 'var(--bg-secondary)',
                  borderRadius: '12px',
                  border: '1px solid var(--border-color)',
                  marginBottom: '1.5rem',
                }}
              >
                <h4 style={{ marginBottom: '1rem', fontSize: '1rem', fontWeight: 600 }}>
                  Actualizar precio
                </h4>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr auto', gap: '1rem' }}>
                  <label>
                    <span className="muted small">Nuevo precio</span>
                    <input
                      className="input"
                      type="number"
                      min="0"
                      step="0.01"
                      value={priceForm.price || ''}
                      onChange={e => setPriceForm({ price: Number(e.target.value) })}
                      placeholder="Ingresa el nuevo precio"
                      disabled={priceMutation.isPending}
                      required
                    />
                  </label>
                  <button
                    className="btn primary"
                    type="submit"
                    disabled={priceMutation.isPending}
                    style={{ alignSelf: 'end' }}
                  >
                    {priceMutation.isPending ? 'Guardando...' : 'Actualizar precio'}
                  </button>
                </div>
                {priceMutation.isError && (
                  <p className="error" style={{ marginTop: '0.75rem' }}>
                    {(priceMutation.error as Error)?.message ?? 'No se pudo actualizar el precio'}
                  </p>
                )}
              </form>

              {/* Histórico de precios */}
              <h4 style={{ marginBottom: '1rem', fontSize: '1rem', fontWeight: 600 }}>
                Histórico de precios
              </h4>
              {pricesQuery.isLoading && <p className="muted">Cargando precios...</p>}
              {pricesQuery.isError && (
                <p className="error">
                  {(pricesQuery.error as Error)?.message ?? 'No se pudieron cargar los precios'}
                </p>
              )}
              {!pricesQuery.isLoading && prices.length === 0 && (
                <p className="muted">No hay histórico de precios</p>
              )}
              {!pricesQuery.isLoading && prices.length > 0 && (
                <div className="table-wrapper">
                  <table className="table compact">
                    <thead>
                      <tr>
                        <th>Precio</th>
                        <th>Desde</th>
                      </tr>
                    </thead>
                    <tbody>
                      {prices.map((entry, index) => (
                        <tr key={index}>
                          <td className="mono" style={{ fontWeight: 600 }}>
                            ${Number(entry.price).toLocaleString()}
                          </td>
                          <td className="mono small">
                            {new Date(entry.validFrom).toLocaleString('es-CL', {
                              year: 'numeric',
                              month: '2-digit',
                              day: '2-digit',
                              hour: '2-digit',
                              minute: '2-digit',
                            })}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {/* Tab: Inventario */}
          {activeTab === 'inventory' && (
            <div className="tab-content">
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  marginBottom: '1rem',
                }}
              >
                <h4 style={{ fontSize: '1.1rem', fontWeight: 600, margin: 0 }}>Inventario</h4>
                <button
                  className="btn ghost small"
                  type="button"
                  onClick={() => setCriticalModalOpen(true)}
                >
                  Definir stock crítico
                </button>
              </div>

              <div style={{ marginBottom: '1rem' }}>
                <p className="muted">
                  Stock total:{' '}
                  {stockQuery.isLoading
                    ? 'Cargando...'
                    : stockQuery.data
                      ? stockQuery.data.total
                      : 'Sin datos'}
                </p>
                <p className="muted">Stock crítico configurado: {criticalStock}</p>
              </div>

              {stockQuery.isError && (
                <p className="error">
                  {(stockQuery.error as Error)?.message ?? 'No se pudo obtener el stock'}
                </p>
              )}

              {stockQuery.isLoading && <p className="muted">Cargando lotes...</p>}

              {stockQuery.data && stockQuery.data.lots.length > 0 && (
                <div className="table-wrapper compact">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Lote</th>
                        <th>Batch</th>
                        <th>Cantidad</th>
                        <th>Costo Unit.</th>
                        <th>Factura Origen</th>
                        <th>Ubicación</th>
                        <th>Mfg.</th>
                        <th>Vence</th>
                        <th>Acciones</th>
                      </tr>
                    </thead>
                    <tbody>
                      {stockQuery.data.lots.map((lot: any) => (
                        <tr key={lot.lotId}>
                          <td className="mono small">{lot.lotId.substring(0, 8)}</td>
                          <td className="mono small">{lot.batchName || '—'}</td>
                          <td className="mono">{lot.quantity}</td>
                          <td className="mono small">
                            {lot.costUnit ? `$${lot.costUnit.toLocaleString()}` : '—'}
                          </td>
                          <td className="mono small">
                            {lot.purchaseDocNumber ? (
                              <span style={{ 
                                background: 'var(--accent-soft)', 
                                padding: '0.25rem 0.5rem',
                                borderRadius: '4px',
                                fontSize: '0.85rem'
                              }}>
                                📄 {lot.purchaseDocNumber}
                              </span>
                            ) : (
                              <span style={{ color: 'var(--muted)' }}>Sin factura</span>
                            )}
                          </td>
                          <td>
                            {lot.locationCode ? (
                              <span title={lot.locationName || ''}>{lot.locationCode}</span>
                            ) : (
                              '—'
                            )}
                          </td>
                          <td className="mono small">
                            {lot.manufactureDate
                              ? new Date(lot.manufactureDate).toLocaleDateString('es-CL')
                              : '—'}
                          </td>
                          <td className="mono small">
                            {lot.expiryDate
                              ? new Date(lot.expiryDate).toLocaleDateString('es-CL')
                              : '—'}
                          </td>
                          <td>
                            <button
                              className="btn small primary"
                              onClick={() => setTransferContext({ product, lot })}
                              type="button"
                            >
                              Transferir
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}

              {stockQuery.data && stockQuery.data.lots.length === 0 && (
                <p className="muted" style={{ textAlign: 'center', padding: '2rem' }}>
                  No hay lotes disponibles
                </p>
              )}
            </div>
          )}
        </div>
      </Modal>

      {/* Dialogs */}
      <LotTransferDialog
        open={!!transferContext}
        lot={transferContext?.lot ?? null}
        productName={transferContext?.product.name ?? ''}
        onClose={() => setTransferContext(null)}
        onTransferred={() => {
          if (transferContext?.product.id) {
            queryClient.invalidateQueries({
              queryKey: ['product', transferContext.product.id, 'stock'],
            })
          }
          queryClient.invalidateQueries({ queryKey: ['inventoryMovements'], exact: false })
        }}
      />

      <ProductInventoryAlertModal
        open={criticalModalOpen}
        product={product}
        onClose={() => setCriticalModalOpen(false)}
        onSubmit={async (value: number) => {
          console.log('Update critical stock to:', value)
          setCriticalModalOpen(false)
        }}
      />
    </>
  )
}

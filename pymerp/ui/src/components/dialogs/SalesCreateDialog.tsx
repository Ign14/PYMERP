import {
  ChangeEvent,
  FormEvent,
  KeyboardEvent,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import {
  createSale,
  listProducts,
  listLocations,
  Page,
  Product,
  Customer,
  Location,
  SaleItemPayload,
  SalePayload,
  SaleRes,
  lookupProduct,
  ProductLookupType,
} from '../../services/client'
import Modal from './Modal'
import { FrequentProductsPanel } from '../sales/FrequentProductsPanel'
import type { FrequentProduct } from '../../services/client'
import { SALE_DOCUMENT_TYPES, SALE_PAYMENT_METHODS } from '../../constants/sales'
import CustomerSelect from '../CustomerSelect'

interface Props {
  open: boolean
  onClose: () => void
  onCreated: (sale: SaleRes) => void
}

type DocumentOption = (typeof SALE_DOCUMENT_TYPES)[number]
type PaymentOption = (typeof SALE_PAYMENT_METHODS)[number]
type ManualLookupType = 'auto' | ProductLookupType
type DiscountType = 'amount' | 'percentage'

const DEFAULT_DOC_TYPE: DocumentOption['value'] = SALE_DOCUMENT_TYPES[0].value
const DEFAULT_PAYMENT_METHOD: PaymentOption['value'] = SALE_PAYMENT_METHODS[2].value
const LOOKUP_ORDER: ProductLookupType[] = ['barcode', 'sku', 'qr']
const SCANNER_TIMEOUT_MS = 100
const SCANNER_PREFIX: string = ''
const SCANNER_SUFFIX: string = ''

const MANUAL_TYPE_OPTIONS: { value: ManualLookupType; label: string }[] = [
  { value: 'auto', label: 'Detección automática' },
  { value: 'barcode', label: 'Código de barras' },
  { value: 'sku', label: 'SKU' },
  { value: 'qr', label: 'QR' },
]

export default function SalesCreateDialog({ open, onClose, onCreated }: Props) {
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null)
  const [customerError, setCustomerError] = useState<string | null>(null)
  const [docType, setDocType] = useState<DocumentOption['value']>(DEFAULT_DOC_TYPE)
  const [paymentMethod, setPaymentMethod] = useState<PaymentOption['value']>(DEFAULT_PAYMENT_METHOD)
  const [items, setItems] = useState<SaleItemPayload[]>([])
  const [selectedProductId, setSelectedProductId] = useState<string>('')
  const [qty, setQty] = useState(1)
  const [unitPrice, setUnitPrice] = useState<number>(0)
  const [discountType, setDiscountType] = useState<DiscountType>('amount')
  const [discountValue, setDiscountValue] = useState(0)
  const [applyVat, setApplyVat] = useState(true)
  const [productCache, setProductCache] = useState<Record<string, Product>>({})
  const [showFrequentProducts, setShowFrequentProducts] = useState(false)
  const scannerInputRef = useRef<HTMLInputElement | null>(null)
  const scannerBufferRef = useRef('')
  const scannerTimerRef = useRef<number | null>(null)
  const [scannerValue, setScannerValue] = useState('')
  const [manualCode, setManualCode] = useState('')
  const [manualType, setManualType] = useState<ManualLookupType>('auto')
  const [lookupPending, setLookupPending] = useState(false)
  const [lookupFeedback, setLookupFeedback] = useState<{
    status: 'idle' | 'loading' | 'success' | 'error'
    message?: string
  }>({ status: 'idle' })

  const productsQuery = useQuery<Page<Product>, Error>({
    queryKey: ['products', { dialog: 'sales' }],
    queryFn: () => listProducts({ size: 200 }),
    enabled: open,
    staleTime: 30_000,
  })

  const locationsQuery = useQuery<Location[], Error>({
    queryKey: ['locations', { dialog: 'sales' }],
    queryFn: () => listLocations(),
    enabled: open,
  })

  const createMutation = useMutation({
    mutationFn: (payload: SalePayload) => createSale(payload),
    onSuccess: sale => {
      onCreated(sale)
      resetForm()
      onClose()
    },
  })

  const productOptions = productsQuery.data?.content ?? []
  const customerId = selectedCustomer?.id ?? ''

  useEffect(() => {
    if (!productOptions.length) return
    setProductCache(prev => {
      let changed = false
      const next = { ...prev }
      for (const product of productOptions) {
        if (!next[product.id]) {
          next[product.id] = product
          changed = true
        }
      }
      return changed ? next : prev
    })
  }, [productOptions])

  useEffect(() => {
    if (!selectedCustomer?.id) {
      setShowFrequentProducts(false)
    }
  }, [selectedCustomer])

  const handleCustomerSelect = useCallback((customer: Customer) => {
    setSelectedCustomer(customer)
    setCustomerError(null)
  }, [])

  const handleCustomerClear = useCallback(() => {
    setSelectedCustomer(null)
    setCustomerError(null)
    setShowFrequentProducts(false)
  }, [])

  const addProductToSale = useCallback((product: Product, quantity = 1, priceOverride?: number) => {
    if (!product) return
    if (quantity <= 0 || Number.isNaN(quantity)) return

    const resolvedPrice =
      priceOverride !== undefined && !Number.isNaN(priceOverride)
        ? priceOverride
        : Number(product.currentPrice ?? 0) || 0

    setProductCache(prev => {
      if (prev[product.id]) {
        return prev
      }
      return { ...prev, [product.id]: product }
    })

    setItems(prev => {
      const index = prev.findIndex(item => item.productId === product.id)
      if (index !== -1) {
        const existing = prev[index]
        const next = [...prev]
        next[index] = {
          ...existing,
          qty: existing.qty + quantity,
          unitPrice:
            priceOverride !== undefined ? priceOverride : existing.unitPrice || resolvedPrice,
        }
        return next
      }

      return [...prev, { productId: product.id, qty: quantity, unitPrice: resolvedPrice }]
    })
  }, [])

  const handleLookup = useCallback(
    async (
      rawCode: string,
      options: { hint?: ProductLookupType; showEmptyError?: boolean } = {}
    ): Promise<Product | null> => {
      const code = rawCode.trim()
      if (!code) {
        if (options.showEmptyError) {
          setLookupFeedback({ status: 'error', message: 'Ingresa un código válido.' })
        }
        return null
      }

      setLookupPending(true)
      setLookupFeedback({ status: 'loading', message: 'Buscando producto...' })

      const order = options.hint
        ? [options.hint, ...LOOKUP_ORDER.filter(type => type !== options.hint)]
        : LOOKUP_ORDER
      const checked = new Set<ProductLookupType>()

      try {
        for (const type of order) {
          if (checked.has(type)) continue
          checked.add(type)
          const product = await lookupProduct({ query: code, type })
          if (product) {
            addProductToSale(product, 1)
            setLookupFeedback({
              status: 'success',
              message: `Producto "${product.name}" agregado a la venta.`,
            })
            return product
          }
        }

        setLookupFeedback({
          status: 'error',
          message: 'Producto no encontrado para el código ingresado.',
        })
        return null
      } catch (error) {
        const message = error instanceof Error ? error.message : 'No se pudo buscar el producto.'
        setLookupFeedback({ status: 'error', message })
        return null
      } finally {
        setLookupPending(false)
      }
    },
    [addProductToSale]
  )

  const finalizeScannerBuffer = useCallback(() => {
    if (scannerTimerRef.current) {
      window.clearTimeout(scannerTimerRef.current)
      scannerTimerRef.current = null
    }

    const rawValue = scannerBufferRef.current.trim()
    if (!rawValue) {
      setScannerValue('')
      scannerBufferRef.current = ''
      return
    }

    let processed = rawValue
    if (SCANNER_PREFIX && processed.startsWith(SCANNER_PREFIX)) {
      processed = processed.slice(SCANNER_PREFIX.length)
    }
    if (SCANNER_SUFFIX && processed.endsWith(SCANNER_SUFFIX)) {
      processed = processed.slice(0, -SCANNER_SUFFIX.length)
    }

    setScannerValue('')
    scannerBufferRef.current = ''
    void handleLookup(processed, { showEmptyError: false })
  }, [handleLookup])

  const handleScannerChange = (event: ChangeEvent<HTMLInputElement>) => {
    const value = event.target.value
    setScannerValue(value)
    scannerBufferRef.current = value

    if (scannerTimerRef.current) {
      window.clearTimeout(scannerTimerRef.current)
    }

    if (value) {
      scannerTimerRef.current = window.setTimeout(finalizeScannerBuffer, SCANNER_TIMEOUT_MS)
    }
  }

  const handleScannerKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Enter') {
      event.preventDefault()
      finalizeScannerBuffer()
    }
  }

  const handleManualSubmit = async () => {
    if (lookupPending) return
    const hint = manualType === 'auto' ? undefined : manualType
    const product = await handleLookup(manualCode, { hint, showEmptyError: true })
    if (product) {
      setManualCode('')
      setManualType('auto')
    }
  }

  const totals = useMemo(() => {
    const subtotal = items.reduce(
      (acc, item) => acc + item.qty * item.unitPrice - (item.discount ?? 0),
      0
    )

    // Aplicar descuento general
    let discountAmount = 0
    if (discountType === 'percentage') {
      discountAmount = subtotal * (discountValue / 100)
    } else {
      discountAmount = discountValue
    }

    const net = subtotal - discountAmount
    const vat = applyVat ? net * 0.19 : 0 // 19% IVA
    const total = net + vat

    return { subtotal, discountAmount, net, vat, total }
  }, [items, applyVat, discountType, discountValue])

  const addItem = () => {
    if (!selectedProductId) {
      setLookupFeedback({ status: 'error', message: 'Selecciona un producto para agregarlo.' })
      return
    }
    const product = productOptions.find(p => p.id === selectedProductId)
    if (!product) {
      setLookupFeedback({ status: 'error', message: 'Producto seleccionado no disponible.' })
      return
    }
    if (qty <= 0 || Number.isNaN(qty)) {
      setLookupFeedback({ status: 'error', message: 'La cantidad debe ser mayor a cero.' })
      return
    }
    if (unitPrice <= 0 || Number.isNaN(unitPrice)) {
      setLookupFeedback({ status: 'error', message: 'El precio unitario debe ser mayor a cero.' })
      return
    }

    addProductToSale(product, qty, unitPrice)
    setLookupFeedback({
      status: 'success',
      message: `Se agregó ${qty}× "${product.name}" al carrito.`,
    })
    setSelectedProductId('')
    setQty(1)
    setUnitPrice(0)
  }

  const removeItem = (index: number) => {
    setItems(prev => prev.filter((_, idx) => idx !== index))
  }

  const resetForm = () => {
    setSelectedCustomer(null)
    setCustomerError(null)
    setDocType(DEFAULT_DOC_TYPE)
    setPaymentMethod(DEFAULT_PAYMENT_METHOD)
    setItems([])
    setSelectedProductId('')
    setQty(1)
    setUnitPrice(0)
    setManualCode('')
    setManualType('auto')
    setLookupFeedback({ status: 'idle' })
    setLookupPending(false)
    setScannerValue('')
    scannerBufferRef.current = ''
    if (scannerTimerRef.current) {
      window.clearTimeout(scannerTimerRef.current)
      scannerTimerRef.current = null
    }
    setShowFrequentProducts(false)
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    if (!selectedCustomer?.id) {
      setCustomerError('Selecciona un cliente')
      return
    }
    if (items.length === 0) {
      return
    }
    const payload: SalePayload = {
      customerId: selectedCustomer.id,
      docType,
      paymentMethod,
      discount:
        discountValue > 0
          ? discountType === 'percentage'
            ? totals.discountAmount
            : discountValue
          : undefined,
      vatRate: applyVat ? 19 : undefined,
      items,
    }
    createMutation.mutate(payload)
  }

  useEffect(() => {
    if (!open) {
      setLookupFeedback({ status: 'idle' })
      setLookupPending(false)
      setScannerValue('')
      scannerBufferRef.current = ''
      if (scannerTimerRef.current) {
        window.clearTimeout(scannerTimerRef.current)
        scannerTimerRef.current = null
      }
      setShowFrequentProducts(false)
      return
    }

    requestAnimationFrame(() => {
      if (scannerInputRef.current) {
        scannerInputRef.current.focus()
        scannerInputRef.current.select()
      }
    })
  }, [open])

  useEffect(() => {
    return () => {
      if (scannerTimerRef.current) {
        window.clearTimeout(scannerTimerRef.current)
        scannerTimerRef.current = null
      }
    }
  }, [])

  const handleFrequentProductPick = useCallback(
    (product: FrequentProduct, meta?: { lastQty?: number }) => {
      const cachedProduct =
        productCache[product.productId] ?? productOptions.find(p => p.id === product.productId)
      if (!cachedProduct) {
        setLookupFeedback({
          status: 'error',
          message: 'El producto ya no está disponible en el catálogo.',
        })
        return
      }

      const lastQty = meta?.lastQty
      const fallbackQty = product.avgQty ? Math.round(product.avgQty) : 1
      const quantityCandidate = lastQty !== undefined && lastQty > 0 ? lastQty : fallbackQty
      const quantity = quantityCandidate > 0 ? quantityCandidate : 1
      const priceOverride =
        cachedProduct.currentPrice !== undefined && cachedProduct.currentPrice !== null
          ? undefined
          : product.lastUnitPrice

      addProductToSale(cachedProduct, quantity, priceOverride)
      setLookupFeedback({
        status: 'success',
        message: `Se agregó ${quantity}× "${cachedProduct.name}" desde frecuentes.`,
      })
    },
    [addProductToSale, productCache, productOptions]
  )

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Registrar venta"
      initialFocusRef={scannerInputRef}
      className="modal--wide sales-create-modal"
    >
      <form className="form-grid" onSubmit={handleSubmit}>
        <CustomerSelect
          value={selectedCustomer}
          onSelect={handleCustomerSelect}
          onClear={handleCustomerClear}
          label="Cliente"
          required
          error={customerError}
          onErrorDismiss={() => setCustomerError(null)}
          disabled={createMutation.isPending}
        />

        <label>
          <span>Documento</span>
          <select
            className="input"
            value={docType}
            onChange={e => setDocType(e.target.value as DocumentOption['value'])}
          >
            {SALE_DOCUMENT_TYPES.map(option => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <label>
          <span>Método de pago</span>
          <select
            className="input"
            value={paymentMethod}
            onChange={e => setPaymentMethod(e.target.value as PaymentOption['value'])}
          >
            {SALE_PAYMENT_METHODS.map(option => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <div className="line"></div>

        <div className="section-title">Descuentos e Impuestos</div>

        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
            gap: '1rem',
          }}
        >
          <label>
            <span>Tipo de descuento</span>
            <select
              className="input"
              value={discountType}
              onChange={e => setDiscountType(e.target.value as DiscountType)}
            >
              <option value="amount">Monto fijo ($)</option>
              <option value="percentage">Porcentaje (%)</option>
            </select>
          </label>

          <label>
            <span>Valor descuento</span>
            <input
              className="input"
              type="number"
              min={0}
              step={discountType === 'percentage' ? 1 : 0.01}
              value={discountValue}
              onChange={e => setDiscountValue(Number(e.target.value))}
              placeholder={discountType === 'percentage' ? '0-100' : '0.00'}
            />
          </label>

          <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <input
              type="checkbox"
              checked={applyVat}
              onChange={e => setApplyVat(e.target.checked)}
            />
            <span>Aplicar IVA 19%</span>
          </label>
        </div>

        <div className="line"></div>

        <div className="lookup-section">
          <div className="scan-input-wrapper">
            <label>
              <span>Escáner USB</span>
              <input
                ref={scannerInputRef}
                className="input scan-input"
                type="text"
                inputMode="text"
                autoComplete="off"
                value={scannerValue}
                onChange={handleScannerChange}
                onKeyDown={handleScannerKeyDown}
                placeholder="Enfoca y escanea el código"
                aria-describedby="scan-hint"
              />
            </label>
            <p id="scan-hint" className="scan-hint">
              Mantén el foco en este campo y escanea código de barras, SKU o QR. El producto se
              agrega automáticamente cuando se detecta la lectura.
            </p>
          </div>

          <div className="manual-entry">
            <label>
              <span>Agregar por código</span>
              <div className="manual-entry__inputs">
                <input
                  className="input"
                  type="text"
                  value={manualCode}
                  onChange={(event: ChangeEvent<HTMLInputElement>) =>
                    setManualCode(event.target.value)
                  }
                  onKeyDown={(event: KeyboardEvent<HTMLInputElement>) => {
                    if (event.key === 'Enter') {
                      event.preventDefault()
                      void handleManualSubmit()
                    }
                  }}
                  placeholder="Ingresa código, SKU o QR"
                />
                <select
                  className="input manual-entry__type"
                  value={manualType}
                  onChange={event => setManualType(event.target.value as ManualLookupType)}
                  aria-label="Tipo de código"
                >
                  {MANUAL_TYPE_OPTIONS.map(option => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  className="btn"
                  onClick={() => void handleManualSubmit()}
                  disabled={lookupPending || manualCode.trim() === ''}
                >
                  {lookupPending ? 'Buscando...' : 'Agregar'}
                </button>
              </div>
            </label>
          </div>
        </div>

        {lookupFeedback.status !== 'idle' && (
          <div
            className={`status-message lookup-feedback ${
              lookupFeedback.status === 'success'
                ? 'success'
                : lookupFeedback.status === 'error'
                  ? 'error'
                  : 'loading'
            }`}
            role="status"
            aria-live="polite"
          >
            {lookupFeedback.message ?? ''}
          </div>
        )}

        <section className="frequent-products-wrapper">
          <label className="frequent-products-toggle">
            <input
              type="checkbox"
              checked={showFrequentProducts}
              onChange={event => setShowFrequentProducts(event.target.checked)}
              disabled={!customerId}
            />
            <span>Productos frecuentes del cliente</span>
          </label>

          <FrequentProductsPanel
            customerId={customerId || undefined}
            visible={showFrequentProducts && Boolean(customerId)}
            onPick={handleFrequentProductPick}
          />
        </section>

        <div className="line"></div>

        <div className="item-builder">
          <select
            className="input"
            value={selectedProductId}
            onChange={e => {
              const id = e.target.value
              setSelectedProductId(id)
              const product = productOptions.find(p => p.id === id)
              if (product?.currentPrice !== undefined && product.currentPrice !== null) {
                setUnitPrice(Number(product.currentPrice))
              } else {
                setUnitPrice(0)
              }
            }}
          >
            <option value="">Selecciona producto</option>
            {productOptions.map(product => (
              <option key={product.id} value={product.id}>
                {product.name}
              </option>
            ))}
          </select>
          <input
            className="input"
            type="number"
            min={1}
            value={qty}
            onChange={e => setQty(Number(e.target.value))}
          />
          <input
            className="input"
            type="number"
            step="0.01"
            min={0}
            value={unitPrice}
            onChange={e => setUnitPrice(Number(e.target.value))}
          />
          <button type="button" className="btn" onClick={addItem}>
            Agregar
          </button>
        </div>

        {items.length > 0 && (
          <div className="table-wrapper compact">
            <table className="table nowrap">
              <thead>
                <tr>
                  <th>Producto</th>
                  <th>Cantidad</th>
                  <th>Precio</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {items.map((item, idx) => {
                  const product =
                    productCache[item.productId] ??
                    productOptions.find(p => p.id === item.productId)
                  const productName = product?.name ?? item.productId
                  const qtyLabel = `${item.qty}`
                  const priceLabel = `$${item.unitPrice.toFixed(2)}`
                  return (
                    <tr key={`${item.productId}-${idx}`}>
                      <td title={productName}>
                        <span className="cell-text">{productName}</span>
                      </td>
                      <td className="mono" title={qtyLabel}>
                        <span className="cell-text">{item.qty}</span>
                      </td>
                      <td className="mono" title={priceLabel}>
                        <span className="cell-text">{priceLabel}</span>
                      </td>
                      <td className="actions-cell">
                        <button type="button" className="btn ghost" onClick={() => removeItem(idx)}>
                          Quitar
                        </button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}

        <div
          className="total-section"
          style={{
            display: 'grid',
            gridTemplateColumns: '1fr auto',
            gap: '0.5rem',
            padding: '1rem',
            background: '#f9f9f9',
            borderRadius: '4px',
          }}
        >
          <span className="muted">Subtotal:</span>
          <strong>${totals.subtotal.toFixed(2)}</strong>

          {discountValue > 0 && (
            <>
              <span className="muted">
                Descuento ({discountType === 'percentage' ? `${discountValue}%` : '$'}):
              </span>
              <strong>-${totals.discountAmount.toFixed(2)}</strong>
            </>
          )}

          <span className="muted">Neto:</span>
          <strong>${totals.net.toFixed(2)}</strong>

          {applyVat && (
            <>
              <span className="muted">IVA (19%):</span>
              <strong>${totals.vat.toFixed(2)}</strong>
            </>
          )}

          <span style={{ fontSize: '1.1rem', fontWeight: 'bold' }}>Total:</span>
          <strong style={{ fontSize: '1.1rem' }}>${totals.total.toFixed(2)}</strong>
        </div>

        {createMutation.isError && (
          <p className="error">{(createMutation.error as Error).message}</p>
        )}

        <div className="buttons">
          <button className="btn" type="submit" disabled={createMutation.isPending}>
            {createMutation.isPending ? 'Guardando...' : 'Registrar'}
          </button>
          <button className="btn ghost" type="button" onClick={resetForm}>
            Limpiar
          </button>
        </div>
      </form>
    </Modal>
  )
}

import { FormEvent, useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import {
  createPurchase,
  listProducts,
  listSuppliers,
  getLocations,
  listServices,
  Page,
  Product,
  Supplier,
  Location,
  ServiceDTO,
  PurchaseItemPayload,
  PurchasePayload,
  createSupplier,
  SupplierPayload,
} from '../../services/client'
import LocationSelect, { ACTIVE_LOCATION_QUERY_KEY } from '../LocationSelect'

// Tipos de documentos de compra
const DOC_TYPES = [
  'Factura',
  'Boleta',
  'Cotización',
  'Orden de Compra',
  'Guía de Despacho',
] as const

type DiscountType = 'amount' | 'percentage'

export interface PurchaseCreateFormProps {
  initialSupplierId?: string
  initialProductId?: string
  initialLocationId?: string
  initialQty?: number
  onSubmitted?: (purchaseId: string) => void
  className?: string
}

const nowToInputValue = () => new Date().toISOString().slice(0, 16)

export default function PurchaseCreateForm({
  initialSupplierId,
  initialProductId,
  initialLocationId,
  initialQty,
  onSubmitted,
  className,
}: PurchaseCreateFormProps) {
  const normalizedSupplierId = useMemo(() => initialSupplierId ?? '', [initialSupplierId])
  const normalizedProductId = useMemo(() => initialProductId ?? '', [initialProductId])
  const normalizedLocationId = useMemo(() => initialLocationId ?? '', [initialLocationId])
  const normalizedQty = useMemo(
    () => Math.max(Math.round(initialQty ?? 1), 1),
    [initialQty]
  )

  const [supplierId, setSupplierId] = useState(normalizedSupplierId)
  const [showNewSupplier, setShowNewSupplier] = useState(false)
  const [newSupplierName, setNewSupplierName] = useState('')
  const [newSupplierRut, setNewSupplierRut] = useState('')
  const [docType, setDocType] = useState<string>('Factura')
  const [docNumber, setDocNumber] = useState('')
  const [paymentTermDays, setPaymentTermDays] = useState<number | null>(null)
  const [issuedAt, setIssuedAt] = useState(nowToInputValue)
  const [receivedAt, setReceivedAt] = useState(nowToInputValue)
  const [items, setItems] = useState<PurchaseItemPayload[]>([])
  const [itemType, setItemType] = useState<'product' | 'service'>('product')
  const [productId, setProductId] = useState(normalizedProductId)
  const [serviceId, setServiceId] = useState('')
  const [qty, setQty] = useState(normalizedQty)
  const [unitCost, setUnitCost] = useState(0)
  const [vatRate, setVatRate] = useState(19)
  const [expDate, setExpDate] = useState('')
  const [mfgDate, setMfgDate] = useState('')
  const [batchName, setBatchName] = useState('')
  const [locationId, setLocationId] = useState(normalizedLocationId)
  const [applyVat, setApplyVat] = useState(true)
  const [discountType, setDiscountType] = useState<DiscountType>('amount')
  const [discountValue, setDiscountValue] = useState(0)
  const [pdfFile, setPdfFile] = useState<File | null>(null)

  useEffect(() => {
    setSupplierId(normalizedSupplierId)
  }, [normalizedSupplierId])

  useEffect(() => {
    setProductId(normalizedProductId)
  }, [normalizedProductId])

  useEffect(() => {
    setLocationId(normalizedLocationId)
  }, [normalizedLocationId])

  useEffect(() => {
    setQty(normalizedQty)
  }, [normalizedQty])

  const productsQuery = useQuery<Page<Product>, Error>({
    queryKey: ['products', { dialog: 'purchases' }],
    queryFn: () => listProducts({ size: 200 }),
  })

  const servicesQuery = useQuery<ServiceDTO[], Error>({
    queryKey: ['services', { dialog: 'purchases' }],
    queryFn: () => listServices({ status: 'ACTIVE' }),
  })

  const suppliersQuery = useQuery<Supplier[], Error>({
    queryKey: ['suppliers', { dialog: 'purchases' }],
    queryFn: () => listSuppliers(),
  })

  const locationsQuery = useQuery<Location[], Error>({
    queryKey: ACTIVE_LOCATION_QUERY_KEY,
    queryFn: () => getLocations({ status: 'ACTIVE' }),
  })

  const supplierMutation = useMutation({
    mutationFn: (payload: SupplierPayload) => createSupplier(payload),
    onSuccess: newSupplier => {
      setSupplierId(newSupplier.id)
      setShowNewSupplier(false)
      setNewSupplierName('')
      setNewSupplierRut('')
      suppliersQuery.refetch()
    },
  })

  useEffect(() => {
    if (itemType === 'service' && serviceId) {
      const selected = servicesQuery.data?.find(service => service.id === serviceId)
      if (selected?.unitPrice) {
        setUnitCost(selected.unitPrice)
      }
    }
  }, [itemType, serviceId, servicesQuery.data])

  const resetForm = () => {
    setSupplierId(normalizedSupplierId)
    setShowNewSupplier(false)
    setNewSupplierName('')
    setNewSupplierRut('')
    setDocType('Factura')
    setDocNumber('')
    setPaymentTermDays(null)
    const now = nowToInputValue()
    setIssuedAt(now)
    setReceivedAt(now)
    setItems([])
    setItemType('product')
    setProductId(normalizedProductId)
    setServiceId('')
    setQty(normalizedQty)
    setUnitCost(0)
    setExpDate('')
    setMfgDate('')
    setBatchName('')
    setLocationId(normalizedLocationId)
    setApplyVat(true)
    setDiscountType('amount')
    setDiscountValue(0)
    setPdfFile(null)
    setVatRate(19)
  }

  const mutation = useMutation({
    mutationFn: (data: { payload: PurchasePayload; file?: File }) =>
      createPurchase(data.payload, data.file),
    onSuccess: created => {
      resetForm()
      onSubmitted?.(created.id)
    },
  })

  const productOptions = productsQuery.data?.content ?? []
  const supplierOptions = suppliersQuery.data ?? []
  const locationOptions = locationsQuery.data ?? []

  const resolveLocationLabel = (id?: string) => {
    if (!id) {
      return 'DEFAULT (automático)'
    }
    const location = locationOptions.find(loc => loc.id === id)
    if (!location) {
      return 'Ubicación seleccionada'
    }
    return location.code ? `${location.code} - ${location.name}` : location.name
  }

  const totals = useMemo(() => {
    const subtotal = items.reduce((acc, item) => acc + item.qty * item.unitCost, 0)

    let discountAmount = 0
    if (discountType === 'percentage') {
      discountAmount = subtotal * (discountValue / 100)
    } else {
      discountAmount = discountValue
    }

    const net = subtotal - discountAmount
    const vat = applyVat ? net * 0.19 : 0
    const total = net + vat

    return { subtotal, discountAmount, net, vat, total }
  }, [items, applyVat, discountType, discountValue])

  const handleCreateSupplier = () => {
    if (!newSupplierName.trim() || !newSupplierRut.trim()) return
    supplierMutation.mutate({
      name: newSupplierName,
      rut: newSupplierRut,
    })
  }

  const addItem = () => {
    if (itemType === 'product' && !productId) return
    if (itemType === 'service' && !serviceId) return
    if (qty <= 0 || unitCost <= 0) return

    const newItem: PurchaseItemPayload = {
      qty,
      unitCost,
      vatRate,
    }

    if (itemType === 'product') {
      newItem.productId = productId
      newItem.mfgDate = mfgDate || undefined
      newItem.expDate = expDate || undefined
      newItem.locationId = locationId || undefined
    } else {
      newItem.serviceId = serviceId
    }

    setItems(prev => [...prev, newItem])
    setProductId(normalizedProductId)
    setServiceId('')
    setQty(normalizedQty)
    setUnitCost(0)
    setExpDate('')
    setMfgDate('')
    setBatchName('')
    setLocationId(normalizedLocationId)
    setVatRate(19)
  }

  const removeItem = (index: number) => {
    setItems(prev => prev.filter((_, idx) => idx !== index))
  }

  const validatePurchaseForm = (): string[] => {
    const errors: string[] = []

    if (!supplierId) errors.push('Selecciona un proveedor')
    if (!docType) errors.push('Selecciona tipo de documento')
    if (!docNumber.trim()) errors.push('Ingresa número de documento')
    if (!items.length) {
      errors.push('Agrega al menos un producto/servicio')
    } else {
      items.forEach((item, idx) => {
        const label = `Item ${idx + 1}`
        if (!item.productId && !item.serviceId) {
          errors.push(`${label}: selecciona un producto o servicio`)
        }
        if (item.productId && item.productId.trim().length === 0) {
          errors.push(`${label}: selecciona un producto válido`)
        }
        if (item.serviceId && item.serviceId.trim().length === 0) {
          errors.push(`${label}: selecciona un servicio válido`)
        }
        if (item.qty <= 0) {
          errors.push(`${label}: cantidad debe ser mayor a 0`)
        }
        if (item.unitCost <= 0) {
          errors.push(`${label}: costo debe ser mayor a 0`)
        }
      })
    }

    return errors
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    const errors = validatePurchaseForm()
    if (errors.length > 0) {
      window.alert(errors.join('\n'))
      return
    }
    const payload: PurchasePayload = {
      supplierId,
      docType,
      docNumber,
      issuedAt: new Date(issuedAt).toISOString(),
      receivedAt: receivedAt ? new Date(receivedAt).toISOString() : undefined,
      paymentTermDays: paymentTermDays ?? undefined,
      net: Number(totals.net.toFixed(2)),
      vat: Number(totals.vat.toFixed(2)),
      total: Number(totals.total.toFixed(2)),
      items,
    }
    mutation.mutate({ payload, file: pdfFile || undefined })
  }

  return (
    <form className={className ?? 'form-grid'} onSubmit={handleSubmit}>
      <div style={{ gridColumn: '1 / -1' }}>
        <h3 style={{ marginBottom: '0.5rem', fontSize: '0.9rem', fontWeight: '600' }}>
          Información del Proveedor
        </h3>
      </div>

      {!showNewSupplier ? (
        <>
          <label style={{ gridColumn: '1 / -1' }}>
            <span>Proveedor *</span>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <select
                className="input"
                value={supplierId}
                onChange={e => setSupplierId(e.target.value)}
                required
                style={{ flex: 1 }}
              >
                <option value="">Selecciona proveedor</option>
                {supplierOptions.map(supplier => (
                  <option key={supplier.id} value={supplier.id}>
                    {supplier.name} {supplier.rut ? `(${supplier.rut})` : ''}
                  </option>
                ))}
              </select>
              <button
                type="button"
                className="btn"
                onClick={() => setShowNewSupplier(true)}
                style={{ whiteSpace: 'nowrap' }}
              >
                + Nuevo
              </button>
            </div>
          </label>
        </>
      ) : (
        <>
          <label style={{ gridColumn: '1 / -1' }}>
            <span>Nombre Proveedor *</span>
            <input
              className="input"
              value={newSupplierName}
              onChange={e => setNewSupplierName(e.target.value)}
              placeholder="Nombre del proveedor"
            />
          </label>
          <label style={{ gridColumn: '1 / -1' }}>
            <span>RUT *</span>
            <input
              className="input"
              value={newSupplierRut}
              onChange={e => setNewSupplierRut(e.target.value)}
              placeholder="12.345.678-9"
            />
          </label>
          <div style={{ gridColumn: '1 / -1', display: 'flex', gap: '0.5rem' }}>
            <button
              type="button"
              className="btn"
              onClick={handleCreateSupplier}
              disabled={supplierMutation.isPending}
            >
              {supplierMutation.isPending ? 'Creando...' : 'Crear Proveedor'}
            </button>
            <button
              type="button"
              className="btn ghost"
              onClick={() => {
                setShowNewSupplier(false)
                setNewSupplierName('')
                setNewSupplierRut('')
              }}
            >
              Cancelar
            </button>
          </div>
        </>
      )}

      <div className="line" style={{ gridColumn: '1 / -1' }}></div>

      <div style={{ gridColumn: '1 / -1' }}>
        <h3 style={{ marginBottom: '0.5rem', fontSize: '0.9rem', fontWeight: '600' }}>
          Información del Documento
        </h3>
      </div>

      <label>
        <span>Tipo de Documento *</span>
        <select
          className="input"
          value={docType}
          onChange={e => setDocType(e.target.value)}
          required
        >
          {DOC_TYPES.map(type => (
            <option key={type} value={type}>
              {type}
            </option>
          ))}
        </select>
      </label>

      <label>
        <span>Número de Documento</span>
        <input
          className="input"
          value={docNumber}
          onChange={e => setDocNumber(e.target.value.slice(0, 35))}
          maxLength={35}
          placeholder="Ej: OC-2025-001 o 12345"
        />
      </label>

      <label>
        <span>Fecha de Emisión *</span>
        <input
          className="input"
          type="datetime-local"
          value={issuedAt}
          onChange={e => setIssuedAt(e.target.value)}
          required
        />
      </label>

      <label>
        <span>Fecha de Recepción</span>
        <input
          className="input"
          type="datetime-local"
          value={receivedAt}
          onChange={e => setReceivedAt(e.target.value)}
        />
      </label>

      <label>
        <span>Términos de pago (opcional)</span>
        <select
          className="input"
          value={paymentTermDays ?? ''}
          onChange={e => setPaymentTermDays(e.target.value ? Number(e.target.value) : null)}
        >
          <option value="">Sin términos de pago</option>
          <option value="7">7 días</option>
          <option value="15">15 días</option>
          <option value="30">30 días</option>
          <option value="60">60 días</option>
        </select>
      </label>

      <label style={{ gridColumn: '1 / -1' }}>
        <span>📎 Adjuntar Documento PDF (opcional)</span>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            marginTop: '0.25rem',
          }}
        >
          <input
            type="file"
            accept=".pdf,application/pdf"
            onChange={e => {
              const file = e.target.files?.[0]
              if (file) {
                if (file.size > 10 * 1024 * 1024) {
                  alert('El archivo es demasiado grande. Máximo 10MB.')
                  e.target.value = ''
                  return
                }
                setPdfFile(file)
              }
            }}
            style={{ flex: 1 }}
          />
          {pdfFile && (
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
                padding: '0.5rem',
                backgroundColor: '#e3f2fd',
                borderRadius: '4px',
                fontSize: '0.85rem',
              }}
            >
              <span>📄 {pdfFile.name}</span>
              <button
                type="button"
                className="btn ghost"
                onClick={() => setPdfFile(null)}
                style={{ padding: '0.25rem 0.5rem', fontSize: '0.75rem' }}
              >
                ✕
              </button>
            </div>
          )}
        </div>
        <small className="muted" style={{ display: 'block', marginTop: '0.25rem' }}>
          Formatos aceptados: PDF. Tamaño máximo: 10MB
        </small>
      </label>

      <div className="line" style={{ gridColumn: '1 / -1' }}></div>

      <div style={{ gridColumn: '1 / -1' }}>
        <h3 style={{ marginBottom: '0.5rem', fontSize: '0.9rem', fontWeight: '600' }}>
          Items de Compra
        </h3>
      </div>

      <div
        className="item-builder"
        style={{
          gridColumn: '1 / -1',
          display: 'grid',
          gridTemplateColumns: 'repeat(2, 1fr)',
          gap: '0.75rem',
        }}
      >
        <div
          style={{ gridColumn: '1 / -1', display: 'flex', gap: '1rem', marginBottom: '0.5rem' }}
        >
          <label
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer' }}
          >
            <input
              type="radio"
              name="itemType"
              value="product"
              checked={itemType === 'product'}
              onChange={e => setItemType(e.target.value as 'product' | 'service')}
            />
            <span>Producto</span>
          </label>
          <label
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer' }}
          >
            <input
              type="radio"
              name="itemType"
              value="service"
              checked={itemType === 'service'}
              onChange={e => setItemType(e.target.value as 'product' | 'service')}
            />
            <span>Servicio</span>
          </label>
        </div>

        <label style={{ gridColumn: '1 / -1' }}>
          <span>{itemType === 'product' ? 'Producto *' : 'Servicio *'}</span>
          {itemType === 'product' ? (
            <select
              className="input"
              value={productId}
              onChange={e => setProductId(e.target.value)}
            >
              <option value="">Seleccionar producto</option>
              {productOptions.map(product => (
                <option key={product.id} value={product.id}>
                  {product.name} {product.sku ? `(${product.sku})` : ''}
                </option>
              ))}
            </select>
          ) : (
            <select
              className="input"
              value={serviceId}
              onChange={e => setServiceId(e.target.value)}
            >
              <option value="">Seleccionar servicio</option>
              {servicesQuery.data?.map(service => (
                <option key={service.id} value={service.id}>
                  {service.code} - {service.name}
                </option>
              ))}
            </select>
          )}
        </label>

        <label>
          <span>Cantidad *</span>
          <input
            className="input"
            type="number"
            min={1}
            value={qty}
            onChange={e => setQty(Number(e.target.value))}
            placeholder="Cantidad"
          />
        </label>

        <label>
          <span>Costo unitario *</span>
          <input
            className="input"
            type="number"
            step="0.01"
            min={0}
            value={unitCost}
            onChange={e => setUnitCost(Number(e.target.value))}
            placeholder="Costo unitario"
          />
        </label>

        {itemType === 'product' && (
          <>
            <label>
              <span>Nombre del lote (opcional)</span>
              <input
                className="input"
                type="text"
                value={batchName}
                onChange={e => setBatchName(e.target.value)}
                placeholder="Ej: LOTE-2025-001"
              />
            </label>

            <label>
              <span>Ubicación de destino</span>
              <LocationSelect
                value={locationId || null}
                onChange={locId => setLocationId(locId ?? '')}
                disabled={locationsQuery.isLoading}
              />
            </label>

            <label>
              <span>Fecha de fabricación</span>
              <input
                className="input"
                type="date"
                value={mfgDate}
                onChange={e => setMfgDate(e.target.value)}
              />
            </label>

            <label>
              <span>Fecha de vencimiento</span>
              <input
                className="input"
                type="date"
                value={expDate}
                onChange={e => setExpDate(e.target.value)}
              />
            </label>
          </>
        )}

        <button type="button" className="btn" onClick={addItem} style={{ gridColumn: '1 / -1' }}>
          + Agregar {itemType === 'product' ? 'Producto' : 'Servicio'}
        </button>
      </div>

      {items.length > 0 && (
        <div style={{ gridColumn: '1 / -1', overflowX: 'auto' }}>
          <table className="table" style={{ minWidth: '600px' }}>
            <thead>
              <tr>
                <th>Tipo</th>
                <th>Item</th>
                <th style={{ textAlign: 'right' }}>Cantidad</th>
                <th style={{ textAlign: 'right' }}>Costo Unit.</th>
                <th style={{ textAlign: 'right' }}>Subtotal</th>
                <th>Info</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {items.map((item, idx) => {
                const supplierText = item.productId ? 'Producto' : 'Servicio'
                return (
                  <tr key={`${item.productId ?? item.serviceId}-${idx}`}>
                    <td>{supplierText}</td>
                    <td>
                      {item.productId && productOptions.find(p => p.id === item.productId)?.name}
                      {item.serviceId &&
                        servicesQuery.data?.find(service => service.id === item.serviceId)?.name}
                    </td>
                    <td style={{ textAlign: 'right' }}>{item.qty}</td>
                    <td style={{ textAlign: 'right' }}>${item.unitCost.toFixed(2)}</td>
                    <td style={{ textAlign: 'right' }}>${(item.qty * item.unitCost).toFixed(2)}</td>
                    <td>
                      {item.productId && (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                          <span>📍 {resolveLocationLabel(item.locationId)}</span>
                          {item.mfgDate && (
                            <span>
                              🏷️ MFG: {new Date(item.mfgDate).toLocaleDateString('es-CL')}
                            </span>
                          )}
                          {item.expDate && (
                            <span>
                              ⏰ Venc: {new Date(item.expDate).toLocaleDateString('es-CL')}
                            </span>
                          )}
                        </div>
                      )}
                      {item.serviceId && '-'}
                    </td>
                    <td>
                      <button
                        type="button"
                        className="btn ghost"
                        onClick={() => removeItem(idx)}
                        style={{ padding: '0.25rem 0.5rem' }}
                      >
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

      <div className="line" style={{ gridColumn: '1 / -1' }}></div>

      <div style={{ gridColumn: '1 / -1' }}>
        <h3 style={{ marginBottom: '0.5rem', fontSize: '0.9rem', fontWeight: '600' }}>
          Descuentos e Impuestos
        </h3>
      </div>

      <label style={{ gridColumn: 'span 1' }}>
        <span>Tipo de Descuento</span>
        <select
          className="input"
          value={discountType}
          onChange={e => setDiscountType(e.target.value as DiscountType)}
        >
          <option value="amount">Monto Fijo ($)</option>
          <option value="percentage">Porcentaje (%)</option>
        </select>
      </label>

      <label style={{ gridColumn: 'span 1' }}>
        <span>Descuento {discountType === 'percentage' ? '(%)' : '($)'}</span>
        <input
          className="input"
          type="number"
          step="0.01"
          min={0}
          max={discountType === 'percentage' ? 100 : undefined}
          value={discountValue}
          onChange={e => setDiscountValue(Number(e.target.value))}
          placeholder={discountType === 'percentage' ? '0-100' : '0.00'}
        />
      </label>

      <label
        style={{
          gridColumn: '1 / -1',
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem',
          cursor: 'pointer',
        }}
      >
        <input
          type="checkbox"
          checked={applyVat}
          onChange={e => setApplyVat(e.target.checked)}
          style={{ width: 'auto' }}
        />
        <span style={{ fontWeight: '600' }}>Aplicar IVA 19%</span>
      </label>

      <div
        style={{
          gridColumn: '1 / -1',
          backgroundColor: '#f8f9fa',
          padding: '1rem',
          borderRadius: '8px',
          marginTop: '0.5rem',
        }}
      >
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))',
            gap: '1rem',
          }}
        >
          <div>
            <span className="muted small">Subtotal</span>
            <div style={{ fontSize: '1.1rem', fontWeight: '600' }}>
              ${totals.subtotal.toFixed(2)}
            </div>
          </div>
          {totals.discountAmount > 0 && (
            <div>
              <span className="muted small">Descuento</span>
              <div style={{ fontSize: '1.1rem', fontWeight: '600', color: '#dc3545' }}>
                -${totals.discountAmount.toFixed(2)}
              </div>
            </div>
          )}
          <div>
            <span className="muted small">Neto</span>
            <div style={{ fontSize: '1.1rem', fontWeight: '600' }}>${totals.net.toFixed(2)}</div>
          </div>
          {applyVat && (
            <div>
              <span className="muted small">IVA (19%)</span>
              <div style={{ fontSize: '1.1rem', fontWeight: '600' }}>${totals.vat.toFixed(2)}</div>
            </div>
          )}
          <div>
            <span className="muted small">Total</span>
            <div style={{ fontSize: '1.3rem', fontWeight: '700', color: '#0066cc' }}>
              ${totals.total.toFixed(2)}
            </div>
          </div>
        </div>
      </div>

      {mutation.isError && (
        <p className="error" style={{ gridColumn: '1 / -1' }}>
          {(mutation.error as Error).message}
        </p>
      )}

      <div className="buttons" style={{ gridColumn: '1 / -1' }}>
        <button className="btn" type="submit" disabled={mutation.isPending || items.length === 0}>
          {mutation.isPending ? 'Guardando...' : 'Registrar Compra'}
        </button>
        <button className="btn ghost" type="button" onClick={resetForm}>
          Limpiar Formulario
        </button>
      </div>
    </form>
  )
}

import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import PageHeader from '../../components/layout/PageHeader'
import PurchaseCreateForm from '../../components/purchases/PurchaseCreateForm'

const parseNumberParam = (value: string | null) => {
  if (!value) return null
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

const normalizeQuantity = (value: number | null | undefined) => {
  if (value == null) return undefined
  return Math.max(Math.ceil(value), 1)
}

export default function PurchaseCreatePage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [successMessage, setSuccessMessage] = useState('')
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const queryParams = useMemo(() => {
    const suggestedQtyParam = normalizeQuantity(parseNumberParam(searchParams.get('suggestedQty')))
    const reorderPoint = parseNumberParam(searchParams.get('reorderPoint'))
    const currentQty = parseNumberParam(searchParams.get('currentQty'))
    const fallbackQty =
      reorderPoint != null && currentQty != null
        ? Math.max(Math.ceil(reorderPoint - currentQty), 1)
        : undefined

    const supplierId =
      searchParams.get('suggestedSupplierId') ?? searchParams.get('supplierId') ?? undefined

    return {
      productId: searchParams.get('productId') ?? undefined,
      supplierId,
      locationId: searchParams.get('locationId') ?? undefined,
      suggestedQty: suggestedQtyParam ?? fallbackQty,
    }
  }, [searchParams.toString()])

  const handleSubmitted = () => {
    setSuccessMessage('Orden registrada correctamente. Redirigiendo al panel de abastecimiento...')
    if (timerRef.current) {
      window.clearTimeout(timerRef.current)
    }
    timerRef.current = window.setTimeout(() => {
      navigate('/app/inventory')
    }, 1400)
  }

  useEffect(() => {
    return () => {
      if (timerRef.current) {
        window.clearTimeout(timerRef.current)
      }
    }
  }, [])

  return (
    <div className="page-section">
      <PageHeader
        title="Registrar orden de compra"
        description="Completa los datos para generar una orden alineada con las necesidades del panel de abastecimiento."
        actions={
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button className="btn ghost" type="button" onClick={() => navigate('/app/inventory')}>
              Volver a abastecimiento
            </button>
            <button className="btn ghost" type="button" onClick={() => navigate('/app/purchases')}>
              Ver compras
            </button>
          </div>
        }
      />

      {successMessage && (
        <div className="bg-green-950 border border-green-800 rounded-lg p-4 text-green-300 text-sm" style={{ marginBottom: '1rem' }}>
          {successMessage}
          <div style={{ marginTop: '0.25rem' }}>
            <button className="btn ghost" type="button" onClick={() => navigate('/app/inventory')}>
              Ir al panel de abastecimiento
            </button>
          </div>
        </div>
      )}

      <div className="card" style={{ padding: '1.25rem 1.5rem' }}>
        <PurchaseCreateForm
          initialProductId={queryParams.productId}
          initialSupplierId={queryParams.supplierId}
          initialLocationId={queryParams.locationId}
          initialQty={queryParams.suggestedQty}
          onSubmitted={handleSubmitted}
        />
      </div>
    </div>
  )
}

import { useEffect, useMemo, useState } from 'react'
import { useFrequentProducts } from '../../hooks/useFrequentProducts'
import type { FrequentProduct } from '../../services/client'

const currencyFormatter = new Intl.NumberFormat('es-CL', {
  style: 'currency',
  currency: 'CLP',
  minimumFractionDigits: 0,
  maximumFractionDigits: 0,
})

function formatDate(value: string) {
  try {
    return new Date(value).toLocaleDateString()
  } catch (error) {
    return value
  }
}

type FrequentProductsPanelProps = {
  customerId?: string
  visible: boolean
  onPick: (product: FrequentProduct, meta?: { lastQty?: number }) => void
}

export function FrequentProductsPanel({ customerId, visible, onPick }: FrequentProductsPanelProps) {
  const [query, setQuery] = useState('')
  const { data, isLoading, isFetching, isError, refetch } = useFrequentProducts(customerId)

  useEffect(() => {
    setQuery('')
  }, [customerId])

  const filtered = useMemo(() => {
    if (!data) {
      return []
    }
    const normalized = query.trim().toLowerCase()
    if (!normalized) {
      return data
    }
    return data.filter(item => {
      const name = item.name?.toLowerCase() ?? ''
      const sku = item.sku?.toLowerCase() ?? ''
      return name.includes(normalized) || sku.includes(normalized)
    })
  }, [data, query])

  if (!visible || !customerId) {
    return null
  }

  const showSearch = (data?.length ?? 0) > 0
  const loading = isLoading || (isFetching && !(data && data.length))

  return (
    <section
      className="card frequent-products-panel"
      aria-label="Productos frecuentes del cliente"
      aria-live="polite"
    >
      <header className="card-header frequent-products-header">
        <div className="frequent-products-heading">
          <h2 className="card-title">Productos frecuentes</h2>
          <p className="muted">Basado en compras anteriores del cliente.</p>
        </div>
        {showSearch ? (
          <div className="frequent-products-search">
            <label className="sr-only" htmlFor="frequent-products-search">
              Buscar producto frecuente
            </label>
            <input
              id="frequent-products-search"
              type="search"
              className="input"
              placeholder="Buscar por nombre o SKU"
              value={query}
              onChange={event => setQuery(event.target.value)}
              autoComplete="off"
            />
          </div>
        ) : null}
      </header>

      <div className="frequent-products-content" aria-busy={loading}>
        {loading ? (
          <div className="frequent-products-loading" role="status" aria-live="polite">
            <div className="skeleton skeleton-text" aria-hidden="true" />
            <div className="skeleton skeleton-text" aria-hidden="true" />
            <div className="skeleton skeleton-text" aria-hidden="true" />
          </div>
        ) : null}

        {!loading && isError ? (
          <div className="frequent-products-error" role="alert">
            <p>No pudimos cargar los productos frecuentes.</p>
            <button type="button" className="btn" onClick={() => refetch()}>
              Reintentar
            </button>
          </div>
        ) : null}

        {!loading && !isError && (data?.length ?? 0) === 0 ? (
          <p className="muted" role="status">
            Este cliente no tiene compras previas.
          </p>
        ) : null}

        {!loading && !isError && filtered.length > 0 ? (
          <ul className="frequent-products-list">
            {filtered.map(item => {
              const lastDateLabel = formatDate(item.lastPurchasedAt)
              const averageLabel =
                item.avgQty !== undefined && item.avgQty !== null
                  ? Number(item.avgQty).toFixed(1)
                  : null
              const priceLabel =
                item.lastUnitPrice !== undefined && item.lastUnitPrice !== null
                  ? currencyFormatter.format(item.lastUnitPrice)
                  : null
              const handleActivate = () => onPick(item, { lastQty: item.lastQty })

              return (
                <li key={item.productId}>
                  <div
                    role="button"
                    tabIndex={0}
                    className="frequent-product-row"
                    onClick={handleActivate}
                    onKeyDown={event => {
                      if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault()
                        handleActivate()
                      }
                    }}
                    aria-label={`Agregar ${item.name}`}
                  >
                    <div className="frequent-product-main">
                      <span className="frequent-product-name">{item.name}</span>
                      <span className="frequent-product-sku">SKU: {item.sku || '—'}</span>
                    </div>
                    <dl className="frequent-product-meta">
                      <div>
                        <dt>Última compra</dt>
                        <dd>{lastDateLabel}</dd>
                      </div>
                      <div>
                        <dt>Veces compradas</dt>
                        <dd>{item.totalPurchases}</dd>
                      </div>
                      {item.lastQty !== undefined ? (
                        <div>
                          <dt>Última cantidad</dt>
                          <dd>{item.lastQty}</dd>
                        </div>
                      ) : null}
                      {averageLabel ? (
                        <div>
                          <dt>Cantidad promedio</dt>
                          <dd>{averageLabel}</dd>
                        </div>
                      ) : null}
                      {priceLabel ? (
                        <div>
                          <dt>Último precio</dt>
                          <dd>{priceLabel}</dd>
                        </div>
                      ) : null}
                    </dl>
                  </div>
                </li>
              )
            })}
          </ul>
        ) : null}

        {!loading && !isError && filtered.length === 0 && (data?.length ?? 0) > 0 ? (
          <p className="muted" role="status">
            No encontramos coincidencias para "{query}".
          </p>
        ) : null}
      </div>
    </section>
  )
}

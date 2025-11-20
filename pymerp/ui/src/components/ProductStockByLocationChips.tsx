import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getStockByProduct, StockByLocation } from '../services/inventory'

type Props = {
  productId: string
  enabled?: boolean
  displayLimit?: number
}

const DEFAULT_DISPLAY_LIMIT = 3
const STALE_TIME_MS = 1000 * 60 * 5
const CACHE_TIME_MS = 1000 * 60 * 30

function formatAvailableQty(value: number) {
  if (!Number.isFinite(value)) {
    return '0'
  }
  if (Number.isInteger(value)) {
    return value.toLocaleString('es-CL')
  }
  return value.toLocaleString('es-CL', { maximumFractionDigits: 2 })
}

function buildOverflowTooltip(locations: StockByLocation[]) {
  if (locations.length === 0) {
    return ''
  }
  const padding = 'Ubicaciones adicionales:'
  const lines = locations.map(location => `${location.locationName}: ${formatAvailableQty(location.availableQty)}`)
  return [padding, ...lines].join('\n')
}

export default function ProductStockByLocationChips({
  productId,
  enabled = true,
  displayLimit = DEFAULT_DISPLAY_LIMIT,
}: Props) {
  const normalizedLimit = Math.max(0, Math.floor(displayLimit))

  const stockQuery = useQuery({
    queryKey: ['inventory', 'stockByProduct', productId],
    queryFn: () => getStockByProduct(productId),
    enabled: Boolean(productId) && enabled,
    staleTime: STALE_TIME_MS,
    cacheTime: CACHE_TIME_MS,
  })

  const locationsWithStock = useMemo(() => {
    const candidates = stockQuery.data ?? []
    return candidates.filter(location => location.availableQty > 0)
  }, [stockQuery.data])

  const visibleLocations = useMemo(
    () => locationsWithStock.slice(0, normalizedLimit),
    [locationsWithStock, normalizedLimit]
  )

  const hiddenLocations = useMemo(() => {
    if (normalizedLimit >= locationsWithStock.length) {
      return []
    }
    return locationsWithStock.slice(normalizedLimit)
  }, [locationsWithStock, normalizedLimit])

  if (visibleLocations.length === 0 && hiddenLocations.length === 0) {
    return null
  }

  return (
    <div className="product-stock-chips" aria-live="polite">
      {visibleLocations.map(location => (
        <span
          key={location.locationId}
          className="product-stock-chip"
          data-testid="product-stock-chip"
        >
          <span className="product-stock-chip__label">{`${location.locationName}: `}</span>
          <span className="product-stock-chip__value">{formatAvailableQty(location.availableQty)}</span>
        </span>
      ))}
      {hiddenLocations.length > 0 && (
        <span
          className="product-stock-chip product-stock-chip--overflow"
          data-testid="product-stock-chip-overflow"
          title={buildOverflowTooltip(hiddenLocations)}
          aria-label={`${hiddenLocations.length} ubicaciones adicionales con stock`}
        >
          +{hiddenLocations.length}
        </span>
      )}
    </div>
  )
}

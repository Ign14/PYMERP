const DEFAULT_LOCALE = 'es-CL'
const DEFAULT_CURRENCY = 'CLP'

const clpFormatter = new Intl.NumberFormat(DEFAULT_LOCALE, {
  style: 'currency',
  currency: DEFAULT_CURRENCY,
  minimumFractionDigits: 0,
  maximumFractionDigits: 0,
})

export function createCurrencyFormatter(currency?: string): Intl.NumberFormat {
  const normalized = currency && currency.length === 3 ? currency.toUpperCase() : DEFAULT_CURRENCY
  const zeroDecimal = normalized === DEFAULT_CURRENCY
  return new Intl.NumberFormat(DEFAULT_LOCALE, {
    style: 'currency',
    currency: normalized,
    minimumFractionDigits: zeroDecimal ? 0 : 2,
    maximumFractionDigits: zeroDecimal ? 0 : 2,
  })
}

export function formatMoneyCLP(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return clpFormatter.format(0)
  }
  return clpFormatter.format(value)
}

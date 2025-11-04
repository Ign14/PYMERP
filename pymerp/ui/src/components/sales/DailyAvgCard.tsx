import { formatMoneyCLP } from '../../utils/currency'

type DailyAvgCardProps = {
  value: number
  rangeLabel: string
  formatter?: (value: number) => string
}

export default function DailyAvgCard({
  value,
  rangeLabel,
  formatter = formatMoneyCLP,
}: DailyAvgCardProps) {
  return (
    <article className="card stat" aria-label={`Promedio diario de ventas para ${rangeLabel}`}>
      <h3>Promedio diario</h3>
      <p className="stat-value" data-testid="sales-daily-average">
        {formatter(value)}
      </p>
      <span className="stat-trend">Rango seleccionado: {rangeLabel}</span>
    </article>
  )
}

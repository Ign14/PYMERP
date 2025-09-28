import { formatMoneyCLP } from "../../utils/currency";

type DailyAvgCardProps = {
  value: number;
  days?: number;
  formatter?: (value: number) => string;
};

export default function DailyAvgCard({ value, days = 14, formatter = formatMoneyCLP }: DailyAvgCardProps) {
  return (
    <article className="card stat" aria-label={`Promedio diario de ventas considerando ${days} días`}>
      <h3>Promedio diario</h3>
      <p className="stat-value" data-testid="sales-daily-average">
        {formatter(value)}
      </p>
      <span className="stat-trend">Periodo analizado: {days} días</span>
    </article>
  );
}

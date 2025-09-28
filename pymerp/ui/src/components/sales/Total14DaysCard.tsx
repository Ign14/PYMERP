import { formatMoneyCLP } from "../../utils/currency";

type Total14DaysCardProps = {
  value: number;
  days?: number;
  formatter?: (value: number) => string;
};

export default function Total14DaysCard({ value, days = 14, formatter = formatMoneyCLP }: Total14DaysCardProps) {
  return (
    <article className="card stat" aria-label={`Ventas acumuladas durante ${days} días`}>
      <h3>Ventas acumuladas</h3>
      <p className="stat-value" data-testid="sales-total-14d">
        {formatter(value)}
      </p>
      <span className="stat-trend">Montos brutos del periodo</span>
    </article>
  );
}

import { formatMoneyCLP } from "../../utils/currency";

type Total14DaysCardProps = {
  value: number;
  rangeLabel: string;
  formatter?: (value: number) => string;
};

export default function Total14DaysCard({
  value,
  rangeLabel,
  formatter = formatMoneyCLP,
}: Total14DaysCardProps) {
  return (

    <article className="card stat" aria-label={`Ventas acumuladas para ${rangeLabel}`}>

    <article className="card stat" aria-label={`Ventas acumuladas durante ${days} días`}>

      <h3>Ventas acumuladas</h3>
      <p className="stat-value" data-testid="sales-total-14d">
        {formatter(value)}
      </p>

      <span className="stat-trend">Rango seleccionado: {rangeLabel}</span>

      <span className="stat-trend">Montos brutos del periodo</span>

    </article>
  );
}

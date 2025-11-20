import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import DailyAvgCard from './DailyAvgCard'
import Total14DaysCard from './Total14DaysCard'
import { createCurrencyFormatter } from '../../utils/currency'
import client, { getSalesWindowMetrics, SalesKPIs, SalesWindowMetrics } from '../../services/client'

type SalesDashboardOverviewProps = {
  startDate: string
  endDate: string
  onStartDateChange: (date: string) => void
  onEndDateChange: (date: string) => void
}

export default function SalesDashboardOverview({
  startDate,
  endDate,
  onStartDateChange,
  onEndDateChange,
}: SalesDashboardOverviewProps) {
  const currencyFormatter = useMemo(() => createCurrencyFormatter(), [])
  const formatCurrency = (value: number) => currencyFormatter.format(value ?? 0)

  // Calcular el número de días en el rango
  const days = useMemo(() => {
    const start = new Date(startDate)
    const end = new Date(endDate)
    const diffTime = Math.abs(end.getTime() - start.getTime())
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1
    return diffDays
  }, [startDate, endDate])

  const {
    data: kpis,
    isLoading: isKpisLoading,
    error: kpisError,
  } = useQuery<SalesKPIs, Error>({
    queryKey: ['sales-kpis', startDate, endDate],
    queryFn: async () => {
      const params = new URLSearchParams({
        from: startDate,
        to: endDate,
      })

      console.log('Fetching KPIs:', params.toString())

      const response = await client.get<SalesKPIs>(`/v1/sales/kpis?${params.toString()}`)

      console.log('KPIs received:', response.data)

      return response.data
    },
  })

  if (isKpisLoading) {
    return <div>Cargando KPIs...</div>
  }

  if (kpisError) {
    return <div>Error: {kpisError.message}</div>
  }

  if (!kpis) {
    return <div>No hay datos disponibles</div>
  }

  console.log('Rendering KPIs:', {
    totalRevenue: kpis.totalRevenue,
    totalOrders: kpis.totalOrders,
    profitMargin: kpis.profitMargin,
  })

  // Obtener métricas de ventas del período especificado
  const {
    data: salesMetrics,
    isLoading: isMetricsLoading,
    error: metricsError,
  } = useQuery<SalesWindowMetrics, Error>({
    queryKey: ['sales-window-metrics', days],
    queryFn: () => getSalesWindowMetrics(`${days}d`),
  })

  const dailyAverage = useMemo(() => {
    return salesMetrics?.dailyAverage ?? 0
  }, [salesMetrics])

  const totalSales = useMemo(() => {
    return salesMetrics?.totalWithTax ?? 0
  }, [salesMetrics])

  const documentCount = useMemo(() => {
    return salesMetrics?.documentCount ?? 0
  }, [salesMetrics])

  const rangeLabel = `Últimos ${days} días`

  if (isMetricsLoading) {
    return (
      <section aria-labelledby="sales-dashboard-overview-heading" className="mb-6">
        <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
          <header className="mb-4">
            <h2
              id="sales-dashboard-overview-heading"
              className="text-xl font-semibold text-neutral-100"
            >
              📊 Resumen de ventas
            </h2>
          </header>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="animate-pulse bg-neutral-800 rounded-lg h-32"></div>
            <div className="animate-pulse bg-neutral-800 rounded-lg h-32"></div>
          </div>
        </div>
      </section>
    )
  }

  if (metricsError) {
    return (
      <section aria-labelledby="sales-dashboard-overview-heading" className="mb-6">
        <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
          <header className="mb-4">
            <h2
              id="sales-dashboard-overview-heading"
              className="text-xl font-semibold text-neutral-100"
            >
              📊 Resumen de ventas
            </h2>
          </header>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="bg-red-950 border border-red-800 rounded-lg p-4">
              <p className="text-red-400">Error al cargar métricas de ventas</p>
            </div>
            <div className="bg-red-950 border border-red-800 rounded-lg p-4">
              <p className="text-red-400">Error al cargar métricas de ventas</p>
            </div>
          </div>
        </div>
      </section>
    )
  }

  return (
    <section aria-labelledby="sales-dashboard-overview-heading" className="mb-6">
      <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5">
        <header className="mb-4">
          <h2
            id="sales-dashboard-overview-heading"
            className="text-xl font-semibold text-neutral-100"
          >
            📊 Resumen de ventas
          </h2>
        </header>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <DailyAvgCard value={dailyAverage} rangeLabel={rangeLabel} formatter={formatCurrency} />

          <Total14DaysCard value={totalSales} rangeLabel={rangeLabel} formatter={formatCurrency} />
        </div>
      </div>
    </section>
  )
}

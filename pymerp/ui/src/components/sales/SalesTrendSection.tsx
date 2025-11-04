import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import SalesTrendChart from './SalesTrendChart'
import { createCurrencyFormatter } from '../../utils/currency'
import { listSalesDailyByDateRange, SalesDailyPoint } from '../../services/client'

type SalesTrendSectionProps = {
  startDate: string
  endDate: string
  onStartDateChange: (date: string) => void
  onEndDateChange: (date: string) => void
}

export default function SalesTrendSection({
  startDate,
  endDate,
  onStartDateChange,
  onEndDateChange,
}: SalesTrendSectionProps) {
  const today = new Date().toISOString().split('T')[0]

  const currencyFormatter = useMemo(() => createCurrencyFormatter(), [])
  const formatCurrency = (value: number) => currencyFormatter.format(value ?? 0)

  // Query para obtener datos reales de ventas por rango de fechas
  const salesQuery = useQuery<SalesDailyPoint[], Error>({
    queryKey: ['sales', 'daily-range', startDate, endDate],
    queryFn: () => listSalesDailyByDateRange(startDate, endDate),
    enabled: !!startDate && !!endDate && startDate <= endDate,
  })

  const salesData = salesQuery.data ?? []

  // Calcular el promedio de ventas diarias para el rango seleccionado
  const averageDailySales = useMemo(() => {
    if (salesData.length === 0) return 0
    const totalSales = salesData.reduce((sum, point) => sum + point.total, 0)
    return totalSales / salesData.length
  }, [salesData])

  const handleStartChange = (value: string) => {
    if (!value) return
    onStartDateChange(value)
    // Si la fecha de inicio es mayor que la fecha de fin, ajustar la fecha de fin
    if (value > endDate) {
      onEndDateChange(value)
    }
  }

  const handleEndChange = (value: string) => {
    if (!value) return
    onEndDateChange(value)
    // Si la fecha de fin es menor que la fecha de inicio, ajustar la fecha de inicio
    if (value < startDate) {
      onStartDateChange(value)
    }
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5 mb-6">
      <header className="flex flex-wrap gap-4 justify-between items-start mb-4">
        <div>
          <h2 className="text-xl font-semibold text-neutral-100 mb-1">📈 Tendencia de ventas</h2>
          <p className="text-sm text-neutral-400">
            Promedio diario: {formatCurrency(averageDailySales)} | Total período:{' '}
            {formatCurrency(salesData.reduce((sum, point) => sum + point.total, 0))}
          </p>
        </div>
        <div className="flex gap-3 items-center">
          <label className="flex flex-col gap-1">
            <span className="text-xs text-neutral-400">Desde</span>
            <input
              className="input bg-neutral-800 border-neutral-700 text-neutral-100"
              type="date"
              value={startDate}
              max={endDate}
              onChange={event => handleStartChange(event.target.value)}
            />
          </label>
          <label className="flex flex-col gap-1">
            <span className="text-xs text-neutral-400">Hasta</span>
            <input
              className="input bg-neutral-800 border-neutral-700 text-neutral-100"
              type="date"
              value={endDate}
              min={startDate}
              max={today}
              onChange={event => handleEndChange(event.target.value)}
            />
          </label>
        </div>
      </header>

      <div className="bg-neutral-800 border border-neutral-700 rounded-lg p-4">
        {salesQuery.isLoading ? (
          <p className="text-neutral-400" role="status">
            Cargando datos de ventas...
          </p>
        ) : salesQuery.isError ? (
          <p className="text-red-400" role="status">
            Error al cargar los datos de ventas. {salesQuery.error?.message}
          </p>
        ) : salesData.length > 0 ? (
          <SalesTrendChart
            points={salesData}
            formatter={formatCurrency}
            averageLine={averageDailySales}
          />
        ) : (
          <p className="text-neutral-400" role="status">
            No hay ventas registradas en el rango seleccionado ({startDate} - {endDate}).
          </p>
        )}
      </div>
    </div>
  )
}

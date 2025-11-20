import { useEffect, useState } from 'react'
import { getFinanceSummary, type FinanceSummary } from '../../services/client'
import PaymentBucketsChart from './PaymentBucketsChart'

export default function FinanceSummaryCards() {
  const [summary, setSummary] = useState<FinanceSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    loadSummary()
  }, [])

  const loadSummary = async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await getFinanceSummary()
      setSummary(data)
    } catch (err) {
      setError('Error al cargar el resumen financiero')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {[1, 2, 3, 4].map(i => (
          <div
            key={i}
            className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5 animate-pulse"
          >
            <div className="h-4 bg-neutral-800 rounded w-1/2 mb-3"></div>
            <div className="h-8 bg-neutral-800 rounded w-3/4"></div>
          </div>
        ))}
      </div>
    )
  }

  if (error || !summary) {
    const errorMsg = error || 'No se pudo cargar el resumen'
    return (
      <div className="bg-red-950 border border-red-800 text-red-400 rounded-2xl shadow-lg p-4 mb-6">
        <div className="flex items-center gap-3 mb-2">
          <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
            <path
              fillRule="evenodd"
              d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
              clipRule="evenodd"
            />
          </svg>
          <span className="font-semibold">Error al cargar Resumen financiero</span>
        </div>
        <p className="text-sm text-neutral-300 ml-8">{errorMsg}</p>
        <button 
          onClick={loadSummary}
          className="mt-3 ml-8 px-4 py-2 bg-red-800 hover:bg-red-700 rounded text-sm transition-colors"
        >
          Reintentar
        </button>
      </div>
    )
  }

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP',
      minimumFractionDigits: 0,
    }).format(value)
  }

  const cards = [
    {
      title: 'Efectivo Disponible',
      value: summary.cashOnHand,
      icon: '💰',
      color: 'blue',
      bgColor: 'bg-blue-500',
    },
    {
      title: 'Cuentas por Cobrar',
      value: summary.totalReceivables,
      icon: '📈',
      color: 'green',
      bgColor: 'bg-green-500',
      alert: summary.overdueInvoices > 0 ? `${summary.overdueInvoices} vencidas` : null,
    },
    {
      title: 'Cuentas por Pagar',
      value: summary.totalPayables,
      icon: '📉',
      color: 'red',
      bgColor: 'bg-red-500',
      alert: summary.overduePayables > 0 ? `${summary.overduePayables} vencidas` : null,
    },
    {
      title: 'Posición Neta',
      value: summary.netPosition,
      icon: summary.netPosition >= 0 ? '✅' : '⚠️',
      color: summary.netPosition >= 0 ? 'green' : 'red',
      bgColor: summary.netPosition >= 0 ? 'bg-green-500' : 'bg-red-500',
    },
  ]

  const projectionCards = [
    {
      title: 'Ingresos Próximos 7 Días',
      value: summary.next7DaysIncome,
      icon: '🕐',
      color: 'emerald',
      bgColor: 'bg-emerald-100 text-emerald-600',
    },
    {
      title: 'Egresos Próximos 7 Días',
      value: summary.next7DaysExpense,
      icon: '🕐',
      color: 'orange',
      bgColor: 'bg-orange-100 text-orange-600',
    },
    {
      title: 'Ingresos Próximos 30 Días',
      value: summary.next30DaysIncome,
      icon: '📅',
      color: 'emerald',
      bgColor: 'bg-emerald-100 text-emerald-600',
    },
    {
      title: 'Egresos Próximos 30 Días',
      value: summary.next30DaysExpense,
      icon: '📅',
      color: 'orange',
      bgColor: 'bg-orange-100 text-orange-600',
    },
  ]

  return (
    <div className="space-y-6">
      {/* Resumen Principal */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {cards.map(card => (
          <div
            key={card.title}
            className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-5"
          >
            <div className="flex items-start justify-between">
              <div className="flex-1">
                <p className="text-sm text-neutral-400 font-medium mb-1">{card.title}</p>
                <p className="text-2xl font-bold text-neutral-100">{formatCurrency(card.value)}</p>
                {card.alert && (
                  <div className="mt-2 flex items-center gap-1 text-xs text-red-400">
                    <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                      <path
                        fillRule="evenodd"
                        d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
                        clipRule="evenodd"
                      />
                    </svg>
                    <span>{card.alert}</span>
                  </div>
                )}
              </div>
              <div className={`p-3 rounded-lg ${card.bgColor}`}>
                <span className="text-2xl">{card.icon}</span>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Proyecciones */}
      <div>
        <h3 className="text-lg font-semibold text-neutral-100 mb-3">
          Proyecciones de Flujo de Caja
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {projectionCards.map(card => (
            <div
              key={card.title}
              className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-4"
            >
              <div className="flex items-center gap-3">
                <div className={`p-2 rounded ${card.bgColor}`}>
                  <span className="text-xl">{card.icon}</span>
                </div>
                <div>
                  <p className="text-xs text-neutral-400">{card.title}</p>
                  <p className="text-lg font-semibold text-neutral-100">
                    {formatCurrency(card.value)}
                  </p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Buckets de Antigüedad */}
      {summary.receivableBuckets && summary.payableBuckets && (
        <div>
          <h3 className="text-lg font-semibold text-neutral-100 mb-3">
            Análisis de Antigüedad de Saldos
          </h3>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <PaymentBucketsChart
              title="Cuentas por Cobrar - Por Vencimiento"
              buckets={summary.receivableBuckets}
              color="green"
            />
            <PaymentBucketsChart
              title="Cuentas por Pagar - Por Vencimiento"
              buckets={summary.payableBuckets}
              color="red"
            />
          </div>
        </div>
      )}
    </div>
  )
}

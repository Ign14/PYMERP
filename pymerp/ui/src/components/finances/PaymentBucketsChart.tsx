import type { PaymentBucketSummary } from '../../services/client'

interface PaymentBucketsChartProps {
  title: string
  buckets: PaymentBucketSummary[]
  color: 'green' | 'red'
}

export default function PaymentBucketsChart({ title, buckets, color }: PaymentBucketsChartProps) {
  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP',
      minimumFractionDigits: 0,
    }).format(value)
  }

  const maxAmount = Math.max(...buckets.map(b => b.amount), 1)

  const getBucketColor = (key: string) => {
    if (key === 'overdue') return color === 'green' ? 'bg-red-500' : 'bg-red-600'
    if (key === 'days_0_7') return color === 'green' ? 'bg-yellow-500' : 'bg-yellow-600'
    return color === 'green' ? 'bg-green-500' : 'bg-orange-500'
  }

  const getBucketTextColor = (key: string) => {
    if (key === 'overdue') return 'text-red-400'
    if (key === 'days_0_7') return 'text-yellow-400'
    return color === 'green' ? 'text-green-400' : 'text-orange-400'
  }

  return (
    <div className="bg-neutral-900 border border-neutral-800 rounded-2xl shadow-lg p-6">
      <h3 className="text-lg font-semibold text-neutral-100 mb-4">{title}</h3>

      <div className="space-y-4">
        {buckets.map(bucket => {
          const percentage = maxAmount > 0 ? (bucket.amount / maxAmount) * 100 : 0
          const hasDocuments = bucket.documents > 0

          return (
            <div key={bucket.key} className="space-y-2">
              <div className="flex items-center justify-between text-sm">
                <div className="flex items-center gap-2">
                  <span className={`font-medium ${getBucketTextColor(bucket.key)}`}>
                    {bucket.label}
                  </span>
                  {hasDocuments && (
                    <span className="text-xs text-neutral-500">
                      ({bucket.documents} {bucket.documents === 1 ? 'documento' : 'documentos'})
                    </span>
                  )}
                </div>
                <span className="font-semibold text-neutral-100">
                  {formatCurrency(bucket.amount)}
                </span>
              </div>

              <div className="w-full bg-neutral-800 rounded-full h-3 overflow-hidden">
                <div
                  className={`h-full ${getBucketColor(bucket.key)} transition-all duration-500 ease-out rounded-full`}
                  style={{ width: `${percentage}%` }}
                />
              </div>
            </div>
          )
        })}
      </div>

      <div className="mt-6 pt-4 border-t border-neutral-800">
        <div className="flex items-center justify-between">
          <span className="text-sm font-medium text-neutral-400">Total</span>
          <span className="text-lg font-bold text-neutral-100">
            {formatCurrency(buckets.reduce((sum, b) => sum + b.amount, 0))}
          </span>
        </div>
        <div className="flex items-center justify-between mt-1">
          <span className="text-xs text-neutral-500">Documentos</span>
          <span className="text-sm font-semibold text-neutral-300">
            {buckets.reduce((sum, b) => sum + b.documents, 0)}
          </span>
        </div>
      </div>
    </div>
  )
}

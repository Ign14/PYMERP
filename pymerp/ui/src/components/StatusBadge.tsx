const STATUS_LABELS: Record<string, { label: string; className: string }> = {
  OK: { label: 'OK', className: 'bg-green-950 text-green-400 border-green-800' },
  BAJO_STOCK: { label: 'Bajo stock', className: 'bg-yellow-950 text-yellow-400 border-yellow-800' },
  POR_VENCER: { label: 'Por vencer', className: 'bg-orange-950 text-orange-400 border-orange-800' },
  VENCIDO: { label: 'Vencido', className: 'bg-red-950 text-red-400 border-red-800' },
}

type StatusBadgeProps = {
  status: string
}

export default function StatusBadge({ status }: StatusBadgeProps) {
  const normalized = status?.trim().toUpperCase() ?? ''
  const config = STATUS_LABELS[normalized] ?? {
    label: status || 'Desconocido',
    className: 'bg-neutral-950 text-neutral-300 border-neutral-800',
  }

  return (
    <span
      className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold border ${config.className}`}
    >
      {config.label}
    </span>
  )
}

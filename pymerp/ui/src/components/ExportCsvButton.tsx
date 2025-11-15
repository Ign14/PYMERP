import type { MouseEventHandler, ReactNode } from 'react'

export type CsvField<Row extends Record<string, unknown>> = {
  key: keyof Row
  label: string
  format?: (value: Row[keyof Row], row: Row) => string
}

type ExportCsvButtonProps<Row extends Record<string, unknown>> = {
  filename: string
  fields: CsvField<Row>[]
  data: Row[]
  disabled?: boolean
  className?: string
  children?: ReactNode
}

function escapeCsvValue(value: string) {
  const escaped = value.replace(/"/g, '""')
  return `"${escaped}"`
}

export default function ExportCsvButton<Row extends Record<string, unknown> = Record<string, unknown>>({
  filename,
  fields,
  data,
  disabled,
  className,
  children,
}: ExportCsvButtonProps<Row>) {
  const headers = fields.map(field => escapeCsvValue(field.label)).join(',')

  const handleExport: MouseEventHandler<HTMLButtonElement> = event => {
    event.preventDefault()
    if (disabled) return
    const rows = data.map(row =>
      fields
        .map(field => {
          const value = row[field.key]
          const cellValue = field.format ? field.format(value, row) : value
          const text = cellValue === null || cellValue === undefined ? '' : String(cellValue)
          return escapeCsvValue(text)
        })
        .join(',')
    )
    const csv = [headers, ...rows].join('\r\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = filename
    anchor.click()
    URL.revokeObjectURL(url)
  }

  return (
    <button
      className={`btn ghost small ${className ?? ''}`.trim()}
      type="button"
      onClick={handleExport}
      disabled={disabled}
    >
      {children ?? 'Exportar CSV'}
    </button>
  )
}

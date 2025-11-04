export type SampleSalesPoint = {
  date: string
  total: number
}

const SAMPLE_TOTALS: number[] = [
  945000, 1012000, 1125000, 986000, 1324000, 1432000, 1378000, 1250000, 1198000, 1505000, 1623000,
  1587000, 1402000, 1334000, 1298000, 1186000, 1215000, 1358000, 1422000, 1486000, 1550000, 1604000,
  1513000, 1459000, 1395000, 1282000, 1347000, 1418000, 1492000,
]

export function getSampleSalesTimeseries(days = 14): SampleSalesPoint[] {
  if (days <= 0) {
    return []
  }

  const today = new Date()
  today.setHours(0, 0, 0, 0)

  return Array.from({ length: days }, (_, index) => {
    const current = new Date(today)
    const diff = days - 1 - index
    current.setDate(today.getDate() - diff)
    const total = SAMPLE_TOTALS[index % SAMPLE_TOTALS.length]

    return {
      date: formatLocalDate(current),
      total,
    }
  })
}

function formatLocalDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

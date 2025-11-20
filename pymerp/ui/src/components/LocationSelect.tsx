import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getLocations, type Location } from '../services/client'

export const ACTIVE_LOCATION_QUERY_KEY = ['locations', 'active'] as const

type LocationSelectProps = {
  value: string | null
  onChange: (locationId: string | null) => void
  disabled?: boolean
  className?: string
  name?: string
  id?: string
}

export default function LocationSelect({
  value,
  onChange,
  disabled,
  className,
  name,
  id,
}: LocationSelectProps) {
  const { data: locations = [], isLoading } = useQuery<Location[]>({
    queryKey: ACTIVE_LOCATION_QUERY_KEY,
    queryFn: () => getLocations({ status: 'ACTIVE' }),
  })

  const sortedLocations = useMemo(
    () =>
      [...locations].sort((a, b) => {
        const aLabel = `${a.code ?? ''}${a.name}`.toLowerCase()
        const bLabel = `${b.code ?? ''}${b.name}`.toLowerCase()
        return aLabel.localeCompare(bLabel, 'es')
      }),
    [locations]
  )

  return (
    <select
      className={className ?? 'input'}
      value={value ?? ''}
      onChange={event => onChange(event.target.value ? event.target.value : null)}
      disabled={disabled || isLoading}
      name={name}
      id={id}
    >
      <option value="">DEFAULT (automático)</option>
      {sortedLocations.map(location => (
        <option key={location.id} value={location.id}>
          {location.code ? `${location.code} - ${location.name}` : location.name}
        </option>
      ))}
    </select>
  )
}

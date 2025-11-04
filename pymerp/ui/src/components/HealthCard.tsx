import { useQuery } from '@tanstack/react-query'
import { fetchHealth, type HealthResponse } from '../services/client'

export default function HealthCard() {
  const { data, isLoading, isError, error, refetch, isFetching } = useQuery<HealthResponse, Error>({
    queryKey: ['health'],
    queryFn: fetchHealth,
    refetchOnWindowFocus: false,
  })

  return (
    <div className="card">
      <div className="card-header">
        <h2>Health</h2>
        <button className="btn-link" onClick={() => refetch()} disabled={isFetching}>
          {isFetching ? 'Refreshing...' : 'Refresh'}
        </button>
      </div>

      {isLoading && <p>Loading...</p>}
      {isError && <p className="error">{error?.message ?? 'Health check failed'}</p>}

      {!isLoading && !isError && data && (
        <>
          <p>
            Status: <span className={data.status === 'UP' ? 'ok' : 'error'}>{data.status}</span>
          </p>
          <pre className="json">{JSON.stringify(data, null, 2)}</pre>
        </>
      )}
    </div>
  )
}

import { useQuery } from '@tanstack/react-query'
import { getFrequentProducts, type FrequentProduct } from '../services/client'

export function useFrequentProducts(customerId?: string) {
  return useQuery<FrequentProduct[]>({
    queryKey: ['frequent-products', customerId],
    queryFn: () => {
      if (!customerId) {
        throw new Error('customerId is required')
      }
      return getFrequentProducts(customerId)
    },
    enabled: Boolean(customerId),
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  })
}

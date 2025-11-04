import { mergeCustomerPages, computeNextPageParam } from '../customersUtils'
import type { Customer, Page } from '../../services/client'

describe('customersUtils', () => {
  const makeCustomer = (id: string): Customer =>
    ({
      id,
      name: `Customer ${id}`,
      email: `${id}@example.com`,
    }) as Customer

  const makePage = (
    content: Customer[],
    number: number,
    totalPages: number,
    hasNext?: boolean
  ): Page<Customer> => ({
    content,
    number,
    size: 20,
    totalElements: 60,
    totalPages,
    hasNext,
  })

  test('mergeCustomerPages removes duplicates and preserves order', () => {
    const pages: Page<Customer>[] = [
      makePage([makeCustomer('1'), makeCustomer('2')], 0, 3),
      makePage([makeCustomer('2'), makeCustomer('3')], 1, 3),
      makePage([makeCustomer('4')], 2, 3),
    ]
    const result = mergeCustomerPages(pages)
    expect(result.map(c => c.id)).toEqual(['1', '2', '3', '4'])
  })

  test('computeNextPageParam respects hasNext flag when provided', () => {
    const page = makePage([makeCustomer('1')], 0, 3, false)
    expect(computeNextPageParam(page)).toBeUndefined()
  })

  test('computeNextPageParam falls back to page.totalPages and page number', () => {
    const page = makePage([makeCustomer('1')], 1, 3)
    expect(computeNextPageParam(page)).toBe(2)
  })

  test('computeNextPageParam handles legacy page field', () => {
    const legacy = {
      content: [makeCustomer('1')],
      page: 0,
      size: 20,
      totalElements: 20,
      totalPages: 1,
    } as unknown as Page<Customer>
    expect(computeNextPageParam(legacy)).toBeUndefined()
  })
})

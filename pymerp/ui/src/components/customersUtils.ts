import type { Customer, Page } from '../services/client';

export function mergeCustomerPages(pages: Page<Customer>[]): Customer[] {
  const seen = new Set<string>();
  const results: Customer[] = [];
  for (const page of pages) {
    const items = page.content ?? [];
    for (const customer of items) {
      if (!seen.has(customer.id)) {
        seen.add(customer.id);
        results.push(customer);
      }
    }
  }
  return results;
}

export function computeNextPageParam(page: Page<Customer>): number | undefined {
  const fallback = (page as unknown as { page?: number }).page;
  const current = typeof page.number === 'number' ? page.number : typeof fallback === 'number' ? fallback : 0;
  const totalPages = typeof page.totalPages === 'number' ? page.totalPages : 0;
  const hasNextFlag = typeof page.hasNext === 'boolean' ? page.hasNext : (totalPages > 0 ? current + 1 < totalPages : false);
  return hasNextFlag ? current + 1 : undefined;
}

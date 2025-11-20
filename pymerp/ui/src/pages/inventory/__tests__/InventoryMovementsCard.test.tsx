import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi, beforeEach } from 'vitest'

const getMovementsMock = vi.fn()
const listProductsMock = vi.fn()

vi.mock('../../../services/inventory', async () => {
  const actual = await vi.importActual<typeof import('../../../services/inventory')>(
    '../../../services/inventory'
  )
  return {
    ...actual,
    getMovements: (...args: Parameters<typeof actual.getMovements>) => getMovementsMock(...args),
  }
})

vi.mock('../../../services/client', async () => {
  const actual = await vi.importActual<typeof import('../../../services/client')>(
    '../../../services/client'
  )
  return {
    ...actual,
    listProducts: (...args: Parameters<typeof actual.listProducts>) => listProductsMock(...args),
  }
})

import InventoryMovementsCard from '../InventoryMovementsCard'

function renderComponent() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <InventoryMovementsCard />
    </QueryClientProvider>
  )
}

beforeEach(() => {
  getMovementsMock.mockReset()
  listProductsMock.mockReset()
})

describe('InventoryMovementsCard', () => {
  it('renderiza movimientos y filtros básicos', async () => {
    const movementPage = {
      content: [
        {
          id: 'move-1',
          type: 'PURCHASE_IN',
          qtyChange: 12,
          beforeQty: 5,
          afterQty: 17,
          productId: 'prod-1',
          lotId: 'lot-1',
          locationFrom: { id: 'loc-1', name: 'Bodega A' },
          locationTo: { id: 'loc-2', name: 'Bodega B' },
          userId: 'usuario-demo',
          traceId: 'trace-123',
          refType: 'PURCHASE',
          refId: 'purchase-1',
          createdAt: '2024-04-15T09:00:00Z',
          reasonCode: 'PURCHASE_IN',
          note: 'Compra recibida',
        },
      ],
      totalElements: 1,
      totalPages: 1,
      size: 12,
      number: 0,
    }
    const productPage = {
      content: [
        {
          id: 'prod-1',
          sku: 'SKU-001',
          name: 'Producto demo',
          active: true,
        },
      ],
      totalElements: 1,
      totalPages: 1,
      size: 1,
      number: 0,
    }

    getMovementsMock.mockResolvedValueOnce(movementPage)
    listProductsMock.mockResolvedValueOnce(productPage)

    renderComponent()

    await waitFor(() => expect(screen.getByText('lot-1')).toBeInTheDocument())
    expect(screen.getByText('trace-123')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Exportar CSV/i })).toBeInTheDocument()
    expect(screen.getByPlaceholderText(/Filtrar por lote/i)).toBeInTheDocument()
  })
})

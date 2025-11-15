import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type React from 'react'
import ProductStockByLocationChips from '../ProductStockByLocationChips'
import { getStockByProduct } from '../../services/inventory'

vi.mock('../../services/inventory', () => ({
  getStockByProduct: vi.fn(),
}))

const getStockByProductMock = getStockByProduct as unknown as vi.Mock

function renderWithQueryClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  })

  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>)
}

beforeEach(() => {
  getStockByProductMock.mockReset()
})

describe('ProductStockByLocationChips', () => {
  it('renders chips for positive stock per location', async () => {
    getStockByProductMock.mockResolvedValueOnce([
      { productId: 'prod-1', locationId: 'loc-1', locationName: 'Bodega principal', availableQty: 5 },
      { productId: 'prod-1', locationId: 'loc-2', locationName: 'Mostrador', availableQty: 3 },
    ])

    renderWithQueryClient(<ProductStockByLocationChips productId="prod-1" />)

    expect(await screen.findByText('Bodega principal: 5')).toBeInTheDocument()
    expect(screen.getByText('Mostrador: 3')).toBeInTheDocument()
  })

  it('shows overflow chip when there are more locations than the display limit', async () => {
    const data = [
      { productId: 'prod-2', locationId: 'loc-1', locationName: 'Bodega 1', availableQty: 10 },
      { productId: 'prod-2', locationId: 'loc-2', locationName: 'Bodega 2', availableQty: 8 },
      { productId: 'prod-2', locationId: 'loc-3', locationName: 'Bodega 3', availableQty: 4 },
      { productId: 'prod-2', locationId: 'loc-4', locationName: 'Sucursal', availableQty: 2 },
      { productId: 'prod-2', locationId: 'loc-5', locationName: 'Depósito', availableQty: 1 },
    ]
    getStockByProductMock.mockResolvedValueOnce(data)

    renderWithQueryClient(<ProductStockByLocationChips productId="prod-2" />)

    const overflowChip = await screen.findByTestId('product-stock-chip-overflow')
    expect(screen.getAllByTestId('product-stock-chip')).toHaveLength(3)
    expect(overflowChip).toHaveTextContent('+2')
    expect(overflowChip).toHaveAttribute('title', expect.stringContaining('Sucursal: 2'))
  })
})

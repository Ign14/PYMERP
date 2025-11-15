import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import InventoryReplenishmentPanel from '../InventoryReplenishmentPanel'
import * as client from '../../../services/client'
import * as Router from 'react-router-dom'
import { vi } from 'vitest'

const createQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  })

const mockProductPage = {
  content: [
    {
      id: 'prod-1',
      name: 'Producto estacional',
      sku: 'SKU-001',
      stock: 5,
      active: true,
    },
  ],
  number: 0,
  size: 1,
  totalElements: 1,
  totalPages: 1,
}

let mockNavigate = vi.fn()
let navigateSpy: ReturnType<typeof vi.spyOn>

beforeEach(() => {
  mockNavigate = vi.fn()
  navigateSpy = vi.spyOn(Router, 'useNavigate').mockReturnValue(mockNavigate)
  vi.spyOn(Math, 'random').mockReturnValue(0)
  vi.spyOn(client, 'listProducts').mockResolvedValue(mockProductPage)
})

afterEach(() => {
  vi.restoreAllMocks()
})

test('navega al registro de compras con parámetros del producto', async () => {
  render(
    <QueryClientProvider client={createQueryClient()}>
      <InventoryReplenishmentPanel />
    </QueryClientProvider>
  )

  const [button] = await screen.findAllByRole('button', { name: /Generar Orden de Compra/i })
  fireEvent.click(button)

  await waitFor(() => expect(mockNavigate).toHaveBeenCalled())
  expect(mockNavigate).toHaveBeenCalledWith(
    '/app/purchases/new?productId=prod-1&currentQty=5&reorderPoint=10&suggestedQty=25'
  )
})

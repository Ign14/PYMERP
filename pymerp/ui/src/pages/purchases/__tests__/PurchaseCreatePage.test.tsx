import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import PurchaseCreatePage from '../PurchaseCreatePage'
import * as client from '../../../services/client'
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
      name: 'Producto clave',
      sku: 'SKU-123',
      stock: 3,
      active: true,
    },
  ],
  number: 0,
  size: 1,
  totalElements: 1,
  totalPages: 1,
}

const mockSuppliers = [
  {
    id: 'supp-1',
    name: 'Proveedor sugerido',
    rut: '12.345.678-9',
  },
]

const mockLocations = [
  {
    id: 'loc-1',
    companyId: 'company-1',
    code: 'B01',
    name: 'Bodega Sur',
    type: 'BODEGA',
    status: 'ACTIVE',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
]

const mockServices = []

beforeEach(() => {
  vi.spyOn(client, 'listProducts').mockResolvedValue(mockProductPage)
  vi.spyOn(client, 'listSuppliers').mockResolvedValue(mockSuppliers)
  vi.spyOn(client, 'listLocations').mockResolvedValue(mockLocations)
  vi.spyOn(client, 'listServices').mockResolvedValue(mockServices)
})

afterEach(() => {
  vi.restoreAllMocks()
})

test('prellenado desde los query params', async () => {
  render(
    <MemoryRouter
      initialEntries={[
        '/app/purchases/new?productId=prod-1&currentQty=3&reorderPoint=10&locationId=loc-1&suggestedSupplierId=supp-1',
      ]}
    >
      <QueryClientProvider client={createQueryClient()}>
        <Routes>
          <Route path="/app/purchases/new" element={<PurchaseCreatePage />} />
        </Routes>
      </QueryClientProvider>
    </MemoryRouter>
  )

  const quantityInput = await screen.findByLabelText(/Cantidad \*/i)
  expect((quantityInput as HTMLInputElement).value).toBe('7')

  const supplierSelect = await screen.findByRole('combobox', { name: /Proveedor \*/i })
  expect((supplierSelect as HTMLSelectElement).value).toBe('supp-1')

  const locationSelect = await screen.findByRole('combobox', { name: /Ubicación de destino/i })
  expect((locationSelect as HTMLSelectElement).value).toBe('loc-1')
})

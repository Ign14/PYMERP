import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi, beforeEach } from 'vitest'

const getLotsMock = vi.fn()
const getLocationsMock = vi.fn()
const assignLotLocationMock = vi.fn()
const listProductsMock = vi.fn()
const listSuppliersMock = vi.fn()

vi.mock('../../../services/inventory', async () => {
  const actual = await vi.importActual<typeof import('../../../services/inventory')>(
    '../../../services/inventory'
  )
  return {
    ...actual,
    getLots: (...args: Parameters<typeof actual.getLots>) => getLotsMock(...args),
    getLocations: (...args: Parameters<typeof actual.getLocations>) => getLocationsMock(...args),
    assignLotLocation: (...args: Parameters<typeof actual.assignLotLocation>) =>
      assignLotLocationMock(...args),
  }
})

vi.mock('../../../services/client', async () => {
  const actual = await vi.importActual<typeof import('../../../services/client')>(
    '../../../services/client'
  )
  return {
    ...actual,
    listProducts: (...args: Parameters<typeof actual.listProducts>) => listProductsMock(...args),
    listSuppliers: (...args: Parameters<typeof actual.listSuppliers>) =>
      listSuppliersMock(...args),
  }
})

import LotsListPage from '../LotsListPage'

function renderComponent() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <MemoryRouter>
      <QueryClientProvider client={queryClient}>
        <LotsListPage />
      </QueryClientProvider>
    </MemoryRouter>
  )
}

beforeEach(() => {
  getLotsMock.mockReset()
  getLocationsMock.mockReset()
  assignLotLocationMock.mockReset()
  listProductsMock.mockReset()
  listSuppliersMock.mockReset()
})

describe('LotsListPage', () => {
  it('muestra filtros, tabla y badges de estado', async () => {
    const lotPage = {
      content: [
        {
          lotId: 'lot-1',
          product: { id: 'prod-1', name: 'Demo Product', sku: 'SKU-001' },
          supplier: { id: 'sup-1', name: 'Proveedor demo' },
          location: { id: 'loc-1', name: 'Bodega central' },
          qtyAvailable: 8,
          qtyReserved: 2,
          status: 'BAJO_STOCK',
          fechaIngreso: '2024-04-01T00:00:00Z',
          fechaExpiracion: '2024-08-01T00:00:00Z',
        },
      ],
      totalElements: 1,
      totalPages: 1,
      size: 10,
      number: 0,
    }
    const locationPage = {
      content: [
        {
          id: 'loc-1',
          code: 'LOC-1',
          name: 'Bodega central',
          description: 'Principal',
          enabled: true,
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z',
        },
      ],
      totalElements: 1,
      totalPages: 1,
      size: 200,
      number: 0,
    }

    const productPage = {
      content: [
        {
          id: 'prod-1',
          name: 'Demo Product',
          sku: 'SKU-001',
          active: true,
        },
      ],
      totalElements: 1,
      totalPages: 1,
      size: 10,
      number: 0,
    }
    const suppliers = [{ id: 'sup-1', name: 'Proveedor demo' }]

    getLotsMock.mockResolvedValueOnce(lotPage)
    getLocationsMock.mockResolvedValueOnce(locationPage)
    listProductsMock.mockResolvedValueOnce(productPage)
    listSuppliersMock.mockResolvedValueOnce(suppliers)

    renderComponent()

    await waitFor(() => expect(screen.getByText('lot-1')).toBeInTheDocument())
    expect(screen.getAllByText('Bajo stock').length).toBeGreaterThan(0)
    expect(screen.getByRole('button', { name: /LOC-1 · Bodega central/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Asignar ubicación/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Ver movimientos/i })).toBeInTheDocument()
    expect(screen.getAllByRole('combobox').length).toBeGreaterThanOrEqual(4)
  })
})

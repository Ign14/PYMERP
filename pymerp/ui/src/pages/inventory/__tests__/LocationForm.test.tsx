import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi, beforeEach } from 'vitest'

const createLocationMock = vi.fn()
const updateLocationMock = vi.fn()

vi.mock('../../../services/inventory', async () => {
  const actual = await vi.importActual<typeof import('../../../services/inventory')>(
    '../../../services/inventory'
  )
  return {
    ...actual,
    createLocation: (...args: Parameters<typeof actual.createLocation>) =>
      createLocationMock(...args),
    updateLocation: (...args: Parameters<typeof actual.updateLocation>) =>
      updateLocationMock(...args),
  }
})

import LocationForm from '../components/LocationForm'

function renderForm(props: Partial<Parameters<typeof LocationForm>[0]> = {}) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <div id="root">
        <LocationForm open {...props} />
      </div>
    </QueryClientProvider>
  )
}

beforeEach(() => {
  createLocationMock.mockReset()
  updateLocationMock.mockReset()
})

describe('LocationForm', () => {
  it('crea una ubicación con datos válidos', async () => {
    const saved = vi.fn()
    const closed = vi.fn()
    createLocationMock.mockResolvedValue({
      id: 'loc-1',
      name: 'Bodega Demo',
      code: 'BOD',
      description: null,
      enabled: true,
      createdAt: '',
      updatedAt: '',
    })

    renderForm({ location: null, onSaved: saved, onClose: closed })

    const nameInput = screen.getByLabelText(/Nombre \*/i)
    await userEvent.type(nameInput, 'Bodega Demo')
    const codeInput = screen.getByLabelText(/Código/i)
    await userEvent.type(codeInput, 'bod-1')

    const form = screen.getByRole('form')
    await act(async () => {
      fireEvent.submit(form)
    })

    await waitFor(() => expect(createLocationMock).toHaveBeenCalled())
    expect(createLocationMock).toHaveBeenCalledWith({
      name: 'Bodega Demo',
      code: 'BOD-1',
      description: undefined,
      enabled: true,
    })
    await waitFor(() => expect(saved).toHaveBeenCalled())
    expect(closed).toHaveBeenCalled()
  })

  it('muestra error cuando backend indica duplicado', async () => {
    createLocationMock.mockRejectedValueOnce(new Error('Ya existe'))
    const saved = vi.fn()
    renderForm({ onSaved: saved })

    await userEvent.type(screen.getByLabelText(/Nombre \*/i), 'Bodega')
    await userEvent.type(screen.getByLabelText(/Código/i), 'BOD01')

    const form = screen.getByRole('form')
    await act(async () => {
      fireEvent.submit(form)
    })

    expect(await screen.findByText(/Ya existe/i)).toBeInTheDocument()
    expect(saved).not.toHaveBeenCalled()
  })
})

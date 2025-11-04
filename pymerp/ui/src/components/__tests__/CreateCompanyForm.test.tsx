import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import CreateCompanyForm from '../CreateCompanyForm'

const createCompanyMock = vi.fn()

vi.mock('../../services/client', async () => {
  const actual =
    await vi.importActual<typeof import('../../services/client')>('../../services/client')
  return {
    ...actual,
    createCompany: (...args: Parameters<typeof actual.createCompany>) => createCompanyMock(...args),
  }
})

describe('CreateCompanyForm', () => {
  beforeEach(() => {
    createCompanyMock.mockReset()
  })

  function renderForm() {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    })
    const user = userEvent.setup()
    return {
      user,
      ...render(
        <QueryClientProvider client={queryClient}>
          <CreateCompanyForm />
        </QueryClientProvider>
      ),
    }
  }

  it('muestra errores cuando los campos obligatorios están vacíos', async () => {
    const { user } = renderForm()

    await user.click(screen.getByRole('button', { name: /Crear/i }))

    expect(await screen.findByText(/La razón social es obligatoria/i)).toBeInTheDocument()
    expect(await screen.findByText(/El RUT es obligatorio/i)).toBeInTheDocument()
    expect(createCompanyMock).not.toHaveBeenCalled()
  })

  it('valida el formato del email', async () => {
    const { user } = renderForm()

    await user.type(screen.getByLabelText(/Razón social/i), 'Compañía Demo')
    await user.type(screen.getByLabelText(/^RUT/i), '76.123.456-0')
    await user.type(screen.getByLabelText(/^Email/i), 'correo-inválido')

    await user.click(screen.getByRole('button', { name: /Crear/i }))

    expect(await screen.findByText(/Ingresa un email válido/i)).toBeInTheDocument()
    expect(createCompanyMock).not.toHaveBeenCalled()
  })

  it('normaliza y envía los datos correctos', async () => {
    const { user } = renderForm()

    createCompanyMock.mockResolvedValueOnce({
      id: 'cmp-001',
      businessName: 'Comercial Demo',
      rut: '76123456-0',
    })

    await user.type(screen.getByLabelText(/Razón social/i), '  Comercial Demo  ')
    await user.type(screen.getByLabelText(/^RUT/i), '76.123.456-0')
    await user.type(screen.getByLabelText(/Giro/i), 'Servicios')
    await user.type(screen.getByLabelText(/^Dirección/i), 'Av. Central 123')
    await user.type(screen.getByLabelText(/^Comuna/i), 'Providencia')
    await user.type(screen.getByLabelText(/^Teléfono/i), '+56 9 1234 5678')
    await user.type(screen.getByLabelText(/^Email/i), ' Ventas@EMPRESA.cl ')
    await user.type(screen.getByLabelText(/Mensaje personalizado/i), '¡Gracias!')

    await user.click(screen.getByRole('button', { name: /Crear/i }))

    await waitFor(() => expect(createCompanyMock).toHaveBeenCalled())
    expect(createCompanyMock.mock.calls[0][0]).toEqual({
      businessName: 'Comercial Demo',
      rut: '76123456-0',
      businessActivity: 'Servicios',
      address: 'Av. Central 123',
      commune: 'Providencia',
      phone: '+56 9 1234 5678',
      email: 'ventas@empresa.cl',
      receiptFooterMessage: '¡Gracias!',
    })
    expect(await screen.findByText(/Compañía creada correctamente/i)).toBeInTheDocument()
  })

  it('muestra errores del backend con ProblemDetail', async () => {
    const { user } = renderForm()

    createCompanyMock.mockRejectedValueOnce({
      isAxiosError: true,
      response: {
        data: {
          detail: 'Error de validación',
          errors: [{ field: 'rut', defaultMessage: 'El RUT ya existe' }],
        },
      },
    })

    await user.type(screen.getByLabelText(/Razón social/i), 'Compañía Demo')
    await user.type(screen.getByLabelText(/^RUT/i), '76.123.456-0')

    await user.click(screen.getByRole('button', { name: /Crear/i }))

    expect(await screen.findByText(/El RUT ya existe/i)).toBeInTheDocument()
    expect(await screen.findByText(/Error de validación/i)).toBeInTheDocument()
  })
})

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';

vi.mock('../../assets/logo.png', () => ({ default: 'logo.png' }));

const loginMock = vi.fn();
const submitAccountRequestMock = vi.fn();

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => ({
    login: loginMock,
    isAuthenticated: false,
  }),
}));

vi.mock('../../services/client', async () => {
  const actual = await vi.importActual<typeof import('../../services/client')>('../../services/client');
  return {
    ...actual,
    submitAccountRequest: (...args: Parameters<typeof actual.submitAccountRequest>) =>
      submitAccountRequestMock(...args),
  };
});

import LandingExperience from '../LandingExperience';

function renderLanding() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
  return render(
    <MemoryRouter>
      <QueryClientProvider client={queryClient}>
        <div id="root">
          <LandingExperience>
            <div>Children</div>
          </LandingExperience>
        </div>
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

function parseCaptchaAnswerFromInput(input: HTMLInputElement) {
  const label = input.getAttribute('aria-label') ?? '';
  const match = label.match(/(\d+) \+ (\d+)/);
  if (!match) {
    throw new Error(`No se pudo determinar el captcha desde "${label}"`);
  }
  const first = Number.parseInt(match[1], 10);
  const second = Number.parseInt(match[2], 10);
  return first + second;
}

beforeEach(() => {
  loginMock.mockReset();
  submitAccountRequestMock.mockReset();
});

async function openLoginPanel() {
  const user = userEvent.setup();
  renderLanding();
  const overlay = await screen.findByRole('presentation');
  await act(async () => {
    await user.click(overlay);
  });
  await screen.findByRole('dialog', { name: /Iniciar sesión/i });
  return user;
}

describe('LandingExperience login captcha', () => {
  it('bloquea el submit cuando el captcha es incorrecto', async () => {
    const user = await openLoginPanel();

    await user.type(await screen.findByLabelText(/^Email$/i), 'user@example.com');
    await user.type(screen.getByLabelText(/^Contraseña$/i), 'Secret123!');

    const captchaInput = screen.getByLabelText(/Captcha: ¿Cuánto es/i);
    const expected = parseCaptchaAnswerFromInput(captchaInput as HTMLInputElement);
    await user.type(captchaInput, String(expected + 1));

    const loginDialog = screen.getByRole('dialog', { name: /Iniciar sesión/i });
    const form = loginDialog.querySelector('form');
    if (!form) {
      throw new Error('No se encontró el formulario de login');
    }
    await act(async () => {
      fireEvent.submit(form);
    });

    await waitFor(() => {
      expect(loginMock).not.toHaveBeenCalled();
    });
    expect(await screen.findByText(/Debes resolver el captcha correctamente/i)).toBeInTheDocument();
  });

  it('envía el captcha correcto cuando se resuelve', async () => {
    const user = await openLoginPanel();

    await user.type(screen.getByLabelText(/^Email$/i), 'user@example.com');
    await user.type(screen.getByLabelText(/^Contraseña$/i), 'Secret123!');

    const captchaInput = screen.getByLabelText(/Captcha: ¿Cuánto es/i) as HTMLInputElement;
    const expected = parseCaptchaAnswerFromInput(captchaInput);
    loginMock.mockResolvedValueOnce(undefined);

    await user.type(captchaInput, String(expected));
    const loginDialog = screen.getByRole('dialog', { name: /Iniciar sesión/i });
    const form = loginDialog.querySelector('form');
    if (!form) {
      throw new Error('No se encontró el formulario de login');
    }
    await act(async () => {
      fireEvent.submit(form);
    });

    await waitFor(() => expect(loginMock).toHaveBeenCalled());
    expect(loginMock).toHaveBeenCalledWith({
      email: 'user@example.com',
      password: 'Secret123!',
      captcha: expect.objectContaining({ a: expect.any(Number), b: expect.any(Number), answer: String(expected) }),
    });
  });
});

describe('LandingExperience request form', () => {
  it('muestra el error del backend cuando el captcha es inválido', async () => {
    const user = userEvent.setup();
    renderLanding();
    const overlay = await screen.findByRole('presentation');
    await act(async () => {
      await user.click(overlay);
    });
    await screen.findByRole('dialog', { name: /Iniciar sesión/i });
    await act(async () => {
      await user.click(screen.getByRole('button', { name: /problemas para acceder/i }));
    });
    await screen.findByRole('dialog', { name: /Solicitud de registro o recuperación de cuenta/i });

    await user.type(screen.getByLabelText(/^RUT$/i), '12.345.678-5');
    await user.type(screen.getByLabelText(/^Nombre completo$/i), 'Usuario Demo');
    await user.type(screen.getByLabelText(/^Dirección$/i), 'Av. Demo 123');
    await user.type(screen.getByLabelText(/^Email$/i), 'demo@example.com');
    await user.type(screen.getByLabelText(/^Nombre de la empresa$/i), 'Comercial Demo');
    await user.type(screen.getByLabelText(/^Contraseña$/i), 'Secret123!');
    await user.type(screen.getByLabelText(/^Repetir contraseña$/i), 'Secret123!');

    const captchaInput = screen.getByLabelText(/Captcha: ¿Cuánto es/i) as HTMLInputElement;
    const expected = parseCaptchaAnswerFromInput(captchaInput);
    await user.type(captchaInput, String(expected));

    submitAccountRequestMock.mockRejectedValueOnce({
      isAxiosError: true,
      response: { data: { detail: 'Captcha inválido' } },
    });

    const requestDialog = screen.getByRole('dialog', { name: /Solicitud de registro o recuperación de cuenta/i });
    const form = requestDialog.querySelector('form');
    if (!form) {
      throw new Error('No se encontró el formulario de solicitud');
    }
    await act(async () => {
      fireEvent.submit(form);
    });

    await waitFor(() => expect(submitAccountRequestMock).toHaveBeenCalled());
    expect(await screen.findByText(/Captcha inválido/i)).toBeInTheDocument();
  });
});

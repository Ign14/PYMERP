import type { ComponentProps } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import CustomerSelect from "../CustomerSelect";
import type { Customer, Page } from "../../services/client";

const listCustomersMock = vi.fn();
const createCustomerMock = vi.fn();

vi.mock("../../services/client", async () => {
  const actual = await vi.importActual<typeof import("../../services/client")>("../../services/client");
  return {
    ...actual,
    listCustomers: (...args: Parameters<typeof actual.listCustomers>) => listCustomersMock(...args),
    createCustomer: (...args: Parameters<typeof actual.createCustomer>) => createCustomerMock(...args),
  };
});

function createPage(content: Customer[], page = 0): Page<Customer> {
  return {
    content,
    totalElements: content.length,
    totalPages: 1,
    size: content.length || 1,
    number: page,
  };
}

describe("CustomerSelect", () => {
  const baseCustomers: Customer[] = [
    {
      id: "cus-001",
      name: "Cafetería Plaza",
      document: "76.123.456-0",
      email: "contacto@cafeteriaplaza.cl",
    },
    {
      id: "cus-002",
      name: "MiniMarket Central",
      document: "77.987.654-3",
      email: "compras@mmcentral.cl",
    },
  ];

  beforeEach(() => {
    listCustomersMock.mockReset();
    createCustomerMock.mockReset();
    listCustomersMock.mockResolvedValue(createPage(baseCustomers));
  });

  function renderComponent(props: Partial<ComponentProps<typeof CustomerSelect>> = {}) {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });
    const user = userEvent.setup();
    render(
      <QueryClientProvider client={queryClient}>
        <CustomerSelect
          value={null}
          onSelect={vi.fn()}
          label="Cliente"
          required
          debounceMs={0}
          {...props}
        />
      </QueryClientProvider>,
    );
    return { user };
  }

  it("muestra coincidencias y permite seleccionar un cliente", async () => {
    const onSelect = vi.fn();
    const { user } = renderComponent({ onSelect });

    await waitFor(() => expect(listCustomersMock).toHaveBeenCalled());

    await user.click(screen.getByRole("textbox", { name: /cliente/i }));

    const option = await screen.findByRole("option", { name: /MiniMarket Central/i });
    await user.click(option);

    expect(onSelect).toHaveBeenCalledWith(
      expect.objectContaining({ id: "cus-002", name: "MiniMarket Central" }),
    );
    await waitFor(() => expect(screen.queryByRole("listbox")).not.toBeInTheDocument());
  });

  it("muestra mensaje cuando no hay resultados y abre el formulario de creación", async () => {
    listCustomersMock.mockResolvedValueOnce(createPage([]));
    const { user } = renderComponent();

    await waitFor(() => expect(listCustomersMock).toHaveBeenCalled());
    await user.click(screen.getByRole("textbox", { name: /cliente/i }));

    const emptyState = await screen.findByText(/Sin resultados\. ¿Crear cliente\?/i);
    expect(emptyState).toBeInTheDocument();

    const dialogTrigger = screen.getByRole("button", { name: /Crear cliente/i });
    await user.click(dialogTrigger);

    expect(await screen.findByRole("dialog", { name: /Nuevo cliente/i })).toBeInTheDocument();
  });

  it("valida los campos mínimos del formulario de creación", async () => {
    listCustomersMock.mockResolvedValueOnce(createPage([]));
    const { user } = renderComponent();

    await waitFor(() => expect(listCustomersMock).toHaveBeenCalled());
    await user.click(screen.getByRole("textbox", { name: /cliente/i }));
    await user.click(screen.getByRole("button", { name: /Crear cliente/i }));

    const dialog = await screen.findByRole("dialog", { name: /Nuevo cliente/i });
    const saveButton = within(dialog).getByRole("button", { name: /Guardar/i });
    await user.click(saveButton);

    expect(await within(dialog).findByText(/El nombre debe tener al menos 3 caracteres/i)).toBeInTheDocument();

    await user.type(within(dialog).getByLabelText(/^Nombre/i), "AB");
    await user.type(within(dialog).getByLabelText(/^RUT/i), "123");
    await user.click(saveButton);

    expect(await within(dialog).findByText(/Ingresa un RUT válido/i)).toBeInTheDocument();
  });

  it("crea un nuevo cliente y lo selecciona automáticamente", async () => {
    listCustomersMock.mockResolvedValueOnce(createPage([]));
    const onSelect = vi.fn();
    const { user } = renderComponent({ onSelect });

    await waitFor(() => expect(listCustomersMock).toHaveBeenCalled());
    await user.click(screen.getByRole("textbox", { name: /cliente/i }));
    await user.click(screen.getByRole("button", { name: /Crear cliente/i }));

    createCustomerMock.mockResolvedValueOnce({
      id: "cus-999",
      name: "Cliente Demo",
      document: "12345678-5",
      email: "cliente@demo.cl",
    });

    const dialog = await screen.findByRole("dialog", { name: /Nuevo cliente/i });
    await user.type(within(dialog).getByLabelText(/^Nombre/i), "Cliente Demo");
    await user.type(within(dialog).getByLabelText(/^RUT/i), "12.345.678-5");
    await user.type(within(dialog).getByLabelText(/^Email/i), "cliente@demo.cl");

    await user.click(within(dialog).getByRole("button", { name: /Guardar/i }));

    await waitFor(() => expect(createCustomerMock).toHaveBeenCalled());
    expect(createCustomerMock.mock.calls[0][0]).toMatchObject({
      name: "Cliente Demo",
      document: "12345678-5",
      email: "cliente@demo.cl",
    });

    await waitFor(() => expect(onSelect).toHaveBeenCalledWith(
      expect.objectContaining({ id: "cus-999", name: "Cliente Demo" }),
    ));

    expect(await screen.findByText(/Cliente creado/i)).toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: /cliente/i })).toHaveValue("Cliente Demo");
  });
});

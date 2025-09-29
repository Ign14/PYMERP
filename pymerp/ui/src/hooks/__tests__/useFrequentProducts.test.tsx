import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { type ReactNode } from "react";
import { afterEach, describe, expect, it, vi, type Mock } from "vitest";
import { useFrequentProducts } from "../useFrequentProducts";
import { getFrequentProducts, type FrequentProduct } from "../../services/client";

vi.mock("../../services/client", async () => {
  const actual = await vi.importActual<typeof import("../../services/client")>("../../services/client");
  return {
    ...actual,
    getFrequentProducts: vi.fn(),
  };
});

const mockedGetFrequentProducts = getFrequentProducts as unknown as Mock;

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

describe("useFrequentProducts", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("obtiene los productos frecuentes del cliente", async () => {
    const sample: FrequentProduct[] = [
      {
        productId: "prd-1",
        name: "Café",
        sku: "SKU-1",
        lastPurchasedAt: new Date().toISOString(),
        totalPurchases: 3,
        avgQty: 4,
        lastUnitPrice: 1290,
        lastQty: 5,
      },
    ];

    mockedGetFrequentProducts.mockResolvedValueOnce(sample);

    const wrapper = createWrapper();
    const { result } = renderHook(() => useFrequentProducts("cus-1"), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(sample);
    expect(mockedGetFrequentProducts).toHaveBeenCalledWith("cus-1");
  });

  it("no ejecuta la consulta cuando no hay cliente", async () => {
    const wrapper = createWrapper();
    const { result } = renderHook(() => useFrequentProducts(undefined), { wrapper });

    expect(result.current.data).toBeUndefined();
    expect(result.current.isFetched).toBe(false);
    expect(mockedGetFrequentProducts).not.toHaveBeenCalled();
  });

  it("propaga los errores de la API", async () => {
    const error = new Error("Network error");
    mockedGetFrequentProducts.mockRejectedValueOnce(error);

    const wrapper = createWrapper();
    const { result } = renderHook(() => useFrequentProducts("cus-2"), { wrapper });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.error).toBe(error);
  });
});

import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type React from "react";
import ProductFormDialog from "../dialogs/ProductFormDialog";
import ProductQrModal from "../dialogs/ProductQrModal";
import ProductInventoryAlertModal from "../dialogs/ProductInventoryAlertModal";
import {
  createProduct,
  fetchProductQrBlob,
  updateProductInventoryAlert,
} from "../../services/client";

vi.mock("../../services/client", () => ({
  createProduct: vi.fn(),
  updateProduct: vi.fn(),
  fetchProductQrBlob: vi.fn(),
  updateProductInventoryAlert: vi.fn(),
}));

type ProductMock = {
  id: string;
  sku: string;
  name: string;
  description?: string;
  category?: string;
  barcode?: string;
  imageUrl?: string | null;
  qrUrl?: string | null;
  criticalStock?: number;
  currentPrice?: number;
  active: boolean;
};

const createProductMock = createProduct as unknown as vi.Mock;
const fetchProductQrBlobMock = fetchProductQrBlob as unknown as vi.Mock;
const updateProductInventoryAlertMock = updateProductInventoryAlert as unknown as vi.Mock;

const createObjectUrlSpy = vi
  .spyOn(global.URL, "createObjectURL")
  .mockImplementation(() => "blob:test");
const revokeObjectUrlSpy = vi
  .spyOn(global.URL, "revokeObjectURL")
  .mockImplementation(() => undefined);

const renderWithQueryClient = (ui: React.ReactElement) => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
};

beforeEach(() => {
  createProductMock.mockReset();
  fetchProductQrBlobMock.mockReset();
  updateProductInventoryAlertMock.mockReset();
  createObjectUrlSpy.mockClear();
  revokeObjectUrlSpy.mockClear();
});

describe("Product form dialog", () => {
  it("submits new product with uploaded image", async () => {
    const onSaved = vi.fn();
    const product: ProductMock = {
      id: "prd-1",
      sku: "SKU-001",
      name: "Demo",
      criticalStock: 0,
      active: true,
    };
    createProductMock.mockResolvedValue(product);

    renderWithQueryClient(
      <ProductFormDialog open product={null} onClose={() => undefined} onSaved={onSaved} />,
    );

    await userEvent.type(screen.getByLabelText(/SKU/i), " SKU-001 ");
    await userEvent.type(screen.getByLabelText(/Nombre/i), " Producto de prueba ");
    await userEvent.type(screen.getByLabelText(/Categoria/i), "Bebidas");
    await userEvent.type(screen.getByLabelText(/Codigo de barras/i), "123");
    await userEvent.type(screen.getByLabelText(/Descripcion/i), "Descripción corta");

    const file = new File(["binary"], "foto.png", { type: "image/png" });
    const fileInput = screen.getByLabelText(/Imagen/i);
    await userEvent.upload(fileInput, file);

    await userEvent.click(screen.getByRole("button", { name: /guardar/i }));

    await waitFor(() => {
      expect(createProductMock).toHaveBeenCalledTimes(1);
    });

    const payload = createProductMock.mock.calls[0][0];
    expect(payload).toMatchObject({
      sku: "SKU-001",
      name: "Producto de prueba",
      category: "Bebidas",
      barcode: "123",
    });
    expect(payload.imageFile).toBeInstanceOf(File);
    expect(payload.imageUrl).toBeNull();
    expect(onSaved).toHaveBeenCalledWith(product);
  });
});

describe("Product QR modal", () => {
  it("renders QR image and triggers download", async () => {
    const product: ProductMock = {
      id: "prd-qr",
      sku: "SKU-QR",
      name: "Producto QR",
      active: true,
    };
    fetchProductQrBlobMock
      .mockResolvedValueOnce(new Blob(["data"], { type: "image/png" }))
      .mockResolvedValueOnce(new Blob(["data"], { type: "image/png" }));

    renderWithQueryClient(
      <ProductQrModal open product={product as any} onClose={() => undefined} />,
    );

    await waitFor(() => {
      expect(screen.getByRole("img", { name: /QR/i })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole("button", { name: /descargar/i }));

    await waitFor(() => {
      expect(fetchProductQrBlobMock).toHaveBeenCalledWith(product.id, { download: true });
    });
  });
});

describe("Product inventory alert modal", () => {
  it("updates critical stock", async () => {
    const onSaved = vi.fn();
    const product: ProductMock = {
      id: "prd-alert",
      sku: "SKU-ALERT",
      name: "Producto alerta",
      criticalStock: 2,
      active: true,
    };
    updateProductInventoryAlertMock.mockResolvedValue({ ...product, criticalStock: 5 });

    renderWithQueryClient(
      <ProductInventoryAlertModal
        open
        product={product as any}
        onClose={() => undefined}
        onSaved={onSaved}
      />,
    );

    const input = screen.getByLabelText(/Stock crítico/i);
    await userEvent.clear(input);
    await userEvent.type(input, "5");

    await userEvent.click(screen.getByRole("button", { name: /guardar/i }));

    await waitFor(() => {
      expect(updateProductInventoryAlertMock).toHaveBeenCalledWith(product.id, 5);
    });
    expect(onSaved).toHaveBeenCalled();
  });
});

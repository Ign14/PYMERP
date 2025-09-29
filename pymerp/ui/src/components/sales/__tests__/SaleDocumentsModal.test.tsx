import { afterEach, describe, expect, it, vi } from "vitest";

vi.mock("../../../hooks/useSaleDocuments", () => ({
  useSaleDocuments: vi.fn(),
}));

import { fireEvent, render, screen } from "@testing-library/react";
import SaleDocumentsModal from "../SaleDocumentsModal";
import type { Page, SaleDocument } from "../../../services/client";
import { useSaleDocuments } from "../../../hooks/useSaleDocuments";

type MockedQueryResult = {
  data?: Page<SaleDocument>;
  error?: Error | null;
  isError?: boolean;
  isLoading?: boolean;
  isFetching?: boolean;
  refetch?: () => unknown;
};

const mockedUseSaleDocuments = vi.mocked(useSaleDocuments);

function mockQuery(result: MockedQueryResult) {
  mockedUseSaleDocuments.mockReturnValue({
    data: undefined,
    error: undefined,
    isError: false,
    isLoading: false,
    isFetching: false,
    refetch: vi.fn(),
    ...result,
  });
}

describe("SaleDocumentsModal", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("muestra el estado de carga", () => {
    mockQuery({ isLoading: true });

    render(<SaleDocumentsModal isOpen onClose={() => undefined} />);

    expect(screen.getByText(/Cargando documentos/i)).toBeInTheDocument();
  });

  it("renderiza la tabla con los documentos", () => {
    const documents: SaleDocument[] = [
      {
        id: "doc-1",
        documentNumber: "F-1001",
        date: "2024-09-01",
        customerName: "Cliente Demo",
        total: 125000,
        status: "emitida",
      },
      {
        id: "doc-2",
        documentNumber: "B-2044",
        date: "2024-09-03",
        customerName: "Otra Empresa",
        total: 89000,
        status: "cancelled",
      },
    ];
    const page: Page<SaleDocument> = {
      content: documents,
      totalElements: documents.length,
      totalPages: 1,
      size: 10,
      number: 0,
    };

    mockQuery({ data: page });

    render(<SaleDocumentsModal isOpen onClose={() => undefined} />);

    expect(screen.getByRole("columnheader", { name: /Número de documento/i })).toBeInTheDocument();
    expect(screen.getByText("F-1001")).toBeInTheDocument();
    expect(screen.getByText("B-2044")).toBeInTheDocument();
    expect(screen.getByText("Cliente Demo")).toBeInTheDocument();
  });

  it("permite reintentar cuando ocurre un error", () => {
    const refetch = vi.fn();
    const error = new Error("falló");
    mockQuery({ isError: true, error, refetch });

    render(<SaleDocumentsModal isOpen onClose={() => undefined} />);

    fireEvent.click(screen.getByRole("button", { name: /reintentar/i }));

    expect(refetch).toHaveBeenCalledTimes(1);
  });
});

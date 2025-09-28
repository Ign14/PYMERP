import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import SalesDashboardOverview from "../SalesDashboardOverview";
import { fetchSalesSummary, fetchSalesTimeseries } from "../../../services/reports";

vi.mock("../../../services/reports", () => ({
  fetchSalesSummary: vi.fn(),
  fetchSalesTimeseries: vi.fn(),
}));

const mockFetchSalesSummary = fetchSalesSummary as unknown as vi.MockedFunction<typeof fetchSalesSummary>;
const mockFetchSalesTimeseries = fetchSalesTimeseries as unknown as vi.MockedFunction<typeof fetchSalesTimeseries>;

const currencyFormatter = new Intl.NumberFormat("es-CL", {
  style: "currency",
  currency: "CLP",
  minimumFractionDigits: 0,
  maximumFractionDigits: 0,
});

function renderDashboard() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <SalesDashboardOverview days={14} />
    </QueryClientProvider>,
  );
}

describe("SalesDashboardOverview", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("renders zero state when there are no sales", async () => {
    mockFetchSalesSummary.mockResolvedValue({ total14d: 0, avgDaily14d: 0 });
    mockFetchSalesTimeseries.mockResolvedValue({ points: [] });

    renderDashboard();

    expect(await screen.findByRole("heading", { name: /Resumen de ventas/i })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /Tendencia de ventas/i })).toBeInTheDocument();

    await waitFor(() => expect(screen.getByTestId("sales-daily-average")).toHaveTextContent("$0"));
    expect(screen.getByTestId("sales-total-14d")).toHaveTextContent("$0");
    expect(screen.getAllByTestId("sales-trend-point")).toHaveLength(14);
    expect(screen.getByText(/Sin ventas registradas en los últimos días/i)).toBeInTheDocument();

    expect(screen.getByLabelText(/Desde/i)).toHaveAttribute("type", "date");
    expect(screen.getByLabelText(/Hasta/i)).toHaveAttribute("type", "date");
  });

  it("fills missing days with zeros", async () => {
    const frame = buildFrame(14);
    mockFetchSalesSummary.mockResolvedValue({ total14d: 300, avgDaily14d: 21.43 });
    mockFetchSalesTimeseries.mockResolvedValue({
      points: [
        { date: frame[5], total: 100 },
        { date: frame[13], total: 200 },
      ],
    });

    renderDashboard();

    await waitFor(() => expect(screen.getByTestId("sales-total-14d")).toHaveTextContent("$300"));

    const pointTexts = screen.getAllByTestId("sales-trend-point").map((item) => item.textContent ?? "");
    expect(pointTexts).toHaveLength(14);
    expect(pointTexts.some((text) => text.startsWith(`${frame[6]}: 0`))).toBe(true);
    expect(pointTexts.some((text) => text.startsWith(`${frame[5]}: 100`))).toBe(true);
    expect(pointTexts.some((text) => text.startsWith(`${frame[13]}: 200`))).toBe(true);
  });

  it("displays consecutive daily totals", async () => {
    const frame = buildFrame(14);
    const points = frame.map((date, index) => ({ date, total: (index + 1) * 10 }));
    const total = points.reduce((acc, point) => acc + point.total, 0);
    const average = total / 14;

    mockFetchSalesSummary.mockResolvedValue({ total14d: total, avgDaily14d: average });
    mockFetchSalesTimeseries.mockResolvedValue({ points });

    renderDashboard();

    await waitFor(() => expect(screen.getByTestId("sales-total-14d")).toHaveTextContent(currencyFormatter.format(total)));
    expect(screen.getByTestId("sales-daily-average")).toHaveTextContent(currencyFormatter.format(average));
    const trendPoints = screen.getAllByTestId("sales-trend-point");
    expect(trendPoints[0].textContent).toContain(`${frame[0]}: 10`);
    expect(trendPoints.at(-1)?.textContent).toContain(`${frame[13]}: 140`);
    expect(screen.queryByText(/Sin ventas registradas en los últimos días/i)).not.toBeInTheDocument();
  });
});

function buildFrame(days: number): string[] {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const dates: string[] = [];
  for (let i = days - 1; i >= 0; i -= 1) {
    const current = new Date(today);
    current.setDate(today.getDate() - i);
    dates.push(formatLocalDate(current));
  }
  return dates;
}

function formatLocalDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

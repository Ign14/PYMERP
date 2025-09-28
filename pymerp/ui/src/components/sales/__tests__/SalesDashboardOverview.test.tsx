import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import SalesDashboardOverview from "../SalesDashboardOverview";
import { getSampleSalesTimeseries } from "../../../data/sampleSalesTimeseries";

const currencyFormatter = new Intl.NumberFormat("es-CL", {
  style: "currency",
  currency: "CLP",
  minimumFractionDigits: 0,
  maximumFractionDigits: 0,
});

describe("SalesDashboardOverview", () => {
  it("muestra los totales del conjunto de datos de ejemplo", async () => {
    const points = getSampleSalesTimeseries(14);
    const total = points.reduce((acc, point) => acc + point.total, 0);
    const average = total / points.length;

    render(<SalesDashboardOverview days={14} />);

    expect(await screen.findByRole("heading", { name: /Resumen de ventas/i })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /Tendencia de ventas/i })).toBeInTheDocument();

    await waitFor(() =>
      expect(screen.getByTestId("sales-total-14d")).toHaveTextContent(currencyFormatter.format(total)),
    );
    expect(screen.getByTestId("sales-daily-average")).toHaveTextContent(currencyFormatter.format(average));
    expect(screen.getAllByTestId("sales-trend-point")).toHaveLength(points.length);
    expect(screen.queryByText(/Sin ventas registradas en los últimos días/i)).not.toBeInTheDocument();

    const rangeLabel = `Rango seleccionado: ${points[0].date} a ${points.at(-1)?.date}`;
    expect(screen.getAllByText(rangeLabel)).toHaveLength(2);
  });

  it("actualiza los indicadores al acotar el rango", async () => {
    const points = getSampleSalesTimeseries(14);
    const lastPoint = points.at(-1);
    if (!lastPoint) {
      throw new Error("Expected sample data");
    }

    render(<SalesDashboardOverview days={14} />);

    const startInput = await screen.findByLabelText(/Desde/i);
    const endInput = screen.getByLabelText(/Hasta/i);

    fireEvent.change(startInput, { target: { value: lastPoint.date } });
    fireEvent.change(endInput, { target: { value: lastPoint.date } });

    await waitFor(() =>
      expect(screen.getByTestId("sales-total-14d")).toHaveTextContent(currencyFormatter.format(lastPoint.total)),
    );
    expect(screen.getByTestId("sales-daily-average")).toHaveTextContent(currencyFormatter.format(lastPoint.total));
    expect(screen.getAllByTestId("sales-trend-point")).toHaveLength(1);

    const updatedRangeLabel = `Rango seleccionado: ${lastPoint.date}`;
    expect(screen.getAllByText(updatedRangeLabel)).toHaveLength(2);
  });
});

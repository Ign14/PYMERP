import client from "./client";

export type SalesSummaryResponse = {
  total14d: number;
  avgDaily14d: number;
};

export type SalesTimeseriesPoint = {
  date: string;
  total: number;
};

export type SalesTimeseriesResponse = {
  points: SalesTimeseriesPoint[];
};

export async function fetchSalesSummary(days = 14): Promise<SalesSummaryResponse> {
  const { data } = await client.get<SalesSummaryResponse>("/v1/reports/sales/summary", {
    params: { days },
  });
  return data;
}

export async function fetchSalesTimeseries(days = 14): Promise<SalesTimeseriesResponse> {
  const { data } = await client.get<SalesTimeseriesResponse>("/v1/reports/sales/timeseries", {
    params: { days, bucket: "day" },
  });
  return data;
}

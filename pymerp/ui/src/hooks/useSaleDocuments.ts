import { keepPreviousData, useQuery } from "@tanstack/react-query";
import {
  listSaleDocuments,
  type ListSaleDocumentsParams,
  type Page,
  type SaleDocument,
} from "../services/client";

export type UseSaleDocumentsParams = {
  page?: number;
  size?: number;
  sort?: string;
  enabled?: boolean;
};

export function useSaleDocuments({
  page = 0,
  size = 10,
  sort = "date,DESC",
  enabled = true,
}: UseSaleDocumentsParams = {}) {
  return useQuery<Page<SaleDocument>, Error>({
    queryKey: ["sale-documents", page, size, sort],
    queryFn: () =>
      listSaleDocuments({ page, size, sort } satisfies ListSaleDocumentsParams),
    enabled,
    placeholderData: keepPreviousData,
  });
}

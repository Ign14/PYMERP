import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { listDocuments, type DocumentSummary, type DocumentType, type ListDocumentsParams, type Page } from "../services/client";

export type UseDocumentsOptions = {
  page?: number;
  size?: number;
  enabled?: boolean;
  keepPrevious?: boolean;
};

export function useDocuments(type: DocumentType | undefined, options: UseDocumentsOptions = {}) {
  const { page = 0, size = 5, enabled = true, keepPrevious = true } = options;

  return useQuery<Page<DocumentSummary>, Error>({
    queryKey: ["documents", type, page, size],
    queryFn: () => {
      if (!type) {
        throw new Error("Debe especificar el tipo de documento.");
      }
      return listDocuments({ type, page, size } satisfies ListDocumentsParams);
    },
    enabled: Boolean(type) && enabled,
    placeholderData: keepPrevious ? keepPreviousData : undefined,
  });
}

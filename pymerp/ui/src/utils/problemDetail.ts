import axios from "axios";

export type ProblemDetailFieldError = {
  field?: string;
  defaultMessage?: string;
  message?: string;
  code?: string;
};

export type ProblemDetailResponse = {
  title?: string;
  detail?: string;
  message?: string;
  errors?: ProblemDetailFieldError[] | Record<string, unknown> | null;
};

export type ParsedProblemDetail = {
  message?: string;
  fieldErrors: Record<string, string>;
};

export function parseProblemDetail(error: unknown): ParsedProblemDetail {
  const result: ParsedProblemDetail = { fieldErrors: {} };

  if (axios.isAxiosError(error)) {
    const data = error.response?.data as ProblemDetailResponse | string | undefined;
    if (typeof data === "string") {
      if (data.trim().length > 0) {
        result.message = data;
      }
      return result;
    }
    if (data) {
      if (Array.isArray(data.errors)) {
        for (const fieldError of data.errors) {
          const field = typeof fieldError.field === "string" ? fieldError.field : undefined;
          const message = typeof fieldError.defaultMessage === "string" && fieldError.defaultMessage.trim().length > 0
            ? fieldError.defaultMessage
            : typeof fieldError.message === "string" && fieldError.message.trim().length > 0
              ? fieldError.message
              : undefined;
          if (field && message) {
            result.fieldErrors[field] = message;
          }
        }
      }
      if (!result.message) {
        if (typeof data.detail === "string" && data.detail.trim().length > 0) {
          result.message = data.detail;
        } else if (typeof data.message === "string" && data.message.trim().length > 0) {
          result.message = data.message;
        } else if (typeof data.title === "string" && data.title.trim().length > 0) {
          result.message = data.title;
        }
      }
      return result;
    }
  }

  if (error instanceof Error && error.message.trim().length > 0) {
    result.message = error.message;
  }
  return result;
}

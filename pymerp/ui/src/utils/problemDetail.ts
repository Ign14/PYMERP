import axios from 'axios'

export type ProblemDetailFieldError = {
  field?: string
  defaultMessage?: string
  message?: string
  code?: string
}

export type ProblemDetailResponse = {
  title?: string
  detail?: string
  message?: string
  field?: string
  errors?: ProblemDetailFieldError[] | Record<string, unknown> | null
}

export type ParsedProblemDetail = {
  message?: string
  fieldErrors: Record<string, string>
}

export function parseProblemDetail(error: unknown): ParsedProblemDetail {
  const result: ParsedProblemDetail = { fieldErrors: {} }

  if (axios.isAxiosError(error)) {
    const data = error.response?.data as ProblemDetailResponse | string | undefined
    if (typeof data === 'string') {
      if (data.trim().length > 0) {
        result.message = data
      }
      return result
    }
    if (data) {
      if (Array.isArray(data.errors)) {
        for (const fieldError of data.errors) {
          const field = typeof fieldError.field === 'string' ? fieldError.field : undefined
          const message = extractErrorMessage(fieldError)
          if (field && message) {
            result.fieldErrors[field] = message
          }
        }
      } else if (data.errors && typeof data.errors === 'object') {
        for (const [field, detail] of Object.entries(data.errors)) {
          if (typeof field !== 'string') continue
          const message = extractErrorMessage(detail)
          if (message) {
            result.fieldErrors[field] = message
          }
        }
      }
      if (!result.message) {
        if (typeof data.detail === 'string' && data.detail.trim().length > 0) {
          result.message = data.detail
        } else if (typeof data.message === 'string' && data.message.trim().length > 0) {
          result.message = data.message
        } else if (typeof data.title === 'string' && data.title.trim().length > 0) {
          result.message = data.title
        }
      }
      if (typeof data.field === 'string') {
        const messageForField = result.fieldErrors[data.field] ?? result.message
        if (messageForField) {
          result.fieldErrors[data.field] = messageForField
        }
      }
      return result
    }
  }

  if (error instanceof Error && error.message.trim().length > 0) {
    result.message = error.message
  }
  return result
}

function extractErrorMessage(detail: unknown): string | undefined {
  if (!detail) {
    return undefined
  }
  if (typeof detail === 'string') {
    const trimmed = detail.trim()
    return trimmed.length > 0 ? trimmed : undefined
  }
  if (Array.isArray(detail)) {
    for (const item of detail) {
      const message = extractErrorMessage(item)
      if (message) return message
    }
    return undefined
  }
  if (typeof detail === 'object') {
    const maybeMessage = (detail as ProblemDetailFieldError).defaultMessage
    if (typeof maybeMessage === 'string' && maybeMessage.trim().length > 0) {
      return maybeMessage
    }
    const alt = (detail as ProblemDetailFieldError).message
    if (typeof alt === 'string' && alt.trim().length > 0) {
      return alt
    }
  }
  return undefined
}

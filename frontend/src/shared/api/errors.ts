// The backend's GlobalExceptionHandler wraps every error response the same
// way, but it's a @RestControllerAdvice, not a per-endpoint OpenAPI response
// — springdoc doesn't document it, so openapi-fetch types `error` as
// `unknown`. This mirrors com.tabibma.shared.web.ErrorResponse by hand.
type ApiErrorBody = {
  code: string
  message: string
  details: { field: string; message: string }[]
  requestId: string
}

export type ApiError = { error: ApiErrorBody }

function isApiError(value: unknown): value is ApiError {
  if (typeof value !== 'object' || value === null || !('error' in value)) {
    return false
  }
  const body = (value as { error: unknown }).error
  return typeof body === 'object' && body !== null && 'code' in body && 'message' in body
}

export function getApiErrorCode(error: unknown): string | null {
  return isApiError(error) ? error.error.code : null
}

export function getApiErrorFieldMessages(error: unknown): Record<string, string> {
  if (!isApiError(error)) {
    return {}
  }
  return Object.fromEntries(error.error.details.map((d) => [d.field, d.message]))
}

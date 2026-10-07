/**
 * Pull a readable message out of a ProblemDetail or validation error response.
 */
export function getApiErrorMessage(error, fallback = "Something went wrong") {
  const data = error?.response?.data;
  if (error?.response?.status === 429) {
    return "Too many attempts. Please wait a minute and try again.";
  }
  if (error?.response?.status === 503) {
    return data?.detail || fallback;
  }
  if (data?.detail) {
    return data.detail;
  }
  const fieldErrors = data?.errors && Object.values(data.errors).flat();
  if (fieldErrors?.length) {
    return fieldErrors[0];
  }
  if (typeof data === "string" && data.trim()) {
    return data;
  }
  return fallback;
}

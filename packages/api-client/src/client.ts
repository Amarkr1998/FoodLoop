/**
 * Thin typed fetch wrapper over the API gateway. Not a full client generator
 * (paths/params/body typing comes from the generated `paths` types in
 * ./generated/<service>.d.ts) — this just centralizes base URL, auth header
 * injection, idempotency keys, and the platform's error envelope
 * ({code, message, traceId} — verified against live api-gateway responses).
 */

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
    public readonly traceId?: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export interface ApiClientConfig {
  /** Defaults to the api-gateway origin; override per environment. */
  baseUrl?: string;
  /** Called per-request so a fresh/refreshed token is always used. */
  getAccessToken: () => string | null | Promise<string | null>;
}

export interface RequestOptions {
  method?: "GET" | "POST" | "PATCH" | "PUT" | "DELETE";
  params?: Record<string, string | number | boolean | undefined>;
  body?: unknown;
  /** Required by the gateway on mutating calls where retries are plausible (§ API catalog). */
  idempotencyKey?: string;
  signal?: AbortSignal;
}

function buildUrl(baseUrl: string, path: string, params?: RequestOptions["params"]): string {
  const url = new URL(path.replace(/^\//, ""), baseUrl.endsWith("/") ? baseUrl : `${baseUrl}/`);
  if (params) {
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined) url.searchParams.set(key, String(value));
    }
  }
  return url.toString();
}

export function createApiClient(config: ApiClientConfig) {
  const baseUrl = config.baseUrl ?? "http://localhost:8080";

  async function request<TResponse>(path: string, options: RequestOptions = {}): Promise<TResponse> {
    const token = await config.getAccessToken();
    const headers: Record<string, string> = {
      Accept: "application/json",
    };
    if (token) headers.Authorization = `Bearer ${token}`;
    if (options.idempotencyKey) headers["Idempotency-Key"] = options.idempotencyKey;

    let body: BodyInit | undefined;
    if (options.body !== undefined) {
      headers["Content-Type"] = "application/json";
      body = JSON.stringify(options.body);
    }

    const response = await fetch(buildUrl(baseUrl, path, options.params), {
      method: options.method ?? "GET",
      headers,
      body,
      signal: options.signal,
    });

    if (response.status === 204) return undefined as TResponse;

    const text = await response.text();
    const data = text ? JSON.parse(text) : undefined;

    if (!response.ok) {
      const envelope = data as { code?: string; message?: string; traceId?: string } | undefined;
      throw new ApiError(
        response.status,
        envelope?.code ?? "UNKNOWN_ERROR",
        envelope?.message ?? response.statusText,
        envelope?.traceId,
      );
    }

    return data as TResponse;
  }

  return {
    get: <T>(path: string, options?: Omit<RequestOptions, "method" | "body">) =>
      request<T>(path, { ...options, method: "GET" }),
    post: <T>(path: string, body?: unknown, options?: Omit<RequestOptions, "method" | "body">) =>
      request<T>(path, { ...options, method: "POST", body }),
    patch: <T>(path: string, body?: unknown, options?: Omit<RequestOptions, "method" | "body">) =>
      request<T>(path, { ...options, method: "PATCH", body }),
    put: <T>(path: string, body?: unknown, options?: Omit<RequestOptions, "method" | "body">) =>
      request<T>(path, { ...options, method: "PUT", body }),
    delete: <T>(path: string, options?: Omit<RequestOptions, "method" | "body">) =>
      request<T>(path, { ...options, method: "DELETE" }),
  };
}

export type ApiClient = ReturnType<typeof createApiClient>;

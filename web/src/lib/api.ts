// Backend dùng CookieCsrfTokenRepository: token nằm trong cookie XSRF-TOKEN,
// gửi lại qua header X-XSRF-TOKEN cho mọi request làm thay đổi dữ liệu.
const BASE = '/api/v1'

export type ProblemDetail = { title?: string; status?: number; detail?: string; code?: string }

export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail | null

  constructor(status: number, problem: ProblemDetail | null) {
    super(problem?.title ?? `HTTP ${status}`)
    this.status = status
    this.problem = problem
  }
}

function readCookie(name: string): string | null {
  const match = document.cookie.split('; ').find((c) => c.startsWith(`${name}=`))
  return match ? decodeURIComponent(match.slice(name.length + 1)) : null
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers = new Headers({ Accept: 'application/json' })
  if (body !== undefined) headers.set('Content-Type', 'application/json')
  if (method !== 'GET') {
    const token = readCookie('XSRF-TOKEN')
    if (token) headers.set('X-XSRF-TOKEN', token)
  }

  const res = await fetch(BASE + path, {
    method,
    headers,
    credentials: 'same-origin',
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (!res.ok) {
    const problem = (await res.json().catch(() => null)) as ProblemDetail | null
    // Điều khoản đổi phiên bản: mọi API (trừ vài cái) trả 403 cho đến khi người dùng đồng ý lại.
    if (res.status === 403 && problem?.code === 'TERMS_REQUIRED') location.hash = '#/dong-y'
    throw new ApiError(res.status, problem)
  }
  return (res.status === 204 ? undefined : await res.json()) as T
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T = void>(path: string, body?: unknown) => request<T>('POST', path, body),
  put: <T = void>(path: string, body?: unknown) => request<T>('PUT', path, body),
  patch: <T = void>(path: string, body?: unknown) => request<T>('PATCH', path, body),
  delete: <T = void>(path: string, body?: unknown) => request<T>('DELETE', path, body),
}

import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, api } from './api'

function stubFetch(response: Response) {
  const fetchMock = vi.fn().mockResolvedValue(response)
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

afterEach(() => vi.unstubAllGlobals())

describe('api', () => {
  it('gửi CSRF token đọc từ cookie khi gọi POST', async () => {
    vi.stubGlobal('document', { cookie: 'a=1; XSRF-TOKEN=abc%3D123' })
    const fetchMock = stubFetch(new Response(null, { status: 204 }))

    await api.post('/auth/logout')

    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/v1/auth/logout')
    expect(new Headers(init.headers).get('X-XSRF-TOKEN')).toBe('abc=123')
  })

  it('không gửi CSRF token khi gọi GET và trả JSON', async () => {
    vi.stubGlobal('document', { cookie: 'XSRF-TOKEN=abc' })
    const fetchMock = stubFetch(Response.json({ id: 1 }))

    const body = await api.get<{ id: number }>('/me')

    expect(body).toEqual({ id: 1 })
    expect(new Headers(fetchMock.mock.calls[0][1].headers).has('X-XSRF-TOKEN')).toBe(false)
  })

  it('chuyển sang màn đồng ý khi điều khoản đổi phiên bản (403 TERMS_REQUIRED)', async () => {
    vi.stubGlobal('document', { cookie: '' })
    const location = { hash: '#/ho-so' }
    vi.stubGlobal('location', location)
    stubFetch(Response.json({ status: 403, code: 'TERMS_REQUIRED' }, { status: 403 }))

    await api.patch('/me', { displayName: 'An' }).catch(() => {})

    expect(location.hash).toBe('#/dong-y')
  })

  it('ném ApiError kèm status và ProblemDetail khi lỗi', async () => {
    vi.stubGlobal('document', { cookie: '' })
    stubFetch(Response.json({ title: 'Unauthorized', status: 401 }, { status: 401 }))

    const error = await api.get('/me').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(401)
    expect((error as ApiError).problem?.title).toBe('Unauthorized')
  })
})

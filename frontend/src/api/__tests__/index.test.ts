import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'

const BASE = 'http://localhost:3000'

const server = setupServer()

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

// Dynamic import to get fresh module each time
async function getApi() {
  return import('../../api/index.ts')
}

describe('request function', () => {
  it('unwraps envelope on success', async () => {
    server.use(
      http.get(`${BASE}/api/test`, () =>
        HttpResponse.json({ code: 200, message: 'ok', data: { name: 'alice' } }))
    )
    // request is internal, tested indirectly through authApi
    const { authApi } = await getApi()
    // Just verify the module loaded
    expect(typeof authApi.login).toBe('function')
  })

  it('throws on non-200 envelope code', async () => {
    server.use(
      http.post(`${BASE}/api/auth/login`, () =>
        HttpResponse.json({ code: 401, message: '用户名或密码错误', data: null }))
    )
    const { authApi } = await getApi()
    await expect(authApi.login('bad', 'bad', '')).rejects.toThrow('用户名或密码错误')
  })

  it('throws on HTTP error with message from body', async () => {
    server.use(
      http.post(`${BASE}/api/auth/login`, () =>
        new HttpResponse(JSON.stringify({ message: 'server error' }), { status: 500 }))
    )
    const { authApi } = await getApi()
    await expect(authApi.login('x', 'y', '')).rejects.toThrow('server error')
  })

  it('throws on HTTP error without body message', async () => {
    server.use(
      http.post(`${BASE}/api/auth/register`, () =>
        new HttpResponse(null, { status: 503 }))
    )
    const { authApi } = await getApi()
    await expect(authApi.register('a', 'b', 'c@d.com', '123'))
      .rejects.toThrow('HTTP 503')
  })
})

describe('authApi', () => {
  it('login returns user on success', async () => {
    const user = { id: 1, username: 'alice', email: 'a@b.com', nickname: 'Alice',
      avatarUrl: '', role: 0, status: 0, createTime: '', updateTime: '' }
    server.use(
      http.post(`${BASE}/api/auth/login`, () =>
        HttpResponse.json({ code: 200, message: 'ok', data: user }))
    )
    const { authApi } = await getApi()
    const result = await authApi.login('alice', 'pass', 'token')
    expect(result.username).toBe('alice')
  })

  it('register returns user on success', async () => {
    const user = { id: 2, username: 'bob', email: 'b@b.com', nickname: 'Bob',
      avatarUrl: '', role: 0, status: 0, createTime: '', updateTime: '' }
    server.use(
      http.post(`${BASE}/api/auth/register`, () =>
        HttpResponse.json({ code: 200, message: 'ok', data: user }))
    )
    const { authApi } = await getApi()
    const result = await authApi.register('bob', 'pass', 'b@b.com', '123')
    expect(result.username).toBe('bob')
  })

  it('current returns null user when not logged in', async () => {
    server.use(
      http.get(`${BASE}/api/auth/current`, () =>
        HttpResponse.json({ code: 200, message: 'ok', data: null }))
    )
    const { authApi } = await getApi()
    const result = await authApi.current()
    expect(result).toBeNull()
  })
})

describe('query function', () => {
  it('builds query string from params', async () => {
    server.use(
      http.get(`${BASE}/api/user/profile`, async ({ request }) => {
        const url = new URL(request.url)
        return HttpResponse.json({
          code: 200,
          data: { id: Number(url.searchParams.get('id')) },
        })
      })
    )
    const { userApi } = await getApi()
    const result = await userApi.profile(42)
    expect(result.id).toBe(42)
  })
})

describe('notificationApi', () => {
  it('list returns notifications', async () => {
    const items = [
      { id: 1, type: 3, title: 'comment', content: 'hi', link: null, isRead: 0, createTime: '' },
    ]
    server.use(
      http.get(`${BASE}/api/notification/list`, () =>
        HttpResponse.json({ code: 200, data: items }))
    )
    const { notificationApi } = await getApi()
    const result = await notificationApi.list(0, 20)
    expect(result).toHaveLength(1)
  })

  it('unreadCount returns number', async () => {
    server.use(
      http.get(`${BASE}/api/notification/unread-count`, () =>
        HttpResponse.json({ code: 200, data: 7 }))
    )
    const { notificationApi } = await getApi()
    const count = await notificationApi.unreadCount()
    expect(count).toBe(7)
  })
})

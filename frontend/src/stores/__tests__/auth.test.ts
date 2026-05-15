import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useAuthStore } from '../auth'
import { authApi } from '../../api'

vi.mock('../../api', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    current: vi.fn(),
  },
}))

const mockUser = { id: 1, username: 'alice', nickname: 'Alice', email: 'a@t.com', role: 0, status: 0 }

describe('useAuthStore', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useAuthStore.setState({ user: null, loading: false, initialized: false })
  })

  it('init fetches current user and sets initialized', async () => {
    vi.mocked(authApi.current).mockResolvedValue(mockUser)
    await useAuthStore.getState().init()
    expect(useAuthStore.getState().user).toEqual(mockUser)
    expect(useAuthStore.getState().initialized).toBe(true)
  })

  it('init on 401 sets initialized with null user', async () => {
    vi.mocked(authApi.current).mockRejectedValue(new Error('401'))
    await useAuthStore.getState().init()
    expect(useAuthStore.getState().user).toBeNull()
    expect(useAuthStore.getState().initialized).toBe(true)
  })

  it('init skips if already initialized', async () => {
    useAuthStore.setState({ user: mockUser, initialized: true })
    await useAuthStore.getState().init()
    expect(authApi.current).not.toHaveBeenCalled()
  })

  it('login sets user on success', async () => {
    vi.mocked(authApi.login).mockResolvedValue(mockUser)
    await useAuthStore.getState().login('alice', 'pass', 'token')
    expect(useAuthStore.getState().user).toEqual(mockUser)
    expect(useAuthStore.getState().loading).toBe(false)
  })

  it('login rethrows on error', async () => {
    vi.mocked(authApi.login).mockRejectedValue(new Error('fail'))
    await expect(useAuthStore.getState().login('a', 'p', 't')).rejects.toThrow('fail')
    expect(useAuthStore.getState().loading).toBe(false)
  })

  it('register sets user on success', async () => {
    vi.mocked(authApi.register).mockResolvedValue(mockUser)
    await useAuthStore.getState().register('alice', 'pass', 'a@t.com', '123456')
    expect(useAuthStore.getState().user).toEqual(mockUser)
  })

  it('logout clears user', async () => {
    useAuthStore.setState({ user: mockUser })
    await useAuthStore.getState().logout()
    expect(useAuthStore.getState().user).toBeNull()
  })
})

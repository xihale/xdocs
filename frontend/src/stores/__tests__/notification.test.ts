import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useNotificationStore } from '../notification'
import { notificationApi } from '../../api'

vi.mock('../../api', () => ({
  notificationApi: {
    unreadCount: vi.fn(),
    list: vi.fn(),
    read: vi.fn(),
    readAll: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('useNotificationStore', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useNotificationStore.setState({
      unreadCount: 0,
      notifications: [],
      loading: false,
    })
  })

  it('fetchUnreadCount updates count', async () => {
    vi.mocked(notificationApi.unreadCount).mockResolvedValue(5)
    await useNotificationStore.getState().fetchUnreadCount()
    expect(useNotificationStore.getState().unreadCount).toBe(5)
  })

  it('fetchUnreadCount ignores error', async () => {
    vi.mocked(notificationApi.unreadCount).mockRejectedValue(new Error('fail'))
    await useNotificationStore.getState().fetchUnreadCount()
    expect(useNotificationStore.getState().unreadCount).toBe(0)
  })

  it('fetchNotifications populates list', async () => {
    const items = [{ id: 1, type: 0, title: 't', content: 'c', link: null, isRead: 0, createTime: '' }]
    vi.mocked(notificationApi.list).mockResolvedValue(items as any[])
    await useNotificationStore.getState().fetchNotifications()
    expect(useNotificationStore.getState().notifications).toHaveLength(1)
    expect(useNotificationStore.getState().loading).toBe(false)
  })

  it('markRead updates single notification and decrements count', async () => {
    useNotificationStore.setState({
      notifications: [
        { id: 1, type: 0, title: 't', content: 'c', link: null, isRead: 0, createTime: '' },
      ] as any[],
      unreadCount: 1,
    })
    vi.mocked(notificationApi.read).mockResolvedValue(undefined)

    await useNotificationStore.getState().markRead(1)

    const state = useNotificationStore.getState()
    expect(state.notifications[0].isRead).toBe(1)
    expect(state.unreadCount).toBe(0)
  })

  it('markAllRead sets all to read', async () => {
    useNotificationStore.setState({
      notifications: [
        { id: 1, type: 0, title: 'a', content: '', link: null, isRead: 0, createTime: '' },
        { id: 2, type: 0, title: 'b', content: '', link: null, isRead: 0, createTime: '' },
      ] as any[],
      unreadCount: 2,
    })
    vi.mocked(notificationApi.readAll).mockResolvedValue(undefined)

    await useNotificationStore.getState().markAllRead()

    const state = useNotificationStore.getState()
    expect(state.notifications.every(n => n.isRead === 1)).toBe(true)
    expect(state.unreadCount).toBe(0)
  })

  it('deleteNotification removes item and adjusts count', async () => {
    useNotificationStore.setState({
      notifications: [
        { id: 1, type: 0, title: 'a', content: '', link: null, isRead: 0, createTime: '' },
        { id: 2, type: 0, title: 'b', content: '', link: null, isRead: 1, createTime: '' },
      ] as any[],
      unreadCount: 1,
    })
    vi.mocked(notificationApi.delete).mockResolvedValue(undefined)

    await useNotificationStore.getState().deleteNotification(1)

    const state = useNotificationStore.getState()
    expect(state.notifications).toHaveLength(1)
    expect(state.notifications[0].id).toBe(2)
    expect(state.unreadCount).toBe(0)
  })

  it('deleteNotification read item does not change count', async () => {
    useNotificationStore.setState({
      notifications: [
        { id: 1, type: 0, title: 'a', content: '', link: null, isRead: 0, createTime: '' },
        { id: 2, type: 0, title: 'b', content: '', link: null, isRead: 1, createTime: '' },
      ] as any[],
      unreadCount: 1,
    })
    vi.mocked(notificationApi.delete).mockResolvedValue(undefined)

    await useNotificationStore.getState().deleteNotification(2)

    expect(useNotificationStore.getState().unreadCount).toBe(1)
  })
})

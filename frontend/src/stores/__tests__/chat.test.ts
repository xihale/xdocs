import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useChatStore, getChatMessages, saveChatMessages, clearChatMessages } from '../chat'

describe('useChatStore', () => {
  beforeEach(() => {
    localStorage.clear()
    useChatStore.setState({ openMap: {} })
  })

  it('initializes with empty openMap', () => {
    const state = useChatStore.getState()
    expect(state.openMap).toEqual({})
  })

  it('setOpen updates openMap', () => {
    useChatStore.getState().setOpen(1, true)
    expect(useChatStore.getState().openMap[1]).toBe(true)
  })

  it('setOpen persists to localStorage', () => {
    useChatStore.getState().setOpen(42, true)
    const stored = JSON.parse(localStorage.getItem('xdocs-chat-open')!)
    expect(stored[42]).toBe(true)
  })

  it('setOpen can close', () => {
    useChatStore.getState().setOpen(1, true)
    useChatStore.getState().setOpen(1, false)
    expect(useChatStore.getState().openMap[1]).toBe(false)
  })
})

describe('chat message persistence', () => {
  beforeEach(() => {
    localStorage.clear()
    // Reset the module-level cache by importing fresh — but since we can't,
    // we use clearChatMessages to clean up
  })

  it('saveChatMessages then getChatMessages returns messages', () => {
    const msgs = [
      { id: 1, senderId: 1, content: 'hello', type: 0, createTime: '' },
    ] as any[]
    saveChatMessages(10, msgs)
    const result = getChatMessages(10)
    expect(result).toHaveLength(1)
    expect(result[0].content).toBe('hello')
  })

  it('getChatMessages returns empty for unknown article', () => {
    expect(getChatMessages(9999)).toEqual([])
  })

  it('clearChatMessages removes messages', () => {
    saveChatMessages(20, [{ id: 1, content: 'x' }] as any[])
    clearChatMessages(20)
    expect(getChatMessages(20)).toEqual([])
  })
})

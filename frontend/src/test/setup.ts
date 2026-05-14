import '@testing-library/jest-dom'

// vitest 3 overrides global localStorage with a broken stub.
// Restore a proper in-memory localStorage.
const store: Record<string, string> = {}

const memoryLocalStorage = {
  getItem(key: string) { return key in store ? store[key] : null },
  setItem(key: string, value: string) { store[key] = String(value) },
  removeItem(key: string) { delete store[key] },
  clear() { Object.keys(store).forEach(k => delete store[k]) },
  key(index: number) { const keys = Object.keys(store); return keys[index] ?? null },
  get length() { return Object.keys(store).length },
}

Object.defineProperty(globalThis, 'localStorage', { value: memoryLocalStorage, configurable: true })
Object.defineProperty(globalThis.window, 'localStorage', { value: memoryLocalStorage, configurable: true })

import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { tokenStore as TokenStore } from './tokenStore'

// tokenStore holds its state in module-level singletons (by design — it's not
// a React hook, apiClient needs to reach it outside any component tree), so
// each test re-imports a fresh module instance rather than sharing state
// across `it` blocks the way a class instance would let us.
async function freshTokenStore() {
  vi.resetModules()
  const mod = await import('./tokenStore')
  return mod.tokenStore as typeof TokenStore
}

describe('tokenStore', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('starts with no access token and not ready', async () => {
    const tokenStore = await freshTokenStore()
    expect(tokenStore.getAccessToken()).toBeNull()
    expect(tokenStore.getAccessTokenExpiresInMs()).toBeNull()

    let resolved = false
    tokenStore.waitUntilReady().then(() => {
      resolved = true
    })
    await Promise.resolve()
    expect(resolved).toBe(false)
  })

  it('setAccessToken marks readiness and resolves waitUntilReady', async () => {
    const tokenStore = await freshTokenStore()
    const waiter = tokenStore.waitUntilReady()

    tokenStore.setAccessToken('a-token', 900_000)

    await expect(waiter).resolves.toBeUndefined()
    expect(tokenStore.getAccessToken()).toBe('a-token')
    expect(tokenStore.getAccessTokenExpiresInMs()).toBe(900_000)
  })

  it('waitUntilReady resolves immediately once already ready', async () => {
    const tokenStore = await freshTokenStore()
    tokenStore.markReady()

    let resolved = false
    tokenStore.waitUntilReady().then(() => {
      resolved = true
    })
    await Promise.resolve()
    expect(resolved).toBe(true)
  })

  it('markReady is idempotent and safe to call before any token is set', async () => {
    const tokenStore = await freshTokenStore()
    tokenStore.markReady()
    tokenStore.markReady()

    await expect(tokenStore.waitUntilReady()).resolves.toBeUndefined()
    expect(tokenStore.getAccessToken()).toBeNull()
  })

  it('setAccessToken(null) clears the expiry alongside the token', async () => {
    const tokenStore = await freshTokenStore()
    tokenStore.setAccessToken('a-token', 900_000)
    tokenStore.setAccessToken(null)

    expect(tokenStore.getAccessToken()).toBeNull()
    expect(tokenStore.getAccessTokenExpiresInMs()).toBeNull()
  })

  it('persists and clears the refresh token via localStorage', async () => {
    const tokenStore = await freshTokenStore()
    expect(tokenStore.getRefreshToken()).toBeNull()

    tokenStore.setRefreshToken('a-refresh-token')
    expect(tokenStore.getRefreshToken()).toBe('a-refresh-token')
    expect(localStorage.getItem('tabibma-refresh-token')).toBe('a-refresh-token')

    tokenStore.setRefreshToken(null)
    expect(tokenStore.getRefreshToken()).toBeNull()
    expect(localStorage.getItem('tabibma-refresh-token')).toBeNull()
  })

  it('clear() wipes the access token and the persisted refresh token', async () => {
    const tokenStore = await freshTokenStore()
    tokenStore.setAccessToken('a-token', 900_000)
    tokenStore.setRefreshToken('a-refresh-token')

    tokenStore.clear()

    expect(tokenStore.getAccessToken()).toBeNull()
    expect(tokenStore.getAccessTokenExpiresInMs()).toBeNull()
    expect(tokenStore.getRefreshToken()).toBeNull()
  })

  it('notifies onSessionRefreshed listeners with the new expiry on every setAccessToken call', async () => {
    const tokenStore = await freshTokenStore()
    const listener = vi.fn()
    tokenStore.onSessionRefreshed(listener)

    tokenStore.setAccessToken('a-token', 900_000)
    tokenStore.setAccessToken('b-token', 300_000)
    tokenStore.setAccessToken(null)

    expect(listener).toHaveBeenNthCalledWith(1, 900_000)
    expect(listener).toHaveBeenNthCalledWith(2, 300_000)
    expect(listener).toHaveBeenNthCalledWith(3, null)
  })

  it('onSessionRefreshed unsubscribe stops further notifications', async () => {
    const tokenStore = await freshTokenStore()
    const listener = vi.fn()
    const unsubscribe = tokenStore.onSessionRefreshed(listener)

    tokenStore.setAccessToken('a-token', 900_000)
    unsubscribe()
    tokenStore.setAccessToken('b-token', 300_000)

    expect(listener).toHaveBeenCalledTimes(1)
  })

  it('notifySessionExpired calls onSessionExpired listeners, and unsubscribe stops it', async () => {
    const tokenStore = await freshTokenStore()
    const listener = vi.fn()
    const unsubscribe = tokenStore.onSessionExpired(listener)

    tokenStore.notifySessionExpired()
    expect(listener).toHaveBeenCalledTimes(1)

    unsubscribe()
    tokenStore.notifySessionExpired()
    expect(listener).toHaveBeenCalledTimes(1)
  })
})

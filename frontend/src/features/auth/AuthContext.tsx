import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { apiClient } from '@/shared/api/client'
import { tokenStore } from '@/shared/api/tokenStore'
import type { components } from '@/shared/api/schema'

type UserSummary = components['schemas']['UserSummaryResponse']
type AuthResponse = components['schemas']['AuthResponse']

type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated'

type RegisterInput = {
  email: string
  password: string
  firstName: string
  lastName: string
  role: 'PATIENT' | 'DOCTOR'
}

type AuthContextValue = {
  status: AuthStatus
  user: UserSummary | null
  login: (email: string, password: string) => Promise<void>
  register: (input: RegisterInput) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

// A token this close to expiry is refreshed proactively rather than waiting
// for a request to hit a 401 (covers request latency + a little clock skew).
const PROACTIVE_REFRESH_MARGIN_MS = 60_000
const MIN_PROACTIVE_REFRESH_DELAY_MS = 5_000

async function applySession(session: AuthResponse) {
  tokenStore.setAccessToken(session.accessToken ?? null, session.expiresInMs ?? null)
  tokenStore.setRefreshToken(session.refreshToken ?? null)
  const { data, error } = await apiClient.GET('/api/v1/users/me')
  if (error) {
    throw error
  }
  return data
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('loading')
  const [user, setUser] = useState<UserSummary | null>(null)
  const hasBootstrapped = useRef(false)
  const refreshTimerRef = useRef<ReturnType<typeof setTimeout>>(undefined)

  const clearProactiveRefreshTimer = useCallback(() => {
    clearTimeout(refreshTimerRef.current)
    refreshTimerRef.current = undefined
  }, [])

  // Shared by the initial bootstrap and the proactive-refresh timer, so a
  // successful refresh always lands in the same authenticated state via the
  // same applySession path regardless of who triggered it.
  const refreshSession = useCallback(async () => {
    const refreshToken = tokenStore.getRefreshToken()
    if (!refreshToken) {
      throw new Error('no refresh token to refresh with')
    }
    const { data, error } = await apiClient.POST('/api/v1/auth/refresh', { body: { refreshToken } })
    if (error || !data) {
      throw error
    }
    const me = await applySession(data)
    setUser(me)
    setStatus('authenticated')
  }, [])

  useEffect(() => {
    // Refresh tokens are single-use (rotated server-side on every call), so
    // this must run exactly once per app load — guard against StrictMode's
    // dev-only double effect invocation, which would otherwise burn the
    // token twice and race the rotation.
    if (hasBootstrapped.current) {
      return
    }
    hasBootstrapped.current = true

    if (!tokenStore.getRefreshToken()) {
      // No session to restore — still mark readiness, otherwise apiClient's
      // waitUntilReady() would hang forever on every request an anonymous
      // visitor makes.
      tokenStore.markReady()
      setStatus('unauthenticated')
      return
    }

    refreshSession().catch(() => {
      tokenStore.clear()
      tokenStore.markReady()
      setUser(null)
      setStatus('unauthenticated')
    })
  }, [refreshSession])

  useEffect(() => {
    // Reschedules the proactive refresh at every new expiry (bootstrap,
    // login, or a previous proactive refresh) and flips to logged-out the
    // moment a 401 slips through (apiClient) or a refresh attempt fails.
    const unsubscribeRefreshed = tokenStore.onSessionRefreshed((expiresInMs) => {
      clearProactiveRefreshTimer()
      if (expiresInMs === null) {
        return
      }
      const delay = Math.max(expiresInMs - PROACTIVE_REFRESH_MARGIN_MS, MIN_PROACTIVE_REFRESH_DELAY_MS)
      refreshTimerRef.current = setTimeout(() => {
        refreshSession().catch(() => {
          tokenStore.clear()
          tokenStore.notifySessionExpired()
        })
      }, delay)
    })

    const unsubscribeExpired = tokenStore.onSessionExpired(() => {
      clearProactiveRefreshTimer()
      setUser(null)
      setStatus('unauthenticated')
    })

    return () => {
      unsubscribeRefreshed()
      unsubscribeExpired()
      clearProactiveRefreshTimer()
    }
  }, [refreshSession, clearProactiveRefreshTimer])

  const login = useCallback(async (email: string, password: string) => {
    const { data, error } = await apiClient.POST('/api/v1/auth/login', {
      body: { email, password },
    })
    if (error || !data) {
      throw error
    }
    const me = await applySession(data)
    setUser(me)
    setStatus('authenticated')
  }, [])

  const register = useCallback(
    async ({ email, password, firstName, lastName, role }: RegisterInput) => {
      const { error } = await apiClient.POST('/api/v1/auth/register', {
        body: { email, password, firstName, lastName, role },
      })
      if (error) {
        throw error
      }
      // Registration doesn't issue tokens — log in right after with the same
      // credentials to start the session.
      await login(email, password)
    },
    [login]
  )

  const logout = useCallback(() => {
    clearProactiveRefreshTimer()
    tokenStore.clear()
    setUser(null)
    setStatus('unauthenticated')
  }, [clearProactiveRefreshTimer])

  const value = useMemo(
    () => ({ status, user, login, register, logout }),
    [status, user, login, register, logout]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}

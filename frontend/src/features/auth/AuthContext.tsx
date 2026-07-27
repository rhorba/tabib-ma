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

async function applySession(session: AuthResponse) {
  tokenStore.setAccessToken(session.accessToken ?? null)
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

  useEffect(() => {
    // Refresh tokens are single-use (rotated server-side on every call), so
    // this must run exactly once per app load — guard against StrictMode's
    // dev-only double effect invocation, which would otherwise burn the
    // token twice and race the rotation.
    if (hasBootstrapped.current) {
      return
    }
    hasBootstrapped.current = true

    const refreshToken = tokenStore.getRefreshToken()
    if (!refreshToken) {
      setStatus('unauthenticated')
      return
    }

    apiClient
      .POST('/api/v1/auth/refresh', { body: { refreshToken } })
      .then(async ({ data, error }) => {
        if (error || !data) {
          throw error
        }
        const me = await applySession(data)
        setUser(me)
        setStatus('authenticated')
      })
      .catch(() => {
        tokenStore.clear()
        setUser(null)
        setStatus('unauthenticated')
      })
  }, [])

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
    tokenStore.clear()
    setUser(null)
    setStatus('unauthenticated')
  }, [])

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

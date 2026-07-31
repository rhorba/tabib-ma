import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiClient } from '@/shared/api/client'
import { tokenStore } from '@/shared/api/tokenStore'
import { AuthProvider, useAuth } from './AuthContext'

async function registerAndLogin(userEventOptions?: Parameters<typeof userEvent.setup>[0]) {
  await fetch('/api/v1/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      email: 'amina@example.com',
      password: 'correct-password',
      role: 'PATIENT',
      firstName: 'Amina',
      lastName: 'Bennis',
    }),
  })

  renderHarness()
  await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated'))

  const user = userEvent.setup(userEventOptions)
  await user.click(screen.getByRole('button', { name: 'login' }))
  await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'))
}

function TestHarness() {
  const { status, user, login, logout } = useAuth()
  return (
    <div>
      <div data-testid="status">{status}</div>
      <div data-testid="user">{user ? user.email : 'none'}</div>
      <button onClick={() => login('amina@example.com', 'correct-password')}>login</button>
      <button onClick={logout}>logout</button>
    </div>
  )
}

function renderHarness() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <TestHarness />
      </AuthProvider>
    </QueryClientProvider>
  )
}

describe('AuthProvider', () => {
  it('starts unauthenticated when no refresh token is stored', async () => {
    renderHarness()
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated'))
    expect(screen.getByTestId('user')).toHaveTextContent('none')
  })

  it('logs in and exposes the current user', async () => {
    await fetch('/api/v1/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: 'amina@example.com',
        password: 'correct-password',
        role: 'PATIENT',
        firstName: 'Amina',
        lastName: 'Bennis',
      }),
    })

    renderHarness()
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated'))

    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: 'login' }))

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'))
    expect(screen.getByTestId('user')).toHaveTextContent('amina@example.com')
    expect(localStorage.getItem('tabibma-refresh-token')).toBeTruthy()
  })

  it('logout clears the session and the persisted refresh token', async () => {
    await fetch('/api/v1/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: 'amina@example.com',
        password: 'correct-password',
        role: 'PATIENT',
        firstName: 'Amina',
        lastName: 'Bennis',
      }),
    })

    renderHarness()
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated'))

    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: 'login' }))
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'))

    await user.click(screen.getByRole('button', { name: 'logout' }))

    expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated')
    expect(screen.getByTestId('user')).toHaveTextContent('none')
    expect(localStorage.getItem('tabibma-refresh-token')).toBeNull()
  })

  it('silently re-establishes a session from a stored refresh token (survives reload)', async () => {
    await fetch('/api/v1/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: 'amina@example.com',
        password: 'correct-password',
        role: 'PATIENT',
        firstName: 'Amina',
        lastName: 'Bennis',
      }),
    })
    const loginResponse = await fetch('/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: 'amina@example.com', password: 'correct-password' }),
    })
    const { refreshToken } = await loginResponse.json()
    localStorage.setItem('tabibma-refresh-token', refreshToken)

    renderHarness()

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'))
    expect(screen.getByTestId('user')).toHaveTextContent('amina@example.com')
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it(
    'proactively refreshes the session before the access token expires',
    async () => {
      await registerAndLogin()
      const firstRefreshToken = localStorage.getItem('tabibma-refresh-token')

      // Simulating (rather than faking timers for) a token that's about to
      // expire: this fires the same onSessionRefreshed notification a real
      // login/refresh would, with a short enough expiry that AuthContext's
      // *real* setTimeout lands well inside this test's timeout instead of
      // the real 15-minute token lifetime authHandlers issues.
      tokenStore.setAccessToken(tokenStore.getAccessToken(), 6_000)

      await waitFor(
        () => {
          const refreshedToken = localStorage.getItem('tabibma-refresh-token')
          expect(refreshedToken).toBeTruthy()
          expect(refreshedToken).not.toBe(firstRefreshToken)
        },
        { timeout: 8_000 }
      )
      expect(screen.getByTestId('status')).toHaveTextContent('authenticated')
      expect(screen.getByTestId('user')).toHaveTextContent('amina@example.com')
    },
    10_000
  )

  it('flips to unauthenticated when a non-auth request comes back 401', async () => {
    await registerAndLogin()

    // Simulate the access token going bad server-side (expired mid-session,
    // revoked, etc.) without waiting for the real 15min expiry — apiClient's
    // onResponse should catch the 401 and clear the session.
    tokenStore.setAccessToken('a-tampered-token', 900_000)
    await apiClient.GET('/api/v1/users/me')

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('unauthenticated'))
    expect(screen.getByTestId('user')).toHaveTextContent('none')
    expect(localStorage.getItem('tabibma-refresh-token')).toBeNull()
  })
})

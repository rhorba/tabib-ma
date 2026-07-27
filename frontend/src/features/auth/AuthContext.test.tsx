import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { AuthProvider, useAuth } from './AuthContext'

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
})

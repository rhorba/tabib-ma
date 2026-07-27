import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { renderWithProviders, screen, waitFor } from '@/test/renderWithProviders'
import { LoginForm } from './LoginForm'

async function fillAndSubmit(email: string, password: string) {
  const user = userEvent.setup()
  if (email) {
    await user.type(screen.getByLabelText(/adresse e-mail/i), email)
  }
  if (password) {
    await user.type(screen.getByLabelText(/mot de passe/i), password)
  }
  await user.click(screen.getByRole('button', { name: /se connecter/i }))
}

describe('LoginForm', () => {
  it('shows validation errors and does not submit when fields are empty', async () => {
    const onSuccess = vi.fn()
    renderWithProviders(<LoginForm onSuccess={onSuccess} />)

    await fillAndSubmit('', '')

    expect(await screen.findByText(/adresse e-mail invalide/i)).toBeInTheDocument()
    expect(onSuccess).not.toHaveBeenCalled()
  })

  it('shows the generic invalid-credentials error on a failed login (no user-enumeration hint)', async () => {
    const onSuccess = vi.fn()
    renderWithProviders(<LoginForm onSuccess={onSuccess} />)

    await fillAndSubmit('nobody@example.com', 'wrong-password')

    expect(await screen.findByText(/e-mail ou mot de passe incorrect/i)).toBeInTheDocument()
    expect(onSuccess).not.toHaveBeenCalled()
  })

  it('calls onSuccess after a real login against a previously registered user', async () => {
    // Seed a user via the same MSW-backed register endpoint the app itself uses.
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

    const onSuccess = vi.fn()
    renderWithProviders(<LoginForm onSuccess={onSuccess} />)

    await fillAndSubmit('amina@example.com', 'correct-password')

    await waitFor(() => expect(onSuccess).toHaveBeenCalledTimes(1))
  })
})

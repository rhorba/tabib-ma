import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { renderWithProviders, screen, waitFor } from '@/test/renderWithProviders'
import { RegisterForm } from './RegisterForm'

async function fillForm({
  firstName = 'Amina',
  lastName = 'Bennis',
  email = 'amina@example.com',
  password = 'longenough',
}: Partial<Record<'firstName' | 'lastName' | 'email' | 'password', string>> = {}) {
  const user = userEvent.setup()
  if (firstName) await user.type(screen.getByLabelText(/prénom/i), firstName)
  if (lastName) await user.type(screen.getByLabelText(/^nom$/i), lastName)
  if (email) await user.type(screen.getByLabelText(/adresse e-mail/i), email)
  if (password) await user.type(screen.getByLabelText(/mot de passe/i), password)
  await user.click(screen.getByRole('button', { name: /créer mon compte/i }))
}

describe('RegisterForm', () => {
  it('defaults the role to Patient', () => {
    renderWithProviders(<RegisterForm onSuccess={vi.fn()} />)
    expect(screen.getByRole('combobox')).toHaveTextContent(/patient/i)
  })

  it('rejects a password shorter than 8 characters, matching the backend constraint', async () => {
    const onSuccess = vi.fn()
    renderWithProviders(<RegisterForm onSuccess={onSuccess} />)

    await fillForm({ password: 'short1' })

    expect(await screen.findByText(/au moins 8 caractères/i)).toBeInTheDocument()
    expect(onSuccess).not.toHaveBeenCalled()
  })

  it('registers and auto-logs-in a new user, calling onSuccess', async () => {
    const onSuccess = vi.fn()
    renderWithProviders(<RegisterForm onSuccess={onSuccess} />)

    await fillForm()

    await waitFor(() => expect(onSuccess).toHaveBeenCalledTimes(1))
  })

  it('shows the email-taken error on a duplicate registration', async () => {
    await fetch('/api/v1/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: 'amina@example.com',
        password: 'longenough',
        role: 'PATIENT',
        firstName: 'Amina',
        lastName: 'Bennis',
      }),
    })

    const onSuccess = vi.fn()
    renderWithProviders(<RegisterForm onSuccess={onSuccess} />)

    await fillForm()

    expect(await screen.findByText(/compte existe déjà avec cette adresse/i)).toBeInTheDocument()
    expect(onSuccess).not.toHaveBeenCalled()
  })
})

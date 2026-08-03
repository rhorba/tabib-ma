import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { renderWithProviders, screen, waitFor } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { ReviewForm } from './ReviewForm'

describe('ReviewForm', () => {
  it('requires a rating before submitting', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    renderWithProviders(<ReviewForm appointmentId="appt-1" onSubmitted={vi.fn()} />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /envoyer l'avis/i }))

    expect(await screen.findByText(/veuillez choisir une note/i)).toBeInTheDocument()
  })

  it('submits a rating and comment, then calls onSubmitted', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    const onSubmitted = vi.fn()
    renderWithProviders(<ReviewForm appointmentId="appt-1" onSubmitted={onSubmitted} />)
    const user = userEvent.setup()

    await user.click(screen.getAllByRole('radio', { name: /étoile/i })[3])
    await user.type(screen.getByLabelText(/commentaire/i), 'Très bon suivi')
    await user.click(screen.getByRole('button', { name: /envoyer l'avis/i }))

    await waitFor(() => expect(onSubmitted).toHaveBeenCalled())
  })

  it('shows a generic error when the request fails', async () => {
    // Only patients can submit reviews — logging in as a doctor exercises a
    // real 403 from the fake handler rather than a monkey-patched one.
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    renderWithProviders(<ReviewForm appointmentId="appt-1" onSubmitted={vi.fn()} />)
    const user = userEvent.setup()

    await user.click(screen.getAllByRole('radio', { name: /étoile/i })[0])
    await user.click(screen.getByRole('button', { name: /envoyer l'avis/i }))

    expect(await screen.findByText(/une erreur est survenue\. veuillez réessayer\./i)).toBeInTheDocument()
  })
})

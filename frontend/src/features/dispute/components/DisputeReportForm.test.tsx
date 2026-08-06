import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { renderWithProviders, screen, waitFor } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { DisputeReportForm } from './DisputeReportForm'

describe('DisputeReportForm', () => {
  it('requires a reason before submitting', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    const onSubmitted = vi.fn()
    renderWithProviders(<DisputeReportForm appointmentId="appt-1" onSubmitted={onSubmitted} />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /envoyer le signalement/i }))

    expect(await screen.findByText(/veuillez décrire le problème/i)).toBeInTheDocument()
    expect(onSubmitted).not.toHaveBeenCalled()
  })

  it('submits and calls onSubmitted', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    const onSubmitted = vi.fn()
    renderWithProviders(<DisputeReportForm appointmentId="appt-1" onSubmitted={onSubmitted} />)
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/description/i), "Le medecin n'est pas venu.")
    await user.click(screen.getByRole('button', { name: /envoyer le signalement/i }))

    await waitFor(() => expect(onSubmitted).toHaveBeenCalled())
  })
})

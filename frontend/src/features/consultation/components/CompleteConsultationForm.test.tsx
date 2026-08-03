import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { renderWithProviders, screen, waitFor } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedConsultation } from '@/test/consultationHandlers'
import { CompleteConsultationForm } from './CompleteConsultationForm'

describe('CompleteConsultationForm', () => {
  it('requires a medication name and dosage before submitting', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    renderWithProviders(<CompleteConsultationForm consultationId="c-1" onCompleted={vi.fn()} />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /terminer et envoyer l'ordonnance/i }))

    expect(await screen.findByText(/le nom du médicament est requis/i)).toBeInTheDocument()
    expect(screen.getByText(/la posologie est requise/i)).toBeInTheDocument()
  })

  it('adds and removes medication rows', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    renderWithProviders(<CompleteConsultationForm consultationId="c-1" onCompleted={vi.fn()} />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /ajouter un médicament/i }))
    expect(screen.getAllByLabelText(/^médicament$/i)).toHaveLength(2)

    await user.click(screen.getAllByRole('button', { name: /retirer/i })[0])
    expect(screen.getAllByLabelText(/^médicament$/i)).toHaveLength(1)
  })

  it('completes the consultation and calls onCompleted on success', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    const consultation = seedConsultation({
      appointmentId: 'appt-1',
      doctorId: '1',
      patientId: '2',
      status: 'IN_PROGRESS',
      joinable: true,
    })
    const onCompleted = vi.fn()
    renderWithProviders(<CompleteConsultationForm consultationId={consultation.id} onCompleted={onCompleted} />)
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/^médicament$/i), 'Paracétamol')
    await user.type(screen.getByLabelText(/^posologie$/i), '500mg')
    await user.click(screen.getByRole('button', { name: /terminer et envoyer l'ordonnance/i }))

    await waitFor(() => expect(onCompleted).toHaveBeenCalled())
  })

  it('shows a generic error when the request fails', async () => {
    // Only doctors can complete a consultation — logging in as a patient
    // exercises a real 403 from the fake handler rather than a monkey-patched one.
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    renderWithProviders(<CompleteConsultationForm consultationId="1" onCompleted={vi.fn()} />)
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/^médicament$/i), 'Paracétamol')
    await user.type(screen.getByLabelText(/^posologie$/i), '500mg')
    await user.click(screen.getByRole('button', { name: /terminer et envoyer l'ordonnance/i }))

    expect(await screen.findByText(/une erreur est survenue\. veuillez réessayer\./i)).toBeInTheDocument()
  })
})

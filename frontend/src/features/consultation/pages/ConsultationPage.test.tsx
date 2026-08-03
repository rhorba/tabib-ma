import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedConsultation } from '@/test/consultationHandlers'
import { ConsultationPage } from './ConsultationPage'

function renderPage(appointmentId: string) {
  return renderWithProviders(
    <Routes>
      <Route path="/appointments/:appointmentId/consultation" element={<ConsultationPage />} />
    </Routes>,
    { initialEntries: [`/appointments/${appointmentId}/consultation`] }
  )
}

describe('ConsultationPage', () => {
  it('shows the not-yet-open gate before the join window opens', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    seedConsultation({
      appointmentId: 'appt-1',
      doctorId: 'doc-1',
      patientId: '1',
      status: 'SCHEDULED',
      joinable: false,
    })
    renderPage('appt-1')

    expect(await screen.findByText(/la salle vidéo n'est pas disponible actuellement/i)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /rejoindre la consultation vidéo/i })).not.toBeInTheDocument()
  })

  it('shows a Join button once the join window is open', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    seedConsultation({
      appointmentId: 'appt-1',
      doctorId: 'doc-1',
      patientId: '1',
      status: 'SCHEDULED',
      joinable: true,
    })
    renderPage('appt-1')

    expect(await screen.findByRole('button', { name: /rejoindre la consultation vidéo/i })).toBeInTheDocument()
  })

  it('falls back to a permission-denied state after joining without camera/mic access', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    seedConsultation({
      appointmentId: 'appt-1',
      doctorId: 'doc-1',
      patientId: '1',
      status: 'SCHEDULED',
      joinable: true,
    })
    renderPage('appt-1')
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: /rejoindre la consultation vidéo/i }))

    // jsdom has no navigator.mediaDevices at all, so the real hook's
    // getUserMedia call rejects exactly like a browser permission denial would
    // — this exercises useConsultationCall's actual join()/error handling, not
    // a mock of it.
    expect(await screen.findByText(/accès à la caméra\/micro refusé/i)).toBeInTheDocument()
  })

  it('shows a not-found message when no consultation exists for the appointment', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    renderPage('appt-missing')

    expect(await screen.findByText(/aucune consultation vidéo n'existe pour ce rendez-vous/i)).toBeInTheDocument()
  })
})

import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedDoctorProfile } from '@/test/clinicHandlers'
import { seedAvailabilitySlot } from '@/test/bookingHandlers'
import { BookAppointmentPage } from './BookAppointmentPage'

function renderAt(path: string) {
  return renderWithProviders(
    <Routes>
      <Route path="/doctors/:doctorProfileId/book" element={<BookAppointmentPage />} />
    </Routes>,
    { initialEntries: [path] }
  )
}

describe('BookAppointmentPage', () => {
  it('shows a not-found message for a doctor that does not exist', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    renderAt('/doctors/999/book')

    expect(await screen.findByText(/médecin n'est pas disponible/i)).toBeInTheDocument()
  })

  it('books the selected slot and shows the confirmed result', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    const profile = seedDoctorProfile({
      userId: '1',
      specialty: 'Cardiologie',
      consultationFeeMad: 300,
      city: 'Rabat',
      verificationStatus: 'APPROVED',
    })
    const soon = new Date(Date.now() + 3600 * 1000).toISOString()
    seedAvailabilitySlot({ doctorProfileId: profile.id, startsAt: soon, endsAt: soon, locationType: 'IN_PERSON' })
    renderAt(`/doctors/${profile.id}/book`)
    const user = userEvent.setup()

    const slotButton = await screen.findByRole(
      'button',
      { name: new Date(soon).toLocaleString() },
      { timeout: 3000 }
    )
    await user.click(slotButton)
    await user.click(screen.getByRole('button', { name: /confirmer et payer/i }))

    expect(await screen.findByText(/rendez-vous est confirmé/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /voir mes rendez-vous/i })).toHaveAttribute(
      'href',
      '/appointments'
    )
  })

  it('shows a conflict message when the slot is taken between selection and confirmation', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    const profile = seedDoctorProfile({
      userId: '1',
      specialty: 'Cardiologie',
      consultationFeeMad: 300,
      city: 'Rabat',
      verificationStatus: 'APPROVED',
    })
    const soon = new Date(Date.now() + 3600 * 1000).toISOString()
    const slot = seedAvailabilitySlot({
      doctorProfileId: profile.id,
      startsAt: soon,
      endsAt: soon,
      locationType: 'IN_PERSON',
    })
    renderAt(`/doctors/${profile.id}/book`)
    const user = userEvent.setup()

    const slotButton = await screen.findByRole('button', { name: new Date(soon).toLocaleString() })
    await user.click(slotButton)
    // Simulate another patient winning the race after the picker rendered but
    // before this one confirms — exactly the TOCTOU window Story 4.3 guards.
    slot.booked = true
    await user.click(screen.getByRole('button', { name: /confirmer et payer/i }))

    expect(await screen.findByText(/vient d'être réservé/i)).toBeInTheDocument()
  })
})

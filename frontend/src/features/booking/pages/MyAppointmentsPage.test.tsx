import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedAppointment, seedAvailabilitySlot } from '@/test/bookingHandlers'
import { MyAppointmentsPage } from './MyAppointmentsPage'

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route path="/appointments" element={<MyAppointmentsPage />} />
      <Route path="/doctors/:doctorProfileId/book" element={<div>booking page</div>} />
    </Routes>,
    { initialEntries: ['/appointments'] }
  )
}

describe('MyAppointmentsPage', () => {
  it('shows the empty state with no appointments', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    renderPage()

    expect(await screen.findByText(/vous n'avez pas encore de rendez-vous/i)).toBeInTheDocument()
  })

  it('shows a CONFIRMED appointment with cancel and reschedule actions', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    const slot = seedAvailabilitySlot({
      doctorProfileId: 'doc-1',
      startsAt: '2026-08-03T09:00:00Z',
      endsAt: '2026-08-03T09:30:00Z',
      locationType: 'IN_PERSON',
      booked: true,
    })
    seedAppointment({
      patientId: '1',
      doctorProfileId: 'doc-1',
      availabilitySlotId: slot.id,
      startsAt: slot.startsAt,
      endsAt: slot.endsAt,
      locationType: 'IN_PERSON',
      status: 'CONFIRMED',
      cancellationWindowHours: 24,
    })
    renderPage()

    expect(await screen.findByText(/confirmé/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /annuler/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /reprogrammer/i })).toBeInTheDocument()
  })

  it('does not offer cancel/reschedule for an already cancelled appointment', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    const slot = seedAvailabilitySlot({
      doctorProfileId: 'doc-1',
      startsAt: '2026-08-03T09:00:00Z',
      endsAt: '2026-08-03T09:30:00Z',
      locationType: 'IN_PERSON',
    })
    seedAppointment({
      patientId: '1',
      doctorProfileId: 'doc-1',
      availabilitySlotId: slot.id,
      startsAt: slot.startsAt,
      endsAt: slot.endsAt,
      locationType: 'IN_PERSON',
      status: 'CANCELLED',
      cancellationWindowHours: 24,
    })
    renderPage()

    expect(await screen.findByText(/^annulé$/i)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^annuler$/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /reprogrammer/i })).not.toBeInTheDocument()
  })

  it('cancels a CONFIRMED appointment and updates its status in place', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    const slot = seedAvailabilitySlot({
      doctorProfileId: 'doc-1',
      startsAt: '2026-08-03T09:00:00Z',
      endsAt: '2026-08-03T09:30:00Z',
      locationType: 'IN_PERSON',
      booked: true,
    })
    seedAppointment({
      patientId: '1',
      doctorProfileId: 'doc-1',
      availabilitySlotId: slot.id,
      startsAt: slot.startsAt,
      endsAt: slot.endsAt,
      locationType: 'IN_PERSON',
      status: 'CONFIRMED',
      cancellationWindowHours: 24,
    })
    renderPage()
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: /annuler/i }))

    expect(await screen.findByText(/^annulé$/i)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^annuler$/i })).not.toBeInTheDocument()
  })

  it('reschedules by cancelling the appointment then navigating to the booking page', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    const slot = seedAvailabilitySlot({
      doctorProfileId: 'doc-1',
      startsAt: '2026-08-03T09:00:00Z',
      endsAt: '2026-08-03T09:30:00Z',
      locationType: 'IN_PERSON',
      booked: true,
    })
    seedAppointment({
      patientId: '1',
      doctorProfileId: 'doc-1',
      availabilitySlotId: slot.id,
      startsAt: slot.startsAt,
      endsAt: slot.endsAt,
      locationType: 'IN_PERSON',
      status: 'CONFIRMED',
      cancellationWindowHours: 24,
    })
    renderPage()
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: /reprogrammer/i }))

    expect(await screen.findByText('booking page')).toBeInTheDocument()
  })
})

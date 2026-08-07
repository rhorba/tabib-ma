import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedAppointment, seedAvailabilitySlot } from '@/test/bookingHandlers'
import { seedDoctorProfile } from '@/test/clinicHandlers'
import { seedReview } from '@/test/reviewHandlers'
import { expectNoA11yViolations, withArabic } from '@/test/a11y'
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

  it("shows a doctor's own appointments read-only, with no cancel/reschedule/view-doctor actions", async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    const profile = seedDoctorProfile({
      userId: '1',
      specialty: 'Cardiologie',
      consultationFeeMad: 300,
      city: 'Rabat',
      verificationStatus: 'APPROVED',
    })
    const slot = seedAvailabilitySlot({
      doctorProfileId: profile.id,
      startsAt: '2026-08-03T09:00:00Z',
      endsAt: '2026-08-03T09:30:00Z',
      locationType: 'VIDEO',
      booked: true,
    })
    seedAppointment({
      patientId: '2',
      doctorProfileId: profile.id,
      availabilitySlotId: slot.id,
      startsAt: slot.startsAt,
      endsAt: slot.endsAt,
      locationType: 'VIDEO',
      status: 'CONFIRMED',
      cancellationWindowHours: 24,
    })
    renderPage()

    expect(await screen.findByText(/confirmé/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /rejoindre la vidéo/i })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /annuler/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /reprogrammer/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /voir le médecin/i })).not.toBeInTheDocument()
  })

  it('offers to leave a review for a COMPLETED appointment and hides the button once submitted', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    const slot = seedAvailabilitySlot({
      doctorProfileId: 'doc-1',
      startsAt: '2026-08-03T09:00:00Z',
      endsAt: '2026-08-03T09:30:00Z',
      locationType: 'VIDEO',
      booked: true,
    })
    seedAppointment({
      patientId: '1',
      doctorProfileId: 'doc-1',
      availabilitySlotId: slot.id,
      startsAt: slot.startsAt,
      endsAt: slot.endsAt,
      locationType: 'VIDEO',
      status: 'COMPLETED',
      cancellationWindowHours: 24,
    })
    renderPage()
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: /laisser un avis/i }))
    await user.click(screen.getAllByRole('radio', { name: /étoile/i })[4])
    await user.click(screen.getByRole('button', { name: /envoyer l'avis/i }))

    expect(await screen.findByText(/avis envoyé/i)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /laisser un avis/i })).not.toBeInTheDocument()
  })

  it('shows "already submitted" instead of the review button for an already-reviewed appointment', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    const slot = seedAvailabilitySlot({
      doctorProfileId: 'doc-1',
      startsAt: '2026-08-03T09:00:00Z',
      endsAt: '2026-08-03T09:30:00Z',
      locationType: 'IN_PERSON',
      booked: true,
    })
    const appointment = seedAppointment({
      patientId: '1',
      doctorProfileId: 'doc-1',
      availabilitySlotId: slot.id,
      startsAt: slot.startsAt,
      endsAt: slot.endsAt,
      locationType: 'IN_PERSON',
      status: 'COMPLETED',
      cancellationWindowHours: 24,
    })
    seedReview({ appointmentId: appointment.id, patientId: '1', doctorProfileId: 'doc-1', rating: 4 })
    renderPage()

    expect(await screen.findByText(/avis envoyé/i)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /laisser un avis/i })).not.toBeInTheDocument()
  })

  it('lets a patient report a problem on a CONFIRMED appointment', async () => {
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

    await user.click(await screen.findByRole('button', { name: /signaler un problème/i }))
    await user.type(screen.getByLabelText(/description/i), "Le medecin n'est pas venu.")
    await user.click(screen.getByRole('button', { name: /envoyer le signalement/i }))

    expect(await screen.findByText(/problème signalé/i)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^signaler un problème$/i })).not.toBeInTheDocument()
  })

  it("offers a doctor the same 'report a problem' action on their own read-only view", async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    const profile = seedDoctorProfile({
      userId: '1',
      specialty: 'Cardiologie',
      consultationFeeMad: 300,
      city: 'Rabat',
      verificationStatus: 'APPROVED',
    })
    const slot = seedAvailabilitySlot({
      doctorProfileId: profile.id,
      startsAt: '2026-08-03T09:00:00Z',
      endsAt: '2026-08-03T09:30:00Z',
      locationType: 'IN_PERSON',
      booked: true,
    })
    seedAppointment({
      patientId: '2',
      doctorProfileId: profile.id,
      availabilitySlotId: slot.id,
      startsAt: slot.startsAt,
      endsAt: slot.endsAt,
      locationType: 'IN_PERSON',
      status: 'CONFIRMED',
      cancellationWindowHours: 24,
    })
    renderPage()

    expect(await screen.findByRole('button', { name: /signaler un problème/i })).toBeInTheDocument()
  })

  it('has no automated accessibility violations with a CONFIRMED appointment shown (NFR-6)', async () => {
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
    const { container } = renderPage()
    await screen.findByText(/confirmé/i)

    await expectNoA11yViolations(container)
  })

  it('has no automated accessibility violations in Arabic (RTL, NFR-6)', async () => {
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

    await withArabic(async () => {
      const { container } = renderPage()
      await screen.findByText(/مؤكد/i)
      await expectNoA11yViolations(container)
    })
  })
})

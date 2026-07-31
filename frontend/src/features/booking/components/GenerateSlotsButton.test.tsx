import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedDoctorProfile } from '@/test/clinicHandlers'
import { seedAvailabilitySlot } from '@/test/bookingHandlers'
import { GenerateSlotsButton } from './GenerateSlotsButton'

describe('GenerateSlotsButton', () => {
  it('shows the count of open slots returned by the generate endpoint', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    const profile = seedDoctorProfile({
      userId: '1',
      specialty: 'Cardiologie',
      consultationFeeMad: 300,
      city: 'Rabat',
      verificationStatus: 'APPROVED',
    })
    seedAvailabilitySlot({
      doctorProfileId: profile.id,
      startsAt: '2026-08-03T09:00:00Z',
      endsAt: '2026-08-03T09:30:00Z',
      locationType: 'IN_PERSON',
    })
    renderWithProviders(<GenerateSlotsButton />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /générer les créneaux/i }))

    expect(await screen.findByText(/1 créneaux générés/i)).toBeInTheDocument()
  })

  it('shows an error when the doctor has no profile yet', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    renderWithProviders(<GenerateSlotsButton />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /générer les créneaux/i }))

    expect(await screen.findByText(/une erreur est survenue/i)).toBeInTheDocument()
  })
})

import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedDoctorProfile } from '@/test/clinicHandlers'
import { apiClient } from '@/shared/api/client'
import { AvailabilityRulesList } from './AvailabilityRulesList'

describe('AvailabilityRulesList', () => {
  it('shows the empty state with no rules', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    seedDoctorProfile({
      userId: '1',
      specialty: 'Cardiologie',
      consultationFeeMad: 300,
      city: 'Rabat',
      verificationStatus: 'APPROVED',
    })
    renderWithProviders(<AvailabilityRulesList />)

    expect(await screen.findByText(/aucun créneau récurrent/i)).toBeInTheDocument()
  })

  it('lists an existing rule', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    seedDoctorProfile({
      userId: '1',
      specialty: 'Cardiologie',
      consultationFeeMad: 300,
      city: 'Rabat',
      verificationStatus: 'APPROVED',
    })
    await apiClient.POST('/api/v1/booking/availability/rules', {
      body: {
        dayOfWeek: 'TUESDAY',
        startTime: '09:00:00',
        endTime: '12:00:00',
        slotDurationMinutes: 30,
        locationType: 'VIDEO',
      },
    })
    renderWithProviders(<AvailabilityRulesList />)

    expect(await screen.findByText(/mardi/i)).toBeInTheDocument()
    expect(screen.getByText(/09:00–12:00/)).toBeInTheDocument()
    expect(screen.getByText(/téléconsultation/i)).toBeInTheDocument()
  })
})

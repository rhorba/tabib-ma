import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedDoctorProfile } from '@/test/clinicHandlers'
import { apiClient } from '@/shared/api/client'
import { AvailabilityExceptionsList } from './AvailabilityExceptionsList'

describe('AvailabilityExceptionsList', () => {
  it('shows the empty state with no exceptions', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    seedDoctorProfile({
      userId: '1',
      specialty: 'Cardiologie',
      consultationFeeMad: 300,
      city: 'Rabat',
      verificationStatus: 'APPROVED',
    })
    renderWithProviders(<AvailabilityExceptionsList />)

    expect(await screen.findByText(/aucun jour bloqué/i)).toBeInTheDocument()
  })

  it('lists an existing exception with its reason', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    seedDoctorProfile({
      userId: '1',
      specialty: 'Cardiologie',
      consultationFeeMad: 300,
      city: 'Rabat',
      verificationStatus: 'APPROVED',
    })
    await apiClient.POST('/api/v1/booking/availability/exceptions', {
      body: { exceptionDate: '2026-08-10', reason: 'Vacances' },
    })
    renderWithProviders(<AvailabilityExceptionsList />)

    expect(await screen.findByText('2026-08-10')).toBeInTheDocument()
    expect(screen.getByText('Vacances')).toBeInTheDocument()
  })
})

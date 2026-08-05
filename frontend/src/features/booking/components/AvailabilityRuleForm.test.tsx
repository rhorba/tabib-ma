import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen, waitFor } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedClinic, seedClinicStaffMembership, seedDoctorProfile } from '@/test/clinicHandlers'
import { apiClient } from '@/shared/api/client'
import { AvailabilityRuleForm } from './AvailabilityRuleForm'

describe('AvailabilityRuleForm', () => {
  it('submits the default Monday 09:00-17:00 rule with seconds appended to the times', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    const profile = seedDoctorProfile({
      userId: '1',
      specialty: 'Cardiologie',
      consultationFeeMad: 300,
      city: 'Rabat',
      verificationStatus: 'APPROVED',
    })
    renderWithProviders(<AvailabilityRuleForm />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /ajouter ce créneau récurrent/i }))

    await waitFor(async () => {
      const { data } = await apiClient.GET('/api/v1/booking/availability/rules')
      expect(data).toHaveLength(1)
      expect(data?.[0]).toMatchObject({
        dayOfWeek: 'MONDAY',
        startTime: '09:00:00',
        endTime: '17:00:00',
        slotDurationMinutes: 30,
        locationType: 'IN_PERSON',
      })
    })
    // profile is only used to seed ownership; referencing it keeps the test explicit about whose rule this is
    expect(profile.userId).toBe('1')
  })

  it('shows a client-side error when endTime is before startTime', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    seedDoctorProfile({
      userId: '1',
      specialty: 'Cardiologie',
      consultationFeeMad: 300,
      city: 'Rabat',
      verificationStatus: 'APPROVED',
    })
    renderWithProviders(<AvailabilityRuleForm />)
    const user = userEvent.setup()

    const endTimeInput = screen.getByLabelText(/heure de fin/i)
    await user.clear(endTimeInput)
    await user.type(endTimeInput, '0800')
    await user.click(screen.getByRole('button', { name: /ajouter ce créneau récurrent/i }))

    expect(await screen.findByText(/après l'heure de début/i)).toBeInTheDocument()
  })

  it('shows the clinic picker for a doctor who is staff at a clinic (IN_PERSON is the default)', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    const profile = seedDoctorProfile({
      userId: '1',
      specialty: 'Cardiologie',
      consultationFeeMad: 300,
      city: 'Rabat',
      verificationStatus: 'APPROVED',
    })
    const clinic = seedClinic({ adminUserId: '2', name: 'Cabinet Al Amal', city: 'Rabat' })
    seedClinicStaffMembership({ doctorProfileId: profile.id, clinicId: clinic.id })
    renderWithProviders(<AvailabilityRuleForm />)

    expect(await screen.findByText('Clinique')).toBeInTheDocument()
  })

  it('does not show a clinic picker for a doctor with no clinic memberships', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    seedDoctorProfile({
      userId: '1',
      specialty: 'Cardiologie',
      consultationFeeMad: 300,
      city: 'Rabat',
      verificationStatus: 'APPROVED',
    })
    renderWithProviders(<AvailabilityRuleForm />)

    await screen.findByRole('button', { name: /ajouter ce créneau récurrent/i })
    expect(screen.queryByText('Clinique')).not.toBeInTheDocument()
  })
})

import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedDoctorProfile } from '@/test/clinicHandlers'
import { apiClient } from '@/shared/api/client'
import { AvailabilityExceptionForm } from './AvailabilityExceptionForm'

function ownProfile() {
  return seedDoctorProfile({
    userId: '1',
    specialty: 'Cardiologie',
    consultationFeeMad: 300,
    city: 'Rabat',
    verificationStatus: 'APPROVED',
  })
}

describe('AvailabilityExceptionForm', () => {
  it('submits a blocked date with a reason', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    ownProfile()
    renderWithProviders(<AvailabilityExceptionForm />)
    const user = userEvent.setup()

    fireEvent.change(screen.getByLabelText(/^date$/i), { target: { value: '2026-08-10' } })
    await user.type(screen.getByLabelText(/motif/i), 'Vacances')
    await user.click(screen.getByRole('button', { name: /bloquer cette date/i }))

    await waitFor(async () => {
      const { data } = await apiClient.GET('/api/v1/booking/availability/exceptions')
      expect(data).toEqual([expect.objectContaining({ exceptionDate: '2026-08-10', reason: 'Vacances' })])
    })
  })

  it('shows a conflict message when the date is already blocked', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    const profile = ownProfile()
    await apiClient.POST('/api/v1/booking/availability/exceptions', {
      body: { exceptionDate: '2026-08-10' },
    })
    renderWithProviders(<AvailabilityExceptionForm />)
    const user = userEvent.setup()

    fireEvent.change(screen.getByLabelText(/^date$/i), { target: { value: '2026-08-10' } })
    await user.click(screen.getByRole('button', { name: /bloquer cette date/i }))

    expect(await screen.findByText(/déjà bloquée/i)).toBeInTheDocument()
    expect(profile.verificationStatus).toBe('APPROVED')
  })
})

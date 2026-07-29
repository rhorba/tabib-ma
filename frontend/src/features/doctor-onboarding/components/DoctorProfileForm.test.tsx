import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen, waitFor } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { apiClient } from '@/shared/api/client'
import { DoctorProfileForm } from './DoctorProfileForm'

describe('DoctorProfileForm', () => {
  it('rejects an empty specialty and city', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    renderWithProviders(<DoctorProfileForm />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /créer mon profil/i }))

    expect(await screen.findByText(/spécialité est requise/i)).toBeInTheDocument()
    expect(await screen.findByText(/ville est requise/i)).toBeInTheDocument()
  })

  it('submits a valid profile and it becomes retrievable via GET /me', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    renderWithProviders(<DoctorProfileForm />)
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/spécialité/i), 'Cardiologie')
    await user.type(screen.getByLabelText(/ville/i), 'Rabat')
    await user.clear(screen.getByLabelText(/tarif de consultation/i))
    await user.type(screen.getByLabelText(/tarif de consultation/i), '300')
    await user.click(screen.getByRole('button', { name: /créer mon profil/i }))

    await waitFor(async () => {
      const { data } = await apiClient.GET('/api/v1/clinic/doctor-profiles/me')
      expect(data?.specialty).toBe('Cardiologie')
      expect(data?.verificationStatus).toBe('PENDING')
    })
  })
})

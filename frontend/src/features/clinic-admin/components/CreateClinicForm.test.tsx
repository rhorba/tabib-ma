import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen, waitFor } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { apiClient } from '@/shared/api/client'
import { CreateClinicForm } from './CreateClinicForm'

describe('CreateClinicForm', () => {
  it('rejects an empty name and city', async () => {
    loginAs({ email: 'ca@example.com', password: 'x', role: 'CLINIC_ADMIN', firstName: 'A', lastName: 'B' })
    renderWithProviders(<CreateClinicForm />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /créer ma clinique/i }))

    expect(await screen.findByText(/nom de la clinique est requis/i)).toBeInTheDocument()
    expect(await screen.findByText(/ville est requise/i)).toBeInTheDocument()
  })

  it('submits a valid clinic and it becomes retrievable via GET /me', async () => {
    loginAs({ email: 'ca@example.com', password: 'x', role: 'CLINIC_ADMIN', firstName: 'A', lastName: 'B' })
    renderWithProviders(<CreateClinicForm />)
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/nom de la clinique/i), 'Clinique Al Amal')
    await user.type(screen.getByLabelText(/ville/i), 'Rabat')
    await user.click(screen.getByRole('button', { name: /créer ma clinique/i }))

    await waitFor(async () => {
      const { data } = await apiClient.GET('/api/v1/clinic/clinics/me')
      expect(data?.name).toBe('Clinique Al Amal')
      expect(data?.city).toBe('Rabat')
    })
  })
})

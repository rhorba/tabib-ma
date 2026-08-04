import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen, waitFor } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedClinic } from '@/test/clinicHandlers'
import { apiClient } from '@/shared/api/client'
import { ClinicResourceForm } from './ClinicResourceForm'

describe('ClinicResourceForm', () => {
  it('rejects an empty name', async () => {
    loginAs({ email: 'ca@example.com', password: 'x', role: 'CLINIC_ADMIN', firstName: 'A', lastName: 'B' })
    const clinic = seedClinic({ adminUserId: '1', name: 'Clinique Al Amal', city: 'Rabat' })
    renderWithProviders(<ClinicResourceForm clinicId={clinic.id} />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /ajouter la ressource/i }))

    expect(await screen.findByText(/nom de la ressource est requis/i)).toBeInTheDocument()
  })

  it('creates a room resource and it becomes retrievable via GET resources', async () => {
    loginAs({ email: 'ca@example.com', password: 'x', role: 'CLINIC_ADMIN', firstName: 'A', lastName: 'B' })
    const clinic = seedClinic({ adminUserId: '1', name: 'Clinique Al Amal', city: 'Rabat' })
    renderWithProviders(<ClinicResourceForm clinicId={clinic.id} />)
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/^nom$/i), 'Salle 1')
    await user.click(screen.getByRole('button', { name: /ajouter la ressource/i }))

    await waitFor(async () => {
      const { data } = await apiClient.GET('/api/v1/clinic/clinics/{clinicId}/resources', {
        params: { path: { clinicId: clinic.id } },
      })
      expect(data?.[0]?.name).toBe('Salle 1')
      expect(data?.[0]?.type).toBe('ROOM')
      expect(data?.[0]?.active).toBe(true)
    })
  })

  it('resets the form after a successful submission', async () => {
    loginAs({ email: 'ca@example.com', password: 'x', role: 'CLINIC_ADMIN', firstName: 'A', lastName: 'B' })
    const clinic = seedClinic({ adminUserId: '1', name: 'Clinique Al Amal', city: 'Rabat' })
    renderWithProviders(<ClinicResourceForm clinicId={clinic.id} />)
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/^nom$/i), 'Salle 1')
    await user.click(screen.getByRole('button', { name: /ajouter la ressource/i }))

    await waitFor(() => expect(screen.getByLabelText(/^nom$/i)).toHaveValue(''))
  })
})

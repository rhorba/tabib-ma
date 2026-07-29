import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen, waitFor } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedClinic } from '@/test/clinicHandlers'
import { apiClient } from '@/shared/api/client'
import { InviteDoctorForm } from './InviteDoctorForm'

describe('InviteDoctorForm', () => {
  it('rejects an invalid email', async () => {
    loginAs({ email: 'ca@example.com', password: 'x', role: 'CLINIC_ADMIN', firstName: 'A', lastName: 'B' })
    const clinic = seedClinic({ adminUserId: '1', name: 'Clinique Al Amal', city: 'Rabat' })
    renderWithProviders(<InviteDoctorForm clinicId={clinic.id} />)
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/adresse e-mail du médecin/i), 'not-an-email')
    await user.click(screen.getByRole('button', { name: /inviter/i }))

    expect(await screen.findByText(/adresse e-mail invalide/i)).toBeInTheDocument()
  })

  it('sends a valid invite and it becomes retrievable via GET invitations', async () => {
    loginAs({ email: 'ca@example.com', password: 'x', role: 'CLINIC_ADMIN', firstName: 'A', lastName: 'B' })
    const clinic = seedClinic({ adminUserId: '1', name: 'Clinique Al Amal', city: 'Rabat' })
    renderWithProviders(<InviteDoctorForm clinicId={clinic.id} />)
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/adresse e-mail du médecin/i), 'doc@example.com')
    await user.click(screen.getByRole('button', { name: /inviter/i }))

    await waitFor(async () => {
      const { data } = await apiClient.GET('/api/v1/clinic/clinics/{clinicId}/invitations', {
        params: { path: { clinicId: clinic.id } },
      })
      expect(data?.[0]?.invitedEmail).toBe('doc@example.com')
      expect(data?.[0]?.status).toBe('PENDING')
    })
  })

  it('shows an already-invited error on a duplicate pending invite', async () => {
    loginAs({ email: 'ca@example.com', password: 'x', role: 'CLINIC_ADMIN', firstName: 'A', lastName: 'B' })
    const clinic = seedClinic({ adminUserId: '1', name: 'Clinique Al Amal', city: 'Rabat' })
    renderWithProviders(<InviteDoctorForm clinicId={clinic.id} />)
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/adresse e-mail du médecin/i), 'doc@example.com')
    await user.click(screen.getByRole('button', { name: /inviter/i }))
    await waitFor(() => expect(screen.getByLabelText(/adresse e-mail du médecin/i)).toHaveValue(''))

    await user.type(screen.getByLabelText(/adresse e-mail du médecin/i), 'doc@example.com')
    await user.click(screen.getByRole('button', { name: /inviter/i }))

    expect(await screen.findByText(/a déjà une invitation en attente/i)).toBeInTheDocument()
  })
})

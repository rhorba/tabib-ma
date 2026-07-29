import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen, waitFor } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedClinic, seedClinicInvitation, seedDoctorProfile } from '@/test/clinicHandlers'
import { PendingInvitationsList } from './PendingInvitationsList'

describe('PendingInvitationsList', () => {
  it('renders nothing when there are no pending invitations', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    const { container } = renderWithProviders(<PendingInvitationsList />)

    await waitFor(() => expect(container).toBeEmptyDOMElement())
  })

  it('shows the clinic name and lets the doctor accept an invitation', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    seedDoctorProfile({
      userId: '1',
      specialty: 'Cardiologie',
      consultationFeeMad: 300,
      city: 'Rabat',
      verificationStatus: 'PENDING',
    })
    const clinic = seedClinic({ adminUserId: '2', name: 'Clinique Al Amal', city: 'Rabat' })
    seedClinicInvitation({ clinicId: clinic.id, invitedEmail: 'd@example.com' })
    renderWithProviders(<PendingInvitationsList />)
    const user = userEvent.setup()

    expect(await screen.findByText('Clinique Al Amal')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /accepter/i }))

    await waitFor(() => expect(screen.queryByText('Clinique Al Amal')).not.toBeInTheDocument())
  })

  it('lets the doctor decline an invitation', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    const clinic = seedClinic({ adminUserId: '2', name: 'Clinique Al Amal', city: 'Rabat' })
    seedClinicInvitation({ clinicId: clinic.id, invitedEmail: 'd@example.com' })
    renderWithProviders(<PendingInvitationsList />)
    const user = userEvent.setup()

    expect(await screen.findByText('Clinique Al Amal')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /refuser/i }))

    await waitFor(() => expect(screen.queryByText('Clinique Al Amal')).not.toBeInTheDocument())
  })

  it('shows a needs-profile error when accepting without a doctor profile yet', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    const clinic = seedClinic({ adminUserId: '2', name: 'Clinique Al Amal', city: 'Rabat' })
    seedClinicInvitation({ clinicId: clinic.id, invitedEmail: 'd@example.com' })
    renderWithProviders(<PendingInvitationsList />)
    const user = userEvent.setup()

    await screen.findByText('Clinique Al Amal')
    await user.click(screen.getByRole('button', { name: /accepter/i }))

    expect(await screen.findByText(/créer votre profil médecin/i)).toBeInTheDocument()
  })
})

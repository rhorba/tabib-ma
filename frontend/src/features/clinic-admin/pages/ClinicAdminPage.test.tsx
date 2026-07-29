import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedClinic, seedClinicInvitation } from '@/test/clinicHandlers'
import { ClinicAdminPage } from './ClinicAdminPage'

describe('ClinicAdminPage', () => {
  it('shows the create-clinic form when the admin has no clinic yet', async () => {
    loginAs({ email: 'ca@example.com', password: 'x', role: 'CLINIC_ADMIN', firstName: 'A', lastName: 'B' })
    renderWithProviders(<ClinicAdminPage />)

    expect(await screen.findByRole('button', { name: /créer ma clinique/i })).toBeInTheDocument()
  })

  it('shows clinic details, the invite form, and the empty invitations state once a clinic exists', async () => {
    loginAs({ email: 'ca@example.com', password: 'x', role: 'CLINIC_ADMIN', firstName: 'A', lastName: 'B' })
    seedClinic({ adminUserId: '1', name: 'Clinique Al Amal', city: 'Rabat' })
    renderWithProviders(<ClinicAdminPage />)

    expect(await screen.findByText('Clinique Al Amal')).toBeInTheDocument()
    expect(screen.getByText('Rabat')).toBeInTheDocument()
    expect(screen.getByText(/aucune invitation envoyée/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /inviter/i })).toBeInTheDocument()
  })

  it('lists existing invitations with their status', async () => {
    loginAs({ email: 'ca@example.com', password: 'x', role: 'CLINIC_ADMIN', firstName: 'A', lastName: 'B' })
    const clinic = seedClinic({ adminUserId: '1', name: 'Clinique Al Amal', city: 'Rabat' })
    seedClinicInvitation({ clinicId: clinic.id, invitedEmail: 'doc@example.com' })
    renderWithProviders(<ClinicAdminPage />)

    expect(await screen.findByText('doc@example.com')).toBeInTheDocument()
    expect(screen.getByText(/en attente/i)).toBeInTheDocument()
  })
})

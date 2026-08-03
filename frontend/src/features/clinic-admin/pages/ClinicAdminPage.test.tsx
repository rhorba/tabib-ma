import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedClinic, seedClinicDashboard, seedClinicInvitation } from '@/test/clinicHandlers'
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

  it('shows a zero-state dashboard when the clinic has no bookings yet', async () => {
    loginAs({ email: 'ca@example.com', password: 'x', role: 'CLINIC_ADMIN', firstName: 'A', lastName: 'B' })
    seedClinic({ adminUserId: '1', name: 'Clinique Al Amal', city: 'Rabat' })
    renderWithProviders(<ClinicAdminPage />)

    expect(await screen.findByText('Tableau de bord')).toBeInTheDocument()
    expect(screen.getByText('Rendez-vous réservés')).toBeInTheDocument()
    expect(screen.getAllByText('0')).toHaveLength(2)
  })

  it('shows the booking volume and revenue once the clinic has bookings', async () => {
    loginAs({ email: 'ca@example.com', password: 'x', role: 'CLINIC_ADMIN', firstName: 'A', lastName: 'B' })
    const clinic = seedClinic({ adminUserId: '1', name: 'Clinique Al Amal', city: 'Rabat' })
    seedClinicDashboard({ clinicId: clinic.id, bookingVolume: 3, revenueMad: 900 })
    renderWithProviders(<ClinicAdminPage />)

    expect(await screen.findByText('Tableau de bord')).toBeInTheDocument()
    expect(screen.getByText('3')).toBeInTheDocument()
    expect(screen.getByText('900')).toBeInTheDocument()
  })
})

import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedDoctorProfile } from '@/test/clinicHandlers'
import { DoctorAvailabilityPage } from './DoctorAvailabilityPage'

describe('DoctorAvailabilityPage', () => {
  it('prompts to create a doctor profile first when none exists', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    renderWithProviders(<DoctorAvailabilityPage />)

    expect(await screen.findByText(/créer votre profil médecin/i)).toBeInTheDocument()
    expect(screen.queryByText(/créneaux récurrents/i)).not.toBeInTheDocument()
  })

  it('renders the rules, exceptions, and generate sections for an existing profile', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    seedDoctorProfile({
      userId: '1',
      specialty: 'Cardiologie',
      consultationFeeMad: 300,
      city: 'Rabat',
      verificationStatus: 'APPROVED',
    })
    renderWithProviders(<DoctorAvailabilityPage />)

    expect(await screen.findByText(/créneaux récurrents/i)).toBeInTheDocument()
    expect(screen.getByText(/jours bloqués/i)).toBeInTheDocument()
    expect(screen.getByText('Générer les créneaux')).toBeInTheDocument()
  })
})

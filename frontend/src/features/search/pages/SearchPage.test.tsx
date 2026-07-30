import { describe, expect, it } from 'vitest'
import userEvent from '@testing-library/user-event'
import { renderWithProviders, screen } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedDoctorProfile } from '@/test/clinicHandlers'
import { SearchPage } from './SearchPage'

describe('SearchPage', () => {
  it('shows the empty state when no doctors are approved yet', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    renderWithProviders(<SearchPage />)

    expect(await screen.findByText(/aucun médecin trouvé/i)).toBeInTheDocument()
  })

  it('lists approved doctors matching the specialty and city filters', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    seedDoctorProfile({
      userId: '1',
      specialty: 'Cardiologie',
      consultationFeeMad: 300,
      city: 'Rabat',
      verificationStatus: 'APPROVED',
    })
    const user = userEvent.setup()
    renderWithProviders(<SearchPage />)

    await user.type(screen.getByLabelText(/spécialité/i), 'Cardiologie')
    await user.type(screen.getByLabelText(/ville/i), 'Rabat')
    await user.click(screen.getByRole('button', { name: /^rechercher$/i }))

    expect(await screen.findByText('A B')).toBeInTheDocument()
    expect(screen.getByText('Cardiologie')).toBeInTheDocument()
  })

  it('excludes doctors that are not yet approved', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    seedDoctorProfile({
      userId: '1',
      specialty: 'Dermatologie',
      consultationFeeMad: 200,
      city: 'Fes',
      verificationStatus: 'PENDING',
    })
    renderWithProviders(<SearchPage />)

    expect(await screen.findByText(/aucun médecin trouvé/i)).toBeInTheDocument()
    expect(screen.queryByText('Dermatologie')).not.toBeInTheDocument()
  })
})

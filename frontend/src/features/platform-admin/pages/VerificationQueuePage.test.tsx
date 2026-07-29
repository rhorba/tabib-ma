import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen, waitFor } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedDoctorProfile } from '@/test/clinicHandlers'
import { VerificationQueuePage } from './VerificationQueuePage'

function loginAsAdmin() {
  return loginAs({
    email: 'admin@example.com',
    password: 'x',
    role: 'PLATFORM_ADMIN',
    firstName: 'P',
    lastName: 'A',
  })
}

describe('VerificationQueuePage', () => {
  it('shows the empty state when nothing is pending', async () => {
    loginAsAdmin()
    renderWithProviders(<VerificationQueuePage />)

    expect(await screen.findByText(/aucune soumission en attente/i)).toBeInTheDocument()
  })

  it('lists a pending profile', async () => {
    loginAsAdmin()
    seedDoctorProfile({
      userId: '2',
      specialty: 'Cardiologie',
      consultationFeeMad: 300,
      city: 'Rabat',
      verificationStatus: 'PENDING',
    })
    renderWithProviders(<VerificationQueuePage />)

    expect(await screen.findByText('Cardiologie')).toBeInTheDocument()
    expect(screen.getByText('Rabat')).toBeInTheDocument()
  })

  it('approving a profile removes it from the queue', async () => {
    loginAsAdmin()
    seedDoctorProfile({
      userId: '2',
      specialty: 'Cardiologie',
      consultationFeeMad: 300,
      city: 'Rabat',
      verificationStatus: 'PENDING',
    })
    renderWithProviders(<VerificationQueuePage />)
    await screen.findByText('Cardiologie')

    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: /approuver/i }))

    await waitFor(() => expect(screen.queryByText('Cardiologie')).not.toBeInTheDocument())
    expect(screen.getByText(/aucune soumission en attente/i)).toBeInTheDocument()
  })

  it('rejecting a profile removes it from the queue', async () => {
    loginAsAdmin()
    seedDoctorProfile({
      userId: '2',
      specialty: 'Dermatologie',
      consultationFeeMad: 200,
      city: 'Casablanca',
      verificationStatus: 'PENDING',
    })
    renderWithProviders(<VerificationQueuePage />)
    await screen.findByText('Dermatologie')

    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: /rejeter/i }))

    await waitFor(() => expect(screen.queryByText('Dermatologie')).not.toBeInTheDocument())
  })
})

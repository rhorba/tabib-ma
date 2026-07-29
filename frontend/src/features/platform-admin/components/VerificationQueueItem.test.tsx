import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedDoctorProfile } from '@/test/clinicHandlers'
import { VerificationQueueItem } from './VerificationQueueItem'

describe('VerificationQueueItem', () => {
  it('shows the already-reviewed error when approving a non-pending profile', async () => {
    loginAs({
      email: 'admin@example.com',
      password: 'x',
      role: 'PLATFORM_ADMIN',
      firstName: 'P',
      lastName: 'A',
    })
    const profile = seedDoctorProfile({
      userId: '2',
      specialty: 'Cardiologie',
      consultationFeeMad: 300,
      city: 'Rabat',
      verificationStatus: 'APPROVED',
    })
    renderWithProviders(<VerificationQueueItem profile={{ ...profile }} />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /approuver/i }))

    expect(await screen.findByText(/déjà été examiné/i)).toBeInTheDocument()
  })

  it('toggles the document list open and closed', async () => {
    loginAs({
      email: 'admin@example.com',
      password: 'x',
      role: 'PLATFORM_ADMIN',
      firstName: 'P',
      lastName: 'A',
    })
    const profile = seedDoctorProfile({
      userId: '2',
      specialty: 'Cardiologie',
      consultationFeeMad: 300,
      city: 'Rabat',
      verificationStatus: 'PENDING',
    })
    renderWithProviders(<VerificationQueueItem profile={{ ...profile }} />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /voir les documents/i }))
    expect(await screen.findByText(/aucun document téléversé/i)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /masquer les documents/i }))
    expect(screen.queryByText(/aucun document téléversé/i)).not.toBeInTheDocument()
  })
})

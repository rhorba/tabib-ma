import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen, waitFor } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedDoctorProfile } from '@/test/clinicHandlers'
import { DoctorOnboardingPage } from './DoctorOnboardingPage'

describe('DoctorOnboardingPage', () => {
  it('shows the create-profile form when the doctor has no profile yet', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    renderWithProviders(<DoctorOnboardingPage />)

    expect(await screen.findByRole('button', { name: /créer mon profil/i })).toBeInTheDocument()
  })

  it('shows the status badge and document upload form once a profile exists', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    seedDoctorProfile({
      userId: '1',
      specialty: 'Cardiologie',
      consultationFeeMad: 300,
      city: 'Rabat',
      verificationStatus: 'PENDING',
    })
    renderWithProviders(<DoctorOnboardingPage />)

    expect(await screen.findByText(/en attente de vérification/i)).toBeInTheDocument()
    expect(screen.getByText('Cardiologie')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /téléverser/i })).toBeInTheDocument()
  })

  it('uploading a document adds it to the visible list', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    seedDoctorProfile({
      userId: '1',
      specialty: 'Cardiologie',
      consultationFeeMad: 300,
      city: 'Rabat',
      verificationStatus: 'PENDING',
    })
    renderWithProviders(<DoctorOnboardingPage />)
    await screen.findByText(/en attente de vérification/i)

    const user = userEvent.setup()
    const file = new File(['content'], 'license.pdf', { type: 'application/pdf' })
    await user.upload(screen.getByLabelText(/fichier/i), file)
    await user.click(screen.getByRole('button', { name: /téléverser/i }))

    await waitFor(() => expect(screen.getByRole('listitem')).toHaveTextContent(/licence médicale/i))
  })
})

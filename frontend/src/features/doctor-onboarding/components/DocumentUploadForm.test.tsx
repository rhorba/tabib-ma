import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen, waitFor } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedDoctorProfile } from '@/test/clinicHandlers'
import { apiClient } from '@/shared/api/client'
import { DocumentUploadForm } from './DocumentUploadForm'

function ownProfile(userId: string) {
  return seedDoctorProfile({
    userId,
    specialty: 'Cardiologie',
    consultationFeeMad: 300,
    city: 'Rabat',
    verificationStatus: 'PENDING',
  })
}

describe('DocumentUploadForm', () => {
  it('uploads a valid PDF and it becomes retrievable', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    const profile = ownProfile('1')
    renderWithProviders(<DocumentUploadForm doctorProfileId={profile.id} />)
    const user = userEvent.setup()

    const file = new File(['content'], 'license.pdf', { type: 'application/pdf' })
    await user.upload(screen.getByLabelText(/fichier/i), file)
    await user.click(screen.getByRole('button', { name: /téléverser/i }))

    await waitFor(async () => {
      const { data } = await apiClient.GET(
        '/api/v1/clinic/doctor-profiles/{doctorProfileId}/documents',
        { params: { path: { doctorProfileId: profile.id } } }
      )
      expect(data).toHaveLength(1)
      expect(data?.[0].documentType).toBe('MEDICAL_LICENSE')
    })
  })

  it('rejects a disallowed content type client-side, mirroring the backend check', async () => {
    loginAs({ email: 'd@example.com', password: 'x', role: 'DOCTOR', firstName: 'A', lastName: 'B' })
    const profile = ownProfile('1')
    renderWithProviders(<DocumentUploadForm doctorProfileId={profile.id} />)
    // The input's accept attribute already filters this in a real browser (and
    // in user-event's default simulation of that); disable that simulation so
    // this test can exercise the zod-side content-type check independently.
    const user = userEvent.setup({ applyAccept: false })

    const badFile = new File(['content'], 'payload.exe', { type: 'application/x-msdownload' })
    await user.upload(screen.getByLabelText(/fichier/i), badFile)
    await user.click(screen.getByRole('button', { name: /téléverser/i }))

    expect(await screen.findByText(/seuls les fichiers pdf, png et jpeg/i)).toBeInTheDocument()
  })
})

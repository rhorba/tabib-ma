import { Route, Routes } from 'react-router'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedDoctorProfile } from '@/test/clinicHandlers'
import { seedReview } from '@/test/reviewHandlers'
import { DoctorPublicProfilePage } from './DoctorPublicProfilePage'

function renderAt(path: string) {
  return renderWithProviders(
    <Routes>
      <Route path="/doctors/:doctorProfileId" element={<DoctorPublicProfilePage />} />
    </Routes>,
    { initialEntries: [path] }
  )
}

describe('DoctorPublicProfilePage', () => {
  it('shows a not-found message for a profile that does not exist', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    renderAt('/doctors/999')

    expect(await screen.findByText(/médecin n'est pas disponible/i)).toBeInTheDocument()
  })

  it('shows a not-found message for a profile that is not yet approved', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    const profile = seedDoctorProfile({
      userId: '1',
      specialty: 'Neurologie',
      consultationFeeMad: 250,
      city: 'Meknes',
      verificationStatus: 'PENDING',
    })
    renderAt(`/doctors/${profile.id}`)

    expect(await screen.findByText(/médecin n'est pas disponible/i)).toBeInTheDocument()
  })

  it('shows the profile details and "no reviews yet" for an approved profile', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    const profile = seedDoctorProfile({
      userId: '1',
      specialty: 'Neurologie',
      bio: 'Experienced neurologist',
      consultationFeeMad: 250,
      city: 'Meknes',
      verificationStatus: 'APPROVED',
    })
    renderAt(`/doctors/${profile.id}`)

    expect(await screen.findByText('Neurologie')).toBeInTheDocument()
    expect(screen.getByText('Meknes')).toBeInTheDocument()
    expect(screen.getByText('Experienced neurologist')).toBeInTheDocument()
    expect(screen.getByText(/pas encore d'avis/i)).toBeInTheDocument()
  })

  it('shows the real average rating and recent review comments once reviews exist', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    const profile = seedDoctorProfile({
      userId: '1',
      specialty: 'Neurologie',
      consultationFeeMad: 250,
      city: 'Meknes',
      verificationStatus: 'APPROVED',
    })
    seedReview({
      appointmentId: 'appt-1',
      patientId: '2',
      doctorProfileId: profile.id,
      rating: 5,
      comment: 'Excellent suivi',
    })
    seedReview({ appointmentId: 'appt-2', patientId: '3', doctorProfileId: profile.id, rating: 3 })
    renderAt(`/doctors/${profile.id}`)

    expect(await screen.findByText('4.0/5 (2 avis)')).toBeInTheDocument()
    expect(screen.getByText('Excellent suivi')).toBeInTheDocument()
    expect(screen.getByText('5/5')).toBeInTheDocument()
    expect(screen.getByText('3/5')).toBeInTheDocument()
  })
})

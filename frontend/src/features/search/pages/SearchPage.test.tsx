import { describe, expect, it } from 'vitest'
import userEvent from '@testing-library/user-event'
import { renderWithProviders, screen } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedDoctorProfile } from '@/test/clinicHandlers'
import { expectNoA11yViolations, withArabic } from '@/test/a11y'
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

  it('excludes doctors above the max-fee filter', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    seedDoctorProfile({
      userId: '1',
      specialty: 'Endodontie',
      consultationFeeMad: 100,
      city: 'Sale',
      verificationStatus: 'APPROVED',
    })
    seedDoctorProfile({
      userId: '2',
      specialty: 'Endodontie',
      consultationFeeMad: 400,
      city: 'Sale',
      verificationStatus: 'APPROVED',
    })
    const user = userEvent.setup()
    renderWithProviders(<SearchPage />)

    await user.type(screen.getByLabelText(/spécialité/i), 'Endodontie')
    await user.type(screen.getByLabelText(/tarif maximum/i), '200')
    await user.click(screen.getByRole('button', { name: /^rechercher$/i }))

    expect(await screen.findByText('100')).toBeInTheDocument()
    expect(screen.queryByText('400')).not.toBeInTheDocument()
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

  it('has no automated accessibility violations with results shown (NFR-6)', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    seedDoctorProfile({
      userId: '1',
      specialty: 'Cardiologie',
      consultationFeeMad: 300,
      city: 'Rabat',
      verificationStatus: 'APPROVED',
    })
    const { container } = renderWithProviders(<SearchPage />)
    await screen.findByText('A B')

    await expectNoA11yViolations(container)
  })

  it('has no automated accessibility violations in Arabic (RTL, NFR-6)', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    seedDoctorProfile({
      userId: '1',
      specialty: 'Cardiologie',
      consultationFeeMad: 300,
      city: 'Rabat',
      verificationStatus: 'APPROVED',
    })

    await withArabic(async () => {
      const { container } = renderWithProviders(<SearchPage />)
      await screen.findByText('A B')
      await expectNoA11yViolations(container)
    })
  })
})

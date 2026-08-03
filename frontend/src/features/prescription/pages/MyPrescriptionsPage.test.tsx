import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { renderWithProviders, screen, waitFor } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedPrescription } from '@/test/prescriptionHandlers'
import { MyPrescriptionsPage } from './MyPrescriptionsPage'

describe('MyPrescriptionsPage', () => {
  it('shows the empty state with no prescriptions', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    renderWithProviders(<MyPrescriptionsPage />)

    expect(await screen.findByText(/vous n'avez pas encore d'ordonnance/i)).toBeInTheDocument()
  })

  it('lists a prescription with its medications', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    seedPrescription({
      consultationId: 'c-1',
      doctorId: 'doc-1',
      patientId: '1',
      items: [{ medicationName: 'Paracétamol', dosage: '500mg', instructions: 'Matin et soir' }],
    })
    renderWithProviders(<MyPrescriptionsPage />)

    expect(await screen.findByText('Paracétamol', { exact: false })).toBeInTheDocument()
    expect(screen.getByText(/500mg/)).toBeInTheDocument()
    expect(screen.getByText(/matin et soir/i)).toBeInTheDocument()
  })

  it('does not show a prescription belonging to another patient', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    seedPrescription({
      consultationId: 'c-1',
      doctorId: 'doc-1',
      patientId: '999',
      items: [{ medicationName: 'Amoxicilline', dosage: '250mg' }],
    })
    renderWithProviders(<MyPrescriptionsPage />)

    expect(await screen.findByText(/vous n'avez pas encore d'ordonnance/i)).toBeInTheDocument()
    expect(screen.queryByText('Amoxicilline', { exact: false })).not.toBeInTheDocument()
  })

  it('shows a corrected notice on a prescription that has since been superseded', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    const original = seedPrescription({
      consultationId: 'c-1',
      doctorId: 'doc-1',
      patientId: '1',
      items: [{ medicationName: 'Ibuprofène', dosage: '200mg' }],
    })
    seedPrescription({
      consultationId: 'c-1',
      doctorId: 'doc-1',
      patientId: '1',
      supersedesId: original.id,
      items: [{ medicationName: 'Ibuprofène', dosage: '400mg' }],
    })
    renderWithProviders(<MyPrescriptionsPage />)

    expect(await screen.findAllByText(/a été corrigée par une version plus récente/i)).toHaveLength(1)
  })

  it('downloads the PDF when the download button is clicked', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    seedPrescription({
      consultationId: 'c-1',
      doctorId: 'doc-1',
      patientId: '1',
      items: [{ medicationName: 'Paracétamol', dosage: '500mg' }],
    })
    URL.createObjectURL = vi.fn().mockReturnValue('blob:fake-url')
    URL.revokeObjectURL = vi.fn()
    renderWithProviders(<MyPrescriptionsPage />)
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: /télécharger le pdf/i }))

    await waitFor(() => expect(URL.createObjectURL).toHaveBeenCalled())
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:fake-url')
  })
})

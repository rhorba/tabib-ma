import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedDispute } from '@/test/adminHandlers'
import { DisputeQueueItem } from './DisputeQueueItem'

function loginAsAdmin() {
  return loginAs({
    email: 'admin@example.com',
    password: 'x',
    role: 'PLATFORM_ADMIN',
    firstName: 'P',
    lastName: 'A',
  })
}

describe('DisputeQueueItem', () => {
  it('shows dispute context and a system-reported note when self-reported by no one', async () => {
    loginAsAdmin()
    const dispute = seedDispute({
      type: 'NO_SHOW',
      patientName: 'Amine Patient',
      doctorName: 'Dr. Sara Doctor',
      reportedByUserId: undefined,
    })
    renderWithProviders(<DisputeQueueItem dispute={{ ...dispute }} />)

    expect(screen.getByText('Absence')).toBeInTheDocument()
    expect(screen.getByText('Amine Patient')).toBeInTheDocument()
    expect(screen.getByText('Dr. Sara Doctor')).toBeInTheDocument()
    expect(screen.getByText(/signalé automatiquement/i)).toBeInTheDocument()
  })

  it('refunds the payment and shows a success message', async () => {
    loginAsAdmin()
    const dispute = seedDispute({ hasSucceededPayment: true })
    renderWithProviders(<DisputeQueueItem dispute={{ ...dispute }} />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /rembourser/i }))

    expect(await screen.findByText(/paiement remboursé/i)).toBeInTheDocument()
  })

  it('shows the not-refundable error when the appointment has no succeeded payment', async () => {
    loginAsAdmin()
    const dispute = seedDispute({ hasSucceededPayment: false })
    renderWithProviders(<DisputeQueueItem dispute={{ ...dispute }} />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /rembourser/i }))

    expect(await screen.findByText(/n'est plus remboursable/i)).toBeInTheDocument()
  })

  it('force-cancels the appointment and shows a success message', async () => {
    loginAsAdmin()
    const dispute = seedDispute({})
    renderWithProviders(<DisputeQueueItem dispute={{ ...dispute }} />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /annuler le rendez-vous/i }))

    expect(await screen.findByText(/rendez-vous annulé/i)).toBeInTheDocument()
  })

  it('resolves the dispute', async () => {
    loginAsAdmin()
    const dispute = seedDispute({})
    renderWithProviders(<DisputeQueueItem dispute={{ ...dispute }} />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /marquer comme résolu/i }))

    expect(screen.queryByText(/erreur/i)).not.toBeInTheDocument()
  })
})

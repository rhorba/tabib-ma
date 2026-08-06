import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen, waitFor } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedDispute } from '@/test/adminHandlers'
import { DisputeQueuePage } from './DisputeQueuePage'

function loginAsAdmin() {
  return loginAs({
    email: 'admin@example.com',
    password: 'x',
    role: 'PLATFORM_ADMIN',
    firstName: 'P',
    lastName: 'A',
  })
}

describe('DisputeQueuePage', () => {
  it('shows the empty state when nothing is open', async () => {
    loginAsAdmin()
    renderWithProviders(<DisputeQueuePage />)

    expect(await screen.findByText(/aucun litige ouvert/i)).toBeInTheDocument()
  })

  it('lists an open dispute', async () => {
    loginAsAdmin()
    seedDispute({ patientName: 'Amine Patient', reason: 'Le patient ne répond pas.' })
    renderWithProviders(<DisputeQueuePage />)

    expect(await screen.findByText('Amine Patient')).toBeInTheDocument()
    expect(screen.getByText('Le patient ne répond pas.')).toBeInTheDocument()
  })

  it('resolving a dispute removes it from the queue', async () => {
    loginAsAdmin()
    seedDispute({ patientName: 'Amine Patient' })
    renderWithProviders(<DisputeQueuePage />)
    await screen.findByText('Amine Patient')

    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: /marquer comme résolu/i }))

    await waitFor(() => expect(screen.queryByText('Amine Patient')).not.toBeInTheDocument())
    expect(screen.getByText(/aucun litige ouvert/i)).toBeInTheDocument()
  })
})

import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedPlatformHealth } from '@/test/adminHandlers'
import { PlatformHealthPage } from './PlatformHealthPage'

function loginAsAdmin() {
  return loginAs({
    email: 'admin@example.com',
    password: 'x',
    role: 'PLATFORM_ADMIN',
    firstName: 'P',
    lastName: 'A',
  })
}

describe('PlatformHealthPage', () => {
  it('shows zeroes when nothing has happened yet', async () => {
    loginAsAdmin()
    renderWithProviders(<PlatformHealthPage />)

    expect(await screen.findByText(/qualité des appels vidéo/i)).toBeInTheDocument()
    const zeroes = screen.getAllByText('0')
    expect(zeroes.length).toBeGreaterThan(0)
  })

  it('renders the seeded metric values', async () => {
    loginAsAdmin()
    seedPlatformHealth({
      totalAppointments: 12,
      confirmedAppointments: 5,
      succeededPayments: 8,
      failedPayments: 2,
    })
    renderWithProviders(<PlatformHealthPage />)

    expect(await screen.findByText('12')).toBeInTheDocument()
    expect(screen.getByText('5')).toBeInTheDocument()
    expect(screen.getByText('8')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
  })
})

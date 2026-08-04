import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderWithProviders, screen, waitFor } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedClinic, seedClinicResource } from '@/test/clinicHandlers'
import { ClinicResourceList } from './ClinicResourceList'

describe('ClinicResourceList', () => {
  it('shows the empty state when the clinic has no resources yet', async () => {
    loginAs({ email: 'ca@example.com', password: 'x', role: 'CLINIC_ADMIN', firstName: 'A', lastName: 'B' })
    const clinic = seedClinic({ adminUserId: '1', name: 'Clinique Al Amal', city: 'Rabat' })
    renderWithProviders(<ClinicResourceList clinicId={clinic.id} />)

    expect(await screen.findByText(/aucune ressource/i)).toBeInTheDocument()
  })

  it('lists an active resource with a deactivate button', async () => {
    loginAs({ email: 'ca@example.com', password: 'x', role: 'CLINIC_ADMIN', firstName: 'A', lastName: 'B' })
    const clinic = seedClinic({ adminUserId: '1', name: 'Clinique Al Amal', city: 'Rabat' })
    seedClinicResource({ clinicId: clinic.id, type: 'ROOM', name: 'Salle 1', active: true })
    renderWithProviders(<ClinicResourceList clinicId={clinic.id} />)

    expect(await screen.findByText('Salle 1')).toBeInTheDocument()
    expect(screen.getByText('Active')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /désactiver/i })).toBeInTheDocument()
  })

  it('deactivating a resource updates its status and hides the deactivate button', async () => {
    loginAs({ email: 'ca@example.com', password: 'x', role: 'CLINIC_ADMIN', firstName: 'A', lastName: 'B' })
    const clinic = seedClinic({ adminUserId: '1', name: 'Clinique Al Amal', city: 'Rabat' })
    seedClinicResource({ clinicId: clinic.id, type: 'EQUIPMENT', name: 'Echographe', active: true })
    renderWithProviders(<ClinicResourceList clinicId={clinic.id} />)
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: /désactiver/i }))

    await waitFor(() => expect(screen.getByText('Désactivée')).toBeInTheDocument())
    expect(screen.queryByRole('button', { name: /désactiver/i })).not.toBeInTheDocument()
  })
})

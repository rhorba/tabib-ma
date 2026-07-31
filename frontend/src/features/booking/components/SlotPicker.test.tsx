import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { renderWithProviders, screen } from '@/test/renderWithProviders'
import { loginAs } from '@/test/loginAs'
import { seedAvailabilitySlot } from '@/test/bookingHandlers'
import { SlotPicker } from './SlotPicker'

describe('SlotPicker', () => {
  it('shows the empty state when there are no open slots', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    renderWithProviders(<SlotPicker doctorProfileId="1" selectedSlotId={null} onSelect={() => {}} />)

    expect(await screen.findByText(/aucun créneau disponible/i)).toBeInTheDocument()
  })

  it('calls onSelect with the slot id when a slot is clicked', async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    const soon = new Date(Date.now() + 3600 * 1000).toISOString()
    const slot = seedAvailabilitySlot({
      doctorProfileId: '1',
      startsAt: soon,
      endsAt: soon,
      locationType: 'IN_PERSON',
    })
    const onSelect = vi.fn()
    renderWithProviders(<SlotPicker doctorProfileId="1" selectedSlotId={null} onSelect={onSelect} />)
    const user = userEvent.setup()

    const slotButton = await screen.findByRole('button', { name: new Date(soon).toLocaleString() })
    await user.click(slotButton)

    expect(onSelect).toHaveBeenCalledWith(slot.id)
  })

  it("does not list a different doctor's slots", async () => {
    loginAs({ email: 'p@example.com', password: 'x', role: 'PATIENT', firstName: 'A', lastName: 'B' })
    seedAvailabilitySlot({
      doctorProfileId: 'other-doctor',
      startsAt: new Date().toISOString(),
      endsAt: new Date().toISOString(),
      locationType: 'IN_PERSON',
    })
    renderWithProviders(<SlotPicker doctorProfileId="1" selectedSlotId={null} onSelect={() => {}} />)

    expect(await screen.findByText(/aucun créneau disponible/i)).toBeInTheDocument()
  })
})

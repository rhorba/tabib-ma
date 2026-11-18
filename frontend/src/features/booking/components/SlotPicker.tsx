import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Button } from '@/shared/components/ui/button'
import { apiClient } from '@/shared/api/client'

const THIRTY_DAYS_MS = 30 * 24 * 60 * 60 * 1000

function useOpenSlots(doctorProfileId: string) {
  const { from, to } = useMemo(() => {
    const now = new Date()
    return { from: now.toISOString(), to: new Date(now.getTime() + THIRTY_DAYS_MS).toISOString() }
  }, [])

  return useQuery({
    queryKey: ['availability-slots', doctorProfileId, from, to],
    queryFn: async () => {
      const { data, error } = await apiClient.GET('/api/v1/booking/availability/slots', {
        params: { query: { doctorProfileId, from, to } },
      })
      if (error) {
        throw error
      }
      return data ?? []
    },
  })
}

type Slot = { id?: string; startsAt?: string }

function dayKey(iso: string) {
  const d = new Date(iso)
  return `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`
}

export function SlotPicker({
  doctorProfileId,
  selectedSlotId,
  onSelect,
}: {
  doctorProfileId: string
  selectedSlotId: string | null
  onSelect: (slotId: string) => void
}) {
  const { t, i18n } = useTranslation()
  const slotsQuery = useOpenSlots(doctorProfileId)
  const [pickedDay, setPickedDay] = useState<string | null>(null)

  // Group the 30-day window by local calendar day so a phone shows one day's
  // times at a time instead of hundreds of buttons in a single list.
  const days = useMemo(() => {
    const groups = new Map<string, Slot[]>()
    for (const slot of (slotsQuery.data ?? []) as Slot[]) {
      if (!slot.startsAt) continue
      const key = dayKey(slot.startsAt)
      const list = groups.get(key)
      if (list) list.push(slot)
      else groups.set(key, [slot])
    }
    return [...groups.entries()].map(([key, slots]) => ({ key, slots }))
  }, [slotsQuery.data])

  if (slotsQuery.isLoading) {
    return null
  }

  if (days.length === 0) {
    return <p className="text-sm text-muted-foreground">{t('booking.book.slots.empty')}</p>
  }

  const activeKey =
    pickedDay ??
    (selectedSlotId &&
      days.find((d) => d.slots.some((s) => s.id === selectedSlotId))?.key) ??
    days[0].key
  const active = days.find((d) => d.key === activeKey) ?? days[0]
  const dayFormat = new Intl.DateTimeFormat(i18n.language, {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
  })
  const timeFormat = new Intl.DateTimeFormat(i18n.language, { hour: '2-digit', minute: '2-digit' })

  return (
    <div className="min-w-0 space-y-3">
      <div className="flex min-w-0 gap-2 overflow-x-auto pb-1" role="group">
        {days.map((day) => (
          <Button
            key={day.key}
            type="button"
            variant={day.key === active.key ? 'default' : 'outline'}
            size="sm"
            className="shrink-0"
            aria-pressed={day.key === active.key}
            onClick={() => setPickedDay(day.key)}
          >
            {dayFormat.format(new Date(day.slots[0].startsAt!))}
          </Button>
        ))}
      </div>
      <div className="grid grid-cols-3 gap-2 sm:grid-cols-4">
        {active.slots.map((slot) => (
          <Button
            key={slot.id}
            type="button"
            variant={selectedSlotId === slot.id ? 'default' : 'outline'}
            size="sm"
            // Full date + time as the accessible name: the visible label is
            // time-only, which is ambiguous without the selected day.
            aria-label={new Date(slot.startsAt!).toLocaleString()}
            onClick={() => onSelect(slot.id!)}
          >
            {timeFormat.format(new Date(slot.startsAt!))}
          </Button>
        ))}
      </div>
    </div>
  )
}

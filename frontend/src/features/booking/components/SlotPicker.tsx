import { useMemo } from 'react'
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

export function SlotPicker({
  doctorProfileId,
  selectedSlotId,
  onSelect,
}: {
  doctorProfileId: string
  selectedSlotId: string | null
  onSelect: (slotId: string) => void
}) {
  const { t } = useTranslation()
  const slotsQuery = useOpenSlots(doctorProfileId)

  if (slotsQuery.isLoading) {
    return null
  }

  if (!slotsQuery.data || slotsQuery.data.length === 0) {
    return <p className="text-sm text-muted-foreground">{t('booking.book.slots.empty')}</p>
  }

  return (
    <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
      {slotsQuery.data.map((slot) => (
        <Button
          key={slot.id}
          type="button"
          variant={selectedSlotId === slot.id ? 'default' : 'outline'}
          size="sm"
          onClick={() => onSelect(slot.id!)}
        >
          {slot.startsAt && new Date(slot.startsAt).toLocaleString()}
        </Button>
      ))}
    </div>
  )
}

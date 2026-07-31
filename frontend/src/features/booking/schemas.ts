import { z } from 'zod'
import type { TFunction } from 'i18next'

const TIME_PATTERN = /^([01]\d|2[0-3]):([0-5]\d)$/

// Mirrors backend/.../booking/dto/CreateAvailabilityRuleRequest.java. Time inputs are "HH:mm"
// (native <input type="time">); the mutation appends ":00" before sending to match the backend's
// LocalTime JSON format.
export function createAvailabilityRuleSchema(t: TFunction) {
  return z
    .object({
      dayOfWeek: z.enum(
        ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'],
        { message: t('booking.availability.validation.dayOfWeekRequired') }
      ),
      startTime: z.string().regex(TIME_PATTERN, t('booking.availability.validation.timeInvalid')),
      endTime: z.string().regex(TIME_PATTERN, t('booking.availability.validation.timeInvalid')),
      slotDurationMinutes: z
        .number({ message: t('booking.availability.validation.durationRequired') })
        .int()
        .min(5, t('booking.availability.validation.durationTooShort')),
      locationType: z.enum(['IN_PERSON', 'VIDEO'], {
        message: t('booking.availability.validation.locationTypeRequired'),
      }),
    })
    .refine((values) => values.endTime > values.startTime, {
      message: t('booking.availability.validation.endBeforeStart'),
      path: ['endTime'],
    })
}

// Mirrors CreateAvailabilityExceptionRequest.java.
export function createExceptionSchema(t: TFunction) {
  return z.object({
    exceptionDate: z.string().min(1, t('booking.availability.validation.dateRequired')),
    reason: z.string().optional(),
  })
}

export type AvailabilityRuleFormValues = z.infer<ReturnType<typeof createAvailabilityRuleSchema>>
export type AvailabilityExceptionFormValues = z.infer<ReturnType<typeof createExceptionSchema>>

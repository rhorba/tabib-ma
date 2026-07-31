import { describe, expect, it } from 'vitest'
import i18n from '@/shared/i18n/config'
import { createAvailabilityRuleSchema, createExceptionSchema } from './schemas'

const t = i18n.t.bind(i18n)

describe('createAvailabilityRuleSchema', () => {
  const schema = createAvailabilityRuleSchema(t)

  it('accepts a valid rule', () => {
    const result = schema.safeParse({
      dayOfWeek: 'MONDAY',
      startTime: '09:00',
      endTime: '17:00',
      slotDurationMinutes: 30,
      locationType: 'IN_PERSON',
    })
    expect(result.success).toBe(true)
  })

  it('rejects endTime before startTime', () => {
    const result = schema.safeParse({
      dayOfWeek: 'MONDAY',
      startTime: '17:00',
      endTime: '09:00',
      slotDurationMinutes: 30,
      locationType: 'IN_PERSON',
    })
    expect(result.success).toBe(false)
  })

  it('rejects a malformed time', () => {
    const result = schema.safeParse({
      dayOfWeek: 'MONDAY',
      startTime: '9am',
      endTime: '17:00',
      slotDurationMinutes: 30,
      locationType: 'IN_PERSON',
    })
    expect(result.success).toBe(false)
  })

  it('rejects a duration under 5 minutes', () => {
    const result = schema.safeParse({
      dayOfWeek: 'MONDAY',
      startTime: '09:00',
      endTime: '17:00',
      slotDurationMinutes: 2,
      locationType: 'IN_PERSON',
    })
    expect(result.success).toBe(false)
  })
})

describe('createExceptionSchema', () => {
  const schema = createExceptionSchema(t)

  it('accepts a date with no reason', () => {
    expect(schema.safeParse({ exceptionDate: '2026-08-10' }).success).toBe(true)
  })

  it('rejects a missing date', () => {
    expect(schema.safeParse({ exceptionDate: '' }).success).toBe(false)
  })
})

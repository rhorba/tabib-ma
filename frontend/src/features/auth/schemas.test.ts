import { describe, expect, it } from 'vitest'
import i18n from '@/shared/i18n/config'
import { createLoginSchema, createRegisterSchema } from './schemas'

const t = i18n.t.bind(i18n)

describe('createLoginSchema', () => {
  const schema = createLoginSchema(t)

  it('accepts a valid email and non-empty password', () => {
    const result = schema.safeParse({ email: 'a@example.com', password: 'x' })
    expect(result.success).toBe(true)
  })

  it('rejects an invalid email', () => {
    const result = schema.safeParse({ email: 'not-an-email', password: 'x' })
    expect(result.success).toBe(false)
  })

  it('rejects an empty password', () => {
    const result = schema.safeParse({ email: 'a@example.com', password: '' })
    expect(result.success).toBe(false)
  })
})

describe('createRegisterSchema', () => {
  const schema = createRegisterSchema(t)
  const valid = {
    email: 'a@example.com',
    password: 'longenough',
    firstName: 'Amina',
    lastName: 'Bennis',
    role: 'PATIENT' as const,
  }

  it('accepts a fully valid payload', () => {
    expect(schema.safeParse(valid).success).toBe(true)
  })

  it('rejects a password shorter than 8 characters (matches backend @Size(min = 8))', () => {
    const result = schema.safeParse({ ...valid, password: 'short1' })
    expect(result.success).toBe(false)
  })

  it('rejects an empty firstName', () => {
    const result = schema.safeParse({ ...valid, firstName: '' })
    expect(result.success).toBe(false)
  })

  it('rejects a role outside PATIENT/DOCTOR', () => {
    const result = schema.safeParse({ ...valid, role: 'CLINIC_ADMIN' })
    expect(result.success).toBe(false)
  })
})

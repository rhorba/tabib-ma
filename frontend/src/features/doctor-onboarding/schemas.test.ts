import { describe, expect, it } from 'vitest'
import i18n from '@/shared/i18n/config'
import { createDoctorProfileSchema, createDocumentUploadSchema } from './schemas'

const t = i18n.t.bind(i18n)

describe('createDoctorProfileSchema', () => {
  const schema = createDoctorProfileSchema(t)
  const valid = { specialty: 'Cardiology', bio: 'bio', consultationFeeMad: 250, city: 'Rabat' }

  it('accepts a fully valid payload', () => {
    expect(schema.safeParse(valid).success).toBe(true)
  })

  it('accepts a payload with no bio, matching the backend optional field', () => {
    const { bio: _bio, ...withoutBio } = valid
    expect(schema.safeParse(withoutBio).success).toBe(true)
  })

  it('rejects an empty specialty', () => {
    expect(schema.safeParse({ ...valid, specialty: '' }).success).toBe(false)
  })

  it('rejects an empty city', () => {
    expect(schema.safeParse({ ...valid, city: '' }).success).toBe(false)
  })

  it('rejects a zero or negative consultation fee', () => {
    expect(schema.safeParse({ ...valid, consultationFeeMad: 0 }).success).toBe(false)
    expect(schema.safeParse({ ...valid, consultationFeeMad: -10 }).success).toBe(false)
  })
})

describe('createDocumentUploadSchema', () => {
  const schema = createDocumentUploadSchema(t)
  const file = new File(['content'], 'license.pdf', { type: 'application/pdf' })

  it('accepts a valid PDF under the size limit', () => {
    const result = schema.safeParse({ documentType: 'MEDICAL_LICENSE', file })
    expect(result.success).toBe(true)
  })

  it('rejects a disallowed content type, matching the backend ALLOWED_CONTENT_TYPES', () => {
    const badFile = new File(['content'], 'payload.exe', { type: 'application/x-msdownload' })
    const result = schema.safeParse({ documentType: 'MEDICAL_LICENSE', file: badFile })
    expect(result.success).toBe(false)
  })

  it('rejects a file over 10MB, matching the backend MAX_DOCUMENT_BYTES', () => {
    const bigFile = new File([new Uint8Array(10 * 1024 * 1024 + 1)], 'big.pdf', {
      type: 'application/pdf',
    })
    const result = schema.safeParse({ documentType: 'MEDICAL_LICENSE', file: bigFile })
    expect(result.success).toBe(false)
  })

  it('rejects a document type outside the backend DocumentType enum', () => {
    const result = schema.safeParse({ documentType: 'PASSPORT', file })
    expect(result.success).toBe(false)
  })
})

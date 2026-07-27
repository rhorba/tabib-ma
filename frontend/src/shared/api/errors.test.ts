import { describe, expect, it } from 'vitest'
import { getApiErrorCode, getApiErrorFieldMessages } from './errors'

const apiError = {
  error: {
    code: 'VALIDATION_FAILED',
    message: 'The request contains invalid fields.',
    details: [{ field: 'email', message: 'must be a valid email' }],
    requestId: 'req-1',
  },
}

describe('getApiErrorCode', () => {
  it('extracts the code from a well-formed backend ErrorResponse', () => {
    expect(getApiErrorCode(apiError)).toBe('VALIDATION_FAILED')
  })

  it('returns null for a plain network/thrown error', () => {
    expect(getApiErrorCode(new TypeError('fetch failed'))).toBeNull()
  })

  it('returns null for undefined/null', () => {
    expect(getApiErrorCode(undefined)).toBeNull()
    expect(getApiErrorCode(null)).toBeNull()
  })

  it('returns null for a shape missing the message field', () => {
    expect(getApiErrorCode({ error: { code: 'X' } })).toBeNull()
  })
})

describe('getApiErrorFieldMessages', () => {
  it('maps field-level details to a field -> message record', () => {
    expect(getApiErrorFieldMessages(apiError)).toEqual({ email: 'must be a valid email' })
  })

  it('returns an empty object for a non-ApiError value', () => {
    expect(getApiErrorFieldMessages(new Error('boom'))).toEqual({})
  })
})

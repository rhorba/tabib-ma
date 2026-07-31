import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import i18n, { defaultLanguage } from '@/shared/i18n/config'
import { server } from './mswServer'
import { resetAuthState } from './authHandlers'
import { resetClinicState } from './clinicHandlers'
import { resetBookingState } from './bookingHandlers'

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(async () => {
  cleanup()
  server.resetHandlers()
  resetAuthState()
  resetClinicState()
  resetBookingState()
  localStorage.clear()
  if (i18n.language !== defaultLanguage) {
    await i18n.changeLanguage(defaultLanguage)
  }
})
afterAll(() => server.close())

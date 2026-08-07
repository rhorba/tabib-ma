import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll, expect } from 'vitest'
import { toHaveNoViolations } from 'jest-axe'
import i18n, { defaultLanguage } from '@/shared/i18n/config'
import { server } from './mswServer'
import { resetAuthState } from './authHandlers'
import { resetClinicState } from './clinicHandlers'
import { resetBookingState } from './bookingHandlers'
import { resetConsultationState } from './consultationHandlers'
import { resetPrescriptionState } from './prescriptionHandlers'
import { resetReviewState } from './reviewHandlers'
import { resetAdminState } from './adminHandlers'

expect.extend(toHaveNoViolations)

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(async () => {
  cleanup()
  server.resetHandlers()
  resetAuthState()
  resetClinicState()
  resetBookingState()
  resetConsultationState()
  resetPrescriptionState()
  resetReviewState()
  resetAdminState()
  localStorage.clear()
  if (i18n.language !== defaultLanguage) {
    await i18n.changeLanguage(defaultLanguage)
  }
})
afterAll(() => server.close())

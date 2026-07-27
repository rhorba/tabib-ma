import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import i18n, { defaultLanguage } from '@/shared/i18n/config'
import { server } from './mswServer'
import { resetAuthState } from './authHandlers'

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(async () => {
  cleanup()
  server.resetHandlers()
  resetAuthState()
  localStorage.clear()
  if (i18n.language !== defaultLanguage) {
    await i18n.changeLanguage(defaultLanguage)
  }
})
afterAll(() => server.close())

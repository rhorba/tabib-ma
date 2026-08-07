import { axe } from 'jest-axe'
import { expect } from 'vitest'
import i18n from '@/shared/i18n/config'

// NFR-6 (WCAG 2.1 AA, docs/prd-tabib-ma.md) — automated axe-core coverage only. A true manual
// screen-reader pass (Test Strategy §7's other half of this NFR) needs a human with NVDA/JAWS/
// VoiceOver and stays an explicitly open item (.logs/decisions.md 2026-08-07), not silently dropped.
export async function expectNoA11yViolations(container: Element) {
  const results = await axe(container)
  expect(results).toHaveNoViolations()
}

export async function withArabic<T>(run: () => Promise<T> | T): Promise<T> {
  await i18n.changeLanguage('ar')
  try {
    return await run()
  } finally {
    await i18n.changeLanguage('fr')
  }
}

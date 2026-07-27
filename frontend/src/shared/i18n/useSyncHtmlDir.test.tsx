import { act, render, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import i18n, { defaultLanguage } from './config'
import { useSyncHtmlDir } from './useSyncHtmlDir'

function Harness() {
  useSyncHtmlDir()
  return null
}

afterEach(async () => {
  await act(async () => {
    await i18n.changeLanguage(defaultLanguage)
  })
})

describe('useSyncHtmlDir', () => {
  it('sets <html lang>/<html dir> for the default (French) language', async () => {
    render(<Harness />)
    await waitFor(() => expect(document.documentElement.lang).toBe('fr'))
    expect(document.documentElement.dir).toBe('ltr')
  })

  it('flips <html dir> to rtl when switching to Arabic', async () => {
    render(<Harness />)
    await waitFor(() => expect(document.documentElement.lang).toBe('fr'))

    await act(async () => {
      await i18n.changeLanguage('ar')
    })

    expect(document.documentElement.lang).toBe('ar')
    expect(document.documentElement.dir).toBe('rtl')
  })
})

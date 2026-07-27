import { useEffect } from 'react'
import { useTranslation } from 'react-i18next'

// Keeps <html lang>/<html dir> in sync with the active i18next language.
// RTL (Arabic) mirroring depends on dir="rtl" being set here — Tailwind's
// logical properties (ps-/pe-/start-/end-) then flip automatically.
export function useSyncHtmlDir() {
  const { i18n } = useTranslation()

  useEffect(() => {
    const applyDir = (language: string) => {
      document.documentElement.lang = language
      document.documentElement.dir = i18n.dir(language)
    }

    applyDir(i18n.language)
    i18n.on('languageChanged', applyDir)
    return () => {
      i18n.off('languageChanged', applyDir)
    }
  }, [i18n])
}

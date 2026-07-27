import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'

// Keeps <html lang>/<html dir> in sync with the active i18next language, and
// returns the current direction so it can be threaded into Radix UI's
// DirectionProvider — Radix popover-based primitives (Select, DropdownMenu,
// etc.) render into a portal and otherwise default to their own internal
// dir="ltr", silently ignoring <html dir>. Tailwind's logical properties
// (ps-/pe-/start-/end-) only flip correctly once Radix knows the real
// direction too.
export function useSyncHtmlDir() {
  const { i18n } = useTranslation()
  const [dir, setDir] = useState(() => i18n.dir())

  useEffect(() => {
    const applyDir = (language: string) => {
      const nextDir = i18n.dir(language)
      document.documentElement.lang = language
      document.documentElement.dir = nextDir
      setDir(nextDir)
    }

    applyDir(i18n.language)
    i18n.on('languageChanged', applyDir)
    return () => {
      i18n.off('languageChanged', applyDir)
    }
  }, [i18n])

  return dir
}

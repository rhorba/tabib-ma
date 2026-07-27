import i18n from 'i18next'
import LanguageDetector from 'i18next-browser-languagedetector'
import { initReactI18next } from 'react-i18next'
import ar from './locales/ar/common.json'
import fr from './locales/fr/common.json'

export const supportedLanguages = ['fr', 'ar'] as const
export type SupportedLanguage = (typeof supportedLanguages)[number]

// French is primary per docs/ux-tabib-ma.md — used whenever the detector can't
// match the browser locale to a supported language.
export const defaultLanguage: SupportedLanguage = 'fr'

void i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      fr: { common: fr },
      ar: { common: ar },
    },
    supportedLngs: supportedLanguages,
    fallbackLng: defaultLanguage,
    defaultNS: 'common',
    detection: {
      order: ['localStorage', 'navigator'],
      caches: ['localStorage'],
      lookupLocalStorage: 'tabibma-language',
    },
    interpolation: {
      escapeValue: false,
    },
  })

export default i18n

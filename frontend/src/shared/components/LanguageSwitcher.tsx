import { useTranslation } from 'react-i18next'
import { Button } from '@/shared/components/ui/button'
import { supportedLanguages, type SupportedLanguage } from '@/shared/i18n/config'

export function LanguageSwitcher() {
  const { t, i18n } = useTranslation()
  const current = i18n.language as SupportedLanguage

  return (
    <div role="group" aria-label={t('language.label')} className="flex gap-1">
      {supportedLanguages.map((lang) => (
        <Button
          key={lang}
          type="button"
          size="sm"
          variant={current === lang ? 'default' : 'ghost'}
          aria-pressed={current === lang}
          onClick={() => i18n.changeLanguage(lang)}
        >
          {t(`language.${lang}`)}
        </Button>
      ))}
    </div>
  )
}

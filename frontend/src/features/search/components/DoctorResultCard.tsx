import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'
import type { components } from '@/shared/api/schema'

type DoctorSearchResult = components['schemas']['DoctorSearchResultResponse']

export function DoctorResultCard({ result }: { result: DoctorSearchResult }) {
  const { t } = useTranslation()

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg" role="heading" aria-level={2}>
          {result.firstName} {result.lastName}
        </CardTitle>
      </CardHeader>
      <CardContent className="grid gap-2 text-sm">
        <dl className="grid grid-cols-2 gap-x-4 gap-y-1">
          <dt className="text-muted-foreground">{t('search.filters.specialtyLabel')}</dt>
          <dd>{result.specialty}</dd>
          <dt className="text-muted-foreground">{t('search.filters.cityLabel')}</dt>
          <dd>{result.city}</dd>
          <dt className="text-muted-foreground">{t('search.results.feeLabel')}</dt>
          <dd>{result.consultationFeeMad}</dd>
        </dl>
        <Link
          to={`/doctors/${result.doctorProfileId}`}
          className="text-sm font-medium text-primary hover:underline"
        >
          {t('search.results.viewProfile')}
        </Link>
      </CardContent>
    </Card>
  )
}

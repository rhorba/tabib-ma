import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { AlertCircleIcon } from 'lucide-react'
import { Alert, AlertDescription } from '@/shared/components/ui/alert'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Label } from '@/shared/components/ui/label'
import { apiClient } from '@/shared/api/client'
import { DoctorResultCard } from '../components/DoctorResultCard'

function useDoctorSearch(specialty: string, city: string, maxFeeMad: string) {
  return useQuery({
    queryKey: ['doctor-search', specialty, city, maxFeeMad],
    queryFn: async () => {
      const { data, error } = await apiClient.GET('/api/v1/clinic/doctor-profiles/search', {
        params: {
          query: {
            specialty: specialty || undefined,
            city: city || undefined,
            maxFeeMad: maxFeeMad ? Number(maxFeeMad) : undefined,
          },
        },
      })
      if (error) {
        throw error
      }
      return data
    },
  })
}

export function SearchPage() {
  const { t } = useTranslation()
  const [specialtyInput, setSpecialtyInput] = useState('')
  const [cityInput, setCityInput] = useState('')
  const [maxFeeInput, setMaxFeeInput] = useState('')
  const [specialty, setSpecialty] = useState('')
  const [city, setCity] = useState('')
  const [maxFeeMad, setMaxFeeMad] = useState('')

  const searchQuery = useDoctorSearch(specialty, city, maxFeeMad)

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-6 px-4 py-12">
      <h1 className="text-xl font-semibold text-foreground" role="heading" aria-level={1}>
        {t('search.title')}
      </h1>
      <form
        className="flex flex-wrap items-end gap-4"
        onSubmit={(e) => {
          e.preventDefault()
          setSpecialty(specialtyInput)
          setCity(cityInput)
          setMaxFeeMad(maxFeeInput)
        }}
      >
        <div className="grid gap-1.5">
          <Label htmlFor="search-specialty">{t('search.filters.specialtyLabel')}</Label>
          <Input
            id="search-specialty"
            value={specialtyInput}
            onChange={(e) => setSpecialtyInput(e.target.value)}
          />
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="search-city">{t('search.filters.cityLabel')}</Label>
          <Input id="search-city" value={cityInput} onChange={(e) => setCityInput(e.target.value)} />
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="search-max-fee">{t('search.filters.maxFeeLabel')}</Label>
          <Input
            id="search-max-fee"
            type="number"
            min="0"
            inputMode="numeric"
            value={maxFeeInput}
            onChange={(e) => setMaxFeeInput(e.target.value)}
          />
        </div>
        <Button type="submit">{t('search.filters.submit')}</Button>
      </form>

      {searchQuery.isError && (
        <Alert variant="destructive">
          <AlertCircleIcon />
          <AlertDescription>{t('search.errors.generic')}</AlertDescription>
        </Alert>
      )}

      {searchQuery.data && searchQuery.data.results && searchQuery.data.results.length > 0 ? (
        <div className="grid gap-4 sm:grid-cols-2">
          {searchQuery.data.results.map((result) => (
            <DoctorResultCard key={result.doctorProfileId} result={result} />
          ))}
        </div>
      ) : (
        searchQuery.isSuccess && (
          <p className="text-sm text-muted-foreground">{t('search.results.empty')}</p>
        )
      )}
    </div>
  )
}

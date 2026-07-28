import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/shared/components/ui/card'
import { RegisterForm } from '../components/RegisterForm'

export function RegisterPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()

  return (
    <div className="mx-auto flex w-full max-w-sm flex-1 items-center px-4 py-12">
      <Card className="w-full">
        <CardHeader>
          <CardTitle className="text-xl" role="heading" aria-level={1}>
            {t('auth.register.title')}
          </CardTitle>
          <CardDescription>
            {t('auth.register.haveAccount')}{' '}
            <Link to="/login" className="text-primary underline-offset-4 hover:underline">
              {t('auth.register.loginLink')}
            </Link>
          </CardDescription>
        </CardHeader>
        <CardContent>
          <RegisterForm onSuccess={() => navigate('/')} />
        </CardContent>
      </Card>
    </div>
  )
}

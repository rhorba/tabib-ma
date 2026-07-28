import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router-dom'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/shared/components/ui/card'
import { LoginForm } from '../components/LoginForm'

export function LoginPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()

  return (
    <div className="mx-auto flex w-full max-w-sm flex-1 items-center px-4 py-12">
      <Card className="w-full">
        <CardHeader>
          <CardTitle className="text-xl" role="heading" aria-level={1}>
            {t('auth.login.title')}
          </CardTitle>
          <CardDescription>
            {t('auth.login.noAccount')}{' '}
            <Link to="/register" className="text-primary underline-offset-4 hover:underline">
              {t('auth.login.registerLink')}
            </Link>
          </CardDescription>
        </CardHeader>
        <CardContent>
          <LoginForm onSuccess={() => navigate('/')} />
        </CardContent>
      </Card>
    </div>
  )
}

import { createBrowserRouter } from 'react-router'
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { RegisterPage } from '@/features/auth/pages/RegisterPage'
import { DoctorOnboardingPage } from '@/features/doctor-onboarding/pages/DoctorOnboardingPage'
import { VerificationQueuePage } from '@/features/platform-admin/pages/VerificationQueuePage'
import { ClinicAdminPage } from '@/features/clinic-admin/pages/ClinicAdminPage'
import { RequireRole } from '@/shared/components/RequireRole'
import { RootLayout } from './RootLayout'
import { HomePage } from './HomePage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <RootLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'register', element: <RegisterPage /> },
      {
        element: <RequireRole roles={['DOCTOR']} />,
        children: [{ path: 'doctor/onboarding', element: <DoctorOnboardingPage /> }],
      },
      {
        element: <RequireRole roles={['PLATFORM_ADMIN']} />,
        children: [
          { path: 'platform-admin/verification-queue', element: <VerificationQueuePage /> },
        ],
      },
      {
        element: <RequireRole roles={['CLINIC_ADMIN']} />,
        children: [{ path: 'clinic-admin', element: <ClinicAdminPage /> }],
      },
    ],
  },
])

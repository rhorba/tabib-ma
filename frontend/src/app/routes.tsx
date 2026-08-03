import { createBrowserRouter } from 'react-router'
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { RegisterPage } from '@/features/auth/pages/RegisterPage'
import { DoctorOnboardingPage } from '@/features/doctor-onboarding/pages/DoctorOnboardingPage'
import { VerificationQueuePage } from '@/features/platform-admin/pages/VerificationQueuePage'
import { ClinicAdminPage } from '@/features/clinic-admin/pages/ClinicAdminPage'
import { SearchPage } from '@/features/search/pages/SearchPage'
import { DoctorPublicProfilePage } from '@/features/search/pages/DoctorPublicProfilePage'
import { DoctorAvailabilityPage } from '@/features/booking/pages/DoctorAvailabilityPage'
import { BookAppointmentPage } from '@/features/booking/pages/BookAppointmentPage'
import { MyAppointmentsPage } from '@/features/booking/pages/MyAppointmentsPage'
import { ConsultationPage } from '@/features/consultation/pages/ConsultationPage'
import { MyPrescriptionsPage } from '@/features/prescription/pages/MyPrescriptionsPage'
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
      { path: 'search', element: <SearchPage /> },
      { path: 'doctors/:doctorProfileId', element: <DoctorPublicProfilePage /> },
      {
        element: <RequireRole roles={['DOCTOR']} />,
        children: [
          { path: 'doctor/onboarding', element: <DoctorOnboardingPage /> },
          { path: 'doctor/availability', element: <DoctorAvailabilityPage /> },
        ],
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
      {
        element: <RequireRole roles={['PATIENT']} />,
        children: [
          { path: 'doctors/:doctorProfileId/book', element: <BookAppointmentPage /> },
          { path: 'prescriptions', element: <MyPrescriptionsPage /> },
        ],
      },
      {
        element: <RequireRole roles={['PATIENT', 'DOCTOR']} />,
        children: [
          { path: 'appointments', element: <MyAppointmentsPage /> },
          { path: 'appointments/:appointmentId/consultation', element: <ConsultationPage /> },
        ],
      },
    ],
  },
])

import { lazy } from 'react'
import { createBrowserRouter } from 'react-router'
import { RequireRole } from '@/shared/components/RequireRole'
import { RootLayout } from './RootLayout'
import { HomePage } from './HomePage'

// Route-based code splitting (fast-follow, 2026-08-07): everything except the index route
// (rendered on the most common first load — no reason to add a network round trip to it) is
// lazy-loaded, so a visitor only downloads the page they're actually on. RootLayout wraps
// <Outlet /> in a single shared <Suspense>, so no per-route fallback wiring is needed here.
const LoginPage = lazy(() => import('@/features/auth/pages/LoginPage').then((m) => ({ default: m.LoginPage })))
const RegisterPage = lazy(() =>
  import('@/features/auth/pages/RegisterPage').then((m) => ({ default: m.RegisterPage })),
)
const DoctorOnboardingPage = lazy(() =>
  import('@/features/doctor-onboarding/pages/DoctorOnboardingPage').then((m) => ({
    default: m.DoctorOnboardingPage,
  })),
)
const VerificationQueuePage = lazy(() =>
  import('@/features/platform-admin/pages/VerificationQueuePage').then((m) => ({
    default: m.VerificationQueuePage,
  })),
)
const DisputeQueuePage = lazy(() =>
  import('@/features/platform-admin/pages/DisputeQueuePage').then((m) => ({ default: m.DisputeQueuePage })),
)
const PlatformHealthPage = lazy(() =>
  import('@/features/platform-admin/pages/PlatformHealthPage').then((m) => ({ default: m.PlatformHealthPage })),
)
const ClinicAdminPage = lazy(() =>
  import('@/features/clinic-admin/pages/ClinicAdminPage').then((m) => ({ default: m.ClinicAdminPage })),
)
const SearchPage = lazy(() => import('@/features/search/pages/SearchPage').then((m) => ({ default: m.SearchPage })))
const DoctorPublicProfilePage = lazy(() =>
  import('@/features/search/pages/DoctorPublicProfilePage').then((m) => ({ default: m.DoctorPublicProfilePage })),
)
const DoctorAvailabilityPage = lazy(() =>
  import('@/features/booking/pages/DoctorAvailabilityPage').then((m) => ({ default: m.DoctorAvailabilityPage })),
)
const BookAppointmentPage = lazy(() =>
  import('@/features/booking/pages/BookAppointmentPage').then((m) => ({ default: m.BookAppointmentPage })),
)
const MyAppointmentsPage = lazy(() =>
  import('@/features/booking/pages/MyAppointmentsPage').then((m) => ({ default: m.MyAppointmentsPage })),
)
const ConsultationPage = lazy(() =>
  import('@/features/consultation/pages/ConsultationPage').then((m) => ({ default: m.ConsultationPage })),
)
const MyPrescriptionsPage = lazy(() =>
  import('@/features/prescription/pages/MyPrescriptionsPage').then((m) => ({ default: m.MyPrescriptionsPage })),
)

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
          { path: 'platform-admin/disputes', element: <DisputeQueuePage /> },
          { path: 'platform-admin/health', element: <PlatformHealthPage /> },
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

import { http, HttpResponse } from 'msw'
import { getAuthenticatedUser } from './authHandlers'

// A tiny in-memory fake of the admin module's dispute-queue / appointment-action
// contract. DisputeResponse is already enriched with patient/doctor names and
// appointment context server-side, so the fake stores that shape directly rather
// than resolving it from bookingHandlers.ts — not a reimplementation of
// DisputeService/AdminAppointmentActionService's business rules (those are
// covered by the backend's own tests).
type FakeDispute = {
  id: string
  appointmentId: string
  appointmentStartsAt: string
  locationType: 'IN_PERSON' | 'VIDEO'
  patientId: string
  patientName: string
  doctorProfileId: string
  doctorName: string
  type: 'NO_SHOW' | 'PAYMENT_ISSUE' | 'COMPLAINT'
  status: 'OPEN' | 'RESOLVED'
  reason: string
  reportedByUserId?: string
  createdAt: string
}

type FakePaymentState = 'PENDING' | 'SUCCEEDED' | 'REFUNDED'

type FakePlatformHealth = {
  totalAppointments: number
  confirmedAppointments: number
  cancelledAppointments: number
  completedAppointments: number
  noShowAppointments: number
  pendingPaymentAppointments: number
  succeededPayments: number
  failedPayments: number
  refundedPayments: number
}

const ZERO_HEALTH: FakePlatformHealth = {
  totalAppointments: 0,
  confirmedAppointments: 0,
  cancelledAppointments: 0,
  completedAppointments: 0,
  noShowAppointments: 0,
  pendingPaymentAppointments: 0,
  succeededPayments: 0,
  failedPayments: 0,
  refundedPayments: 0,
}

let disputes: FakeDispute[] = []
let appointmentPayments = new Map<string, FakePaymentState>()
let cancelledAppointments = new Set<string>()
let nextDisputeId = 1
let nextAppointmentId = 1
let platformHealth: FakePlatformHealth = ZERO_HEALTH

export function resetAdminState() {
  disputes = []
  appointmentPayments = new Map()
  cancelledAppointments = new Set()
  nextDisputeId = 1
  nextAppointmentId = 1
  platformHealth = ZERO_HEALTH
}

export function seedPlatformHealth(overrides: Partial<FakePlatformHealth>) {
  platformHealth = { ...ZERO_HEALTH, ...overrides }
  return platformHealth
}

export function seedDispute(
  overrides: Partial<Omit<FakeDispute, 'id'>> & { hasSucceededPayment?: boolean } = {}
) {
  const { hasSucceededPayment = true, appointmentId, ...rest } = overrides
  const resolvedAppointmentId = appointmentId ?? `appointment-${nextAppointmentId++}`
  const dispute: FakeDispute = {
    id: String(nextDisputeId++),
    appointmentId: resolvedAppointmentId,
    appointmentStartsAt: new Date(Date.now() + 3600_000).toISOString(),
    locationType: 'IN_PERSON',
    patientId: 'patient-1',
    patientName: 'Amine Patient',
    doctorProfileId: 'doctor-1',
    doctorName: 'Dr. Sara Doctor',
    type: 'COMPLAINT',
    status: 'OPEN',
    reason: "Le patient ne s'est pas présenté au rendez-vous.",
    createdAt: new Date().toISOString(),
    ...rest,
  }
  disputes.push(dispute)
  appointmentPayments.set(resolvedAppointmentId, hasSucceededPayment ? 'SUCCEEDED' : 'PENDING')
  return dispute
}

function errorResponse(status: number, code: string, message: string) {
  return HttpResponse.json({ error: { code, message, details: [], requestId: 'test' } }, { status })
}

function pathSegment(request: Request, indexFromEnd: number) {
  const segments = new URL(request.url).pathname.split('/')
  return segments[segments.length - indexFromEnd]
}

export const adminHandlers = [
  http.post(/\/api\/v1\/disputes$/, async ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    const body = (await request.json()) as { appointmentId: string; type: FakeDispute['type']; reason: string }
    if (body.type === 'NO_SHOW') {
      return errorResponse(400, 'VALIDATION_FAILED', 'NO_SHOW disputes are recorded automatically, not self-reported.')
    }
    const dispute: FakeDispute = {
      id: String(nextDisputeId++),
      appointmentId: body.appointmentId,
      appointmentStartsAt: new Date(Date.now() + 3600_000).toISOString(),
      locationType: 'IN_PERSON',
      patientId: 'patient-1',
      patientName: 'Amine Patient',
      doctorProfileId: 'doctor-1',
      doctorName: 'Dr. Sara Doctor',
      type: body.type,
      status: 'OPEN',
      reason: body.reason,
      reportedByUserId: user.id,
      createdAt: new Date().toISOString(),
    }
    disputes.push(dispute)
    appointmentPayments.set(body.appointmentId, appointmentPayments.get(body.appointmentId) ?? 'SUCCEEDED')
    return HttpResponse.json(dispute, { status: 201 })
  }),

  http.get(/\/api\/v1\/admin\/platform\/disputes$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    return HttpResponse.json(disputes.filter((d) => d.status === 'OPEN'))
  }),

  http.post(/\/api\/v1\/admin\/platform\/disputes\/[^/]+\/resolve$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    const disputeId = pathSegment(request, 2)
    const dispute = disputes.find((d) => d.id === disputeId)
    if (!dispute) {
      return errorResponse(404, 'NOT_FOUND', 'Dispute not found.')
    }
    if (dispute.status !== 'OPEN') {
      return errorResponse(409, 'CONFLICT', 'This dispute has already been resolved.')
    }
    dispute.status = 'RESOLVED'
    return new HttpResponse(null, { status: 200 })
  }),

  http.post(/\/api\/v1\/admin\/platform\/appointments\/[^/]+\/refund$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    const appointmentId = pathSegment(request, 2)
    const paymentState = appointmentPayments.get(appointmentId)
    if (paymentState === undefined) {
      return errorResponse(404, 'NOT_FOUND', 'No payment found for this appointment.')
    }
    if (paymentState !== 'SUCCEEDED') {
      return errorResponse(409, 'CONFLICT', 'This payment is not refundable.')
    }
    appointmentPayments.set(appointmentId, 'REFUNDED')
    return new HttpResponse(null, { status: 200 })
  }),

  http.post(/\/api\/v1\/admin\/platform\/appointments\/[^/]+\/force-cancel$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    const appointmentId = pathSegment(request, 2)
    const isKnownAppointment =
      disputes.some((d) => d.appointmentId === appointmentId) || appointmentPayments.has(appointmentId)
    if (!isKnownAppointment) {
      return errorResponse(404, 'NOT_FOUND', 'Appointment not found.')
    }
    if (cancelledAppointments.has(appointmentId)) {
      return errorResponse(409, 'CONFLICT', 'This appointment is already cancelled.')
    }
    cancelledAppointments.add(appointmentId)
    return new HttpResponse(null, { status: 200 })
  }),

  http.get(/\/api\/v1\/admin\/platform\/health$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    return HttpResponse.json(platformHealth)
  }),
]

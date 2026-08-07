import { http, HttpResponse } from 'msw'
import { getAuthenticatedUser } from './authHandlers'
import { seedPrescription } from './prescriptionHandlers'

// A tiny in-memory fake of the consultation module's contract (join-window
// gate + join + complete) — close enough to exercise the frontend's request/
// response handling, not a reimplementation of JoinWindowPolicy or the real
// WebRTC signaling relay (there is no WebSocket fake here; useConsultationCall's
// real join-window/permission-denied paths are exercised through jsdom's own
// absence of navigator.mediaDevices, and the full two-peer connect is Playwright
// e2e's job — see Test Strategy doc).
type ConsultationStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
type FakeConsultation = {
  id: string
  appointmentId: string
  doctorId: string
  patientId: string
  status: ConsultationStatus
  joinable: boolean
}

let consultations: FakeConsultation[] = []
let nextId = 1

export function resetConsultationState() {
  consultations = []
  nextId = 1
}

function errorResponse(status: number, code: string, message: string) {
  return HttpResponse.json(
    { error: { code, message, details: [], requestId: 'test-request-id' } },
    { status }
  )
}

function pathSegment(request: Request, indexFromEnd: number) {
  const segments = new URL(request.url).pathname.split('/').filter(Boolean)
  return segments[segments.length - indexFromEnd]
}

function toResponse(consultation: FakeConsultation) {
  const { id, appointmentId, status, joinable } = consultation
  return { id, appointmentId, status, joinable }
}

export const consultationHandlers = [
  http.get(/\/api\/v1\/consultations\/by-appointment\/[^/]+$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    const appointmentId = pathSegment(request, 1)
    const consultation = consultations.find((c) => c.appointmentId === appointmentId)
    if (!consultation) return new HttpResponse(null, { status: 404 })
    return HttpResponse.json(toResponse(consultation))
  }),

  http.post(/\/api\/v1\/consultations\/[^/]+\/join$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    const consultationId = pathSegment(request, 2)
    const consultation = consultations.find((c) => c.id === consultationId)
    if (!consultation) return errorResponse(404, 'NOT_FOUND', 'Consultation not found.')
    if (!consultation.joinable) return errorResponse(409, 'CONFLICT', 'The join window is not open.')
    return HttpResponse.json({
      consultationId: consultation.id,
      signalingToken: 'fake-signaling-token',
      signalingTokenExpiresAt: new Date(Date.now() + 60_000).toISOString(),
      iceServers: [],
    })
  }),

  http.post(/\/api\/v1\/consultations\/[^/]+\/complete$/, async ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    if (user.role !== 'DOCTOR') {
      return errorResponse(403, 'FORBIDDEN', 'Only doctors can complete a consultation.')
    }
    const consultationId = pathSegment(request, 2)
    const consultation = consultations.find((c) => c.id === consultationId)
    if (!consultation) return errorResponse(404, 'NOT_FOUND', 'Consultation not found.')
    const body = (await request.json()) as { items: { medicationName: string; dosage: string; instructions?: string }[] }
    consultation.status = 'COMPLETED'
    // Story 6.3 (amended 2026-08-07): empty items means the doctor skipped prescribing.
    const prescription = body.items.length > 0
      ? seedPrescription({
          consultationId: consultation.id,
          doctorId: consultation.doctorId,
          patientId: consultation.patientId,
          items: body.items,
        })
      : null
    return HttpResponse.json({ consultationId: consultation.id, prescription })
  }),
]

// Test-only helper mirroring seedAppointment: lets tests seed a consultation
// directly at whatever status/joinable state a test needs.
export function seedConsultation(consultation: Omit<FakeConsultation, 'id'>) {
  const fake: FakeConsultation = { id: String(nextId++), ...consultation }
  consultations.push(fake)
  return fake
}

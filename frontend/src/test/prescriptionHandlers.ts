import { http, HttpResponse } from 'msw'
import { getAuthenticatedUser } from './authHandlers'

// A tiny in-memory fake of the prescription module's contract — close enough
// to exercise the frontend's request/response handling, not a reimplementation
// of PrescriptionService's immutability/ownership rules (those are covered by
// the backend's own PrescriptionAccessControlIntegrationTest).
type FakePrescriptionItem = { medicationName: string; dosage: string; instructions?: string }
type FakePrescription = {
  id: string
  consultationId: string
  doctorId: string
  patientId: string
  supersedesId?: string
  items: FakePrescriptionItem[]
  createdAt: string
}

let prescriptions: FakePrescription[] = []
let nextId = 1

export function resetPrescriptionState() {
  prescriptions = []
  nextId = 1
}

function errorResponse(status: number, code: string, message: string) {
  return HttpResponse.json(
    { error: { code, message, details: [], requestId: 'test-request-id' } },
    { status }
  )
}

function toResponse(prescription: FakePrescription) {
  const { id, consultationId, doctorId, patientId, supersedesId, items, createdAt } = prescription
  return { id, consultationId, doctorId, patientId, supersedesId, items, createdAt }
}

export const prescriptionHandlers = [
  http.get(/\/api\/v1\/prescriptions\/mine$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    return HttpResponse.json(prescriptions.filter((p) => p.patientId === user.id).map(toResponse))
  }),

  http.get(/\/api\/v1\/prescriptions\/[^/]+\/pdf$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    return new HttpResponse('%PDF-1.4 fake prescription pdf', {
      status: 200,
      headers: { 'Content-Type': 'application/pdf' },
    })
  }),
]

// Test-only helper mirroring seedAppointment: lets tests create a prescription
// directly without going through a full consult-completion flow first.
export function seedPrescription(prescription: Omit<FakePrescription, 'id' | 'createdAt'> & { createdAt?: string }) {
  const fake: FakePrescription = {
    id: String(nextId++),
    createdAt: new Date().toISOString(),
    ...prescription,
  }
  prescriptions.push(fake)
  return fake
}

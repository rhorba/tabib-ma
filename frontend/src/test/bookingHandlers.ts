import { http, HttpResponse } from 'msw'
import { getAuthenticatedUser } from './authHandlers'
import { findDoctorProfileByUserId } from './clinicHandlers'

// A tiny in-memory fake of the booking module's contract (availability rules/
// exceptions/slots + appointments) — close enough to exercise the frontend's
// request/response handling, not a reimplementation of AvailabilityService/
// BookingService/CancellationService's business rules (those are covered by
// the backend's own tests). generate() doesn't actually expand rules into
// slots by date the way the real backend does — tests seed open slots
// directly via seedAvailabilitySlot when they need one to book.
type LocationType = 'IN_PERSON' | 'VIDEO'
type AppointmentStatus = 'PENDING_PAYMENT' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED' | 'NO_SHOW'

type FakeRule = {
  id: string
  doctorProfileId: string
  dayOfWeek: string
  startTime: string
  endTime: string
  slotDurationMinutes: number
  locationType: LocationType
  active: boolean
}
type FakeException = { id: string; doctorProfileId: string; exceptionDate: string; reason?: string }
type FakeSlot = {
  id: string
  doctorProfileId: string
  startsAt: string
  endsAt: string
  locationType: LocationType
  booked: boolean
}
type FakeAppointment = {
  id: string
  patientId: string
  doctorProfileId: string
  availabilitySlotId: string
  startsAt: string
  endsAt: string
  locationType: LocationType
  status: AppointmentStatus
  cancellationWindowHours: number
  createdAt: string
}

let rules: FakeRule[] = []
let exceptions: FakeException[] = []
let slots: FakeSlot[] = []
let appointments: FakeAppointment[] = []
let nextRuleId = 1
let nextExceptionId = 1
let nextSlotId = 1
let nextAppointmentId = 1

export function resetBookingState() {
  rules = []
  exceptions = []
  slots = []
  appointments = []
  nextRuleId = 1
  nextExceptionId = 1
  nextSlotId = 1
  nextAppointmentId = 1
}

function errorResponse(status: number, code: string, message: string) {
  return HttpResponse.json(
    { error: { code, message, details: [], requestId: 'test-request-id' } },
    { status }
  )
}

function toRuleResponse(rule: FakeRule) {
  const { id, dayOfWeek, startTime, endTime, slotDurationMinutes, locationType, active } = rule
  return { id, dayOfWeek, startTime, endTime, slotDurationMinutes, locationType, active }
}
function toExceptionResponse(exception: FakeException) {
  const { id, exceptionDate, reason } = exception
  return { id, exceptionDate, reason: reason ?? null }
}
function toSlotResponse(slot: FakeSlot) {
  const { id, doctorProfileId, startsAt, endsAt, locationType, booked } = slot
  return { id, doctorProfileId, startsAt, endsAt, locationType, booked }
}
function toAppointmentResponse(appointment: FakeAppointment) {
  const {
    id,
    doctorProfileId,
    availabilitySlotId,
    startsAt,
    endsAt,
    locationType,
    status,
    cancellationWindowHours,
    createdAt,
  } = appointment
  return {
    id,
    doctorProfileId,
    availabilitySlotId,
    startsAt,
    endsAt,
    locationType,
    status,
    cancellationWindowHours,
    createdAt,
  }
}

function pathSegment(request: Request, indexFromEnd: number) {
  const segments = new URL(request.url).pathname.split('/').filter(Boolean)
  return segments[segments.length - indexFromEnd]
}

function myDoctorProfileOrError(request: Request) {
  const user = getAuthenticatedUser(request)
  if (!user) {
    return { error: errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.') } as const
  }
  if (user.role !== 'DOCTOR') {
    return { error: errorResponse(403, 'FORBIDDEN', 'Only doctors can manage availability.') } as const
  }
  const profile = findDoctorProfileByUserId(user.id)
  if (!profile) {
    return { error: errorResponse(404, 'NOT_FOUND', "You don't have a doctor profile yet.") } as const
  }
  return { user, profile } as const
}

export const bookingHandlers = [
  http.post(/\/api\/v1\/booking\/availability\/rules$/, async ({ request }) => {
    const result = myDoctorProfileOrError(request)
    if ('error' in result) return result.error
    const body = (await request.json()) as {
      dayOfWeek: string
      startTime: string
      endTime: string
      slotDurationMinutes: number
      locationType: LocationType
    }
    if (body.endTime <= body.startTime) {
      return errorResponse(400, 'VALIDATION_FAILED', 'endTime must be after startTime.')
    }
    const rule: FakeRule = { id: String(nextRuleId++), doctorProfileId: result.profile.id, active: true, ...body }
    rules.push(rule)
    return HttpResponse.json(toRuleResponse(rule), { status: 201 })
  }),

  http.get(/\/api\/v1\/booking\/availability\/rules$/, ({ request }) => {
    const result = myDoctorProfileOrError(request)
    if ('error' in result) return result.error
    return HttpResponse.json(rules.filter((r) => r.doctorProfileId === result.profile.id).map(toRuleResponse))
  }),

  http.post(/\/api\/v1\/booking\/availability\/exceptions$/, async ({ request }) => {
    const result = myDoctorProfileOrError(request)
    if ('error' in result) return result.error
    const body = (await request.json()) as { exceptionDate: string; reason?: string }
    if (exceptions.some((e) => e.doctorProfileId === result.profile.id && e.exceptionDate === body.exceptionDate)) {
      return errorResponse(409, 'CONFLICT', 'This date is already marked as an exception.')
    }
    const exception: FakeException = { id: String(nextExceptionId++), doctorProfileId: result.profile.id, ...body }
    exceptions.push(exception)
    return HttpResponse.json(toExceptionResponse(exception), { status: 201 })
  }),

  http.get(/\/api\/v1\/booking\/availability\/exceptions$/, ({ request }) => {
    const result = myDoctorProfileOrError(request)
    if ('error' in result) return result.error
    return HttpResponse.json(
      exceptions.filter((e) => e.doctorProfileId === result.profile.id).map(toExceptionResponse)
    )
  }),

  http.post(/\/api\/v1\/booking\/availability\/generate$/, ({ request }) => {
    const result = myDoctorProfileOrError(request)
    if ('error' in result) return result.error
    return HttpResponse.json(
      slots.filter((s) => s.doctorProfileId === result.profile.id && !s.booked).map(toSlotResponse)
    )
  }),

  http.get(/\/api\/v1\/booking\/availability\/slots$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    const doctorProfileId = new URL(request.url).searchParams.get('doctorProfileId')
    return HttpResponse.json(
      slots.filter((s) => s.doctorProfileId === doctorProfileId && !s.booked).map(toSlotResponse)
    )
  }),

  http.post(/\/api\/v1\/booking\/appointments$/, async ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    if (user.role !== 'PATIENT') {
      return errorResponse(403, 'FORBIDDEN', 'Only patients can book appointments.')
    }
    const body = (await request.json()) as { availabilitySlotId: string }
    const slot = slots.find((s) => s.id === body.availabilitySlotId)
    if (!slot) return errorResponse(404, 'NOT_FOUND', 'Availability slot not found.')
    if (slot.booked) return errorResponse(409, 'CONFLICT', 'This slot was just booked.')
    slot.booked = true
    const appointment: FakeAppointment = {
      id: String(nextAppointmentId++),
      patientId: user.id,
      doctorProfileId: slot.doctorProfileId,
      availabilitySlotId: slot.id,
      startsAt: slot.startsAt,
      endsAt: slot.endsAt,
      locationType: slot.locationType,
      status: 'CONFIRMED',
      cancellationWindowHours: 24,
      createdAt: new Date().toISOString(),
    }
    appointments.push(appointment)
    return HttpResponse.json(toAppointmentResponse(appointment), { status: 201 })
  }),

  http.get(/\/api\/v1\/booking\/appointments$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    if (user.role === 'DOCTOR') {
      const profile = findDoctorProfileByUserId(user.id)
      if (!profile) return errorResponse(404, 'NOT_FOUND', "You don't have a doctor profile yet.")
      return HttpResponse.json(
        appointments.filter((a) => a.doctorProfileId === profile.id).map(toAppointmentResponse)
      )
    }
    return HttpResponse.json(appointments.filter((a) => a.patientId === user.id).map(toAppointmentResponse))
  }),

  http.post(/\/api\/v1\/booking\/appointments\/[^/]+\/cancel$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    const appointmentId = pathSegment(request, 2)
    const appointment = appointments.find((a) => a.id === appointmentId)
    if (!appointment) return errorResponse(404, 'NOT_FOUND', 'Appointment not found.')
    if (appointment.patientId !== user.id) {
      return errorResponse(403, 'FORBIDDEN', 'You can only cancel your own appointments.')
    }
    if (appointment.status === 'CANCELLED' || appointment.status === 'COMPLETED') {
      return errorResponse(409, 'CONFLICT', 'This appointment can no longer be cancelled.')
    }
    appointment.status = 'CANCELLED'
    const slot = slots.find((s) => s.id === appointment.availabilitySlotId)
    if (slot) slot.booked = false
    return HttpResponse.json(toAppointmentResponse(appointment))
  }),
]

// Test-only helper mirroring seedDoctorProfile: lets tests create an open slot
// directly without going through the rule+generate flow first.
export function seedAvailabilitySlot(slot: Omit<FakeSlot, 'id' | 'booked'> & { booked?: boolean }) {
  const fakeSlot: FakeSlot = { id: String(nextSlotId++), booked: false, ...slot }
  slots.push(fakeSlot)
  return fakeSlot
}

// Test-only helper: lets tests seed an appointment directly (e.g. to test
// MyAppointmentsPage's cancel/reschedule actions without a full booking flow).
export function seedAppointment(appointment: Omit<FakeAppointment, 'id' | 'createdAt'> & { createdAt?: string }) {
  const fakeAppointment: FakeAppointment = {
    id: String(nextAppointmentId++),
    createdAt: new Date().toISOString(),
    ...appointment,
  }
  appointments.push(fakeAppointment)
  return fakeAppointment
}

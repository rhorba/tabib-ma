package com.tabibma.booking;

import java.util.UUID;

/**
 * Domain event (Architecture doc §4, Domain Events pattern) — Story 10.1 Batch 2. The admin
 * module's DisputeEventListener reacts to this by auto-filing a NO_SHOW dispute; booking doesn't
 * know disputes exist, same dependency-direction reasoning as BookingConfirmedEvent.
 */
public record AppointmentNoShowEvent(UUID appointmentId) {
}

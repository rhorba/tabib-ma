package com.tabibma.booking;

import java.util.UUID;

/**
 * Domain event (Architecture doc §4, Domain Events pattern) — decouples booking confirmation
 * from how it gets communicated to the patient. No listener yet: Story 4.5's notification module
 * (Batch 5) adds one; publishing it now means BookingService won't need to change when it does.
 */
public record BookingConfirmedEvent(UUID appointmentId) {
}

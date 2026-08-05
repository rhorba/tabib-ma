package com.tabibma.booking;

import java.util.UUID;

/**
 * Domain event (Architecture doc §4, Domain Events pattern) — Story 10.1 Batch 2, published from
 * BookingService's existing payment-not-SUCCEEDED branch (symmetric with the success branch's
 * BookingConfirmedEvent). The admin module's DisputeEventListener reacts by auto-filing a
 * PAYMENT_ISSUE dispute.
 */
public record AppointmentPaymentFailedEvent(UUID appointmentId) {
}

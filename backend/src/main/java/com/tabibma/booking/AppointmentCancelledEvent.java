package com.tabibma.booking;

import java.util.UUID;

/**
 * Domain event mirroring {@link BookingConfirmedEvent} — published whenever an appointment is
 * cancelled (patient-initiated {@link CancellationService#cancel} or admin-initiated
 * {@link CancellationService#forceCancel}), so the notification listener can tell both the patient
 * and doctor without CancellationService needing to know how notifications work.
 */
public record AppointmentCancelledEvent(UUID appointmentId) {
}

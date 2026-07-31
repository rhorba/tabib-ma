package com.tabibma.booking;

import java.time.Duration;
import java.time.Instant;

/**
 * Specification-lite pattern (Architecture doc §4) — the reschedule/cancel window rule lives in
 * one testable place so the boundary condition (Test Strategy §4 TIME: "23h59m vs 24h before")
 * has a single, deterministic answer instead of being re-derived ad hoc at call sites.
 * <p>
 * Cancellation itself is always allowed (Appointment.cancel()'s own guard is the only gate on
 * that); this class only decides refund eligibility. The boundary is inclusive: a cancellation
 * submitted at exactly {@code windowHours} before the appointment counts as within the window.
 */
public final class CancellationPolicy {

    private final int windowHours;

    public CancellationPolicy(int windowHours) {
        this.windowHours = windowHours;
    }

    public boolean isWithinWindow(Instant now, Instant appointmentStart) {
        Duration timeUntilAppointment = Duration.between(now, appointmentStart);
        return timeUntilAppointment.compareTo(Duration.ofHours(windowHours)) >= 0;
    }
}

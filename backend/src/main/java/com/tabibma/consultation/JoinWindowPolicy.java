package com.tabibma.consultation;

import java.time.Duration;
import java.time.Instant;

/**
 * Specification-lite pattern (Architecture doc §4), same style as booking's CancellationPolicy —
 * Story 6.1's AC: "join button is disabled until 10 minutes before the appointment" (±10min window
 * per UX doc). The window stays open until the slot's end plus the same margin, so a consult
 * running slightly over doesn't get cut off mid-reconnect. Both boundaries are inclusive.
 */
public final class JoinWindowPolicy {

    private final int marginMinutes;

    public JoinWindowPolicy(int marginMinutes) {
        this.marginMinutes = marginMinutes;
    }

    public boolean isJoinable(Instant now, Instant appointmentStart, Instant appointmentEnd) {
        Instant windowOpensAt = appointmentStart.minus(Duration.ofMinutes(marginMinutes));
        Instant windowClosesAt = appointmentEnd.plus(Duration.ofMinutes(marginMinutes));
        return !now.isBefore(windowOpensAt) && !now.isAfter(windowClosesAt);
    }
}

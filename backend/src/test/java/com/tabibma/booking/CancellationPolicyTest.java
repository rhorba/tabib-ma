package com.tabibma.booking;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CancellationPolicyTest {

    private final CancellationPolicy policy = new CancellationPolicy(24);

    @Test
    void isWithinWindow_trueWellBeforeTheWindow() {
        Instant appointmentStart = Instant.now().plusSeconds(48 * 3600);
        Instant now = Instant.now();

        assertThat(policy.isWithinWindow(now, appointmentStart)).isTrue();
    }

    @Test
    void isWithinWindow_trueExactlyAtTheBoundary() {
        Instant now = Instant.now();
        Instant appointmentStart = now.plusSeconds(24 * 3600);

        assertThat(policy.isWithinWindow(now, appointmentStart)).isTrue();
    }

    @Test
    void isWithinWindow_falseJustInsideTheBoundary() {
        Instant now = Instant.now();
        Instant appointmentStart = now.plusSeconds(24 * 3600 - 60);

        assertThat(policy.isWithinWindow(now, appointmentStart)).isFalse();
    }

    @Test
    void isWithinWindow_falseAfterTheAppointmentHasAlreadyStarted() {
        Instant now = Instant.now();
        Instant appointmentStart = now.minusSeconds(3600);

        assertThat(policy.isWithinWindow(now, appointmentStart)).isFalse();
    }
}

package com.tabibma.consultation;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JoinWindowPolicyTest {

    private final JoinWindowPolicy policy = new JoinWindowPolicy(10);

    private static final Instant START = Instant.parse("2026-08-01T14:00:00Z");
    private static final Instant END = Instant.parse("2026-08-01T14:30:00Z");

    @Test
    void isJoinable_falseWellBeforeTheWindowOpens() {
        Instant now = START.minusSeconds(30 * 60);

        assertThat(policy.isJoinable(now, START, END)).isFalse();
    }

    @Test
    void isJoinable_trueExactlyAtTheOpeningBoundary() {
        Instant now = START.minusSeconds(10 * 60);

        assertThat(policy.isJoinable(now, START, END)).isTrue();
    }

    @Test
    void isJoinable_trueDuringTheAppointment() {
        assertThat(policy.isJoinable(START.plusSeconds(60), START, END)).isTrue();
    }

    @Test
    void isJoinable_trueExactlyAtTheClosingBoundary() {
        Instant now = END.plusSeconds(10 * 60);

        assertThat(policy.isJoinable(now, START, END)).isTrue();
    }

    @Test
    void isJoinable_falseWellAfterTheWindowCloses() {
        Instant now = END.plusSeconds(30 * 60);

        assertThat(policy.isJoinable(now, START, END)).isFalse();
    }
}

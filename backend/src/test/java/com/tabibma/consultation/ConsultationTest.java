package com.tabibma.consultation;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultationTest {

    @Test
    void start_transitionsFromScheduledToInProgressAndSetsStartedAt() {
        Consultation consultation = new Consultation(UUID.randomUUID());

        consultation.start();

        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.IN_PROGRESS);
        assertThat(consultation.getStartedAt()).isNotNull();
    }

    @Test
    void start_isIdempotentWhenAlreadyInProgress() {
        Consultation consultation = new Consultation(UUID.randomUUID());
        consultation.start();

        consultation.start();

        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.IN_PROGRESS);
    }

    @Test
    void start_rejectsAConsultationThatIsAlreadyCompleted() {
        Consultation consultation = new Consultation(UUID.randomUUID());
        consultation.start();
        consultation.complete();

        assertThatThrownBy(consultation::start).isInstanceOf(com.tabibma.shared.exception.ConflictException.class);
    }

    @Test
    void complete_setsEndedAtAndStatus() {
        Consultation consultation = new Consultation(UUID.randomUUID());
        consultation.start();

        consultation.complete();

        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.COMPLETED);
        assertThat(consultation.getEndedAt()).isNotNull();
    }

    @Test
    void complete_allowedDirectlyFromScheduled() {
        Consultation consultation = new Consultation(UUID.randomUUID());

        consultation.complete();

        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.COMPLETED);
    }

    @Test
    void complete_rejectsAnAlreadyCompletedConsultation() {
        Consultation consultation = new Consultation(UUID.randomUUID());
        consultation.complete();

        assertThatThrownBy(consultation::complete).isInstanceOf(com.tabibma.shared.exception.ConflictException.class);
    }
}

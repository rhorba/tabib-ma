package com.tabibma.consultation.dto;

import com.tabibma.consultation.Consultation;
import com.tabibma.consultation.ConsultationService.ConsultationView;
import com.tabibma.consultation.ConsultationStatus;

import java.time.Instant;
import java.util.UUID;

public record ConsultationResponse(
        UUID id,
        UUID appointmentId,
        ConsultationStatus status,
        Instant appointmentStartsAt,
        Instant appointmentEndsAt,
        Instant startedAt,
        Instant endedAt,
        boolean joinable
) {
    public static ConsultationResponse from(ConsultationView view) {
        Consultation c = view.consultation();
        return new ConsultationResponse(c.getId(), c.getAppointmentId(), c.getStatus(),
                view.appointment().getStartsAt(), view.appointment().getEndsAt(),
                c.getStartedAt(), c.getEndedAt(), view.joinable());
    }
}

package com.tabibma.booking.dto;

import com.tabibma.booking.Appointment;
import com.tabibma.booking.AppointmentStatus;
import com.tabibma.booking.LocationType;

import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID doctorProfileId,
        UUID availabilitySlotId,
        Instant startsAt,
        Instant endsAt,
        LocationType locationType,
        AppointmentStatus status,
        int cancellationWindowHours,
        Instant createdAt
) {
    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(appointment.getId(), appointment.getDoctorProfileId(),
                appointment.getAvailabilitySlotId(), appointment.getStartsAt(), appointment.getEndsAt(),
                appointment.getLocationType(), appointment.getStatus(), appointment.getCancellationWindowHours(),
                appointment.getCreatedAt());
    }
}

package com.tabibma.admin.dto;

import com.tabibma.admin.Dispute;
import com.tabibma.admin.DisputeStatus;
import com.tabibma.admin.DisputeType;
import com.tabibma.booking.Appointment;
import com.tabibma.booking.LocationType;

import java.time.Instant;
import java.util.UUID;

/** Story 10.1 AC: "enough context to act (appointment, patient, doctor, reason)" — enriched with
 * the appointment's own fields plus resolved patient/doctor display names, not just raw ids. */
public record DisputeResponse(
        UUID id,
        UUID appointmentId,
        Instant appointmentStartsAt,
        LocationType locationType,
        UUID patientId,
        String patientName,
        UUID doctorProfileId,
        String doctorName,
        DisputeType type,
        DisputeStatus status,
        String reason,
        UUID reportedByUserId,
        Instant createdAt,
        Instant resolvedAt,
        UUID resolvedByUserId
) {
    public static DisputeResponse from(Dispute dispute, Appointment appointment, String patientName, String doctorName) {
        return new DisputeResponse(
                dispute.getId(),
                dispute.getAppointmentId(),
                appointment.getStartsAt(),
                appointment.getLocationType(),
                appointment.getPatientId(),
                patientName,
                appointment.getDoctorProfileId(),
                doctorName,
                dispute.getType(),
                dispute.getStatus(),
                dispute.getReason(),
                dispute.getReportedByUserId(),
                dispute.getCreatedAt(),
                dispute.getResolvedAt(),
                dispute.getResolvedByUserId());
    }
}

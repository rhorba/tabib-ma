package com.tabibma.prescription.dto;

import com.tabibma.prescription.Prescription;
import com.tabibma.prescription.PrescriptionItem;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PrescriptionResponse(
        UUID id,
        UUID consultationId,
        UUID doctorId,
        UUID patientId,
        UUID supersedesId,
        List<PrescriptionItemRequest> items,
        Instant signedAt,
        Instant createdAt
) {
    public static PrescriptionResponse from(Prescription prescription) {
        List<PrescriptionItemRequest> items = prescription.getItems().stream()
                .map(PrescriptionResponse::toItemDto)
                .toList();
        return new PrescriptionResponse(prescription.getId(), prescription.getConsultationId(),
                prescription.getDoctorId(), prescription.getPatientId(), prescription.getSupersedesId(), items,
                prescription.getSignedAt(), prescription.getCreatedAt());
    }

    private static PrescriptionItemRequest toItemDto(PrescriptionItem item) {
        return new PrescriptionItemRequest(item.getMedicationName(), item.getDosage(), item.getInstructions());
    }
}

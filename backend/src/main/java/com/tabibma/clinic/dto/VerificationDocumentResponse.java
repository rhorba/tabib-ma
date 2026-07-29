package com.tabibma.clinic.dto;

import com.tabibma.clinic.VerificationDocument;

import java.time.Instant;
import java.util.UUID;

public record VerificationDocumentResponse(
        UUID id,
        UUID doctorProfileId,
        String documentType,
        Instant createdAt
) {
    public static VerificationDocumentResponse from(VerificationDocument document) {
        return new VerificationDocumentResponse(document.getId(), document.getDoctorProfileId(),
                document.getDocumentType(), document.getCreatedAt());
    }
}

package com.tabibma.consultation.dto;

import com.tabibma.consultation.ConsultationService.CompletionResult;
import com.tabibma.prescription.dto.PrescriptionResponse;

import java.util.UUID;

public record CompleteConsultationResponse(
        UUID consultationId,
        PrescriptionResponse prescription
) {
    public static CompleteConsultationResponse from(CompletionResult result) {
        return new CompleteConsultationResponse(result.consultation().getId(),
                PrescriptionResponse.from(result.prescription()));
    }
}

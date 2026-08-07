package com.tabibma.consultation.dto;

import com.tabibma.consultation.ConsultationService.CompletionResult;
import com.tabibma.prescription.dto.PrescriptionResponse;

import java.util.UUID;

public record CompleteConsultationResponse(
        UUID consultationId,
        PrescriptionResponse prescription
) {
    public static CompleteConsultationResponse from(CompletionResult result) {
        PrescriptionResponse prescription = result.prescription() == null ? null
                : PrescriptionResponse.from(result.prescription());
        return new CompleteConsultationResponse(result.consultation().getId(), prescription);
    }
}

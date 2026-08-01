package com.tabibma.consultation.dto;

import com.tabibma.prescription.dto.PrescriptionItemRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CompleteConsultationRequest(
        @NotEmpty @Valid List<PrescriptionItemRequest> items
) {
}

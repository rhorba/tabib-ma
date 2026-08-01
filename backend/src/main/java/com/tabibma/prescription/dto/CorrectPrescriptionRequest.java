package com.tabibma.prescription.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CorrectPrescriptionRequest(
        @NotEmpty @Valid List<PrescriptionItemRequest> items
) {
}

package com.tabibma.prescription.dto;

import jakarta.validation.constraints.NotBlank;

public record PrescriptionItemRequest(
        @NotBlank String medicationName,
        @NotBlank String dosage,
        String instructions
) {
}

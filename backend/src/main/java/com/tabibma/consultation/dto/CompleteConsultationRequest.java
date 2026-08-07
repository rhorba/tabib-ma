package com.tabibma.consultation.dto;

import com.tabibma.prescription.dto.PrescriptionItemRequest;
import jakarta.validation.Valid;

import java.util.List;

/** items is optional (Story 6.3, amended 2026-08-07, .logs/decisions.md) — null or empty means
 * the doctor completed the consult without issuing a prescription. */
public record CompleteConsultationRequest(
        @Valid List<PrescriptionItemRequest> items
) {
}

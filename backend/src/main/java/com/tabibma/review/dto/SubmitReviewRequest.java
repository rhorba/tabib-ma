package com.tabibma.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubmitReviewRequest(
        @NotNull UUID appointmentId,
        @Min(1) @Max(5) int rating,
        String comment
) {
}

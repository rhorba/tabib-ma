package com.tabibma.review.dto;

import com.tabibma.review.Review;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID appointmentId,
        UUID doctorProfileId,
        int rating,
        String comment,
        Instant createdAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(review.getId(), review.getAppointmentId(), review.getDoctorProfileId(),
                review.getRating(), review.getComment(), review.getCreatedAt());
    }
}

package com.tabibma.clinic.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Deliberately self-contained — no reference to any `review` module type, even though this
 * response's review fields are actually populated by review.PublicDoctorProfileController.
 * clinic must stay a dependency-free module (see DoctorSearchService's own comment); reusing
 * review.dto.DoctorReviewSummary.Entry here would create clinic -> review purely from this
 * field's type, regardless of which class constructs an instance. */
public record DoctorPublicProfileResponse(
        UUID doctorProfileId,
        String firstName,
        String lastName,
        String specialty,
        String city,
        String bio,
        BigDecimal consultationFeeMad,
        Double averageRating,
        long reviewCount,
        List<ReviewEntry> recentReviews
) {
    public record ReviewEntry(String patientFirstName, int rating, String comment, Instant createdAt) {
    }
}

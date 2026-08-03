package com.tabibma.review.dto;

import java.time.Instant;
import java.util.List;

/** Read model DoctorSearchService composes into DoctorPublicProfileResponse — keeps the
 * clinic module from querying the reviews table directly (Architecture doc §2: no direct
 * cross-module table access, compose via service calls instead). */
public record DoctorReviewSummary(Double averageRating, long reviewCount, List<Entry> recent) {

    public record Entry(String patientFirstName, int rating, String comment, Instant createdAt) {
    }
}

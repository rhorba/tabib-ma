package com.tabibma.review;

import com.tabibma.clinic.DoctorSearchService;
import com.tabibma.clinic.dto.DoctorPublicProfileResponse;
import com.tabibma.review.dto.DoctorReviewSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Serves {@code GET /api/v1/clinic/doctor-profiles/{doctorProfileId}/public} — same route
 * DoctorProfileController used to own outright, deliberately moved here (Spring routes by
 * {@code @RequestMapping} value, not package/class name, so the URL is unaffected).
 *
 * <p>Story 9.1 needs a doctor's real average rating/review count/recent comments on this
 * response. Composing that in {@code clinic} would create a module dependency cycle:
 * {@code review} already depends on {@code booking} (Appointment, for the review-submission
 * ownership/status checks) and {@code booking} already depends on {@code clinic} (DoctorProfile,
 * for the booking fee lookup), so {@code clinic -> review} would close the loop
 * clinic -> review -> booking -> clinic. Composing it here instead (review depends on clinic,
 * clinic stays a dependency-free leaf) is a diamond, not a cycle — ArchitectureTest's
 * {@code featureModulesShouldHaveNoCyclicDependencies} enforces this isn't reintroduced.
 */
@RestController
@RequestMapping("/api/v1/clinic/doctor-profiles")
public class PublicDoctorProfileController {

    private static final int RECENT_REVIEWS_LIMIT = 5;

    private final DoctorSearchService doctorSearchService;
    private final ReviewService reviewService;

    public PublicDoctorProfileController(DoctorSearchService doctorSearchService, ReviewService reviewService) {
        this.doctorSearchService = doctorSearchService;
        this.reviewService = reviewService;
    }

    @GetMapping("/{doctorProfileId}/public")
    public DoctorPublicProfileResponse getPublicProfile(@PathVariable UUID doctorProfileId) {
        DoctorPublicProfileResponse base = doctorSearchService.getPublicProfile(doctorProfileId);
        DoctorReviewSummary reviews = reviewService.getProfileSummary(doctorProfileId, RECENT_REVIEWS_LIMIT);
        List<DoctorPublicProfileResponse.ReviewEntry> recentReviews = reviews.recent().stream()
                .map(entry -> new DoctorPublicProfileResponse.ReviewEntry(
                        entry.patientFirstName(), entry.rating(), entry.comment(), entry.createdAt()))
                .toList();
        return new DoctorPublicProfileResponse(
                base.doctorProfileId(), base.firstName(), base.lastName(), base.specialty(), base.city(),
                base.bio(), base.consultationFeeMad(), reviews.averageRating(), reviews.reviewCount(),
                recentReviews);
    }
}

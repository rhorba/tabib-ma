package com.tabibma.review;

import com.tabibma.clinic.DoctorSearchService;
import com.tabibma.clinic.dto.DoctorPublicProfileResponse;
import com.tabibma.review.dto.DoctorReviewSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicDoctorProfileControllerTest {

    @Mock
    private DoctorSearchService doctorSearchService;
    @Mock
    private ReviewService reviewService;

    private PublicDoctorProfileController controller() {
        return new PublicDoctorProfileController(doctorSearchService, reviewService);
    }

    @Test
    void getPublicProfile_mergesTheBaseProfileWithRealReviewStats() {
        UUID profileId = UUID.randomUUID();
        DoctorPublicProfileResponse base = new DoctorPublicProfileResponse(
                profileId, "Karim", "Fassi", "Neurology", "Meknes", "bio", BigDecimal.valueOf(200), null, 0L, List.of());
        when(doctorSearchService.getPublicProfile(profileId)).thenReturn(base);

        DoctorReviewSummary summary = new DoctorReviewSummary(4.5, 2L,
                List.of(new DoctorReviewSummary.Entry("Youssef", 5, "Great doctor", Instant.now())));
        when(reviewService.getProfileSummary(eq(profileId), anyInt())).thenReturn(summary);

        DoctorPublicProfileResponse response = controller().getPublicProfile(profileId);

        assertThat(response.firstName()).isEqualTo("Karim");
        assertThat(response.specialty()).isEqualTo("Neurology");
        assertThat(response.averageRating()).isEqualTo(4.5);
        assertThat(response.reviewCount()).isEqualTo(2L);
        assertThat(response.recentReviews()).hasSize(1);
        assertThat(response.recentReviews().get(0).patientFirstName()).isEqualTo("Youssef");
    }

    @Test
    void getPublicProfile_degradesGracefullyWithNoReviewsYet() {
        UUID profileId = UUID.randomUUID();
        DoctorPublicProfileResponse base = new DoctorPublicProfileResponse(
                profileId, "Karim", "Fassi", "Neurology", "Meknes", "bio", BigDecimal.valueOf(200), null, 0L, List.of());
        when(doctorSearchService.getPublicProfile(profileId)).thenReturn(base);
        when(reviewService.getProfileSummary(eq(profileId), anyInt()))
                .thenReturn(new DoctorReviewSummary(null, 0L, List.of()));

        DoctorPublicProfileResponse response = controller().getPublicProfile(profileId);

        assertThat(response.averageRating()).isNull();
        assertThat(response.reviewCount()).isZero();
        assertThat(response.recentReviews()).isEmpty();
    }
}

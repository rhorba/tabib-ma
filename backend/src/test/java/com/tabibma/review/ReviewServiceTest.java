package com.tabibma.review;

import com.tabibma.booking.Appointment;
import com.tabibma.booking.AppointmentRepository;
import com.tabibma.booking.LocationType;
import com.tabibma.identity.Role;
import com.tabibma.identity.User;
import com.tabibma.identity.UserContext;
import com.tabibma.identity.UserRepository;
import com.tabibma.review.dto.DoctorReviewSummary;
import com.tabibma.shared.exception.ConflictException;
import com.tabibma.shared.exception.ForbiddenException;
import com.tabibma.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private UserRepository userRepository;

    private ReviewService service;

    private UUID patientId;
    private UUID doctorProfileId;
    private Appointment completedAppointment;

    @BeforeEach
    void setUp() {
        service = new ReviewService(reviewRepository, appointmentRepository, userRepository);
        patientId = UUID.randomUUID();
        doctorProfileId = UUID.randomUUID();
        Instant start = Instant.now().minusSeconds(3600);
        completedAppointment = new Appointment(patientId, doctorProfileId, UUID.randomUUID(), start,
                start.plusSeconds(1800), LocationType.VIDEO);
        completedAppointment.confirm();
        completedAppointment.complete();
    }

    private UserContext patientPrincipal() {
        return new UserContext(patientId, "p@example.com", Role.PATIENT);
    }

    @Test
    void submit_rejectsNonPatientRole() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);

        assertThatThrownBy(() -> service.submit(doctor, UUID.randomUUID(), 5, "great"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void submit_throwsNotFoundForAnUnknownAppointment() {
        UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(patientPrincipal(), appointmentId, 5, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void submit_rejectsSomeoneElsesAppointment() {
        UUID appointmentId = completedAppointment.getId();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(completedAppointment));
        UserContext stranger = new UserContext(UUID.randomUUID(), "x@example.com", Role.PATIENT);

        assertThatThrownBy(() -> service.submit(stranger, appointmentId, 4, null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void submit_rejectsAnAppointmentThatIsNotCompleted() {
        Instant start = Instant.now();
        Appointment confirmedOnly = new Appointment(patientId, doctorProfileId, UUID.randomUUID(), start,
                start.plusSeconds(1800), LocationType.VIDEO);
        confirmedOnly.confirm();
        UUID appointmentId = confirmedOnly.getId();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(confirmedOnly));

        assertThatThrownBy(() -> service.submit(patientPrincipal(), appointmentId, 4, null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void submit_rejectsADuplicateReviewForTheSameAppointment() {
        UUID appointmentId = completedAppointment.getId();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(completedAppointment));
        when(reviewRepository.existsByAppointmentId(appointmentId)).thenReturn(true);

        assertThatThrownBy(() -> service.submit(patientPrincipal(), appointmentId, 4, null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void submit_savesAReviewForACompletedOwnAppointment() {
        UUID appointmentId = completedAppointment.getId();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(completedAppointment));
        when(reviewRepository.existsByAppointmentId(appointmentId)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        Review review = service.submit(patientPrincipal(), appointmentId, 5, "Excellent");

        assertThat(review.getAppointmentId()).isEqualTo(appointmentId);
        assertThat(review.getPatientId()).isEqualTo(patientId);
        assertThat(review.getDoctorProfileId()).isEqualTo(doctorProfileId);
        assertThat(review.getRating()).isEqualTo(5);
        assertThat(review.getComment()).isEqualTo("Excellent");
    }

    @Test
    void getProfileSummary_returnsEmptySummaryWhenNoReviewsExist() {
        when(reviewRepository.findAllByDoctorProfileIdOrderByCreatedAtDesc(doctorProfileId)).thenReturn(List.of());

        DoctorReviewSummary summary = service.getProfileSummary(doctorProfileId, 5);

        assertThat(summary.averageRating()).isNull();
        assertThat(summary.reviewCount()).isZero();
        assertThat(summary.recent()).isEmpty();
    }

    @Test
    void getProfileSummary_computesAverageAndResolvesReviewerFirstNames() {
        Review fiveStar = new Review(UUID.randomUUID(), patientId, doctorProfileId, 5, "Great");
        Review threeStar = new Review(UUID.randomUUID(), UUID.randomUUID(), doctorProfileId, 3, null);
        when(reviewRepository.findAllByDoctorProfileIdOrderByCreatedAtDesc(doctorProfileId))
                .thenReturn(List.of(fiveStar, threeStar));
        User patient = new User("p@example.com", "hash", Role.PATIENT, "Youssef", "Patient");
        ReflectionTestUtils.setField(patient, "id", patientId);
        when(userRepository.findAllById(any())).thenReturn(List.of(patient));

        DoctorReviewSummary summary = service.getProfileSummary(doctorProfileId, 5);

        assertThat(summary.averageRating()).isEqualTo(4.0);
        assertThat(summary.reviewCount()).isEqualTo(2);
        assertThat(summary.recent()).hasSize(2);
        assertThat(summary.recent().get(0).patientFirstName()).isEqualTo("Youssef");
    }
}

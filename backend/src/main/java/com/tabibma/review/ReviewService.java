package com.tabibma.review;

import com.tabibma.booking.Appointment;
import com.tabibma.booking.AppointmentRepository;
import com.tabibma.booking.AppointmentStatus;
import com.tabibma.identity.Role;
import com.tabibma.identity.User;
import com.tabibma.identity.UserContext;
import com.tabibma.identity.UserRepository;
import com.tabibma.review.dto.DoctorReviewSummary;
import com.tabibma.shared.exception.ConflictException;
import com.tabibma.shared.exception.ForbiddenException;
import com.tabibma.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Story 9.1: a patient reviews a COMPLETED appointment exactly once. */
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository, AppointmentRepository appointmentRepository,
                          UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Review submit(UserContext principal, UUID appointmentId, int rating, String comment) {
        if (principal.role() != Role.PATIENT) {
            throw new ForbiddenException("Only patients can submit reviews.");
        }
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found."));
        if (!appointment.getPatientId().equals(principal.userId())) {
            throw new ForbiddenException("You can only review your own appointments.");
        }
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new ConflictException("Only a completed appointment can be reviewed.");
        }
        if (reviewRepository.existsByAppointmentId(appointmentId)) {
            throw new ConflictException("This appointment has already been reviewed.");
        }

        Review review = new Review(appointmentId, principal.userId(), appointment.getDoctorProfileId(), rating, comment);
        return reviewRepository.save(review);
    }

    public List<Review> getMine(UserContext principal) {
        return reviewRepository.findAllByPatientId(principal.userId());
    }

    public DoctorReviewSummary getProfileSummary(UUID doctorProfileId, int recentLimit) {
        List<Review> reviews = reviewRepository.findAllByDoctorProfileIdOrderByCreatedAtDesc(doctorProfileId);
        if (reviews.isEmpty()) {
            return new DoctorReviewSummary(null, 0L, List.of());
        }

        double average = reviews.stream().mapToInt(Review::getRating).average().orElse(0);
        List<Review> recentReviews = reviews.stream().limit(recentLimit).toList();
        Map<UUID, User> patientsById = userRepository
                .findAllById(recentReviews.stream().map(Review::getPatientId).toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<DoctorReviewSummary.Entry> recent = recentReviews.stream()
                .map(r -> new DoctorReviewSummary.Entry(
                        Optional.ofNullable(patientsById.get(r.getPatientId())).map(User::getFirstName).orElse(null),
                        r.getRating(), r.getComment(), r.getCreatedAt()))
                .toList();

        return new DoctorReviewSummary(average, reviews.size(), recent);
    }
}

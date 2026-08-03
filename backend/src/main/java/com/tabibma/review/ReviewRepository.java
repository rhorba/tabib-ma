package com.tabibma.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByAppointmentId(UUID appointmentId);

    List<Review> findAllByPatientId(UUID patientId);

    List<Review> findAllByDoctorProfileIdOrderByCreatedAtDesc(UUID doctorProfileId);
}

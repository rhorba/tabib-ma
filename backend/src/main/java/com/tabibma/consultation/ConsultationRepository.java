package com.tabibma.consultation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConsultationRepository extends JpaRepository<Consultation, UUID> {

    Optional<Consultation> findByAppointmentId(UUID appointmentId);
}

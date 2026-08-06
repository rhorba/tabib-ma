package com.tabibma.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findAllByPatientId(UUID patientId);

    List<Appointment> findAllByDoctorProfileId(UUID doctorProfileId);

    List<Appointment> findAllByDoctorProfileIdInAndStatusIn(
            Collection<UUID> doctorProfileIds, Collection<AppointmentStatus> statuses);

    List<Appointment> findAllByAvailabilitySlotId(UUID availabilitySlotId);

    List<Appointment> findAllByStatusAndStartsAtBetweenAndReminderSentAtIsNull(
            AppointmentStatus status, Instant from, Instant to);

    long countByStatus(AppointmentStatus status);
}

package com.tabibma.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findAllByPatientId(UUID patientId);

    List<Appointment> findAllByAvailabilitySlotId(UUID availabilitySlotId);
}

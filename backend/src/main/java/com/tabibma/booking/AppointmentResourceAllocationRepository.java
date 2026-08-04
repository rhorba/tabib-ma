package com.tabibma.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AppointmentResourceAllocationRepository extends JpaRepository<AppointmentResourceAllocation, UUID> {

    List<AppointmentResourceAllocation> findAllByAppointmentId(UUID appointmentId);

    void deleteAllByAppointmentId(UUID appointmentId);

    List<AppointmentResourceAllocation> findAllByResourceIdInAndEndsAtAfterOrderByStartsAtAsc(
            List<UUID> resourceIds, Instant after);
}

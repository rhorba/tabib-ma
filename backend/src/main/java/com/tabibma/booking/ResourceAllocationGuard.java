package com.tabibma.booking;

import com.tabibma.shared.exception.ConflictException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Story 8.2 Batch 3: all-or-nothing conflict prevention for the resources (rooms/equipment) an
 * IN_PERSON availability rule may require, mirroring {@link DoubleBookingGuard}'s EXCLUDE-
 * constraint pattern (ADR-4) but keyed on resource_id (Flyway V15) instead of doctor_profile_id.
 * If a slot requires no resources, {@link #allocateForSlot} is a no-op.
 */
@Service
public class ResourceAllocationGuard {

    private final AvailabilitySlotResourceRepository availabilitySlotResourceRepository;
    private final AppointmentResourceAllocationRepository appointmentResourceAllocationRepository;

    public ResourceAllocationGuard(AvailabilitySlotResourceRepository availabilitySlotResourceRepository,
                                    AppointmentResourceAllocationRepository appointmentResourceAllocationRepository) {
        this.availabilitySlotResourceRepository = availabilitySlotResourceRepository;
        this.appointmentResourceAllocationRepository = appointmentResourceAllocationRepository;
    }

    @Transactional
    public void allocateForSlot(UUID appointmentId, UUID availabilitySlotId, Instant startsAt, Instant endsAt) {
        List<UUID> resourceIds = availabilitySlotResourceRepository.findAllByAvailabilitySlotId(availabilitySlotId)
                .stream()
                .map(AvailabilitySlotResource::getResourceId)
                .toList();
        for (UUID resourceId : resourceIds) {
            AppointmentResourceAllocation allocation =
                    new AppointmentResourceAllocation(appointmentId, resourceId, startsAt, endsAt);
            try {
                // saveAndFlush forces the INSERT (and the EXCLUDE check) to run now, inside this
                // try block — a plain save() would defer it to commit, outside our reach (same
                // reasoning as DoubleBookingGuard). Any conflict here must roll back the *whole*
                // booking, including any resources already allocated earlier in this loop and the
                // appointment/slot rows written upstream in BookingService.bookAndPay — achieved by
                // letting ConflictException propagate out of this @Transactional method, which
                // joins that caller's transaction rather than committing one of its own. The catch
                // is DataAccessException, not just DataIntegrityViolationException, per the same
                // GiST-deadlock lesson DoubleBookingGuard already learned under concurrency.
                appointmentResourceAllocationRepository.saveAndFlush(allocation);
            } catch (DataAccessException e) {
                throw new ConflictException("A required resource is no longer available for this time.");
            }
        }
    }

    @Transactional
    public void releaseForAppointment(UUID appointmentId) {
        appointmentResourceAllocationRepository.deleteAllByAppointmentId(appointmentId);
    }
}

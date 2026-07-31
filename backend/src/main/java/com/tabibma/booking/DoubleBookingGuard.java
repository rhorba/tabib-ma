package com.tabibma.booking;

import com.tabibma.shared.exception.ConflictException;
import com.tabibma.shared.exception.NotFoundException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Domain service enforcing the double-booking invariant (ADR-4, Architecture doc §4 —
 * Specification-lite pattern). Two lines of defense, in order:
 *  1. {@code SELECT ... FOR UPDATE} on the target slot serializes concurrent reservation
 *     attempts on that exact row.
 *  2. The `appointments` table's {@code EXCLUDE USING gist} constraint (Flyway V7) rejects any
 *     overlapping-time insert for the same doctor even if it comes from a *different* slot row —
 *     a case the row lock alone cannot catch.
 * Both failure paths surface as the same {@link ConflictException} to the caller. The second path
 * catches the broad {@link DataAccessException}, not just {@code DataIntegrityViolationException}:
 * two transactions concurrently inserting overlapping ranges can make Postgres's GiST exclusion
 * check deadlock (surfaced as {@code CannotAcquireLockException}) instead of cleanly rejecting the
 * second insert — found via the adversarial concurrency suite, not anticipated up front. Either
 * way the insert failed because of contention on this exact invariant, so both map to the same
 * ConflictException.
 */
@Service
public class DoubleBookingGuard {

    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final AppointmentRepository appointmentRepository;

    public DoubleBookingGuard(AvailabilitySlotRepository availabilitySlotRepository,
                               AppointmentRepository appointmentRepository) {
        this.availabilitySlotRepository = availabilitySlotRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional
    public Appointment reserveSlot(UUID patientId, UUID availabilitySlotId) {
        AvailabilitySlot slot = availabilitySlotRepository.findByIdForUpdate(availabilitySlotId)
                .orElseThrow(() -> new NotFoundException("Availability slot not found."));
        if (slot.isBooked()) {
            throw new ConflictException("This slot was just booked.");
        }

        slot.markBooked();
        availabilitySlotRepository.save(slot);

        Appointment appointment = new Appointment(patientId, slot.getDoctorProfileId(), slot.getId(),
                slot.getStartsAt(), slot.getEndsAt(), slot.getLocationType());
        try {
            // saveAndFlush forces the INSERT (and the EXCLUDE constraint check) to run now, inside
            // this try block — a plain save() would defer the INSERT to commit, outside our reach.
            return appointmentRepository.saveAndFlush(appointment);
        } catch (DataAccessException e) {
            throw new ConflictException("This slot was just booked.");
        }
    }
}

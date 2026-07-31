package com.tabibma.booking;

import com.tabibma.shared.exception.ConflictException;
import com.tabibma.shared.exception.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
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
 * Both failure paths surface as the same {@link ConflictException} to the caller.
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
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("This slot was just booked.");
        }
    }
}

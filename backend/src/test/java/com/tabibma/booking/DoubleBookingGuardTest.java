package com.tabibma.booking;

import com.tabibma.shared.exception.ConflictException;
import com.tabibma.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoubleBookingGuardTest {

    @Mock
    private AvailabilitySlotRepository availabilitySlotRepository;
    @Mock
    private AppointmentRepository appointmentRepository;

    private DoubleBookingGuard guard;

    @BeforeEach
    void setUp() {
        guard = new DoubleBookingGuard(availabilitySlotRepository, appointmentRepository);
    }

    @Test
    void reserveSlot_rejectsWhenSlotNotFound() {
        UUID slotId = UUID.randomUUID();
        when(availabilitySlotRepository.findByIdForUpdate(slotId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.reserveSlot(UUID.randomUUID(), slotId))
                .isInstanceOf(NotFoundException.class);
        verify(appointmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void reserveSlot_rejectsWhenAlreadyBooked() {
        UUID doctorProfileId = UUID.randomUUID();
        Instant start = Instant.now();
        AvailabilitySlot slot = new AvailabilitySlot(doctorProfileId, start, start.plusSeconds(1800), LocationType.IN_PERSON, null);
        slot.markBooked();
        UUID slotId = UUID.randomUUID();
        when(availabilitySlotRepository.findByIdForUpdate(slotId)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> guard.reserveSlot(UUID.randomUUID(), slotId))
                .isInstanceOf(ConflictException.class);
        verify(appointmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void reserveSlot_marksSlotBookedAndCreatesAppointment() {
        UUID doctorProfileId = UUID.randomUUID();
        Instant start = Instant.now();
        AvailabilitySlot slot = new AvailabilitySlot(doctorProfileId, start, start.plusSeconds(1800), LocationType.IN_PERSON, null);
        UUID slotId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        when(availabilitySlotRepository.findByIdForUpdate(slotId)).thenReturn(Optional.of(slot));
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment appointment = guard.reserveSlot(patientId, slotId);

        assertThat(slot.isBooked()).isTrue();
        assertThat(appointment.getPatientId()).isEqualTo(patientId);
        assertThat(appointment.getDoctorProfileId()).isEqualTo(doctorProfileId);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PENDING_PAYMENT);
        verify(availabilitySlotRepository).save(slot);
    }

    @Test
    void reserveSlot_translatesExcludeConstraintViolationToConflict() {
        UUID doctorProfileId = UUID.randomUUID();
        Instant start = Instant.now();
        AvailabilitySlot slot = new AvailabilitySlot(doctorProfileId, start, start.plusSeconds(1800), LocationType.IN_PERSON, null);
        UUID slotId = UUID.randomUUID();
        when(availabilitySlotRepository.findByIdForUpdate(slotId)).thenReturn(Optional.of(slot));
        when(appointmentRepository.saveAndFlush(any(Appointment.class)))
                .thenThrow(new DataIntegrityViolationException("exclusion constraint violated"));

        assertThatThrownBy(() -> guard.reserveSlot(UUID.randomUUID(), slotId))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void reserveSlot_translatesDeadlockFromConcurrentExcludeCheckToConflict() {
        // Regression test: Postgres's GiST exclusion-constraint check can deadlock (not just
        // cleanly reject) when two transactions concurrently insert overlapping ranges — found via
        // the real-Postgres adversarial suite (DoubleBookingConcurrencyIntegrationTest), not
        // anticipated up front. CannotAcquireLockException is NOT a DataIntegrityViolationException.
        UUID doctorProfileId = UUID.randomUUID();
        Instant start = Instant.now();
        AvailabilitySlot slot = new AvailabilitySlot(doctorProfileId, start, start.plusSeconds(1800), LocationType.IN_PERSON, null);
        UUID slotId = UUID.randomUUID();
        when(availabilitySlotRepository.findByIdForUpdate(slotId)).thenReturn(Optional.of(slot));
        when(appointmentRepository.saveAndFlush(any(Appointment.class)))
                .thenThrow(new org.springframework.dao.CannotAcquireLockException("deadlock detected"));

        assertThatThrownBy(() -> guard.reserveSlot(UUID.randomUUID(), slotId))
                .isInstanceOf(ConflictException.class);
    }
}

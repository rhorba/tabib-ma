package com.tabibma.booking;

import com.tabibma.identity.Role;
import com.tabibma.identity.UserContext;
import com.tabibma.payment.Payment;
import com.tabibma.payment.PaymentRepository;
import com.tabibma.payment.PaymentStatus;
import com.tabibma.shared.exception.ConflictException;
import com.tabibma.shared.exception.ForbiddenException;
import com.tabibma.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class CancellationServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private AvailabilitySlotRepository availabilitySlotRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ResourceAllocationGuard resourceAllocationGuard;

    private CancellationService service;

    @BeforeEach
    void setUp() {
        service = new CancellationService(appointmentRepository, availabilitySlotRepository, paymentRepository,
                resourceAllocationGuard);
    }

    @Test
    void cancel_rejectsWhenAppointmentNotFound() {
        UserContext patient = new UserContext(UUID.randomUUID(), "p@example.com", Role.PATIENT);
        UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(patient, appointmentId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void cancel_rejectsWhenCallerIsNotThePatient() {
        UserContext caller = new UserContext(UUID.randomUUID(), "p@example.com", Role.PATIENT);
        Appointment appointment = new Appointment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Instant.now().plusSeconds(3600), Instant.now().plusSeconds(5400), LocationType.IN_PERSON);
        UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.cancel(caller, appointmentId)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cancel_rejectsWhenAlreadyCancelled() {
        UUID patientId = UUID.randomUUID();
        UserContext patient = new UserContext(patientId, "p@example.com", Role.PATIENT);
        Appointment appointment = new Appointment(patientId, UUID.randomUUID(), UUID.randomUUID(),
                Instant.now().plusSeconds(3600), Instant.now().plusSeconds(5400), LocationType.IN_PERSON);
        appointment.cancel();
        UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.cancel(patient, appointmentId)).isInstanceOf(ConflictException.class);
    }

    @Test
    void cancel_releasesSlotAndRefundsWhenWellWithinWindow() {
        UUID patientId = UUID.randomUUID();
        UserContext patient = new UserContext(patientId, "p@example.com", Role.PATIENT);
        UUID slotId = UUID.randomUUID();
        UUID doctorProfileId = UUID.randomUUID();
        Instant start = Instant.now().plusSeconds(48 * 3600);
        Appointment appointment = new Appointment(patientId, doctorProfileId, slotId, start, start.plusSeconds(1800), LocationType.IN_PERSON);
        UUID appointmentId = UUID.randomUUID();
        AvailabilitySlot slot = new AvailabilitySlot(doctorProfileId, start, start.plusSeconds(1800), LocationType.IN_PERSON, null);
        slot.markBooked();
        Payment payment = new Payment(appointmentId, new BigDecimal("150.00"), "key");
        payment.succeed("CMI-1");

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(availabilitySlotRepository.findById(slotId)).thenReturn(Optional.of(slot));
        when(paymentRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.of(payment));

        Appointment result = service.cancel(patient, appointmentId);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(slot.isBooked()).isFalse();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(availabilitySlotRepository).save(slot);
        verify(paymentRepository).save(payment);
        verify(resourceAllocationGuard).releaseForAppointment(appointmentId);
    }

    @Test
    void cancel_releasesSlotWithoutRefundingWhenOutsideWindow() {
        UUID patientId = UUID.randomUUID();
        UserContext patient = new UserContext(patientId, "p@example.com", Role.PATIENT);
        UUID slotId = UUID.randomUUID();
        UUID doctorProfileId = UUID.randomUUID();
        Instant start = Instant.now().plusSeconds(3600); // 1h away, inside the default 24h window
        Appointment appointment = new Appointment(patientId, doctorProfileId, slotId, start, start.plusSeconds(1800), LocationType.IN_PERSON);
        UUID appointmentId = UUID.randomUUID();
        AvailabilitySlot slot = new AvailabilitySlot(doctorProfileId, start, start.plusSeconds(1800), LocationType.IN_PERSON, null);
        slot.markBooked();

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(availabilitySlotRepository.findById(slotId)).thenReturn(Optional.of(slot));

        Appointment result = service.cancel(patient, appointmentId);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(slot.isBooked()).isFalse();
        verify(paymentRepository, never()).findByAppointmentId(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void forceCancel_rejectsWhenAppointmentNotFound() {
        UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.forceCancel(appointmentId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void forceCancel_rejectsWhenAlreadyCancelled() {
        Appointment appointment = new Appointment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Instant.now().plusSeconds(3600), Instant.now().plusSeconds(5400), LocationType.IN_PERSON);
        appointment.cancel();
        UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.forceCancel(appointmentId)).isInstanceOf(ConflictException.class);
    }

    @Test
    void forceCancel_releasesSlotRegardlessOfWhoOwnsTheAppointmentAndNeverTouchesPayment() {
        UUID slotId = UUID.randomUUID();
        UUID doctorProfileId = UUID.randomUUID();
        Instant start = Instant.now().plusSeconds(3600);
        Appointment appointment = new Appointment(UUID.randomUUID(), doctorProfileId, slotId, start,
                start.plusSeconds(1800), LocationType.IN_PERSON);
        UUID appointmentId = UUID.randomUUID();
        AvailabilitySlot slot = new AvailabilitySlot(doctorProfileId, start, start.plusSeconds(1800), LocationType.IN_PERSON, null);
        slot.markBooked();

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(availabilitySlotRepository.findById(slotId)).thenReturn(Optional.of(slot));

        Appointment result = service.forceCancel(appointmentId);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(slot.isBooked()).isFalse();
        verify(availabilitySlotRepository).save(slot);
        verify(resourceAllocationGuard).releaseForAppointment(appointmentId);
        verify(paymentRepository, never()).findByAppointmentId(any());
        verify(paymentRepository, never()).save(any());
    }
}

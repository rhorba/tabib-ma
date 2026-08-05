package com.tabibma.booking;

import com.tabibma.clinic.DoctorProfile;
import com.tabibma.clinic.DoctorProfileRepository;
import com.tabibma.identity.Role;
import com.tabibma.identity.UserContext;
import com.tabibma.payment.Payment;
import com.tabibma.payment.PaymentService;
import com.tabibma.shared.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private DoubleBookingGuard doubleBookingGuard;
    @Mock
    private ResourceAllocationGuard resourceAllocationGuard;
    @Mock
    private AvailabilitySlotRepository availabilitySlotRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private DoctorProfileRepository doctorProfileRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private BookingService service;

    @BeforeEach
    void setUp() {
        service = new BookingService(doubleBookingGuard, resourceAllocationGuard, availabilitySlotRepository,
                appointmentRepository, doctorProfileRepository, paymentService, eventPublisher);
    }

    private static Appointment pendingAppointment(UUID doctorProfileId, UUID slotId) {
        Instant start = Instant.now();
        return new Appointment(UUID.randomUUID(), doctorProfileId, slotId, start, start.plusSeconds(1800), LocationType.IN_PERSON);
    }

    @Test
    void bookAndPay_rejectsNonPatientRole() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);

        assertThatThrownBy(() -> service.bookAndPay(doctor, UUID.randomUUID()))
                .isInstanceOf(ForbiddenException.class);
        verify(doubleBookingGuard, never()).reserveSlot(any(), any());
    }

    @Test
    void bookAndPay_confirmsAppointmentAndPublishesEventOnPaymentSuccess() {
        UserContext patient = new UserContext(UUID.randomUUID(), "p@example.com", Role.PATIENT);
        UUID slotId = UUID.randomUUID();
        UUID doctorProfileId = UUID.randomUUID();
        Appointment appointment = pendingAppointment(doctorProfileId, slotId);
        DoctorProfile doctorProfile = new DoctorProfile(UUID.randomUUID(), "Cardiology", "bio", new BigDecimal("250.00"), "Rabat");
        Payment succeededPayment = new Payment(appointment.getId(), new BigDecimal("250.00"), "key");
        succeededPayment.succeed("CMI-1");

        when(doubleBookingGuard.reserveSlot(patient.userId(), slotId)).thenReturn(appointment);
        when(doctorProfileRepository.findById(doctorProfileId)).thenReturn(Optional.of(doctorProfile));
        when(paymentService.capturePayment(eq(appointment.getId()), eq(new BigDecimal("250.00")), any()))
                .thenReturn(succeededPayment);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment result = service.bookAndPay(patient, slotId);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        verify(eventPublisher).publishEvent(new BookingConfirmedEvent(appointment.getId()));
        verify(availabilitySlotRepository, never()).save(any());
        verify(resourceAllocationGuard).allocateForSlot(appointment.getId(), slotId,
                appointment.getStartsAt(), appointment.getEndsAt());
        verify(resourceAllocationGuard, never()).releaseForAppointment(any());
    }

    @Test
    void bookAndPay_cancelsAppointmentAndReleasesSlotOnPaymentFailure() {
        UserContext patient = new UserContext(UUID.randomUUID(), "p@example.com", Role.PATIENT);
        UUID slotId = UUID.randomUUID();
        UUID doctorProfileId = UUID.randomUUID();
        Appointment appointment = pendingAppointment(doctorProfileId, slotId);
        DoctorProfile doctorProfile = new DoctorProfile(UUID.randomUUID(), "Cardiology", "bio", new BigDecimal("250.00"), "Rabat");
        Payment failedPayment = new Payment(appointment.getId(), new BigDecimal("250.00"), "key");
        failedPayment.fail();
        AvailabilitySlot slot = new AvailabilitySlot(doctorProfileId, appointment.getStartsAt(), appointment.getEndsAt(),
                LocationType.IN_PERSON, null);
        slot.markBooked();

        when(doubleBookingGuard.reserveSlot(patient.userId(), slotId)).thenReturn(appointment);
        when(doctorProfileRepository.findById(doctorProfileId)).thenReturn(Optional.of(doctorProfile));
        when(paymentService.capturePayment(eq(appointment.getId()), eq(new BigDecimal("250.00")), any()))
                .thenReturn(failedPayment);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(availabilitySlotRepository.findById(slotId)).thenReturn(Optional.of(slot));

        Appointment result = service.bookAndPay(patient, slotId);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(slot.isBooked()).isFalse();
        verify(availabilitySlotRepository).save(slot);
        verify(eventPublisher).publishEvent(new AppointmentPaymentFailedEvent(appointment.getId()));
        verify(resourceAllocationGuard).releaseForAppointment(appointment.getId());
    }

    @Test
    void bookAndPay_propagatesResourceConflictWithoutCapturingPayment() {
        UserContext patient = new UserContext(UUID.randomUUID(), "p@example.com", Role.PATIENT);
        UUID slotId = UUID.randomUUID();
        UUID doctorProfileId = UUID.randomUUID();
        Appointment appointment = pendingAppointment(doctorProfileId, slotId);

        when(doubleBookingGuard.reserveSlot(patient.userId(), slotId)).thenReturn(appointment);
        org.mockito.Mockito.doThrow(new com.tabibma.shared.exception.ConflictException("conflict"))
                .when(resourceAllocationGuard)
                .allocateForSlot(appointment.getId(), slotId, appointment.getStartsAt(), appointment.getEndsAt());

        assertThatThrownBy(() -> service.bookAndPay(patient, slotId))
                .isInstanceOf(com.tabibma.shared.exception.ConflictException.class);
        verify(paymentService, never()).capturePayment(any(), any(), any());
    }

    @Test
    void listMyAppointments_delegatesToRepository() {
        UserContext patient = new UserContext(UUID.randomUUID(), "p@example.com", Role.PATIENT);
        Appointment appointment = pendingAppointment(UUID.randomUUID(), UUID.randomUUID());
        when(appointmentRepository.findAllByPatientId(patient.userId())).thenReturn(List.of(appointment));

        assertThat(service.listMyAppointments(patient)).containsExactly(appointment);
    }

    @Test
    void listMyAppointments_forADoctorListsByTheirOwnDoctorProfileId() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        DoctorProfile doctorProfile = new DoctorProfile(doctor.userId(), "Cardiology", "bio", new BigDecimal("250.00"), "Rabat");
        Appointment appointment = pendingAppointment(UUID.randomUUID(), UUID.randomUUID());
        when(doctorProfileRepository.findByUserId(doctor.userId())).thenReturn(Optional.of(doctorProfile));
        when(appointmentRepository.findAllByDoctorProfileId(any())).thenReturn(List.of(appointment));

        assertThat(service.listMyAppointments(doctor)).containsExactly(appointment);
        verify(appointmentRepository).findAllByDoctorProfileId(doctorProfile.getId());
        verify(appointmentRepository, never()).findAllByPatientId(any());
    }

    @Test
    void listMyAppointments_rejectsADoctorWithNoProfileYet() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        when(doctorProfileRepository.findByUserId(doctor.userId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listMyAppointments(doctor))
                .isInstanceOf(com.tabibma.shared.exception.NotFoundException.class);
    }
}

package com.tabibma.admin;

import com.tabibma.booking.Appointment;
import com.tabibma.booking.AppointmentRepository;
import com.tabibma.booking.CancellationService;
import com.tabibma.booking.LocationType;
import com.tabibma.identity.Role;
import com.tabibma.identity.UserContext;
import com.tabibma.payment.Payment;
import com.tabibma.payment.PaymentRepository;
import com.tabibma.shared.audit.AuditLogRepository;
import com.tabibma.shared.exception.ConflictException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAppointmentActionServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private CancellationService cancellationService;
    @Mock
    private AuditLogRepository auditLogRepository;

    private AdminAppointmentActionService service;

    private UserContext admin;

    @BeforeEach
    void setUp() {
        service = new AdminAppointmentActionService(appointmentRepository, paymentRepository, cancellationService,
                auditLogRepository);
        admin = new UserContext(UUID.randomUUID(), "admin@example.com", Role.PLATFORM_ADMIN);
    }

    @Test
    void refund_throwsNotFoundForAnUnknownAppointment() {
        UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.existsById(appointmentId)).thenReturn(false);

        assertThatThrownBy(() -> service.refund(admin, appointmentId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void refund_throwsNotFoundWhenNoPaymentExistsForTheAppointment() {
        UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.existsById(appointmentId)).thenReturn(true);
        when(paymentRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refund(admin, appointmentId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void refund_rejectsAPaymentThatIsNotSucceeded() {
        UUID appointmentId = UUID.randomUUID();
        Payment payment = new Payment(appointmentId, new BigDecimal("150.00"), "key");
        when(appointmentRepository.existsById(appointmentId)).thenReturn(true);
        when(paymentRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> service.refund(admin, appointmentId)).isInstanceOf(ConflictException.class);
    }

    @Test
    void refund_marksThePaymentRefundedAndLogsAnAuditEntry() {
        UUID appointmentId = UUID.randomUUID();
        Payment payment = new Payment(appointmentId, new BigDecimal("150.00"), "key");
        payment.succeed("CMI-1");
        when(appointmentRepository.existsById(appointmentId)).thenReturn(true);
        when(paymentRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = service.refund(admin, appointmentId);

        assertThat(result.getStatus()).isEqualTo(com.tabibma.payment.PaymentStatus.REFUNDED);
        verify(auditLogRepository).save(any());
    }

    @Test
    void forceCancel_propagatesNotFoundFromCancellationService() {
        UUID appointmentId = UUID.randomUUID();
        when(cancellationService.forceCancel(appointmentId)).thenThrow(new NotFoundException("Appointment not found."));

        assertThatThrownBy(() -> service.forceCancel(admin, appointmentId)).isInstanceOf(NotFoundException.class);
        verify(auditLogRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void forceCancel_delegatesToCancellationServiceAndLogsAnAuditEntry() {
        UUID appointmentId = UUID.randomUUID();
        Instant start = Instant.now().plusSeconds(3600);
        Appointment appointment = new Appointment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), start,
                start.plusSeconds(1800), LocationType.IN_PERSON);
        appointment.cancel();
        when(cancellationService.forceCancel(appointmentId)).thenReturn(appointment);

        Appointment result = service.forceCancel(admin, appointmentId);

        assertThat(result).isSameAs(appointment);
        verify(auditLogRepository).save(any());
    }
}

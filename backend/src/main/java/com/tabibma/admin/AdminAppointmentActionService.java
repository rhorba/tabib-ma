package com.tabibma.admin;

import com.tabibma.booking.Appointment;
import com.tabibma.booking.AppointmentRepository;
import com.tabibma.booking.CancellationService;
import com.tabibma.identity.UserContext;
import com.tabibma.payment.Payment;
import com.tabibma.payment.PaymentRepository;
import com.tabibma.shared.audit.AuditLog;
import com.tabibma.shared.audit.AuditLogRepository;
import com.tabibma.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Story 10.2: exceptional-case admin actions on a booking — refund and force-cancel, kept as two
 * independent effects (see {@link CancellationService#forceCancel}'s javadoc) reusing the existing
 * guarded domain methods ({@link Payment#refund()}, {@link Appointment#cancel()} via
 * {@link CancellationService#forceCancel}) rather than a new "force-complete" path, for which no
 * concrete use case exists. Every action is audit-logged immutably per PRD NFR-8, same pattern as
 * {@link DisputeService#resolve}.
 */
@Service
public class AdminAppointmentActionService {

    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final CancellationService cancellationService;
    private final AuditLogRepository auditLogRepository;

    public AdminAppointmentActionService(AppointmentRepository appointmentRepository,
                                          PaymentRepository paymentRepository,
                                          CancellationService cancellationService,
                                          AuditLogRepository auditLogRepository) {
        this.appointmentRepository = appointmentRepository;
        this.paymentRepository = paymentRepository;
        this.cancellationService = cancellationService;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public Payment refund(UserContext principal, UUID appointmentId) {
        if (!appointmentRepository.existsById(appointmentId)) {
            throw new NotFoundException("Appointment not found.");
        }
        Payment payment = paymentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new NotFoundException("No payment found for this appointment."));

        payment.refund();
        Payment saved = paymentRepository.save(payment);
        auditLogRepository.save(new AuditLog(principal.userId(), "PAYMENT_REFUNDED", "appointment", appointmentId));
        return saved;
    }

    @Transactional
    public Appointment forceCancel(UserContext principal, UUID appointmentId) {
        Appointment appointment = cancellationService.forceCancel(appointmentId);
        auditLogRepository.save(new AuditLog(principal.userId(), "APPOINTMENT_FORCE_CANCELLED", "appointment", appointmentId));
        return appointment;
    }
}

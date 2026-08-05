package com.tabibma.admin;

import com.tabibma.booking.AppointmentNoShowEvent;
import com.tabibma.booking.AppointmentPaymentFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Story 10.1 Batch 2: auto-files a dispute when a doctor marks a no-show or a payment fails,
 * without booking needing to know disputes exist (Domain Events pattern, Architecture doc §4) —
 * mirrors ConsultationBookingListener's exact shape, including why REQUIRES_NEW is required
 * rather than optional: at AFTER_COMMIT the original transaction's resources are still bound to
 * the thread but already committed, so a plain (REQUIRED) write here would silently "participate"
 * in that dead transaction and never actually persist.
 */
@Component
public class DisputeEventListener {

    private static final Logger log = LoggerFactory.getLogger(DisputeEventListener.class);

    private final DisputeService disputeService;

    public DisputeEventListener(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAppointmentNoShow(AppointmentNoShowEvent event) {
        try {
            disputeService.createSystem(event.appointmentId(), DisputeType.NO_SHOW);
        } catch (Exception e) {
            log.error("Failed to auto-file a NO_SHOW dispute for appointment {}", event.appointmentId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAppointmentPaymentFailed(AppointmentPaymentFailedEvent event) {
        try {
            disputeService.createSystem(event.appointmentId(), DisputeType.PAYMENT_ISSUE);
        } catch (Exception e) {
            log.error("Failed to auto-file a PAYMENT_ISSUE dispute for appointment {}", event.appointmentId(), e);
        }
    }
}

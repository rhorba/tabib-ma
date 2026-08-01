package com.tabibma.consultation;

import com.tabibma.booking.Appointment;
import com.tabibma.booking.AppointmentRepository;
import com.tabibma.booking.BookingConfirmedEvent;
import com.tabibma.booking.LocationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Architecture doc §3: "Consultation created only when appointment.status == CONFIRMED and
 * slot.locationType == VIDEO." Reacts to the same BookingConfirmedEvent the notification module
 * already listens to (Domain Events pattern, Architecture doc §4) — the booking module doesn't
 * know consultations exist. AFTER_COMMIT + a caught exception (not @Async, unlike the notification
 * listener: a failure here is a real bug worth surfacing in logs immediately, not something to
 * silently retry) mirrors BookingNotificationListener's "provider/downstream failure doesn't roll
 * back the already-committed booking" resilience.
 * <p>
 * {@code REQUIRES_NEW} is required, not optional: at AFTER_COMMIT the original transaction's
 * resources are still bound to the thread but already committed, so a plain (REQUIRED)
 * {@code @Transactional} write here would silently "participate" in that dead transaction and
 * never actually commit — a documented Spring caveat (see {@code TransactionalEventListener}
 * Javadoc). Found by an integration test: the Consultation row was returned with a generated id
 * from {@code save()} but never actually visible on a subsequent read.
 */
@Component
public class ConsultationBookingListener {

    private static final Logger log = LoggerFactory.getLogger(ConsultationBookingListener.class);

    private final AppointmentRepository appointmentRepository;
    private final ConsultationRepository consultationRepository;

    public ConsultationBookingListener(AppointmentRepository appointmentRepository,
                                        ConsultationRepository consultationRepository) {
        this.appointmentRepository = appointmentRepository;
        this.consultationRepository = consultationRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        try {
            Appointment appointment = appointmentRepository.findById(event.appointmentId()).orElse(null);
            if (appointment == null || appointment.getLocationType() != LocationType.VIDEO) {
                return;
            }
            if (consultationRepository.findByAppointmentId(appointment.getId()).isPresent()) {
                return;
            }
            consultationRepository.save(new Consultation(appointment.getId()));
        } catch (Exception e) {
            log.error("Failed to create Consultation for appointment {}", event.appointmentId(), e);
        }
    }
}

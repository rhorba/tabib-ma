package com.tabibma.notification;

import com.tabibma.booking.Appointment;
import com.tabibma.booking.AppointmentRepository;
import com.tabibma.booking.BookingConfirmedEvent;
import com.tabibma.booking.ReminderDueEvent;
import com.tabibma.identity.User;
import com.tabibma.identity.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

/**
 * Story 4.2's "patient receives a confirmation SMS and email" + Story 4.5's reminder delivery.
 * {@code @TransactionalEventListener(AFTER_COMMIT)} + {@code @Async} means this runs on a
 * separate thread only after the booking/reminder transaction already committed — a send failure
 * here structurally cannot roll back or block that transaction. The try/catch around each send is
 * the "provider down doesn't block booking confirmation" resilience (Test Strategy §4 NETWORK);
 * a full circuit-breaker library was judged unnecessary for a mocked vendor (see .logs/decisions.md).
 */
@Component
public class BookingNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(BookingNotificationListener.class);

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final SmsSender smsSender;
    private final EmailSender emailSender;

    public BookingNotificationListener(AppointmentRepository appointmentRepository, UserRepository userRepository,
                                        SmsSender smsSender, EmailSender emailSender) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.smsSender = smsSender;
        this.emailSender = emailSender;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        appointmentRepository.findById(event.appointmentId()).ifPresent(appointment -> notify(appointment,
                "Appointment Confirmed", "Your appointment on " + appointment.getStartsAt() + " is confirmed."));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReminderDue(ReminderDueEvent event) {
        appointmentRepository.findById(event.appointmentId()).ifPresent(appointment -> notify(appointment,
                "Appointment Reminder", "Reminder: you have an appointment on " + appointment.getStartsAt() + "."));
    }

    private void notify(Appointment appointment, String subject, String message) {
        Optional<User> patient = userRepository.findById(appointment.getPatientId());
        if (patient.isEmpty()) {
            log.warn("Patient {} not found for appointment {}; skipping notification.",
                    appointment.getPatientId(), appointment.getId());
            return;
        }
        User user = patient.get();

        try {
            emailSender.send(user.getEmail(), subject, message);
        } catch (Exception e) {
            log.warn("Email send failed for appointment {}", appointment.getId(), e);
        }

        if (user.getPhone() != null) {
            try {
                smsSender.send(user.getPhone(), message);
            } catch (Exception e) {
                log.warn("SMS send failed for appointment {}", appointment.getId(), e);
            }
        }
    }
}

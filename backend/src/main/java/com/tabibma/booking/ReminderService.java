package com.tabibma.booking;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Story 4.5's "reminder lead time is reached" sweep: periodically finds CONFIRMED appointments
 * starting within the lead time that haven't had a reminder sent yet, and fires one
 * {@link ReminderDueEvent} each. No lead-time value is specified anywhere in the docs; defaulting
 * to 24h (config: app.notifications.reminder-lead-time-hours) to match the existing 24h
 * cancellation-window default.
 */
@Service
public class ReminderService {

    private final AppointmentRepository appointmentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Duration reminderLeadTime;

    public ReminderService(AppointmentRepository appointmentRepository,
                            ApplicationEventPublisher eventPublisher,
                            @Value("${app.notifications.reminder-lead-time-hours:24}") long reminderLeadTimeHours) {
        this.appointmentRepository = appointmentRepository;
        this.eventPublisher = eventPublisher;
        this.reminderLeadTime = Duration.ofHours(reminderLeadTimeHours);
    }

    @Scheduled(fixedDelayString = "${app.notifications.reminder-sweep-interval-ms:900000}")
    @Transactional
    public void sendDueReminders() {
        Instant now = Instant.now();
        Instant leadTimeCutoff = now.plus(reminderLeadTime);
        List<Appointment> due = appointmentRepository.findAllByStatusAndStartsAtBetweenAndReminderSentAtIsNull(
                AppointmentStatus.CONFIRMED, now, leadTimeCutoff);
        for (Appointment appointment : due) {
            appointment.markReminderSent();
            appointmentRepository.save(appointment);
            eventPublisher.publishEvent(new ReminderDueEvent(appointment.getId()));
        }
    }
}

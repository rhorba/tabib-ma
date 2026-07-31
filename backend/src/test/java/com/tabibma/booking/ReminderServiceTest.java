package com.tabibma.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ReminderService service;

    @BeforeEach
    void setUp() {
        service = new ReminderService(appointmentRepository, eventPublisher, 24);
    }

    @Test
    void sendDueReminders_marksSentAndPublishesEventForEachDueAppointment() {
        Instant start = Instant.now().plusSeconds(3600);
        Appointment appointment = new Appointment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                start, start.plusSeconds(1800), LocationType.IN_PERSON);
        when(appointmentRepository.findAllByStatusAndStartsAtBetweenAndReminderSentAtIsNull(
                eq(AppointmentStatus.CONFIRMED), any(), any())).thenReturn(List.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.sendDueReminders();

        assertThat(appointment.getReminderSentAt()).isNotNull();
        verify(appointmentRepository).save(appointment);
        verify(eventPublisher).publishEvent(new ReminderDueEvent(appointment.getId()));
    }

    @Test
    void sendDueReminders_doesNothingWhenNoneAreDue() {
        when(appointmentRepository.findAllByStatusAndStartsAtBetweenAndReminderSentAtIsNull(
                eq(AppointmentStatus.CONFIRMED), any(), any())).thenReturn(List.of());

        service.sendDueReminders();

        verify(appointmentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}

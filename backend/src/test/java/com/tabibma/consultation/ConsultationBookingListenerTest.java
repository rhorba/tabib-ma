package com.tabibma.consultation;

import com.tabibma.booking.Appointment;
import com.tabibma.booking.AppointmentRepository;
import com.tabibma.booking.BookingConfirmedEvent;
import com.tabibma.booking.LocationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultationBookingListenerTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private ConsultationRepository consultationRepository;

    private ConsultationBookingListener listener;

    @BeforeEach
    void setUp() {
        listener = new ConsultationBookingListener(appointmentRepository, consultationRepository);
    }

    private static Appointment appointment(LocationType locationType) {
        Instant start = Instant.now();
        return new Appointment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), start,
                start.plusSeconds(1800), locationType);
    }

    @Test
    void onBookingConfirmed_createsAConsultationForAVideoAppointment() {
        Appointment appointment = appointment(LocationType.VIDEO);
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(consultationRepository.findByAppointmentId(appointment.getId())).thenReturn(Optional.empty());

        listener.onBookingConfirmed(new BookingConfirmedEvent(appointment.getId()));

        verify(consultationRepository).save(any(Consultation.class));
    }

    @Test
    void onBookingConfirmed_doesNothingForAnInPersonAppointment() {
        Appointment appointment = appointment(LocationType.IN_PERSON);
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));

        listener.onBookingConfirmed(new BookingConfirmedEvent(appointment.getId()));

        verify(consultationRepository, never()).save(any());
    }

    @Test
    void onBookingConfirmed_doesNothingIfAConsultationAlreadyExists() {
        Appointment appointment = appointment(LocationType.VIDEO);
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(consultationRepository.findByAppointmentId(appointment.getId()))
                .thenReturn(Optional.of(new Consultation(appointment.getId())));

        listener.onBookingConfirmed(new BookingConfirmedEvent(appointment.getId()));

        verify(consultationRepository, never()).save(any());
    }

    @Test
    void onBookingConfirmed_swallowsUnexpectedFailuresInsteadOfPropagating() {
        UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId)).thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> listener.onBookingConfirmed(new BookingConfirmedEvent(appointmentId)))
                .doesNotThrowAnyException();
    }
}

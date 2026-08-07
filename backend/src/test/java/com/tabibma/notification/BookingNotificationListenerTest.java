package com.tabibma.notification;

import com.tabibma.booking.Appointment;
import com.tabibma.booking.AppointmentCancelledEvent;
import com.tabibma.booking.AppointmentRepository;
import com.tabibma.booking.BookingConfirmedEvent;
import com.tabibma.booking.LocationType;
import com.tabibma.booking.ReminderDueEvent;
import com.tabibma.clinic.DoctorProfile;
import com.tabibma.clinic.DoctorProfileRepository;
import com.tabibma.identity.Role;
import com.tabibma.identity.User;
import com.tabibma.identity.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingNotificationListenerTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private DoctorProfileRepository doctorProfileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SmsSender smsSender;
    @Mock
    private EmailSender emailSender;

    private BookingNotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new BookingNotificationListener(appointmentRepository, doctorProfileRepository, userRepository,
                smsSender, emailSender);
    }

    private static Appointment appointment(UUID patientId) {
        return appointment(patientId, UUID.randomUUID());
    }

    private static Appointment appointment(UUID patientId, UUID doctorProfileId) {
        Instant start = Instant.now().plusSeconds(3600);
        return new Appointment(patientId, doctorProfileId, UUID.randomUUID(), start, start.plusSeconds(1800), LocationType.IN_PERSON);
    }

    @Test
    void onBookingConfirmed_sendsEmailAndSmsWhenPhonePresent() {
        UUID patientId = UUID.randomUUID();
        Appointment appointment = appointment(patientId);
        UUID appointmentId = UUID.randomUUID();
        User patient = new User("p@example.com", "hash", Role.PATIENT, "A", "B");
        patient.setPhone("+212600000000");
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));

        listener.onBookingConfirmed(new BookingConfirmedEvent(appointmentId));

        verify(emailSender).send(eq("p@example.com"), anyString(), anyString());
        verify(smsSender).send(eq("+212600000000"), anyString());
    }

    @Test
    void onBookingConfirmed_sendsOnlyEmailWhenNoPhoneOnFile() {
        UUID patientId = UUID.randomUUID();
        Appointment appointment = appointment(patientId);
        UUID appointmentId = UUID.randomUUID();
        User patient = new User("p@example.com", "hash", Role.PATIENT, "A", "B");
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));

        listener.onBookingConfirmed(new BookingConfirmedEvent(appointmentId));

        verify(emailSender).send(eq("p@example.com"), anyString(), anyString());
        verify(smsSender, never()).send(any(), any());
    }

    @Test
    void onBookingConfirmed_doesNothingWhenAppointmentMissing() {
        UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        listener.onBookingConfirmed(new BookingConfirmedEvent(appointmentId));

        verify(emailSender, never()).send(any(), any(), any());
    }

    @Test
    void onBookingConfirmed_swallowsEmailSendFailureAndStillAttemptsSms() {
        UUID patientId = UUID.randomUUID();
        Appointment appointment = appointment(patientId);
        UUID appointmentId = UUID.randomUUID();
        User patient = new User("p@example.com", "hash", Role.PATIENT, "A", "B");
        patient.setPhone("+212600000000");
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));
        doThrow(new RuntimeException("provider down")).when(emailSender).send(any(), any(), any());

        listener.onBookingConfirmed(new BookingConfirmedEvent(appointmentId));

        verify(smsSender).send(eq("+212600000000"), anyString());
    }

    @Test
    void onReminderDue_sendsNotification() {
        UUID patientId = UUID.randomUUID();
        Appointment appointment = appointment(patientId);
        UUID appointmentId = UUID.randomUUID();
        User patient = new User("p@example.com", "hash", Role.PATIENT, "A", "B");
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));

        listener.onReminderDue(new ReminderDueEvent(appointmentId));

        verify(emailSender).send(eq("p@example.com"), anyString(), anyString());
    }

    @Test
    void onAppointmentCancelled_notifiesBothThePatientAndTheDoctor() {
        UUID patientId = UUID.randomUUID();
        UUID doctorProfileId = UUID.randomUUID();
        UUID doctorUserId = UUID.randomUUID();
        Appointment appointment = appointment(patientId, doctorProfileId);
        UUID appointmentId = UUID.randomUUID();
        User patient = new User("p@example.com", "hash", Role.PATIENT, "A", "B");
        DoctorProfile doctorProfile = new DoctorProfile(doctorUserId, "Cardiology", "bio", new BigDecimal("250.00"), "Rabat");
        User doctor = new User("d@example.com", "hash", Role.DOCTOR, "C", "D");
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(doctorProfileRepository.findById(doctorProfileId)).thenReturn(Optional.of(doctorProfile));
        when(userRepository.findById(doctorUserId)).thenReturn(Optional.of(doctor));

        listener.onAppointmentCancelled(new AppointmentCancelledEvent(appointmentId));

        verify(emailSender).send(eq("p@example.com"), anyString(), anyString());
        verify(emailSender).send(eq("d@example.com"), anyString(), anyString());
    }

    @Test
    void onAppointmentCancelled_stillNotifiesThePatientWhenTheDoctorProfileIsMissing() {
        UUID patientId = UUID.randomUUID();
        UUID doctorProfileId = UUID.randomUUID();
        Appointment appointment = appointment(patientId, doctorProfileId);
        UUID appointmentId = UUID.randomUUID();
        User patient = new User("p@example.com", "hash", Role.PATIENT, "A", "B");
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(doctorProfileRepository.findById(doctorProfileId)).thenReturn(Optional.empty());

        listener.onAppointmentCancelled(new AppointmentCancelledEvent(appointmentId));

        verify(emailSender).send(eq("p@example.com"), anyString(), anyString());
        verify(emailSender, never()).send(eq("d@example.com"), any(), any());
    }
}

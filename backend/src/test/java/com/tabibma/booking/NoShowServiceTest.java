package com.tabibma.booking;

import com.tabibma.clinic.DoctorProfile;
import com.tabibma.clinic.DoctorProfileRepository;
import com.tabibma.identity.Role;
import com.tabibma.identity.UserContext;
import com.tabibma.shared.exception.ConflictException;
import com.tabibma.shared.exception.ForbiddenException;
import com.tabibma.shared.exception.NotFoundException;
import com.tabibma.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
class NoShowServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private DoctorProfileRepository doctorProfileRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private NoShowService service;

    private UUID doctorUserId;
    private DoctorProfile doctorProfile;
    private Appointment pastConfirmedAppointment;

    @BeforeEach
    void setUp() {
        service = new NoShowService(appointmentRepository, doctorProfileRepository, eventPublisher);
        doctorUserId = UUID.randomUUID();
        doctorProfile = new DoctorProfile(doctorUserId, "Cardiology", "bio", BigDecimal.TEN, "Rabat");
        Instant start = Instant.now().minusSeconds(3600);
        pastConfirmedAppointment = new Appointment(UUID.randomUUID(), doctorProfile.getId(), UUID.randomUUID(),
                start, start.plusSeconds(1800), LocationType.IN_PERSON);
        pastConfirmedAppointment.confirm();
    }

    private UserContext doctorPrincipal() {
        return new UserContext(doctorUserId, "d@example.com", Role.DOCTOR);
    }

    @Test
    void markNoShow_throwsNotFoundForAnUnknownAppointment() {
        UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markNoShow(doctorPrincipal(), appointmentId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void markNoShow_rejectsADoctorWhoDoesNotOwnTheAppointment() {
        UUID appointmentId = pastConfirmedAppointment.getId();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(pastConfirmedAppointment));
        when(doctorProfileRepository.findById(doctorProfile.getId())).thenReturn(Optional.of(doctorProfile));
        UserContext otherDoctor = new UserContext(UUID.randomUUID(), "other@example.com", Role.DOCTOR);

        assertThatThrownBy(() -> service.markNoShow(otherDoctor, appointmentId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void markNoShow_rejectsAnAppointmentThatHasNotStartedYet() {
        Instant start = Instant.now().plusSeconds(3600);
        Appointment future = new Appointment(UUID.randomUUID(), doctorProfile.getId(), UUID.randomUUID(),
                start, start.plusSeconds(1800), LocationType.IN_PERSON);
        future.confirm();
        UUID appointmentId = future.getId();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(future));
        when(doctorProfileRepository.findById(doctorProfile.getId())).thenReturn(Optional.of(doctorProfile));

        assertThatThrownBy(() -> service.markNoShow(doctorPrincipal(), appointmentId))
                .isInstanceOf(ValidationException.class);
        verify(eventPublisher, org.mockito.Mockito.never()).publishEvent(any());
    }

    @Test
    void markNoShow_rejectsAnAppointmentThatIsNotConfirmed() {
        Instant start = Instant.now().minusSeconds(3600);
        Appointment neverConfirmed = new Appointment(UUID.randomUUID(), doctorProfile.getId(), UUID.randomUUID(),
                start, start.plusSeconds(1800), LocationType.IN_PERSON);
        UUID appointmentId = neverConfirmed.getId();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(neverConfirmed));
        when(doctorProfileRepository.findById(doctorProfile.getId())).thenReturn(Optional.of(doctorProfile));

        assertThatThrownBy(() -> service.markNoShow(doctorPrincipal(), appointmentId))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void markNoShow_marksTheAppointmentAndPublishesTheEvent() {
        UUID appointmentId = pastConfirmedAppointment.getId();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(pastConfirmedAppointment));
        when(doctorProfileRepository.findById(doctorProfile.getId())).thenReturn(Optional.of(doctorProfile));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment result = service.markNoShow(doctorPrincipal(), appointmentId);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.NO_SHOW);
        verify(eventPublisher).publishEvent(new AppointmentNoShowEvent(appointmentId));
    }
}

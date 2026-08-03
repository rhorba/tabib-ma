package com.tabibma.consultation;

import com.tabibma.booking.Appointment;
import com.tabibma.booking.AppointmentRepository;
import com.tabibma.booking.LocationType;
import com.tabibma.clinic.DoctorProfile;
import com.tabibma.clinic.DoctorProfileRepository;
import com.tabibma.identity.Role;
import com.tabibma.identity.UserContext;
import com.tabibma.prescription.Prescription;
import com.tabibma.prescription.PrescriptionService;
import com.tabibma.shared.exception.ForbiddenException;
import com.tabibma.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultationServiceTest {

    @Mock
    private ConsultationRepository consultationRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private DoctorProfileRepository doctorProfileRepository;
    @Mock
    private SignalingTokenIssuer signalingTokenIssuer;
    @Mock
    private TurnCredentialProvider turnCredentialProvider;
    @Mock
    private PrescriptionService prescriptionService;

    private ConsultationService service;

    private UUID patientId;
    private UUID doctorUserId;
    private UUID doctorProfileId;
    private Appointment appointment;
    private DoctorProfile doctorProfile;
    private Consultation consultation;

    @BeforeEach
    void setUp() {
        service = new ConsultationService(consultationRepository, appointmentRepository, doctorProfileRepository,
                signalingTokenIssuer, turnCredentialProvider, prescriptionService, 10);

        patientId = UUID.randomUUID();
        doctorUserId = UUID.randomUUID();
        doctorProfileId = UUID.randomUUID();
        Instant start = Instant.now();
        appointment = new Appointment(patientId, doctorProfileId, UUID.randomUUID(), start, start.plusSeconds(1800),
                LocationType.VIDEO);
        appointment.confirm();
        doctorProfile = new DoctorProfile(doctorUserId, "Cardiology", "bio", new BigDecimal("250.00"), "Rabat");
        consultation = new Consultation(appointment.getId());
    }

    private UserContext patientPrincipal() {
        return new UserContext(patientId, "p@example.com", Role.PATIENT);
    }

    private UserContext doctorPrincipal() {
        return new UserContext(doctorUserId, "d@example.com", Role.DOCTOR);
    }

    @Test
    void join_rejectsSomeoneWhoIsNeitherThePatientNorTheDoctor() {
        UUID consultationId = UUID.randomUUID();
        when(consultationRepository.findById(consultationId)).thenReturn(Optional.of(consultation));
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(doctorProfileRepository.findById(doctorProfileId)).thenReturn(Optional.of(doctorProfile));
        UserContext stranger = new UserContext(UUID.randomUUID(), "x@example.com", Role.PATIENT);

        assertThatThrownBy(() -> service.join(stranger, consultationId)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void join_rejectsOutsideTheJoinWindow() {
        Instant farFuture = Instant.now().plusSeconds(3600);
        Appointment farAppointment = new Appointment(patientId, doctorProfileId, UUID.randomUUID(), farFuture,
                farFuture.plusSeconds(1800), LocationType.VIDEO);
        farAppointment.confirm();
        Consultation farConsultation = new Consultation(farAppointment.getId());
        UUID consultationId = UUID.randomUUID();
        when(consultationRepository.findById(consultationId)).thenReturn(Optional.of(farConsultation));
        when(appointmentRepository.findById(farAppointment.getId())).thenReturn(Optional.of(farAppointment));
        when(doctorProfileRepository.findById(doctorProfileId)).thenReturn(Optional.of(doctorProfile));

        assertThatThrownBy(() -> service.join(patientPrincipal(), consultationId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void join_succeedsForThePatientWithinTheWindowAndTransitionsToInProgress() {
        UUID consultationId = UUID.randomUUID();
        when(consultationRepository.findById(consultationId)).thenReturn(Optional.of(consultation));
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(doctorProfileRepository.findById(doctorProfileId)).thenReturn(Optional.of(doctorProfile));
        when(signalingTokenIssuer.issue(any(), any(), any()))
                .thenReturn(new SignalingToken("tok", Instant.now().plusSeconds(60)));
        when(turnCredentialProvider.iceServersFor(any(), any())).thenReturn(List.of(IceServer.stun("stun:x")));

        ConsultationService.JoinResult result = service.join(patientPrincipal(), consultationId);

        assertThat(result.consultation().getStatus()).isEqualTo(ConsultationStatus.IN_PROGRESS);
        assertThat(result.signalingToken().value()).isEqualTo("tok");
        assertThat(result.iceServers()).hasSize(1);
    }

    @Test
    void join_succeedsForTheDoctorToo() {
        UUID consultationId = UUID.randomUUID();
        when(consultationRepository.findById(consultationId)).thenReturn(Optional.of(consultation));
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(doctorProfileRepository.findById(doctorProfileId)).thenReturn(Optional.of(doctorProfile));
        when(signalingTokenIssuer.issue(any(), any(), any()))
                .thenReturn(new SignalingToken("tok", Instant.now().plusSeconds(60)));
        when(turnCredentialProvider.iceServersFor(any(), any())).thenReturn(List.of());

        assertThat(service.join(doctorPrincipal(), consultationId).consultation().getStatus())
                .isEqualTo(ConsultationStatus.IN_PROGRESS);
    }

    @Test
    void join_isIdempotentWhenTheOtherParticipantAlreadyJoined() {
        consultation.start();
        UUID consultationId = UUID.randomUUID();
        when(consultationRepository.findById(consultationId)).thenReturn(Optional.of(consultation));
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(doctorProfileRepository.findById(doctorProfileId)).thenReturn(Optional.of(doctorProfile));
        when(signalingTokenIssuer.issue(any(), any(), any()))
                .thenReturn(new SignalingToken("tok", Instant.now().plusSeconds(60)));
        when(turnCredentialProvider.iceServersFor(any(), any())).thenReturn(List.of());

        assertThat(service.join(doctorPrincipal(), consultationId).consultation().getStatus())
                .isEqualTo(ConsultationStatus.IN_PROGRESS);
    }

    @Test
    void join_throwsNotFoundForAnUnknownConsultation() {
        UUID consultationId = UUID.randomUUID();
        when(consultationRepository.findById(consultationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.join(patientPrincipal(), consultationId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getByAppointmentId_reflectsJoinabilityFromTheWindowAndStatus() {
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(consultationRepository.findByAppointmentId(appointment.getId())).thenReturn(Optional.of(consultation));
        when(doctorProfileRepository.findById(doctorProfileId)).thenReturn(Optional.of(doctorProfile));

        ConsultationService.ConsultationView view = service.getByAppointmentId(patientPrincipal(), appointment.getId());

        assertThat(view.joinable()).isTrue();
    }

    @Test
    void complete_rejectsThePatient() {
        UUID consultationId = UUID.randomUUID();
        when(consultationRepository.findById(consultationId)).thenReturn(Optional.of(consultation));
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(doctorProfileRepository.findById(doctorProfileId)).thenReturn(Optional.of(doctorProfile));

        assertThatThrownBy(() -> service.complete(patientPrincipal(), consultationId, List.of()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void complete_issuesAPrescriptionAndMarksTheConsultationCompleted() {
        UUID consultationId = UUID.randomUUID();
        when(consultationRepository.findById(consultationId)).thenReturn(Optional.of(consultation));
        when(appointmentRepository.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(doctorProfileRepository.findById(doctorProfileId)).thenReturn(Optional.of(doctorProfile));
        Prescription prescription = mock(Prescription.class);
        when(prescriptionService.issue(eq(consultationId), eq(doctorUserId), eq(patientId), any()))
                .thenReturn(prescription);

        ConsultationService.CompletionResult result = service.complete(doctorPrincipal(), consultationId, List.of());

        assertThat(result.consultation().getStatus()).isEqualTo(ConsultationStatus.COMPLETED);
        assertThat(result.prescription()).isSameAs(prescription);
        verify(consultationRepository).save(consultation);
        // Story 9.1 needs a COMPLETED *appointment* to review — completing the
        // consultation must complete the underlying appointment too, not just
        // the Consultation record.
        assertThat(appointment.getStatus()).isEqualTo(com.tabibma.booking.AppointmentStatus.COMPLETED);
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void complete_throwsNotFoundForAnUnknownConsultation() {
        UUID consultationId = UUID.randomUUID();
        when(consultationRepository.findById(consultationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.complete(doctorPrincipal(), consultationId, List.of()))
                .isInstanceOf(NotFoundException.class);
    }
}

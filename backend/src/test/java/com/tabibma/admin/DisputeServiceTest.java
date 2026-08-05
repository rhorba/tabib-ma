package com.tabibma.admin;

import com.tabibma.booking.Appointment;
import com.tabibma.booking.AppointmentRepository;
import com.tabibma.booking.LocationType;
import com.tabibma.clinic.DoctorProfile;
import com.tabibma.clinic.DoctorProfileRepository;
import com.tabibma.identity.Role;
import com.tabibma.identity.UserContext;
import com.tabibma.identity.UserRepository;
import com.tabibma.shared.audit.AuditLogRepository;
import com.tabibma.shared.exception.ConflictException;
import com.tabibma.shared.exception.ForbiddenException;
import com.tabibma.shared.exception.NotFoundException;
import com.tabibma.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @Mock
    private DisputeRepository disputeRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private DoctorProfileRepository doctorProfileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogRepository auditLogRepository;

    private DisputeService service;

    private UUID patientId;
    private UUID doctorUserId;
    private DoctorProfile doctorProfile;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        service = new DisputeService(disputeRepository, appointmentRepository, doctorProfileRepository,
                userRepository, auditLogRepository);
        patientId = UUID.randomUUID();
        doctorUserId = UUID.randomUUID();
        doctorProfile = new DoctorProfile(doctorUserId, "Cardiology", "bio", java.math.BigDecimal.TEN, "Rabat");
        Instant start = Instant.now().plusSeconds(3600);
        appointment = new Appointment(patientId, doctorProfile.getId(), UUID.randomUUID(), start,
                start.plusSeconds(1800), LocationType.IN_PERSON);
    }

    private UserContext patientPrincipal() {
        return new UserContext(patientId, "p@example.com", Role.PATIENT);
    }

    private UserContext doctorPrincipal() {
        return new UserContext(doctorUserId, "d@example.com", Role.DOCTOR);
    }

    @Test
    void report_rejectsNoShowType() {
        assertThatThrownBy(() -> service.report(patientPrincipal(), UUID.randomUUID(), DisputeType.NO_SHOW, "reason"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void report_throwsNotFoundForAnUnknownAppointment() {
        UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.report(patientPrincipal(), appointmentId, DisputeType.COMPLAINT, "reason"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void report_rejectsSomeoneElsesAppointment() {
        UUID appointmentId = appointment.getId();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(doctorProfileRepository.findById(doctorProfile.getId())).thenReturn(Optional.of(doctorProfile));
        UserContext stranger = new UserContext(UUID.randomUUID(), "x@example.com", Role.PATIENT);

        assertThatThrownBy(() -> service.report(stranger, appointmentId, DisputeType.COMPLAINT, "reason"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void report_savesDisputeForTheOwningPatient() {
        UUID appointmentId = appointment.getId();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));

        Dispute dispute = service.report(patientPrincipal(), appointmentId, DisputeType.PAYMENT_ISSUE, "Charged twice");

        assertThat(dispute.getAppointmentId()).isEqualTo(appointmentId);
        assertThat(dispute.getType()).isEqualTo(DisputeType.PAYMENT_ISSUE);
        assertThat(dispute.getReportedByUserId()).isEqualTo(patientId);
        assertThat(dispute.getStatus()).isEqualTo(DisputeStatus.OPEN);
    }

    @Test
    void report_savesDisputeForTheOwningDoctor() {
        UUID appointmentId = appointment.getId();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(doctorProfileRepository.findById(doctorProfile.getId())).thenReturn(Optional.of(doctorProfile));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));

        Dispute dispute = service.report(doctorPrincipal(), appointmentId, DisputeType.COMPLAINT, "Patient was rude");

        assertThat(dispute.getReportedByUserId()).isEqualTo(doctorUserId);
    }

    @Test
    void createManual_allowsNoShowTypeAndRecordsTheAdminAsReporter() {
        UUID appointmentId = appointment.getId();
        UUID adminId = UUID.randomUUID();
        UserContext admin = new UserContext(adminId, "admin@example.com", Role.PLATFORM_ADMIN);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));

        Dispute dispute = service.createManual(admin, appointmentId, DisputeType.NO_SHOW, "Reported via support call");

        assertThat(dispute.getType()).isEqualTo(DisputeType.NO_SHOW);
        assertThat(dispute.getReportedByUserId()).isEqualTo(adminId);
    }

    @Test
    void createSystem_hasNoReporter() {
        UUID appointmentId = appointment.getId();
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));

        Dispute dispute = service.createSystem(appointmentId, DisputeType.NO_SHOW);

        assertThat(dispute.getReportedByUserId()).isNull();
        assertThat(dispute.getReason()).isNull();
    }

    @Test
    void resolve_throwsNotFoundForAnUnknownDispute() {
        UUID disputeId = UUID.randomUUID();
        when(disputeRepository.findById(disputeId)).thenReturn(Optional.empty());
        UserContext admin = new UserContext(UUID.randomUUID(), "admin@example.com", Role.PLATFORM_ADMIN);

        assertThatThrownBy(() -> service.resolve(admin, disputeId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void resolve_marksResolvedAndLogsAnAuditEntry() {
        Dispute dispute = new Dispute(appointment.getId(), DisputeType.COMPLAINT, "reason", patientId);
        UUID disputeId = UUID.randomUUID();
        when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(dispute));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));
        UUID adminId = UUID.randomUUID();
        UserContext admin = new UserContext(adminId, "admin@example.com", Role.PLATFORM_ADMIN);

        Dispute resolved = service.resolve(admin, disputeId);

        assertThat(resolved.getStatus()).isEqualTo(DisputeStatus.RESOLVED);
        assertThat(resolved.getResolvedByUserId()).isEqualTo(adminId);
        verify(auditLogRepository).save(any());
    }

    @Test
    void resolve_rejectsAnAlreadyResolvedDispute() {
        Dispute dispute = new Dispute(appointment.getId(), DisputeType.COMPLAINT, "reason", patientId);
        dispute.resolve(UUID.randomUUID());
        UUID disputeId = UUID.randomUUID();
        when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(dispute));
        UserContext admin = new UserContext(UUID.randomUUID(), "admin@example.com", Role.PLATFORM_ADMIN);

        assertThatThrownBy(() -> service.resolve(admin, disputeId))
                .isInstanceOf(ConflictException.class);
    }
}

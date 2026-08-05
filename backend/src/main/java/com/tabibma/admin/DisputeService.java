package com.tabibma.admin;

import com.tabibma.admin.dto.DisputeResponse;
import com.tabibma.booking.Appointment;
import com.tabibma.booking.AppointmentRepository;
import com.tabibma.clinic.DoctorProfile;
import com.tabibma.clinic.DoctorProfileRepository;
import com.tabibma.identity.User;
import com.tabibma.identity.UserContext;
import com.tabibma.identity.UserRepository;
import com.tabibma.shared.audit.AuditLog;
import com.tabibma.shared.audit.AuditLogRepository;
import com.tabibma.shared.exception.ForbiddenException;
import com.tabibma.shared.exception.NotFoundException;
import com.tabibma.shared.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Story 10.1: the dispute queue. Lives in {@code admin} (not {@code booking}) since it's the
 * module doing cross-module aggregation (appointment + patient + doctor context) — same
 * dependency-direction reasoning as {@code ClinicDashboardService}/{@code ResourceUtilizationService}
 * in {@code booking}: the aggregator depends on the modules it reads, never the reverse.
 */
@Service
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public DisputeService(DisputeRepository disputeRepository, AppointmentRepository appointmentRepository,
                           DoctorProfileRepository doctorProfileRepository, UserRepository userRepository,
                           AuditLogRepository auditLogRepository) {
        this.disputeRepository = disputeRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /** A patient or doctor reporting a problem on their own appointment. NO_SHOW is deliberately
     * excluded here — that's only ever system-generated, via the dedicated no-show action
     * (Batch 2), to keep "doctor marked a no-show" and "someone complained" distinct signals. */
    @Transactional
    public Dispute report(UserContext principal, UUID appointmentId, DisputeType type, String reason) {
        if (type == DisputeType.NO_SHOW) {
            throw new ValidationException("NO_SHOW disputes are recorded automatically, not self-reported.");
        }
        Appointment appointment = getAppointmentOrThrow(appointmentId);
        if (!ownsAppointment(principal, appointment)) {
            throw new ForbiddenException("You can only report a problem on your own appointment.");
        }
        return disputeRepository.save(new Dispute(appointmentId, type, reason, principal.userId()));
    }

    /** A platform admin logging a dispute directly (e.g. an issue reported out-of-band through
     * support). Any type is allowed, including NO_SHOW. */
    @Transactional
    public Dispute createManual(UserContext principal, UUID appointmentId, DisputeType type, String reason) {
        getAppointmentOrThrow(appointmentId);
        return disputeRepository.save(new Dispute(appointmentId, type, reason, principal.userId()));
    }

    /** System-generated (no {@code reportedByUserId}) — called by {@code DisputeEventListener}
     * (Batch 2) in reaction to a no-show being marked or a payment failing. */
    @Transactional
    public Dispute createSystem(UUID appointmentId, DisputeType type) {
        return disputeRepository.save(new Dispute(appointmentId, type, null, null));
    }

    @Transactional(readOnly = true)
    public List<DisputeResponse> listOpen() {
        return enrich(disputeRepository.findAllByStatusOrderByCreatedAtAsc(DisputeStatus.OPEN));
    }

    @Transactional
    public Dispute resolve(UserContext principal, UUID disputeId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new NotFoundException("Dispute not found."));
        dispute.resolve(principal.userId());
        Dispute saved = disputeRepository.save(dispute);
        auditLogRepository.save(new AuditLog(principal.userId(), "DISPUTE_RESOLVED", "dispute", disputeId));
        return saved;
    }

    private boolean ownsAppointment(UserContext principal, Appointment appointment) {
        if (appointment.getPatientId().equals(principal.userId())) {
            return true;
        }
        return doctorProfileRepository.findById(appointment.getDoctorProfileId())
                .map(DoctorProfile::getUserId)
                .map(doctorUserId -> doctorUserId.equals(principal.userId()))
                .orElse(false);
    }

    private Appointment getAppointmentOrThrow(UUID appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found."));
    }

    private List<DisputeResponse> enrich(List<Dispute> disputes) {
        if (disputes.isEmpty()) {
            return List.of();
        }
        Map<UUID, Appointment> appointmentsById = appointmentRepository
                .findAllById(disputes.stream().map(Dispute::getAppointmentId).toList()).stream()
                .collect(Collectors.toMap(Appointment::getId, Function.identity()));

        Map<UUID, DoctorProfile> doctorProfilesById = doctorProfileRepository
                .findAllById(appointmentsById.values().stream().map(Appointment::getDoctorProfileId).toList())
                .stream()
                .collect(Collectors.toMap(DoctorProfile::getId, Function.identity()));

        List<UUID> userIds = appointmentsById.values().stream()
                .flatMap(a -> Stream.of(a.getPatientId(),
                        Optional.ofNullable(doctorProfilesById.get(a.getDoctorProfileId()))
                                .map(DoctorProfile::getUserId)
                                .orElse(null)))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return disputes.stream()
                .map(dispute -> {
                    Appointment appointment = appointmentsById.get(dispute.getAppointmentId());
                    String patientName = displayName(usersById.get(appointment.getPatientId()));
                    UUID doctorUserId = Optional.ofNullable(doctorProfilesById.get(appointment.getDoctorProfileId()))
                            .map(DoctorProfile::getUserId)
                            .orElse(null);
                    String doctorName = doctorUserId == null ? null : displayName(usersById.get(doctorUserId));
                    return DisputeResponse.from(dispute, appointment, patientName, doctorName);
                })
                .toList();
    }

    private String displayName(User user) {
        if (user == null) {
            return null;
        }
        return user.getFirstName() + " " + user.getLastName();
    }
}

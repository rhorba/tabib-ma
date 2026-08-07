package com.tabibma.consultation;

import com.tabibma.booking.Appointment;
import com.tabibma.booking.AppointmentRepository;
import com.tabibma.booking.AppointmentStatus;
import com.tabibma.clinic.DoctorProfile;
import com.tabibma.clinic.DoctorProfileRepository;
import com.tabibma.identity.UserContext;
import com.tabibma.prescription.Prescription;
import com.tabibma.prescription.PrescriptionItem;
import com.tabibma.prescription.PrescriptionService;
import com.tabibma.shared.exception.ForbiddenException;
import com.tabibma.shared.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Story 6.1's join flow: authorize the caller as a participant, enforce the join window
 * (JoinWindowPolicy), then hand back what the browser needs to open a WebRTC peer connection —
 * a SignalingToken scoped to this consultation (for the /ws/consultations relay) and ICE servers
 * (TurnCredentialProvider).
 */
@Service
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final SignalingTokenIssuer signalingTokenIssuer;
    private final TurnCredentialProvider turnCredentialProvider;
    private final PrescriptionService prescriptionService;
    private final JoinWindowPolicy joinWindowPolicy;
    private final int windowMarginMinutes;

    public ConsultationService(ConsultationRepository consultationRepository,
                                AppointmentRepository appointmentRepository,
                                DoctorProfileRepository doctorProfileRepository,
                                SignalingTokenIssuer signalingTokenIssuer,
                                TurnCredentialProvider turnCredentialProvider,
                                PrescriptionService prescriptionService,
                                @Value("${app.consultation.join-window-minutes:10}") int windowMarginMinutes) {
        this.consultationRepository = consultationRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.signalingTokenIssuer = signalingTokenIssuer;
        this.turnCredentialProvider = turnCredentialProvider;
        this.prescriptionService = prescriptionService;
        this.joinWindowPolicy = new JoinWindowPolicy(windowMarginMinutes);
        this.windowMarginMinutes = windowMarginMinutes;
    }

    public ConsultationView getByAppointmentId(UserContext principal, UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found."));
        Consultation consultation = consultationRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new NotFoundException("No video consultation exists for this appointment."));
        authorizeParticipant(principal, appointment);
        boolean joinable = appointment.getStatus() == AppointmentStatus.CONFIRMED
                && joinWindowPolicy.isJoinable(Instant.now(), appointment.getStartsAt(), appointment.getEndsAt());
        return new ConsultationView(consultation, appointment, joinable);
    }

    @Transactional
    public JoinResult join(UserContext principal, UUID consultationId) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new NotFoundException("Consultation not found."));
        Appointment appointment = appointmentRepository.findById(consultation.getAppointmentId())
                .orElseThrow(() -> new NotFoundException("Appointment not found."));
        authorizeParticipant(principal, appointment);

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new ForbiddenException("This appointment is no longer confirmed.");
        }
        Instant now = Instant.now();
        if (!joinWindowPolicy.isJoinable(now, appointment.getStartsAt(), appointment.getEndsAt())) {
            throw new ForbiddenException("The video room is not open yet; it opens "
                    + windowMarginMinutes + " minutes before the appointment.");
        }

        consultation.start();
        consultationRepository.save(consultation);

        Instant tokenExpiresAt = appointment.getEndsAt().plus(Duration.ofMinutes(windowMarginMinutes));
        SignalingToken signalingToken = signalingTokenIssuer.issue(consultation.getId(), principal.userId(), tokenExpiresAt);
        List<IceServer> iceServers = turnCredentialProvider.iceServersFor(consultation.getId(), principal.userId());

        return new JoinResult(consultation, signalingToken, iceServers);
    }

    /**
     * Story 6.3 (amended 2026-08-07, .logs/decisions.md): only the doctor can complete a consult.
     * A prescription is issued in the same flow/transaction when items are provided, matching the
     * AC's "in one session" requirement — but items is optional, since not every consult warrants
     * one (docs/ux-tabib-ma.md Flow 3's "No -> Mark consultation complete without prescription"
     * branch, left unbuilt at Epic 6+7's close because the AC as originally written left no room
     * for it).
     */
    @Transactional
    public CompletionResult complete(UserContext principal, UUID consultationId, List<PrescriptionItem> items) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new NotFoundException("Consultation not found."));
        Appointment appointment = appointmentRepository.findById(consultation.getAppointmentId())
                .orElseThrow(() -> new NotFoundException("Appointment not found."));
        DoctorProfile doctorProfile = doctorProfileRepository.findById(appointment.getDoctorProfileId())
                .orElseThrow(() -> new NotFoundException("Doctor profile not found."));
        if (!doctorProfile.getUserId().equals(principal.userId())) {
            throw new ForbiddenException("Only the doctor can complete this consultation.");
        }

        Prescription prescription = (items == null || items.isEmpty()) ? null
                : prescriptionService.issue(consultationId, principal.userId(), appointment.getPatientId(), items);
        consultation.complete();
        consultationRepository.save(consultation);
        // Completing the *consultation* didn't used to complete the underlying
        // *appointment* at all — AppointmentStatus.COMPLETED was reachable
        // nowhere in the codebase, silently blocking Story 9.1 (a review needs
        // a COMPLETED appointment to exist).
        appointment.complete();
        appointmentRepository.save(appointment);

        return new CompletionResult(consultation, prescription);
    }

    private void authorizeParticipant(UserContext principal, Appointment appointment) {
        DoctorProfile doctorProfile = doctorProfileRepository.findById(appointment.getDoctorProfileId())
                .orElseThrow(() -> new NotFoundException("Doctor profile not found."));
        boolean isPatient = appointment.getPatientId().equals(principal.userId());
        boolean isDoctor = doctorProfile.getUserId().equals(principal.userId());
        if (!isPatient && !isDoctor) {
            throw new ForbiddenException("You are not a participant in this consultation.");
        }
    }

    public record ConsultationView(Consultation consultation, Appointment appointment, boolean joinable) {
    }

    public record JoinResult(Consultation consultation, SignalingToken signalingToken, List<IceServer> iceServers) {
    }

    public record CompletionResult(Consultation consultation, Prescription prescription) {
    }
}

package com.tabibma.clinic;

import com.tabibma.clinic.dto.CreateClinicRequest;
import com.tabibma.identity.Role;
import com.tabibma.identity.UserContext;
import com.tabibma.shared.exception.ConflictException;
import com.tabibma.shared.exception.ForbiddenException;
import com.tabibma.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Story 2.3: clinic admin self-service (create clinic, invite doctors). */
@Service
public class ClinicOnboardingService {

    private final ClinicRepository clinicRepository;
    private final ClinicInvitationRepository clinicInvitationRepository;

    public ClinicOnboardingService(ClinicRepository clinicRepository,
                                    ClinicInvitationRepository clinicInvitationRepository) {
        this.clinicRepository = clinicRepository;
        this.clinicInvitationRepository = clinicInvitationRepository;
    }

    @Transactional
    public Clinic createClinic(UserContext principal, CreateClinicRequest request) {
        if (principal.role() != Role.CLINIC_ADMIN) {
            throw new ForbiddenException("Only clinic admins can create a clinic.");
        }
        if (clinicRepository.existsByAdminUserId(principal.userId())) {
            throw new ConflictException("A clinic already exists for this account.");
        }
        Clinic clinic = new Clinic(principal.userId(), request.name(), request.city(), request.address());
        return clinicRepository.save(clinic);
    }

    public Clinic getMyClinic(UserContext principal) {
        return clinicRepository.findByAdminUserId(principal.userId())
                .orElseThrow(() -> new NotFoundException("You don't have a clinic yet."));
    }

    @Transactional
    public ClinicInvitation inviteDoctor(UserContext principal, UUID clinicId, String email) {
        getOwnedClinicOrThrow(principal, clinicId);
        if (clinicInvitationRepository.existsByClinicIdAndInvitedEmailAndStatus(
                clinicId, email, InvitationStatus.PENDING)) {
            throw new ConflictException("This email already has a pending invitation for this clinic.");
        }
        ClinicInvitation invitation = new ClinicInvitation(clinicId, email);
        return clinicInvitationRepository.save(invitation);
    }

    public List<ClinicInvitation> listInvitations(UserContext principal, UUID clinicId) {
        getOwnedClinicOrThrow(principal, clinicId);
        return clinicInvitationRepository.findAllByClinicId(clinicId);
    }

    private Clinic getOwnedClinicOrThrow(UserContext principal, UUID clinicId) {
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new NotFoundException("Clinic not found."));
        if (!clinic.getAdminUserId().equals(principal.userId())) {
            throw new ForbiddenException("You can only manage your own clinic.");
        }
        return clinic;
    }
}

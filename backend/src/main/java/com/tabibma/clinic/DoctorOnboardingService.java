package com.tabibma.clinic;

import com.tabibma.clinic.dto.CreateDoctorProfileRequest;
import com.tabibma.identity.Role;
import com.tabibma.identity.UserContext;
import com.tabibma.shared.exception.ConflictException;
import com.tabibma.shared.exception.ForbiddenException;
import com.tabibma.shared.exception.NotFoundException;
import com.tabibma.shared.exception.ValidationException;
import com.tabibma.shared.storage.ObjectStorageClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DoctorOnboardingService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("application/pdf", "image/png", "image/jpeg");
    private static final long MAX_DOCUMENT_BYTES = 10L * 1024 * 1024;

    private final DoctorProfileRepository doctorProfileRepository;
    private final VerificationDocumentRepository verificationDocumentRepository;
    private final ObjectStorageClient objectStorageClient;
    private final ClinicInvitationRepository clinicInvitationRepository;
    private final ClinicStaffMembershipRepository clinicStaffMembershipRepository;

    public DoctorOnboardingService(DoctorProfileRepository doctorProfileRepository,
                                    VerificationDocumentRepository verificationDocumentRepository,
                                    ObjectStorageClient objectStorageClient,
                                    ClinicInvitationRepository clinicInvitationRepository,
                                    ClinicStaffMembershipRepository clinicStaffMembershipRepository) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.verificationDocumentRepository = verificationDocumentRepository;
        this.objectStorageClient = objectStorageClient;
        this.clinicInvitationRepository = clinicInvitationRepository;
        this.clinicStaffMembershipRepository = clinicStaffMembershipRepository;
    }

    @Transactional
    public DoctorProfile createProfile(UserContext principal, CreateDoctorProfileRequest request) {
        if (principal.role() != Role.DOCTOR) {
            throw new ForbiddenException("Only doctors can create a doctor profile.");
        }
        if (doctorProfileRepository.existsByUserId(principal.userId())) {
            throw new ConflictException("A doctor profile already exists for this account.");
        }
        DoctorProfile profile = new DoctorProfile(principal.userId(), request.specialty(), request.bio(),
                request.consultationFeeMad(), request.city());
        return doctorProfileRepository.save(profile);
    }

    @Transactional
    public VerificationDocument uploadDocument(UserContext principal, UUID doctorProfileId,
                                                DocumentType documentType, MultipartFile file) {
        DoctorProfile profile = doctorProfileRepository.findById(doctorProfileId)
                .orElseThrow(() -> new NotFoundException("Doctor profile not found."));
        if (!profile.getUserId().equals(principal.userId())) {
            throw new ForbiddenException("You can only upload documents to your own doctor profile.");
        }
        if (file.isEmpty()) {
            throw new ValidationException("The uploaded file is empty.");
        }
        if (file.getSize() > MAX_DOCUMENT_BYTES) {
            throw new ValidationException("The uploaded file exceeds the 10MB limit.");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new ValidationException("Only PDF, PNG, and JPEG documents are accepted.");
        }

        String storageKey;
        try {
            storageKey = objectStorageClient.store("verification-documents/" + doctorProfileId,
                    file.getOriginalFilename(), file.getInputStream(), file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file.", e);
        }

        VerificationDocument document = new VerificationDocument(doctorProfileId, documentType.name(), storageKey);
        return verificationDocumentRepository.save(document);
    }

    public DoctorProfile getMyProfile(UserContext principal) {
        return doctorProfileRepository.findByUserId(principal.userId())
                .orElseThrow(() -> new NotFoundException("You don't have a doctor profile yet."));
    }

    public List<VerificationDocument> listMyDocuments(UserContext principal, UUID doctorProfileId) {
        DoctorProfile profile = doctorProfileRepository.findById(doctorProfileId)
                .orElseThrow(() -> new NotFoundException("Doctor profile not found."));
        if (!profile.getUserId().equals(principal.userId())) {
            throw new ForbiddenException("You can only view documents on your own doctor profile.");
        }
        return verificationDocumentRepository.findAllByDoctorProfileId(doctorProfileId);
    }

    public List<ClinicInvitation> listMyPendingInvitations(UserContext principal) {
        return clinicInvitationRepository.findAllByInvitedEmailAndStatus(principal.email(), InvitationStatus.PENDING);
    }

    @Transactional
    public ClinicInvitation acceptInvitation(UserContext principal, UUID invitationId) {
        ClinicInvitation invitation = getOwnPendingInvitationOrThrow(principal, invitationId);
        DoctorProfile profile = doctorProfileRepository.findByUserId(principal.userId())
                .orElseThrow(() -> new ConflictException(
                        "You need to create your doctor profile before joining a clinic."));
        if (!clinicStaffMembershipRepository.existsByClinicIdAndDoctorProfileId(invitation.getClinicId(), profile.getId())) {
            clinicStaffMembershipRepository.save(new ClinicStaffMembership(invitation.getClinicId(), profile.getId()));
        }
        invitation.accept();
        return clinicInvitationRepository.save(invitation);
    }

    @Transactional
    public ClinicInvitation declineInvitation(UserContext principal, UUID invitationId) {
        ClinicInvitation invitation = getOwnPendingInvitationOrThrow(principal, invitationId);
        invitation.decline();
        return clinicInvitationRepository.save(invitation);
    }

    private ClinicInvitation getOwnPendingInvitationOrThrow(UserContext principal, UUID invitationId) {
        ClinicInvitation invitation = clinicInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new NotFoundException("Invitation not found."));
        if (!invitation.getInvitedEmail().equals(principal.email())) {
            throw new ForbiddenException("This invitation was not sent to you.");
        }
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ConflictException("This invitation has already been decided.");
        }
        return invitation;
    }
}

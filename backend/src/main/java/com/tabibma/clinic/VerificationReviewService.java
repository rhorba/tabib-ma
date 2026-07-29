package com.tabibma.clinic;

import com.tabibma.identity.UserContext;
import com.tabibma.shared.audit.AuditLog;
import com.tabibma.shared.audit.AuditLogRepository;
import com.tabibma.shared.exception.ConflictException;
import com.tabibma.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Story 2.2: platform admin review of pending doctor verification submissions. */
@Service
public class VerificationReviewService {

    private final DoctorProfileRepository doctorProfileRepository;
    private final VerificationDocumentRepository verificationDocumentRepository;
    private final AuditLogRepository auditLogRepository;

    public VerificationReviewService(DoctorProfileRepository doctorProfileRepository,
                                      VerificationDocumentRepository verificationDocumentRepository,
                                      AuditLogRepository auditLogRepository) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.verificationDocumentRepository = verificationDocumentRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<DoctorProfile> listPendingProfiles() {
        return doctorProfileRepository.findAllByVerificationStatus(VerificationStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<VerificationDocument> listDocuments(UUID doctorProfileId) {
        getProfileOrThrow(doctorProfileId);
        return verificationDocumentRepository.findAllByDoctorProfileId(doctorProfileId);
    }

    @Transactional
    public DoctorProfile approve(UserContext principal, UUID doctorProfileId) {
        DoctorProfile profile = getPendingProfileOrThrow(doctorProfileId);
        profile.approve();
        markDocumentsReviewed(doctorProfileId, principal.userId());
        auditLogRepository.save(new AuditLog(principal.userId(), "DOCTOR_PROFILE_APPROVED", "doctor_profile", doctorProfileId));
        return profile;
    }

    @Transactional
    public DoctorProfile reject(UserContext principal, UUID doctorProfileId) {
        DoctorProfile profile = getPendingProfileOrThrow(doctorProfileId);
        profile.reject();
        markDocumentsReviewed(doctorProfileId, principal.userId());
        auditLogRepository.save(new AuditLog(principal.userId(), "DOCTOR_PROFILE_REJECTED", "doctor_profile", doctorProfileId));
        return profile;
    }

    private DoctorProfile getProfileOrThrow(UUID doctorProfileId) {
        return doctorProfileRepository.findById(doctorProfileId)
                .orElseThrow(() -> new NotFoundException("Doctor profile not found."));
    }

    private DoctorProfile getPendingProfileOrThrow(UUID doctorProfileId) {
        DoctorProfile profile = getProfileOrThrow(doctorProfileId);
        if (profile.getVerificationStatus() != VerificationStatus.PENDING) {
            throw new ConflictException("This doctor profile has already been reviewed.");
        }
        return profile;
    }

    private void markDocumentsReviewed(UUID doctorProfileId, UUID reviewerId) {
        List<VerificationDocument> documents = verificationDocumentRepository.findAllByDoctorProfileId(doctorProfileId);
        documents.forEach(document -> document.markReviewed(reviewerId));
        verificationDocumentRepository.saveAll(documents);
    }
}

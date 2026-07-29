package com.tabibma.clinic;

import com.tabibma.identity.Role;
import com.tabibma.identity.UserContext;
import com.tabibma.shared.audit.AuditLog;
import com.tabibma.shared.audit.AuditLogRepository;
import com.tabibma.shared.exception.ConflictException;
import com.tabibma.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationReviewServiceTest {

    @Mock
    private DoctorProfileRepository doctorProfileRepository;
    @Mock
    private VerificationDocumentRepository verificationDocumentRepository;
    @Mock
    private AuditLogRepository auditLogRepository;

    private VerificationReviewService service;

    private final UserContext platformAdmin = new UserContext(UUID.randomUUID(), "admin@example.com", Role.PLATFORM_ADMIN);

    @BeforeEach
    void setUp() {
        service = new VerificationReviewService(doctorProfileRepository, verificationDocumentRepository, auditLogRepository);
    }

    @Test
    void listPendingProfiles_returnsOnlyPending() {
        DoctorProfile pending = new DoctorProfile(UUID.randomUUID(), "Cardiology", "bio", BigDecimal.TEN, "Rabat");
        when(doctorProfileRepository.findAllByVerificationStatus(VerificationStatus.PENDING)).thenReturn(List.of(pending));

        assertThat(service.listPendingProfiles()).containsExactly(pending);
    }

    @Test
    void approve_rejectsUnknownProfile() {
        UUID profileId = UUID.randomUUID();
        when(doctorProfileRepository.findById(profileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(platformAdmin, profileId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void approve_rejectsAlreadyReviewedProfile() {
        DoctorProfile profile = new DoctorProfile(UUID.randomUUID(), "Cardiology", "bio", BigDecimal.TEN, "Rabat");
        profile.approve();
        UUID profileId = UUID.randomUUID();
        when(doctorProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.approve(platformAdmin, profileId))
                .isInstanceOf(ConflictException.class);

        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void approve_marksProfileApprovedAndDocumentsReviewedAndAudits() {
        DoctorProfile profile = new DoctorProfile(UUID.randomUUID(), "Cardiology", "bio", BigDecimal.TEN, "Rabat");
        UUID profileId = UUID.randomUUID();
        VerificationDocument document = new VerificationDocument(profileId, "MEDICAL_LICENSE", "key");
        when(doctorProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(verificationDocumentRepository.findAllByDoctorProfileId(profileId)).thenReturn(List.of(document));

        DoctorProfile result = service.approve(platformAdmin, profileId);

        assertThat(result.getVerificationStatus()).isEqualTo(VerificationStatus.APPROVED);
        assertThat(document.getReviewedBy()).isEqualTo(platformAdmin.userId());
        assertThat(document.getReviewedAt()).isNotNull();

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("DOCTOR_PROFILE_APPROVED");
        assertThat(auditCaptor.getValue().getTargetId()).isEqualTo(profileId);
        assertThat(auditCaptor.getValue().getActorUserId()).isEqualTo(platformAdmin.userId());
    }

    @Test
    void reject_marksProfileRejectedAndAudits() {
        DoctorProfile profile = new DoctorProfile(UUID.randomUUID(), "Cardiology", "bio", BigDecimal.TEN, "Rabat");
        UUID profileId = UUID.randomUUID();
        when(doctorProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(verificationDocumentRepository.findAllByDoctorProfileId(profileId)).thenReturn(List.of());

        DoctorProfile result = service.reject(platformAdmin, profileId);

        assertThat(result.getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("DOCTOR_PROFILE_REJECTED");
    }
}

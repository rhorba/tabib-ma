package com.tabibma.clinic;

import com.tabibma.clinic.dto.CreateDoctorProfileRequest;
import com.tabibma.identity.Role;
import com.tabibma.identity.UserContext;
import com.tabibma.shared.exception.ConflictException;
import com.tabibma.shared.exception.ForbiddenException;
import com.tabibma.shared.exception.NotFoundException;
import com.tabibma.shared.exception.ValidationException;
import com.tabibma.shared.storage.ObjectStorageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorOnboardingServiceTest {

    @Mock
    private DoctorProfileRepository doctorProfileRepository;
    @Mock
    private VerificationDocumentRepository verificationDocumentRepository;
    @Mock
    private ObjectStorageClient objectStorageClient;
    @Mock
    private ClinicInvitationRepository clinicInvitationRepository;
    @Mock
    private ClinicStaffMembershipRepository clinicStaffMembershipRepository;

    private DoctorOnboardingService service;

    @BeforeEach
    void setUp() {
        service = new DoctorOnboardingService(doctorProfileRepository, verificationDocumentRepository,
                objectStorageClient, clinicInvitationRepository, clinicStaffMembershipRepository);
    }

    @Test
    void createProfile_rejectsNonDoctorRole() {
        UserContext patient = new UserContext(UUID.randomUUID(), "p@example.com", Role.PATIENT);
        CreateDoctorProfileRequest request = new CreateDoctorProfileRequest("Cardiology", "bio", BigDecimal.TEN, "Rabat");

        assertThatThrownBy(() -> service.createProfile(patient, request))
                .isInstanceOf(ForbiddenException.class);

        verify(doctorProfileRepository, never()).save(any());
    }

    @Test
    void createProfile_rejectsWhenProfileAlreadyExists() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        CreateDoctorProfileRequest request = new CreateDoctorProfileRequest("Cardiology", "bio", BigDecimal.TEN, "Rabat");
        when(doctorProfileRepository.existsByUserId(doctor.userId())).thenReturn(true);

        assertThatThrownBy(() -> service.createProfile(doctor, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createProfile_savesPendingProfileForDoctor() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        CreateDoctorProfileRequest request = new CreateDoctorProfileRequest("Cardiology", "bio", BigDecimal.TEN, "Rabat");
        when(doctorProfileRepository.existsByUserId(doctor.userId())).thenReturn(false);
        when(doctorProfileRepository.save(any(DoctorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        DoctorProfile saved = service.createProfile(doctor, request);

        assertThat(saved.getUserId()).isEqualTo(doctor.userId());
        assertThat(saved.getVerificationStatus()).isEqualTo(VerificationStatus.PENDING);
    }

    @Test
    void uploadDocument_rejectsWhenProfileNotFound() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        UUID profileId = UUID.randomUUID();
        when(doctorProfileRepository.findById(profileId)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "license.pdf", "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> service.uploadDocument(doctor, profileId, DocumentType.MEDICAL_LICENSE, file))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void uploadDocument_rejectsWhenUploaderDoesNotOwnProfile() {
        UUID ownerId = UUID.randomUUID();
        UserContext otherDoctor = new UserContext(UUID.randomUUID(), "other@example.com", Role.DOCTOR);
        DoctorProfile profile = new DoctorProfile(ownerId, "Cardiology", "bio", BigDecimal.TEN, "Rabat");
        when(doctorProfileRepository.findById(any())).thenReturn(Optional.of(profile));
        MockMultipartFile file = new MockMultipartFile("file", "license.pdf", "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> service.uploadDocument(otherDoctor, UUID.randomUUID(), DocumentType.MEDICAL_LICENSE, file))
                .isInstanceOf(ForbiddenException.class);

        verify(objectStorageClient, never()).store(anyString(), anyString(), any(), anyLong(), anyString());
    }

    @Test
    void uploadDocument_rejectsDisallowedContentType() {
        UUID ownerId = UUID.randomUUID();
        UserContext owner = new UserContext(ownerId, "d@example.com", Role.DOCTOR);
        DoctorProfile profile = new DoctorProfile(ownerId, "Cardiology", "bio", BigDecimal.TEN, "Rabat");
        when(doctorProfileRepository.findById(any())).thenReturn(Optional.of(profile));
        MockMultipartFile file = new MockMultipartFile("file", "malware.exe", "application/x-msdownload", "content".getBytes());

        assertThatThrownBy(() -> service.uploadDocument(owner, UUID.randomUUID(), DocumentType.MEDICAL_LICENSE, file))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void uploadDocument_storesAndSavesDocumentForOwner() {
        UUID ownerId = UUID.randomUUID();
        UserContext owner = new UserContext(ownerId, "d@example.com", Role.DOCTOR);
        DoctorProfile profile = new DoctorProfile(ownerId, "Cardiology", "bio", BigDecimal.TEN, "Rabat");
        UUID profileId = UUID.randomUUID();
        when(doctorProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(objectStorageClient.store(anyString(), anyString(), any(), anyLong(), anyString())).thenReturn("verification-documents/key");
        when(verificationDocumentRepository.save(any(VerificationDocument.class))).thenAnswer(inv -> inv.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("file", "license.pdf", "application/pdf", "content".getBytes());

        VerificationDocument saved = service.uploadDocument(owner, profileId, DocumentType.MEDICAL_LICENSE, file);

        assertThat(saved.getObjectStorageKey()).isEqualTo("verification-documents/key");
        assertThat(saved.getDocumentType()).isEqualTo("MEDICAL_LICENSE");
    }

    @Test
    void getMyProfile_rejectsWhenNoneExists() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        when(doctorProfileRepository.findByUserId(doctor.userId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyProfile(doctor))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getMyProfile_returnsOwnProfile() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        DoctorProfile profile = new DoctorProfile(doctor.userId(), "Cardiology", "bio", BigDecimal.TEN, "Rabat");
        when(doctorProfileRepository.findByUserId(doctor.userId())).thenReturn(Optional.of(profile));

        assertThat(service.getMyProfile(doctor)).isSameAs(profile);
    }

    @Test
    void listMyDocuments_rejectsWhenCallerDoesNotOwnProfile() {
        UUID ownerId = UUID.randomUUID();
        UserContext otherDoctor = new UserContext(UUID.randomUUID(), "other@example.com", Role.DOCTOR);
        DoctorProfile profile = new DoctorProfile(ownerId, "Cardiology", "bio", BigDecimal.TEN, "Rabat");
        UUID profileId = UUID.randomUUID();
        when(doctorProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.listMyDocuments(otherDoctor, profileId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void listMyDocuments_returnsDocumentsForOwner() {
        UUID ownerId = UUID.randomUUID();
        UserContext owner = new UserContext(ownerId, "d@example.com", Role.DOCTOR);
        DoctorProfile profile = new DoctorProfile(ownerId, "Cardiology", "bio", BigDecimal.TEN, "Rabat");
        UUID profileId = UUID.randomUUID();
        VerificationDocument document = new VerificationDocument(profileId, "MEDICAL_LICENSE", "key");
        when(doctorProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(verificationDocumentRepository.findAllByDoctorProfileId(profileId)).thenReturn(List.of(document));

        assertThat(service.listMyDocuments(owner, profileId)).containsExactly(document);
    }

    @Test
    void listMyPendingInvitations_returnsPendingForCallerEmail() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        ClinicInvitation invitation = new ClinicInvitation(UUID.randomUUID(), "d@example.com");
        when(clinicInvitationRepository.findAllByInvitedEmailAndStatus("d@example.com", InvitationStatus.PENDING))
                .thenReturn(List.of(invitation));

        assertThat(service.listMyPendingInvitations(doctor)).containsExactly(invitation);
    }

    @Test
    void acceptInvitation_rejectsWhenNotFound() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        UUID invitationId = UUID.randomUUID();
        when(clinicInvitationRepository.findById(invitationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.acceptInvitation(doctor, invitationId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void acceptInvitation_rejectsWhenNotAddressedToCaller() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        ClinicInvitation invitation = new ClinicInvitation(UUID.randomUUID(), "someone-else@example.com");
        UUID invitationId = UUID.randomUUID();
        when(clinicInvitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> service.acceptInvitation(doctor, invitationId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void acceptInvitation_rejectsWhenAlreadyDecided() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        ClinicInvitation invitation = new ClinicInvitation(UUID.randomUUID(), "d@example.com");
        invitation.decline();
        UUID invitationId = UUID.randomUUID();
        when(clinicInvitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> service.acceptInvitation(doctor, invitationId))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void acceptInvitation_rejectsWhenCallerHasNoDoctorProfile() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        ClinicInvitation invitation = new ClinicInvitation(UUID.randomUUID(), "d@example.com");
        UUID invitationId = UUID.randomUUID();
        when(clinicInvitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
        when(doctorProfileRepository.findByUserId(doctor.userId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.acceptInvitation(doctor, invitationId))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void acceptInvitation_createsMembershipAndMarksAccepted() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        UUID clinicId = UUID.randomUUID();
        ClinicInvitation invitation = new ClinicInvitation(clinicId, "d@example.com");
        UUID invitationId = UUID.randomUUID();
        DoctorProfile profile = new DoctorProfile(doctor.userId(), "Cardiology", "bio", BigDecimal.TEN, "Rabat");
        when(clinicInvitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
        when(doctorProfileRepository.findByUserId(doctor.userId())).thenReturn(Optional.of(profile));
        when(clinicStaffMembershipRepository.existsByClinicIdAndDoctorProfileId(any(), any())).thenReturn(false);
        when(clinicInvitationRepository.save(any(ClinicInvitation.class))).thenAnswer(inv -> inv.getArgument(0));

        ClinicInvitation result = service.acceptInvitation(doctor, invitationId);

        assertThat(result.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        verify(clinicStaffMembershipRepository).save(any(ClinicStaffMembership.class));
    }

    @Test
    void acceptInvitation_skipsDuplicateMembershipButStillAccepts() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        UUID clinicId = UUID.randomUUID();
        ClinicInvitation invitation = new ClinicInvitation(clinicId, "d@example.com");
        UUID invitationId = UUID.randomUUID();
        DoctorProfile profile = new DoctorProfile(doctor.userId(), "Cardiology", "bio", BigDecimal.TEN, "Rabat");
        when(clinicInvitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
        when(doctorProfileRepository.findByUserId(doctor.userId())).thenReturn(Optional.of(profile));
        when(clinicStaffMembershipRepository.existsByClinicIdAndDoctorProfileId(any(), any())).thenReturn(true);
        when(clinicInvitationRepository.save(any(ClinicInvitation.class))).thenAnswer(inv -> inv.getArgument(0));

        ClinicInvitation result = service.acceptInvitation(doctor, invitationId);

        assertThat(result.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        verify(clinicStaffMembershipRepository, never()).save(any());
    }

    @Test
    void declineInvitation_marksDeclined() {
        UserContext doctor = new UserContext(UUID.randomUUID(), "d@example.com", Role.DOCTOR);
        ClinicInvitation invitation = new ClinicInvitation(UUID.randomUUID(), "d@example.com");
        UUID invitationId = UUID.randomUUID();
        when(clinicInvitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
        when(clinicInvitationRepository.save(any(ClinicInvitation.class))).thenAnswer(inv -> inv.getArgument(0));

        ClinicInvitation result = service.declineInvitation(doctor, invitationId);

        assertThat(result.getStatus()).isEqualTo(InvitationStatus.DECLINED);
    }
}
